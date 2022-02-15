package com.zzstack.paas.underlying.httpserver.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface App {

    String path();
    boolean auth() default true; // 为false的时候，不检查底下的所有的路径

}
