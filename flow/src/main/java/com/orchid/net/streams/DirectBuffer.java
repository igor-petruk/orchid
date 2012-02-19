package com.orchid.net.streams;

import java.nio.ByteBuffer;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 13:14
 */
public class DirectBuffer {
    ByteBuffer byteBuffer;
    boolean used;

    public DirectBuffer(int size) {
        this.byteBuffer = ByteBuffer.allocateDirect(size);
        used = false;
    }

    public DirectBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        used = false;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
