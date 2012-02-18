package com.orchid.main;

import com.google.inject.AbstractModule;
import com.orchid.logging.LoggingModule;
import com.orchid.logic.LogicModule;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.ring.DisruptorModule;

/**
 * User: Igor Petruk
 * Date: 18.02.12
 * Time: 16:20
 */
public class ApplicationModule extends AbstractModule{
    @Override
    protected void configure() {
        install(new LoggingModule());
        install(new NetworkServerModule());
        install(new DisruptorModule());
        install(new LogicModule());
    }
}
