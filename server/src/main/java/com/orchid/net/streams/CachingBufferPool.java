package com.orchid.net.streams;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 13:02
 */
public class CachingBufferPool implements BufferPool{
    final ByteBuffer mainBuffer;
    final List<DirectBuffer> buffers = new ArrayList<DirectBuffer>();
    final int bufferSize;

    @Inject
    Logger logger;

    @Inject
    public CachingBufferPool(@BufferSize int bufferSize) {
        this.bufferSize = bufferSize;
        mainBuffer = ByteBuffer.allocateDirect(bufferSize*100);
        for (int i = 0; i < 100; i++){
            mainBuffer.position(i*bufferSize);
            mainBuffer.limit((i+1)*bufferSize);
            ByteBuffer chunk = mainBuffer.slice();
            DirectBuffer newBuffer = new DirectBuffer(chunk);
            newBuffer.setUsed(false);
            buffers.add(newBuffer);
        }
    }

    @Override
    public DirectBuffer allocate(){
        DirectBuffer emptyBuffer = null;
        // TODO: ConcurrentModificatioException happened here
        //System.out.println(Thread.currentThread() + " "+this);
        for (DirectBuffer buffer:buffers){
            if (!buffer.isUsed()){
                emptyBuffer = buffer;
                break;
            }
        }
        if (emptyBuffer!=null){
            emptyBuffer.setUsed(true);
            return emptyBuffer;
        }else{
            DirectBuffer newBuffer = new DirectBuffer(bufferSize);
            newBuffer.setUsed(true);
            buffers.add(newBuffer);
            return newBuffer;
        }
    }

    @Override
    public void free(DirectBuffer buffer){
        buffer.setUsed(false);
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

}
