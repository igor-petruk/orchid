package com.orchid.net.server.main;

import com.google.inject.AbstractModule;
import com.orchid.net.server.annotations.ServerHost;
import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.main.impl.ReactorNetworkServer;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:52
 */
public class MainServerModule extends AbstractModule{
    String host;
    int port;

    public MainServerModule(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(ServerHost.class).toInstance(host);
        bind(Integer.class).annotatedWith(ServerPort.class).toInstance(port);
        bind(NetworkServer.class).to(ReactorNetworkServer.class).in(Singleton.class);
    }
}
