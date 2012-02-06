package com.orchid.streams;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: Igor Petruk
 * Date: 25.12.11
 * Time: 14:17
 */

@BindingAnnotation
@Target({ElementType.METHOD,ElementType.CONSTRUCTOR,ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BufferSize {
}
