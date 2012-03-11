package com.orchid.flow;

import com.google.inject.AbstractModule;
import com.lmax.disruptor.EventHandler;
import com.orchid.logging.LoggingModule;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.ring.InputRingModule;
import com.orchid.ring.RingElement;

/**
 * User: Igor Petruk
 * Date: 18.02.12
 * Time: 16:20
 */
public class FlowModule extends AbstractModule{
    int port;

    public FlowModule(int port) {
        this.port = port;
    }

    @Override
    protected void configure() {
        install(new LoggingModule());
        install(new NetworkServerModule(port));
        install(new InputRingModule());
    }
}
