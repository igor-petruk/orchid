package com.orchid.logic.ring;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.logic.annotations.BusinessLogic;

import javax.inject.Inject;
import java.util.concurrent.*;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:47
 */
public class DisruptorCore {
    Disruptor<RingElement> disruptor;
    RingBuffer<RingElement> ringBuffer;
    
    @Inject
    public DisruptorCore(@BusinessLogic EventHandler<RingElement> businessLogic) {
        ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
            int threadID;
            
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                String name="BussinessLogicThread-"+(threadID++);
                System.out.println("Starting "+name);
                thread.setName(name);
                return thread;
            }
        });
        disruptor = new Disruptor<RingElement>(RingElement.EVENT_FACTORY, executor,
                            new MultiThreadedClaimStrategy(1024),
                            new BlockingWaitStrategy());
        disruptor.handleEventsWith(businessLogic);
        ringBuffer = disruptor.start();
    }

    public Disruptor<RingElement> getDisruptor() {
        return disruptor;
    }

    public RingBuffer<RingElement> getRingBuffer() {
        return ringBuffer;
    }
}
