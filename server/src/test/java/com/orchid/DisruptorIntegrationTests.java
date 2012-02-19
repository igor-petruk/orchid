package com.orchid;

import com.google.guiceberry.GuiceBerryModule;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.logic.LogicModule;
import com.orchid.ring.InputRingModule;
import com.orchid.ring.RingElement;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:06
 */
public class DisruptorIntegrationTests implements EventTranslator<RingElement>{
    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule(Env.class);

    @Inject
    Disruptor<RingElement> disruptor;

    int iValue = 0;

    @Override
    public RingElement translateTo(RingElement event, long sequence) {
        //event.i = iValue;
        return event;
    }

    @Test
    public void testRing(){
        long tick = System.currentTimeMillis();
        final long COUNT =  1000000000;
        for (int i = 0; i < COUNT; i++){
            long seq = 0;
                iValue = i;
                //System.out.println("Publishing "+i);
                disruptor.publishEvent(this);

        }
        tick = System.currentTimeMillis() - tick;
        System.out.println((long)((double)COUNT/(tick)*1000));
    }

    public static final class Extension extends AbstractModule {
        @Override
        protected void configure() {
        }
    }

    public static final class Env extends GuiceBerryModule {
        @Override
        protected void configure() {
            super.configure();
            install(Modules.override(new InputRingModule()).with(new Extension()));
            install(new LogicModule());
        }
    }

}
