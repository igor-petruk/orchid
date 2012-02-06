package com.orchid.logic;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.lmax.disruptor.EventHandler;
import com.orchid.ring.RingElement;

import java.util.HashSet;
import java.util.Set;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:40
 */
public class BusinessLogicEventHandler implements EventHandler<RingElement>{
    long value = 0;
    long tick = 100;
    Multiset<String> numbers = HashMultiset.create();
    
    public BusinessLogicEventHandler() {
        tick = System.currentTimeMillis();
    }

    @Override
    public void onEvent(RingElement event, long sequence, boolean endOfBatch) throws Exception {
        //System.out.println(event.message);
        numbers.add(event.message.getIntroduce().getName());
        value++;

        if (value%1000000==1000000-1){
            long newTick = System.currentTimeMillis();
            System.out.println(1000*1000000/(double)(newTick-tick));
            tick = newTick;
        }
    }

    public Multiset<String> getNumbers() {
        return numbers;
    }
}
