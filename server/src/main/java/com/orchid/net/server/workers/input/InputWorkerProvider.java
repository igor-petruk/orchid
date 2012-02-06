package com.orchid.net.server.workers.input;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:06
 */
public class InputWorkerProvider implements Provider<List<InputWorker>>{
    List<InputWorker> inputWorkers;

    @Inject
    public InputWorkerProvider(Provider<InputWorker> workerProvider, @InputWorkersCount int workersCount) {
        inputWorkers = new ArrayList<InputWorker>(workersCount);
        for (int i = 0; i < workersCount; i++){
            InputWorker newInputWorker = workerProvider.get();
            newInputWorker.setWorkerName("InputWorker-"+(i+1));
            inputWorkers.add(newInputWorker);
        }
    }

    @Override
    public List<InputWorker> get() {
        return inputWorkers;
    }
}
