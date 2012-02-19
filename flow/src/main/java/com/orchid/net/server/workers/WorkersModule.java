package com.orchid.net.server.workers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.orchid.net.server.workers.input.*;
import com.orchid.net.server.workers.output.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:16
 */
public class WorkersModule extends AbstractModule{
    @Override
    protected void configure() {
        install(new InputWorkersModule());
        install(new OutputWorkersModule());
    }
}
