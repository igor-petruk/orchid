package com.orchid.net.server.workers.input;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.orchid.net.server.workers.WorkerSelector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 22:48
 */
public class InputWorkersModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(Integer.class).annotatedWith(InputWorkersCount.class).toInstance(10);
        bind(new TypeLiteral<List<InputWorker>>(){})
                .toProvider(InputWorkerProvider.class).in(Singleton.class);
    }

    @Provides
    @InputWorkerSelector
    @Inject
    public WorkerSelector provideInputWorkerSelector(List<InputWorker> inputWorkers){
        return new WorkerSelector(inputWorkers);
    }
}
