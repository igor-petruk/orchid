package com.orchid;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.logic.ring.RingElement;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * User: Igor Petruk
 * Date: 30.01.12
 * Time: 14:15
 */
public class RingTest implements EventTranslator<RingElement>{
    @Test
    public void test() throws InterruptedException {
        Executor executor = Executors.newCachedThreadPool();
        Disruptor <RingElement> disruptor = new Disruptor<RingElement>(RingElement.EVENT_FACTORY, executor,
                new SingleThreadedClaimStrategy(8),
                new BlockingWaitStrategy());

        RingBuffer<RingElement> ringBuffer = disruptor.getRingBuffer();
        SequenceBarrier barrier = ringBuffer.newBarrier();
        final MyProcessor customProcessor = new MyProcessor(ringBuffer, barrier);
        disruptor.after(customProcessor).handleEventsWith(new MyEventHandler());
        disruptor.start();

        System.out.println("Size: " + ringBuffer.getBufferSize());
        for (int i = 0; i < 200; i++){
            Thread.sleep(1000);
            disruptor.publishEvent(this);
            System.out.println("Published "+i);
        }

    }

    @Override
    public RingElement translateTo(RingElement event, long sequence) {
        if (event.number==0){
            event.number=sequence;
        }
        System.out.println("Number "+event.number);
        return event;
    }

    static class MyProcessor implements EventProcessor{
        RingBuffer<RingElement> ringBuffer;
        Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
        SequenceBarrier sequenceBarrier;

        MyProcessor(RingBuffer<RingElement> ringBuffer, SequenceBarrier sequenceBarrier) {
            this.ringBuffer = ringBuffer;
            this.sequenceBarrier = sequenceBarrier;
        }

        @Override
        public Sequence getSequence() {
            System.out.println("getSeq");
            return sequence;
        }

        @Override
        public void halt() {
            sequenceBarrier.alert();
            System.out.println("Halted");
        }

        @Override
        public void run() {
            System.out.println("Running");
            int i = 0;
            sequenceBarrier.clearAlert();
            long nextSequence = sequence.get() + 1L;
            //sequence.set(1);
            while(true){
                try {
                    System.out.println("S "+sequenceBarrier.getCursor());
                    final long availableSequence = sequenceBarrier.waitFor(nextSequence);
                    while (nextSequence<=availableSequence){
                        System.out.println("E "+sequenceBarrier.getCursor());
                        System.out.println("Waited for "+nextSequence);
                        Thread.sleep(1000000);
                        nextSequence++;
                    }
                    sequence.set(nextSequence-1);
                } catch (AlertException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    static class MyEventHandler implements EventHandler<RingElement>{

        @Override
        public void onEvent(RingElement event, long sequence, boolean endOfBatch) throws Exception {

        }
    }
}


