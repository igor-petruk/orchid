package com.orchid.main;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.orchid.net.server.main.NetworkServer;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 8:37
 */

public class Runner {
    public static void main(String[] argv) {
        Injector injector = Guice.createInjector(
                new RunnerModule()
        );
        NetworkServer server = injector.getInstance(NetworkServer.class);
        server.start();
    }
}
