package com.orchid.net.server.workers.input;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: Igor Petruk
 * Date: 28.12.11
 * Time: 12:09
 */
@BindingAnnotation
@Target({ElementType.METHOD,ElementType.CONSTRUCTOR,ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface InputWorkersCount {
}
