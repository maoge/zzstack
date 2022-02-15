package httpservertest;

import java.util.UUID;

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
public class TestHandler implements IServerHandler {
    
    private static Logger logger = LoggerFactory.getLogger(TestHandler.class);
    
    @Service(id = "test", method = HttpMethodEnum.GET,
            name = "test", auth = false, bwswitch = false)
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
    
    @Service(id = "downloadFile", name = "downloadFile",
            method = HttpMethodEnum.GET, auth = false, bwswitch = false, queryParams = {
            @Parameter(name = "fileName", type = ParamType.ParamString, required = true) })
    public static void downloadFile(RoutingContext routeContext) {
        RequestParameters params = HttpUtils.getValidateParams(routeContext);
        RequestParameter queryParams = params.queryParameter("fileName");
        String fileName = queryParams.getString();
        
        // Map<String, Object> params = HttpUtils.getParamForMap(routeContext);
    	// String fileName = (String) params.get("fileName");
    	
        String path = "//e:";
        String file = String.format("%s%s%s", path, HttpUtils.PATH_SPLIT, fileName);

        HttpUtils.outChunkedFile(routeContext, file);
    }
    
    @Service(id = "uploadFile", name = "uploadFile", method = HttpMethodEnum.POST,
            auth = false, bwswitch = false, headerParams = {
            @Parameter(name = "Content-Length", type = ParamType.ParamInt, required = true) })
    public static void uploadFile(RoutingContext routeContext) {
        String path = "//e:";
        String saveFileName = UUID.randomUUID() + ".uploaded";

        HttpUtils.inChunkedFile(routeContext, path, saveFileName);
    }
    
	@Service(id = "queryParamsTest", method = HttpMethodEnum.GET,
	        auth = false, bwswitch = false,
	        queryParams = {
			@Parameter(name = "fileId", type = ParamType.ParamInt, required = true),
			@Parameter(name = "fileName", type = ParamType.ParamString, required = true),
			@Parameter(name = "address", type = ParamType.ParamString, required = false)})
	public static void queryParamsTest(RoutingContext ctx) {
		RequestParameters params = HttpUtils.getValidateParams(ctx);
		int fileId = params.queryParameter("fileId").getInteger();
		String fileName = params.queryParameter("fileName").getString();
		
		RequestParameter addressRPram = params.queryParameter("address");
		String address = addressRPram != null ? addressRPram.getString() : null;

		JsonObject json = new JsonObject();
		json.put("fileId", fileId);
		json.put("fileName", fileName);
		json.put("address", address != null ? address : "");

		HttpUtils.outJsonObject(ctx, json);
	}
    
	@Service(id = "pathParamsTest/:fileId/:fileName", method = HttpMethodEnum.GET,
	        auth = false, bwswitch = false, pathParams = {
			@Parameter(name = "fileId", type = ParamType.ParamInt, required = true),
			@Parameter(name = "fileName", type = ParamType.ParamString, required = true) })
	public static void pathParamsTest(RoutingContext ctx) {
		RequestParameters params = HttpUtils.getValidateParams(ctx);
		int fileId = params.pathParameter("fileId").getInteger();
		String fileName = params.pathParameter("fileName").getString();

		JsonObject json = new JsonObject();
		json.put("fileId", fileId);
		json.put("fileName", fileName);

		HttpUtils.outJsonObject(ctx, json);
	}
	
	@Service(id = "headerParamsTest", method = HttpMethodEnum.GET,
	        auth = false, bwswitch = false, headerParams = {
			@Parameter(name = "MAGIC_KEY", type = ParamType.ParamString, required = true) })
	public static void headerParamsTest(RoutingContext ctx) {
		RequestParameters params = HttpUtils.getValidateParams(ctx);
		String magicKey = params.headerParameter("MAGIC_KEY").getString();

		JsonObject json = new JsonObject();
		json.put("MAGIC_KEY", magicKey);

		HttpUtils.outJsonObject(ctx, json);
	}

}
