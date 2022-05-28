package com.zzstack.paas.underlying.httpserver.stats;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.singleton.AllServiceMap;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/stats")
public class StatisticHandler implements IServerHandler {
	
	/**
	 * 获取全部api info
	 * @param ctx
	 */
    @Service(id = "apiAllInfo", method = HttpMethodEnum.GET, auth = false, bwswitch = false)
    public static void apiAllInfo(RoutingContext ctx) {
    	JsonArray arr = AllServiceMap.get().getAllApiJson();
    	HttpUtils.outJsonArray(ctx, arr);
    }

    /**
     * 获取指定api info
     * @param ctx
     */
    @Service(id = "apiInfo", method = HttpMethodEnum.GET, auth = false, bwswitch = false, queryParams = {
            @Parameter(name = "URI", type = ParamType.ParamString, required = true)
    })
    public static void apiInfo(RoutingContext ctx) {
    	RequestParameters params = HttpUtils.getValidateParams(ctx);
    	String uri = params.queryParameter("URI").getString();
    	JsonObject obj = AllServiceMap.get().getApiJson(uri);
    	HttpUtils.outJsonObject(ctx, obj);
    }
    
}
