package com.orchid.flow;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.lmax.disruptor.EventHandler;
import com.orchid.logic.annotations.BusinessLogic;
import com.orchid.ring.RingElement;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 11.03.12
 * Time: 19:36
 */
public class ParametrizedLogicModule extends AbstractModule{
    EventHandler<RingElement> eventHandler[];

    public ParametrizedLogicModule(EventHandler<RingElement>... eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Provides
    @Singleton
    @BusinessLogic
    List<EventHandler<RingElement>> handlers(){
        return Arrays.asList(eventHandler);
    }

    @Override
    protected void configure() {
    }
}
