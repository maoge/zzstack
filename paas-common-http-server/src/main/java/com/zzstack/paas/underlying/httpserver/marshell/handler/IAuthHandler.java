package com.zzstack.paas.underlying.httpserver.marshell.handler;

import io.vertx.ext.web.RoutingContext;

public interface IAuthHandler {
    
    boolean doAuth(RoutingContext ctx);

}
