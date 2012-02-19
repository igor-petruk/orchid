package com.orchid.net.server.workers.output;

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
 * Time: 22:50
 */
public class OutputWorkersModule extends AbstractModule{
    @Override
    protected void configure() {
        bind(Integer.class).annotatedWith(OutputWorkersCount.class).toInstance(4);
        bind(new TypeLiteral<List<OutputWorker>>(){})
                .toProvider(OutputWorkerProvider.class).in(Singleton.class);
        bind(OutputPublisher.class).in(Singleton.class);
    }

    @Provides
    @OutputWorkerSelector
    @Inject
    public WorkerSelector provideOutputWorkerSelector(List<OutputWorker> outputWorkers){
        return new WorkerSelector(outputWorkers);
    }
}
