package com.orchid.main;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.orchid.logging.LoggingModule;
import com.orchid.logic.LogicModule;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.logic.ring.DisruptorModule;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 8:37
 */

public class Runner {
    public static void main(String[] argv) {
        Injector injector = Guice.createInjector(
                new LoggingModule(),
                new NetworkServerModule(),
                new DisruptorModule(),
                new LogicModule()
        );
        NetworkServer server = injector.getInstance(NetworkServer.class);
        server.start();
    }
}
