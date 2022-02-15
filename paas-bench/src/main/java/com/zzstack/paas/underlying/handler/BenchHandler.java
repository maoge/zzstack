package com.zzstack.paas.underlying.handler;

import com.zzstack.paas.underlying.dao.ActiveStandbyDBDao;
import com.zzstack.paas.underlying.dao.RedisPoolDao;
import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/test")
public class BenchHandler implements IServerHandler {
    
    @Service(id = "resetWeight", method = HttpMethodEnum.GET, auth = false, bwswitch = false, queryParams = {
            @Parameter(name = "weightParams", type = ParamType.ParamString, required = true) })
    public static void resetWeight(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        String weigth = params.queryParameter("weightParams").getString();

        JsonObject json = new JsonObject();
        RedisPoolDao.adjustRedisPoolWeight(weigth, json);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "switchDBType", method = HttpMethodEnum.GET, auth = false, bwswitch = false, queryParams = {
            @Parameter(name = "DBType", type = ParamType.ParamString, required = true),
            @Parameter(name = "DBName", type = ParamType.ParamString, required = true) })
    public static void switchDB(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        String dbType = params.queryParameter("DBType").getString();
        String dbName = params.queryParameter("DBName").getString();

        JsonObject json = new JsonObject();
        ActiveStandbyDBDao.switchDBType(dbType, dbName, json);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "dbInUse", method = HttpMethodEnum.GET, auth = false, bwswitch = false, queryParams = {
            @Parameter(name = "DBName", type = ParamType.ParamString, required = true) })
    public static void dbInUse(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        String dbName = params.queryParameter("DBName").getString();

        JsonObject json = new JsonObject();
        ActiveStandbyDBDao.dbInUse(dbName, json);

        HttpUtils.outJsonObject(ctx, json);
    }

}
