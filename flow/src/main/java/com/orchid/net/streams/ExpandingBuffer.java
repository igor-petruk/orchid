package com.orchid.net.streams;

import com.orchid.collections.ArrayBackedList;
import com.orchid.messages.generated.Messages;

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
    
    int count = 0;

    public boolean isMessageActive() {
        return messageActive;
    }

    public void setMessageActive(boolean messageActive) {
        this.messageActive = messageActive;
    }

    public ExpandingBuffer(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
        this.bufferSize = bufferPool.getBufferSize();
        usedBuffers = new ArrayBackedList<DirectBuffer>(
                DirectBuffer.class,
                bufferPool,
                0);
        outputStream = new ExpandingBufferOutputStream(usedBuffers, bufferSize);
    }

    public void spill(Messages.MessageContainer container){
        try{
            int size = container.getSerializedSize();
            outputStream.reset();
            OutputStream output = Channels.newOutputStream(outputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(output);
            //System.out.println("Size "+size);
            dataOutputStream.writeInt(size);
            dataOutputStream.flush();
            container.writeTo(output);
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
}
