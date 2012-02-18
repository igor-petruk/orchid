package com.orchid.net.streams;

import com.orchid.collections.ArrayBackedList;
import com.orchid.logic.user.UserID;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 13:31
 */
public class BufferAggerator implements Closeable{
    ArrayBackedList<DirectBuffer> usedBuffers;
    ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
    int position;
    int currentMessageSize;
    boolean readingSize;

    BufferPool bufferPool;
    MessageHandler messageHandler;

    BufferAggregatorInputStream aggregatorInputStream;

    public BufferAggerator(BufferPool bufferPool, MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.bufferPool = bufferPool;
        readingSize = true;
        usedBuffers = new ArrayBackedList<DirectBuffer>(
                DirectBuffer.class,
                bufferPool,
                0);
        aggregatorInputStream = new BufferAggregatorInputStream(usedBuffers, bufferPool.getBufferSize());
    }

    @Override
    public void close() throws IOException {
        usedBuffers.dispose();
    }

    public boolean readSome(UserID userID, ScatteringByteChannel channel) throws IOException{
        while(true){
            if (readingSize){
                int bytesRead = channel.read(sizeBuffer);
                if (bytesRead==-1)
                    return false;
                if (bytesRead<4)
                    return true;
                sizeBuffer.rewind();
                currentMessageSize = sizeBuffer.getInt();
                sizeBuffer.rewind();
                readingSize = false;
                position = 0;
            }
            int currentBuffer = position/bufferPool.getBufferSize();
            if (currentBuffer>=usedBuffers.size()){
                usedBuffers.extend();
            }
            int remaining = currentMessageSize - position;
            boolean last = remaining <= bufferPool.getBufferSize();
            DirectBuffer bufferToUse = usedBuffers.get(currentBuffer);
            if (last){
                bufferToUse.getByteBuffer().limit(remaining);
            }
            int bytesRead = channel.read(bufferToUse.getByteBuffer());
            if (bytesRead==0){
                return true;
            }else if (bytesRead!=-1){
                position += bytesRead;
                if (position==currentMessageSize){
                    aggregatorInputStream.reset(currentMessageSize);
                    messageHandler.handleMessage(userID, aggregatorInputStream);
                    for (DirectBuffer directBuffer: usedBuffers){
                        directBuffer.getByteBuffer().clear();
                        directBuffer.getByteBuffer().rewind();
                    }
                    readingSize = true;
                    position = 0;
                }
                return true;
            }else{
                return false;
            }
        }
    }
}
