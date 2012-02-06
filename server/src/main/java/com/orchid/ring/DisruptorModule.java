package com.orchid.ring;

import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.ring.anotations.InputRing;
import com.orchid.streams.MessageHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:40
 */
public class DisruptorModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(DisruptorCore.class).in(Singleton.class);
        bind(MessageHandler.class).to(DisruptorPublishingMethodHandler.class);
    }

    @Provides
    @Singleton
    @Inject
    public RingBuffer<RingElement> provideRingBuffer(DisruptorCore disruptorCore){
        return disruptorCore.ringBuffer;
    }

    @Provides
    @Singleton
    @Inject
    @InputRing
    public Disruptor<RingElement> provideDisruptor(DisruptorCore disruptorCore){
        return disruptorCore.disruptor;
    }
}
