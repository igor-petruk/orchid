package com.orchid.net.server.accepter;

import com.google.inject.AbstractModule;
import com.orchid.net.server.accepter.impl.SelectorAccepter;

import javax.inject.Singleton;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:23
 */
public class AccepterModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(ConnectionAccepter.class)
                        .to(SelectorAccepter.class).in(Singleton.class);
    }
}
