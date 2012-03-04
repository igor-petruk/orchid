package com.orchid.net.server.connections;

import com.orchid.net.streams.BufferAggerator;
import com.orchid.net.streams.ExpandingBuffer;
import com.orchid.user.UserID;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 10:46
 */
public class Connection {
    SocketChannel socketChannel;
    BufferAggerator bufferAggerator;
    ExpandingBuffer expandingBuffer;
    UserID userID;
    Queue<Object> outputQueue;
    SelectionKey outputKey;
    boolean currentlySending;

    public ExpandingBuffer getExpandingBuffer() {
        return expandingBuffer;
    }

    public void setExpandingBuffer(ExpandingBuffer expandingBuffer) {
        this.expandingBuffer = expandingBuffer;
    }

    public SelectionKey getOutputKey() {
        return outputKey;
    }

    public void setOutputKey(SelectionKey outputKey) {
        this.outputKey = outputKey;
    }

    public Connection(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        outputQueue = new LinkedList<Object>();
    }

    public void setUserID(UserID userID) {
        this.userID = userID;
    }

    public BufferAggerator getBufferAggerator() {
        return bufferAggerator;
    }

    public void setBufferAggerator(BufferAggerator bufferAggerator) {
        this.bufferAggerator = bufferAggerator;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public UserID getUserID() {
        return userID;
    }

    public Queue<Object> getOutputQueue() {
        return outputQueue;
    }

    public boolean isCurrentlySending() {
        return currentlySending;
    }

    public void setCurrentlySending(boolean currentlySending) {
        this.currentlySending = currentlySending;
    }

    public void dispose(){

    }
}
