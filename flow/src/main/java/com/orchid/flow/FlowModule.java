package com.orchid.flow;

import com.google.inject.AbstractModule;
import com.orchid.logging.LoggingModule;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.ring.InputRingModule;

/**
 * User: Igor Petruk
 * Date: 18.02.12
 * Time: 16:20
 */
public class FlowModule extends AbstractModule{
    @Override
    protected void configure() {
        install(new LoggingModule());
        install(new NetworkServerModule());
        install(new InputRingModule());
    }
}
