package com.orchid.net.server;

import com.google.inject.AbstractModule;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.orchid.net.server.accepter.AccepterModule;
import com.orchid.net.server.accepter.ConnectionAccepter;
import com.orchid.net.server.accepter.impl.SelectorAccepter;
import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.main.MainServerModule;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.net.server.main.impl.ReactorNetworkServer;
import com.orchid.net.server.workers.*;
import com.orchid.streams.BufferPoolModule;

import javax.inject.Singleton;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 7:30
 */
public class NetworkServerModule extends AbstractModule{
    @Override
    protected void configure() {
        install(new BufferPoolModule());
        install(new MainServerModule());
        install(new AccepterModule());
        install(new WorkersModule());
    }
}
