package com.orchid.net.server.workers;

import javax.inject.Inject;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 21:24
 */
public class WorkerSelector {
    List<? extends Worker> workerList;

    public WorkerSelector(List<? extends Worker> workerList) {
        this.workerList = workerList;
    }

    public Worker get(){
        Worker currentWorker = workerList.get(0);
        int minConnectionsCount = currentWorker.getConnectionsCount();
        for (int i = 1; i < workerList.size(); i++){
            if (minConnectionsCount>workerList.get(i).getConnectionsCount()){
                currentWorker = workerList.get(i);
                minConnectionsCount = currentWorker.getConnectionsCount();
            }
        }
        return currentWorker;
    }
}
