package com.orchid.net.server.main.impl;

import javax.inject.Inject;
import com.orchid.net.server.accepter.ConnectionAccepter;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.net.server.workers.input.InputWorker;
import org.slf4j.Logger;

import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 7:46
 */
public class ReactorNetworkServer implements NetworkServer {
    ConnectionAccepter accepter;
    List<InputWorker> inputWorkers;

    @Inject
    Logger logger;
    
    @Inject
    public ReactorNetworkServer(ConnectionAccepter accepter, List<InputWorker> inputWorkers) {
        this.accepter = accepter;
        this.inputWorkers = inputWorkers;
    }

    @Override
    public void start() {
        logger.info("Starting Orchid Tracker with "+inputWorkers.size()+" workers");
        for (InputWorker inputWorker : inputWorkers){
            inputWorker.start();
        }
        accepter.start();
    }
}
