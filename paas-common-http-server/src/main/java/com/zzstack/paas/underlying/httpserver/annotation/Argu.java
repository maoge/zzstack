package com.zzstack.paas.underlying.httpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Argu {

    String id();
    String dft() default "";
    boolean option() default true;
    boolean cached() default false;
    String getter() default "Default";
    String parameters() default "";

}
