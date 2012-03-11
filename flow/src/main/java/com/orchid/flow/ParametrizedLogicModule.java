package com.orchid.flow;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.lmax.disruptor.EventHandler;
import com.orchid.logic.annotations.BusinessLogic;
import com.orchid.ring.RingElement;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 11.03.12
 * Time: 19:36
 */
public class ParametrizedLogicModule extends AbstractModule{
    EventHandler<RingElement> eventHandler;

    public ParametrizedLogicModule(EventHandler<RingElement> eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    protected void configure() {
        bind(new TypeLiteral<EventHandler<RingElement> >(){}).
            annotatedWith(BusinessLogic.class).
            toInstance(eventHandler);
    }
}
