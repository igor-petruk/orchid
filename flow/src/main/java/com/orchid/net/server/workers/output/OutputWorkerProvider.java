package com.orchid.net.server.workers.output;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:06
 */
public class OutputWorkerProvider implements Provider<List<OutputWorker>>{
    List<OutputWorker> outputWorkers;

    @Inject
    public OutputWorkerProvider(Provider<OutputWorker> workerProvider, @OutputWorkersCount int workersCount) {
        outputWorkers = new ArrayList<OutputWorker>(workersCount);
        for (int i = 0; i < workersCount; i++){
            OutputWorker newOutputWorker = workerProvider.get();
            newOutputWorker.setWorkerName("OutputWorker-"+(i+1));
            outputWorkers.add(newOutputWorker);
        }
    }

    @Override
    public List<OutputWorker> get() {
        return outputWorkers;
    }
}
