package com.orchid.net.server.workers;

import com.orchid.net.server.connections.Connection;
import com.orchid.net.server.exceptions.NetworkException;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Igor Petruk
 * Date: 26.01.12
 * Time: 21:41
 */
public abstract class Worker{
    protected AtomicInteger connectionsCount = new AtomicInteger();
    protected Selector selector;

    @Inject
    private Logger logger;

    protected String workerName;

    public Worker() {
        try{
            selector = Selector.open();
        }catch(IOException e){
            throw new NetworkException("Unable to create worker selector", e);
        }
    }

    public int getConnectionsCount() {
        return connectionsCount.get();
    }

    public void setWorkerName(String s) {
        this.workerName = s;
    }

    protected void handleDisconnect(SelectionKey key) throws IOException{
        logger.debug("Disconnected {}", key.channel());
        key.cancel();
        connectionsCount.decrementAndGet();
    }

    public abstract void handleConnection(Connection connection);

}
