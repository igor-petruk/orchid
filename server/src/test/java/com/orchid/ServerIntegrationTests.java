package com.orchid;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.guiceberry.GuiceBerryModule;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.lmax.disruptor.EventHandler;
import com.orchid.logging.LoggingModule;
import com.orchid.logic.LogicModule;
import com.orchid.logic.annotations.BusinessLogic;
import com.orchid.messages.generated.Messages;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.net.server.workers.input.InputWorkersCount;
import com.orchid.net.server.workers.output.OutputPublisher;
import com.orchid.net.server.workers.output.OutputWorkersCount;
import com.orchid.ring.DisruptorModule;
import com.orchid.ring.RingElement;
import com.orchid.streams.BufferSize;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;

/**
 * User: Igor Petruk
 * Date: 24.12.11
 * Time: 21:42
 */
public class ServerIntegrationTests {
    final static int SENDERS = 50;
    final static int MESSAGES = 100000;
    final static int BLOCK_SIZE = 1024;
    final static int WORKERS_COUNT = 4;

    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule(Env.class);

    @Inject
    private NetworkServer server;

    @Inject
    @ServerPort
    private int port;

    @Inject
    @BusinessLogic
    EventHandler<RingElement> handler;

    AtomicLong atomicLong = new AtomicLong();

    @Inject
    Logger logger;
    
    class SendingThread implements Callable {
        Socket socket;

        SendingThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public String call() {
            try{
                Messages.MessageContainer.Builder builder = Messages.MessageContainer.newBuilder();
                builder.setMessageType(Messages.MessageType.ECHO);
                OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());

                for (int i = 0; i < MESSAGES/SENDERS; i++){
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                    int num = (int)atomicLong.incrementAndGet();
                    builder.setEcho(Messages.Echo.newBuilder().
                        //    setName(String.valueOf(System.currentTimeMillis())+num).
                            setIntegerValue(num)
                    );
                    Messages.MessageContainer container = builder.build();

                    dataOutputStream.writeInt(container.getSerializedSize());
                    container.writeTo(dataOutputStream);
                    dataOutputStream.flush();

                    byte[] buf = byteArrayOutputStream.toByteArray();
                    dataOutputStream.close();
                    outputStream.write(buf);
                }
                outputStream.flush();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
            return "ok";
        }
    };
    
    class ReceivingThread implements Callable<Void>{
        Socket socket;

        ReceivingThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public Void call() throws Exception {
            InputStream inputStream = socket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            byte[] buf = new byte[1024*128];
            int messagesCount = MESSAGES/SENDERS;
            while(messagesCount>0){
                int data = dataInputStream.readInt();
                long leftSkipping = data;
                while (leftSkipping!=0){
                long skipped = dataInputStream.skip(data);
                    if (skipped==-1){
                        throw new IllegalStateException("Something is wrong");
                    }
                    leftSkipping -= skipped;
                }
                messagesCount--;
            }
            return null;
        }
    }

    @Test
    public void testLargeMessageFlowInput() throws Exception {
        MDC.put("subsystem", "test");
        server.start();
        ExecutorService service = Executors.newCachedThreadPool();
        ArrayList<Future<String>> s = new ArrayList<Future<String>>();
        ArrayList<Future<Void>> receivers = new ArrayList<Future<Void>>();
        ArrayList<Socket> sockets = new ArrayList<Socket>();
        
        for (int i = 0; i < SENDERS; i++){
            Socket socket = new Socket("localhost", port);
            sockets.add(socket);
        }
        Thread.sleep(1000);
        long tick = System.currentTimeMillis();
        for (int i = 0; i < SENDERS; i++){
            s.add(service.submit(new SendingThread(sockets.get(i))));
            receivers.add(service.submit(new ReceivingThread(sockets.get(i))));
        }
        for (int i = 0; i < s.size(); i++){
            s.get(i).get();
        }
        for (int i = 0; i < s.size(); i++){
            receivers.get(i).get();
        }
        System.out.println("All done");
        //MultisetValidatingHandler multisetValidatingHandler = (MultisetValidatingHandler)handler;
        //multisetValidatingHandler.validate();
        tick = System.currentTimeMillis()-tick;
        logger.info("Test completed. Speed=" + (int) ((double) MESSAGES / tick * 1000) + " m/sec");
    }

    public static final class NetworkServerExtension extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(BufferSize.class).toInstance(BLOCK_SIZE);
            bind(Integer.class).annotatedWith(InputWorkersCount.class).toInstance(WORKERS_COUNT);
            bind(Integer.class).annotatedWith(OutputWorkersCount.class).toInstance(WORKERS_COUNT);
        }
   }

    public static final class LogicExtension extends AbstractModule {
        @Override
        protected void configure() {
            bind(Integer.class).annotatedWith(Names.named("messagesCount")).toInstance(MESSAGES);
            bind(new TypeLiteral<EventHandler<RingElement>>() {}).
                    annotatedWith(BusinessLogic.class).
                    to(MultisetValidatingHandler.class).
                    in(Singleton.class);
        }
    }

    public static final class Env extends GuiceBerryModule {
        @Override
        protected void configure() {
            super.configure();
            install(new LoggingModule());
            install(Modules.override(new NetworkServerModule()).with(new NetworkServerExtension()));
            install(new DisruptorModule());
            install(Modules.override(new LogicModule()).with(new LogicExtension()));
        }
    }

    static class MultisetValidatingHandler implements EventHandler<RingElement>{
        Multiset<String> items = HashMultiset.create();

        @Inject
        Logger logger;

        @Inject
        @Named("messagesCount")
        int messagesCount;

        @Inject
        OutputPublisher publisher;

        final Lock lock = new ReentrantLock();
        Condition done = lock.newCondition();

        @Override
        public void onEvent(RingElement event, long sequence, boolean endOfBatch) throws Exception {
            publisher.send(event, event.userID);

            assertNotNull(event.message);
            assertNotNull(event.userID);
            items.add(event.message.getIntroduce().getName());
            if (items.size()==messagesCount){
                try{
                    lock.lock();
                    done.signal();
                }finally{
                    lock.unlock();
                }
            }
        }

        public Multiset<String> getItems() {
            return items;
        }

        public void validate() throws InterruptedException {
            lock.lock();
            done.await();
            lock.unlock();
            logger.info("Received unique {} messages", items.size());
            assertEquals(messagesCount, items.size());
            for (Multiset.Entry<String> entry: items.entrySet()){
                assertEquals(1,entry.getCount());
            }
        }
    }

}

