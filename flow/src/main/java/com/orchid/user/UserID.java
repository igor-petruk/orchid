package com.orchid.user;

import com.orchid.net.server.connections.Connection;
import com.orchid.net.server.workers.input.InputWorker;
import com.orchid.net.server.workers.output.OutputWorker;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 21:37
 *
 * TODO: Please rename this
 */
public class UserID {
    InputWorker inputWorker;
    OutputWorker outputWorker;
    Connection connection;

    public UserID(InputWorker inputWorker, OutputWorker outputWorker,
                  Connection connection) {
        this.inputWorker = inputWorker;
        this.outputWorker = outputWorker;
        this.connection = connection;
    }

    public InputWorker getInputWorker() {
        return inputWorker;
    }

    public OutputWorker getOutputWorker() {
        return outputWorker;
    }

    public Connection getConnection() {
        return connection;
    }
}
