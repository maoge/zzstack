package com.zzstack.paas.underlying.metasvr.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/bench")
public class BenchHandler implements IServerHandler {
	
    private static Logger logger = LoggerFactory.getLogger(BenchHandler.class);
	
	@Service(id = "test", method = HttpMethodEnum.GET, name = "test", auth = false, bwswitch = false)
	public static void test(RoutingContext routeContext) {
		HttpServerRequest req = routeContext.request();
		
		JsonObject json = new JsonObject();
		
		if (req != null) {
			SocketAddress remoteAddr = req.remoteAddress();
			SocketAddress localAddr  = req.localAddress();
			
			json.put(FixHeader.HEADER_RET_CODE,    CONSTS.REVOKE_OK);
			json.put(FixHeader.HEADER_RET_INFO,    "");
			json.put(FixHeader.HEADER_REMOTE_IP,   remoteAddr.host());
			json.put(FixHeader.HEADER_REMOTE_PORT, remoteAddr.port());
			json.put(FixHeader.HEADER_LOCAL_IP,    localAddr.host());
			json.put(FixHeader.HEADER_LOCAL_PORT,  localAddr.port());
			
			logger.debug("respond:{}", json.toString());
		} else {
			json.put(FixHeader.HEADER_RET_CODE,    CONSTS.REVOKE_NOK);
			json.put(FixHeader.HEADER_RET_INFO,    "HttpServerRequest null.");
		}
		
		HttpUtils.outJsonObject(routeContext, json);
	}

	@Service(id = "nextId", method = HttpMethodEnum.POST, name = "nextId", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
	public static void nextId(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);
        String info = String.format("pageNum: %d, pageSize: %d, page test ok", pageNum, pageSize);
        
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        json.put(FixHeader.HEADER_RET_INFO, info);
		
        HttpUtils.outJsonObject(ctx, json);
	}
	
}
