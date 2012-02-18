package com.orchid.logging;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.lang.reflect.Field;

/**
 * User: Igor Petruk
 * Date: 24.01.12
 * Time: 12:53
 */
class Slf4jTypeListener implements TypeListener {
    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        for (Field field : type.getRawType().getDeclaredFields()) {
            if (field.getType() == Logger.class && (field.isAnnotationPresent(Inject.class))) {
                encounter.register(new Slf4jMembersInjector<I>(field));
            }
        }
    }
}
