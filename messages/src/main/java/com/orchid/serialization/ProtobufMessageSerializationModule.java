package com.orchid.serialization;

import com.google.inject.AbstractModule;

/**
 * User: Igor Petruk
 * Date: 19.02.12
 * Time: 18:04
 */
public class ProtobufMessageSerializationModule extends AbstractModule{

    @Override
    protected void configure() {
        bind(FlowMessageSerializer.class).to(ProtobufMessageSerializer.class);
    }
}
