package com.orchid.ring;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.orchid.ring.anotations.InputRing;
import com.orchid.net.streams.MessageHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 16:40
 */
public class InputRingModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(InputRingConfiguration.class).in(Singleton.class);
        bind(MessageHandler.class).to(InputRingPublisher.class);
    }

    @Provides
    @Singleton
    @Inject
    public RingBuffer<RingElement> provideRingBuffer(InputRingConfiguration inputRingConfiguration){
        return inputRingConfiguration.getRingBuffer();
    }

    @Provides
    @Singleton
    @Inject
    @InputRing
    public Disruptor<RingElement> provideDisruptor(InputRingConfiguration inputRingConfiguration){
        return inputRingConfiguration.getDisruptor();
    }
}
