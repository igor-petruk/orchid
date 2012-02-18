package com.orchid.net.server.main.impl;

import javax.inject.Inject;
import com.orchid.net.server.accepter.ConnectionAccepter;
import com.orchid.net.server.main.NetworkServer;
import com.orchid.net.server.workers.input.InputWorker;
import com.orchid.net.server.workers.output.OutputWorker;
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
    List<OutputWorker> outputWorkers;

    @Inject
    Logger logger;
    
    @Inject
    public ReactorNetworkServer(ConnectionAccepter accepter,
                                List<InputWorker> inputWorkers,
                                List<OutputWorker> outputWorkers) {
        this.accepter = accepter;
        this.inputWorkers = inputWorkers;
        this.outputWorkers = outputWorkers;
    }

    @Override
    public void start() {
        logger.info("Starting Orchid Tracker with "+inputWorkers.size()+" workers");
        for (InputWorker inputWorker : inputWorkers){
            inputWorker.start();
        }
        for (OutputWorker outputWorker : outputWorkers){
            outputWorker.start();
        }
        accepter.start();
    }
}
