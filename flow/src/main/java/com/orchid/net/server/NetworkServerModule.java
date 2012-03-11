package com.orchid.net.server;

import com.google.inject.AbstractModule;
import com.orchid.net.server.accepter.AccepterModule;
import com.orchid.net.server.main.MainServerModule;
import com.orchid.net.server.workers.*;
import com.orchid.net.streams.BufferPoolModule;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 7:30
 */
public class NetworkServerModule extends AbstractModule{
    int port;

    public NetworkServerModule(int port) {
        this.port = port;
    }

    @Override
    protected void configure() {
        install(new BufferPoolModule());
        install(new MainServerModule(port));
        install(new AccepterModule());
        install(new WorkersModule());
    }
}
