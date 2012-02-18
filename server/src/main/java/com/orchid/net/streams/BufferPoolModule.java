package com.orchid.net.streams;

import com.google.inject.AbstractModule;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 14:14
 */
public class BufferPoolModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Integer.class).annotatedWith(BufferSize.class).toInstance(1024);
        bind(BufferPool.class).to(CachingBufferPool.class);
    }
}
