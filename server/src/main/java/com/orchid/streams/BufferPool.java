package com.orchid.streams;

import com.orchid.collections.CollectionElementProvider;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 14:15
 */
public interface BufferPool extends CollectionElementProvider<DirectBuffer>{
    public DirectBuffer allocate();
    public void free(DirectBuffer buffer);
    public int getBufferSize();
}
