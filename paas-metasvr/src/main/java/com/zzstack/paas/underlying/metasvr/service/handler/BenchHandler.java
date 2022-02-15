package com.zzstack.paas.underlying.metasvr.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

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

}
