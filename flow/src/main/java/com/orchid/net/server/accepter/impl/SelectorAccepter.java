package com.orchid.net.server.accepter.impl;

import com.orchid.net.server.annotations.ServerPort;
import com.orchid.net.server.connections.Connection;
import com.orchid.net.server.exceptions.NetworkException;
import com.orchid.net.server.accepter.ConnectionAccepter;
import com.orchid.net.server.workers.*;
import com.orchid.net.server.workers.input.InputWorker;
import com.orchid.net.server.workers.input.InputWorkerSelector;
import com.orchid.net.server.workers.output.OutputWorker;
import com.orchid.net.server.workers.output.OutputWorkerSelector;
import com.orchid.user.UserID;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 7:29
 */
public class SelectorAccepter implements ConnectionAccepter, Runnable{
    @Inject
    Logger logger;

    ServerSocketChannel serverSocketChannel;
    Selector serverSelector;

    @Inject
    @ServerPort
    int port;

    @Inject
    @InputWorkerSelector
    WorkerSelector inputWorkerSelector;

    @Inject
    @OutputWorkerSelector
    WorkerSelector outputWorkerSelector;

    Thread thread;

    @Override
    public void start(){
        try{
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            while (true) {
                try {
                    serverSocketChannel.socket().bind(new InetSocketAddress(InetAddress.getByName(System.getenv("JENKINS_HOST")),port));
                    break;
                } catch (IOException e) {
                    logger.info("Retrying with port {}",port);
                    port++;
                    continue;
                }
            }

            logger.info(MessageFormat.format("Started server on port {0}", port));
            serverSelector = Selector.open();
            serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
            thread = new Thread(this);
            thread.start();
        }catch(IOException e){
            throw new NetworkException("Unable to start accepter",e);
        }
    }

    public void handleAccept(SelectionKey selectionKey) throws IOException{
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        InputWorker inputWorker = (InputWorker)inputWorkerSelector.get();
        OutputWorker outputWorker = (OutputWorker)outputWorkerSelector.get();
        Connection connection = new Connection(client);
        UserID userID = new UserID(inputWorker, outputWorker, connection);
        connection.setUserID(userID);
        inputWorker.handleConnection(connection);
        outputWorker.handleConnection(connection);
    }

    @Override
    public void run() {
        while(true){
            try{
                serverSelector.select();
                Set<SelectionKey> keys = serverSelector.selectedKeys();
                Iterator<SelectionKey> i = keys.iterator();
                while (i.hasNext()) {
                    SelectionKey key = (SelectionKey) i.next();
                    i.remove();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                        continue;
                    }
                }
            }catch(IOException e){
                 if(Thread.interrupted()){
                     break;
                 }else{
                    logger.warn("Accepter error {}",
                            new NetworkException("Accepter error",e));
                 }
             }
        }
    }
}
