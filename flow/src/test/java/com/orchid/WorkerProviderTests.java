package com.orchid;

import com.google.guiceberry.GuiceBerryModule;
import com.google.guiceberry.junit4.GuiceBerryRule;
import com.google.inject.AbstractModule;
import com.google.inject.util.Modules;
import com.orchid.net.server.NetworkServerModule;
import com.orchid.net.server.workers.input.InputWorker;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * User: Igor Petruk
 * Date: 24.12.11
 * Time: 21:42
 */
public class WorkerProviderTests {
    @Rule
    public final GuiceBerryRule guiceBerry = new GuiceBerryRule(Env.class);

    @Inject
    private Provider<InputWorker> workers;

    @Test
    public void testHello() throws Exception {
        for (int i = 0; i < 10; i++){
            System.out.println("Got "+workers.get());
        }
    }

    public static final class Extension extends AbstractModule {
        @Override
        protected void configure() {
        }
    }

    public static final class Env extends GuiceBerryModule {
        @Override
        protected void configure() {
            super.configure();
            install(Modules.override(new NetworkServerModule("localhost",9800)).with(new Extension()));
        }
    }
}
