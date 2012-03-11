package com.orchid.flow;

import com.google.inject.*;
import com.lmax.disruptor.EventHandler;
import com.orchid.logging.LoggingModule;
import com.orchid.logic.annotations.BusinessLogic;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.net.server.workers.output.OutputPublisher;
import com.orchid.ring.RingElement;
import com.orchid.serialization.FlowMessageSerializer;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 11.03.12
 * Time: 19:22
 */
public class Flow {
    FlowMessageSerializer<?> messageSerializer;
    int port;
    EventHandler<RingElement> eventHandler;

    Injector injector;
    OutputPublisher publisher;

    public Flow(final FlowMessageSerializer<?> messageSerializer,
                final int port,
                final EventHandler<RingElement> eventHandler) {
        this.messageSerializer = messageSerializer;
        this.port = port;
        this.eventHandler = eventHandler;

        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new FlowModule(port));
                install(new ParametrizedLogicModule(eventHandler));

                bind(FlowMessageSerializer.class).toInstance(messageSerializer);
            }
        });

        publisher = injector.getInstance(Key.get(OutputPublisher.class));
    }

    public OutputPublisher getPublisher() {
        return publisher;
    }

    public void start(){
        NetworkServer server = injector.getInstance(NetworkServer.class);
        server.start();
    }
}
