package com.orchid.net.streams;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 15:40
 */
public class BufferAggregatorInputStream implements ReadableByteChannel{
    List<DirectBuffer> buffers;
    int messageSize, position, bufferSize;

    public int getBufferSize() {
        return bufferSize;
    }

    public void reset(int newMessageSize){
        position = 0;
        messageSize = newMessageSize;
        for (DirectBuffer buffer: buffers){
            buffer.getByteBuffer().rewind();
        }
    }

    public BufferAggregatorInputStream(List<DirectBuffer> buffers, int bufferSize) {
        this.buffers = buffers;
        this.bufferSize = bufferSize;
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if (position==messageSize){
            return -1;
        }
        int currentBufferIndex = position/bufferSize;
        DirectBuffer current = buffers.get(currentBufferIndex);
        int initialPosition = current.getByteBuffer().position();
        byteBuffer.put(current.getByteBuffer());
        int offset = current.getByteBuffer().position() - initialPosition;
        position += offset;
        if (offset==0){
            offset=-1;
        }
        return offset;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }
}
