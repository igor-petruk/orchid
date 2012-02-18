package com.orchid.net.server.workers.output;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.messages.generated.Messages;
import com.orchid.net.server.connections.Connection;
import com.orchid.net.server.workers.Worker;
import com.orchid.logic.ring.RingElement;
import com.orchid.net.streams.BufferPool;
import com.orchid.net.streams.ExpandingBuffer;
import com.orchid.net.streams.SinkOutcome;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 21:38
 */
public class OutputWorker extends Worker implements EventProcessor {
    @Inject
    BufferPool bufferPool;

    Disruptor<RingElement> disruptor;
    RingBuffer<RingElement> ringBuffer;
    Selector selector;
    Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    SequenceBarrier sequenceBarrier;
    long nextSequence;
    int activeConnections = 0;

    @Inject
    private Logger logger;

    public void start(){
        ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
            int threadID;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                String name = "OutputWorker ring thread -" + this;
                System.out.println("Starting " + name);
                thread.setName(name);
                return thread;
            }
        });

        disruptor = new Disruptor<RingElement>(RingElement.EVENT_FACTORY, executor,
                new SingleThreadedClaimStrategy(256),
                new BlockingWaitStrategy());

        ringBuffer = disruptor.getRingBuffer();
        sequenceBarrier = ringBuffer.newBarrier();
        disruptor.after(this).handleEventsWith(new DummyEventHandler());
        disruptor.start();
    }

    public OutputWorker() {
    }

    public Disruptor<RingElement> getDisruptor() {
        return disruptor;
    }

    @Override
    public void handleConnection(Connection connection) {
        try{
            ExpandingBuffer expandingBuffer = new ExpandingBuffer(bufferPool);
            connection.setExpandingBuffer(expandingBuffer);

            synchronized (this){
                selector.wakeup();
                SelectionKey writingKey = connection.getSocketChannel().register(selector,
                        0,
                        connection);
                connection.setOutputKey(writingKey);
            }
            logger.debug("Handling connection {} by {}", connection.getSocketChannel(), this);
        }catch(IOException e){
            logger.error("Error handling connection {}",e);
        }

        connectionsCount.incrementAndGet();
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void halt() {
    }

    @Override
    public void run() {
        sequenceBarrier.clearAlert();
        nextSequence = sequence.get() + 1L;

        while (true){
            try {
                receiveMessages();
                if (activeConnections!=0){
                    int selectedKeys = selector.select();
                    if (selectedKeys==0){
                        System.out.println("Ineffective select");
                    }
                }
                synchronized (this){}
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while(keyIterator.hasNext()){
                    SelectionKey selectionKey = keyIterator.next();
                    Connection connection = (Connection)selectionKey.attachment();
                    if (!handleWrite(connection)){
                        selectionKey.cancel();
                    }
                    keyIterator.remove();
                }
            }
            catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (AlertException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void activateConnection(Connection connection){
        if (!connection.isCurrentlySending()){
            synchronized (this){
                activeConnections ++;
                connection.getOutputKey().interestOps(SelectionKey.OP_WRITE);
                connection.setCurrentlySending(true);
            }
        }
    }

    public void disableConnection(Connection connection){
        if (connection.isCurrentlySending()){
            synchronized (this){
                activeConnections --;
                connection.getOutputKey().interestOps(0);
                connection.setCurrentlySending(false);
            }
        }
    }

    public void disposeConnection(Connection connection){
        if (connection.isCurrentlySending()){
            synchronized (this){
                activeConnections --;
                connection.setCurrentlySending(false);
                connection.dispose();
            }
        }
    }


    private boolean handleWrite(Connection connection){
        ExpandingBuffer expandingBuffer = connection.getExpandingBuffer();
        if (!expandingBuffer.isMessageActive()){
            if (connection.getOutputQueue().isEmpty()){
                logger.error("Unexpected write. It is a bug.");
            }else{
                Messages.MessageContainer container = connection.getOutputQueue().poll();
                expandingBuffer.spill(container);
            }
        }
        SinkOutcome outcome = expandingBuffer.sink(connection.getSocketChannel());
        if (outcome.equals(SinkOutcome.DONE) && (connection.getOutputQueue().isEmpty())){
            disableConnection(connection);
        }
        return !outcome.equals(SinkOutcome.ERROR);
    }

    private void receiveMessages() throws AlertException, InterruptedException {
        long availableSequence;
        if (activeConnections>0){
            availableSequence = sequenceBarrier.getCursor();
        }else{
            availableSequence = sequenceBarrier.waitFor(nextSequence);
            long delta = nextSequence - availableSequence;
        }
        while (nextSequence<=availableSequence){
            RingElement ringElement = ringBuffer.get(nextSequence);
            try{
                handleMessage(ringElement);
            }catch (ClosedChannelException e){
                e.printStackTrace();
            }
            nextSequence++;
        }
        sequence.set(nextSequence-1);
    }

    private void handleMessage(RingElement ringElement) throws ClosedChannelException{
        Connection connection = ringElement.userID.getConnection();
        if (connection.getOutputKey().isValid()){
            activateConnection(connection);
            if (!connection.getOutputQueue().offer(ringElement.message)){
                logger.error("Unable to offer message {}", ringElement.message);
            }else{
                //   logger.info("Successfully offered to {}", connection);
            }
        }else{
            disposeConnection(connection);
        }
    }

    static class DummyEventHandler implements EventHandler<RingElement>{
        @Override
        public void onEvent(RingElement event, long sequence, boolean endOfBatch) throws Exception {

        }
    }
}
