package com.orchid.main;

import com.google.inject.AbstractModule;
import com.orchid.flow.FlowModule;
import com.orchid.logging.LoggingModule;
import com.orchid.logic.LogicModule;
import com.orchid.serialization.ProtobufMessageSerializationModule;

/**
 * User: Igor Petruk
 * Date: 18.02.12
 * Time: 16:20
 */
public class RunnerModule extends AbstractModule{
    @Override
    protected void configure() {
        install(new LoggingModule());
        install(new FlowModule());
        install(new ProtobufMessageSerializationModule());
        install(new LogicModule());
    }
}
