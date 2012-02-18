package com.orchid.net.streams;

import com.orchid.collections.ArrayBackedList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * User: Igor Petruk
 * Date: 03.02.12
 * Time: 17:01
 */
public class ExpandingBufferOutputStream implements WritableByteChannel{
    ArrayBackedList<DirectBuffer> usedBuffers;
    int bufferSize;
    int position, messageSize;
    
    public ExpandingBufferOutputStream(ArrayBackedList<DirectBuffer> usedBuffers, int bufferSize) {
        this.usedBuffers = usedBuffers;
        this.bufferSize = bufferSize;
    }
    
    public SinkOutcome sink(SocketChannel channel){
        //System.out.println("Sinking");
        try{
            while(position<messageSize){
                int bufferId = position/bufferSize;
                ByteBuffer byteBuffer = usedBuffers.get(bufferId).getByteBuffer();
                int written = channel.write(byteBuffer);
                if (written==-1){
                    return SinkOutcome.ERROR;
                }
                position += written;
            }
        }catch(IOException e){
            e.printStackTrace();
            return SinkOutcome.DONE;
        }
        if (position==messageSize){
            return SinkOutcome.DONE;
        }else{
            return SinkOutcome.IN_PROGRESS;
        }
        //ByteBuffer byteBuffer = usedBuffers.get(bufferId).getByteBuffer();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int bufferId = position/bufferSize;
        if (bufferId==usedBuffers.size()){
            usedBuffers.extend();
        }
        ByteBuffer byteBuffer = usedBuffers.get(bufferId).getByteBuffer();
        int offset = 0;
        while((src.remaining()>0)&&(byteBuffer.remaining()>0)){
            byteBuffer.put(src.get());
            offset++;
        }
        position+=offset;
        //System.out.println("Writing "+offset);
        return offset;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    public void reset(){
        //System.out.println("Reseting");
        for (DirectBuffer directBuffer: usedBuffers){
            directBuffer.getByteBuffer().clear();
        }
        position = 0;
    }

    public void startSending(){
        //System.out.println("Starting sending "+messageSize);
        messageSize = position;
        for (DirectBuffer directBuffer: usedBuffers){
            ByteBuffer buffer = directBuffer.getByteBuffer();
            buffer.limit(buffer.position());
            buffer.position(0);
        }
        position = 0;
    }

    @Override
    public void close() throws IOException {
    }
}
