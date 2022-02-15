package com.zzstack.paas.underlying.collectd.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
// import io.vertx.ext.web.validation.RequestParameter;
// import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/collectd")
public class CollectdHanlder implements IServerHandler {
    
    @Service(id = "check", auth = false, bwswitch = false)
    public static void check(RoutingContext ctx) {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "servTopoChange", auth = false, bwswitch = false)
    public static void getCurrData(RoutingContext ctx) {
        JsonObject json = new JsonObject();
        // TODO 填充最新的采集数据

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "servTopoChange", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_TOPO_JSON, type = ParamType.ParamObject, required = true) })
    public static void servTopoChange(RoutingContext ctx) {

        // RequestParameters params = HttpUtils.getValidateParams(ctx);
        // RequestParameter body = params.body();
        // JsonObject bodyJson = body.getJsonObject();
        
        // String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        // JsonObject topoJson = bodyJson.getJsonObject(FixHeader.HEADER_TOPO_JSON);

        JsonObject json = new JsonObject();
        // TODO
        // 服务增\删节点或卸载\部署, 需要通知collectd最新的服务拓扑元数据

        HttpUtils.outJsonObject(ctx, json);
    }

}
