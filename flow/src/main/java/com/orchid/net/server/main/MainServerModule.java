package com.orchid.net.server.main;

import com.google.inject.AbstractModule;
import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.main.impl.ReactorNetworkServer;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:52
 */
public class MainServerModule extends AbstractModule{
    int port;

    public MainServerModule(int port) {
        this.port = port;
    }

    @Override
    protected void configure() {
        bind(Integer.class).annotatedWith(ServerPort.class).toInstance(port);
        bind(NetworkServer.class).to(ReactorNetworkServer.class).in(Singleton.class);
    }
}
