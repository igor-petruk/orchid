package com.orchid.net.streams;

import com.orchid.collections.ArrayBackedList;
import com.orchid.serialization.FlowMessageSerializer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

/**
 * User: Igor Petruk
 * Date: 02.02.12
 * Time: 22:26
 */
public class ExpandingBuffer {
    BufferPool bufferPool;
    int bufferSize;
    ArrayBackedList<DirectBuffer> usedBuffers;
    ExpandingBufferOutputStream outputStream;
    boolean messageActive;
    
    FlowMessageSerializer serializer;
    
    int count = 0;

    public boolean isMessageActive() {
        return messageActive;
    }

    public void setMessageActive(boolean messageActive) {
        this.messageActive = messageActive;
    }

    public ExpandingBuffer(BufferPool bufferPool, FlowMessageSerializer serializer) {
        this.serializer = serializer;
        this.bufferPool = bufferPool;
        this.bufferSize = bufferPool.getBufferSize();
        usedBuffers = new ArrayBackedList<DirectBuffer>(
                DirectBuffer.class,
                bufferPool,
                0);
        outputStream = new ExpandingBufferOutputStream(usedBuffers, bufferSize);
    }

    public void spill(Object message){
        try{
            int size = serializer.getMessageSize(message);
            outputStream.reset();
            OutputStream output = Channels.newOutputStream(outputStream);

            DataOutputStream dataOutputStream = new DataOutputStream(output);
            dataOutputStream.writeInt(serializer.getMessageSize(message));
            dataOutputStream.flush();

            serializer.writeMessage(message, output);
            output.flush();
            outputStream.startSending();
            setMessageActive(true);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public SinkOutcome sink(SocketChannel channel){
        SinkOutcome outcome = outputStream.sink(channel);
        if (outcome.equals(SinkOutcome.DONE)){
            setMessageActive(false);
        }
        return outcome;
    }

    public void close() {
        usedBuffers.dispose();
    }
}
