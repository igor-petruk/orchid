package com.orchid.logic.annotations;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: Igor Petruk
 * Date: 01.01.12
 * Time: 17:39
 */

@BindingAnnotation
@Target({ElementType.METHOD,ElementType.CONSTRUCTOR,ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLogic {
}
