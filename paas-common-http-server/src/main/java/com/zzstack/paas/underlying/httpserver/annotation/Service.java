package com.zzstack.paas.underlying.httpserver.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface Service {
    
    String id() default "";
    String visible() default "public";
    String name() default "";
    HttpMethodEnum method() default HttpMethodEnum.POST;
    
    boolean auth() default true;
    boolean bwswitch() default true;
    
    Parameter[] headerParams() default {};
    Parameter[] pathParams() default {};
    Parameter[] queryParams() default {};
    Parameter[] bodyParams() default {};
    
    Property[] properties() default {};
    Argu[] arguments() default {};
    
}
