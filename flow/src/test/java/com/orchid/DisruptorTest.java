package com.orchid;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: Igor Petruk
 * Date: 03.01.12
 * Time: 18:32
 */

public class DisruptorTest{

    static class Value{
        int i;
    }

    static class DisruptorEventTranslator implements EventTranslator<Value>{
        int val;

        @Override
        public Value translateTo(Value event, long sequence) {
            event.i = val;
            return event;
        }
    }

    EventHandler<Value> businessLogic = new EventHandler<Value>() {
        @Override
        public void onEvent(Value event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println(event.i);
        }
    };

    public final static EventFactory<Value> EVENT_FACTORY = new EventFactory<Value>()
    {
        public Value newInstance()
        {
            return new Value();
        }
    };

    DisruptorEventTranslator translator = new DisruptorEventTranslator();

    @Test
    public void testWrapping(){
        ExecutorService executor = Executors.newCachedThreadPool();
        Disruptor disruptor = new Disruptor<Value>(EVENT_FACTORY, executor,
                new MultiThreadedClaimStrategy(256*256),
                new SleepingWaitStrategy());
        disruptor.handleEventsWith(businessLogic);
        RingBuffer<Value> ringBuffer = disruptor.start();
        for (int i = 0; i < 1000000; i++){
            translator.val = i;
            System.out.println("Publishing "+i);
            disruptor.publishEvent(translator);
        }
    }
}
