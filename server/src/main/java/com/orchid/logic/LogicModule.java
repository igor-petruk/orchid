package com.orchid.logic;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.lmax.disruptor.EventHandler;
import com.orchid.logic.annotations.BusinessLogic;
import com.orchid.ring.RingElement;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:39
 */
public class LogicModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<EventHandler<RingElement>>() {}).
                annotatedWith(BusinessLogic.class).
                to(BusinessLogicEventHandler.class).
                in(Singleton.class);
    }
}
