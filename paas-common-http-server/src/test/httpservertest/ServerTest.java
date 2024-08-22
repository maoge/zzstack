package httpservertest;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

public class ServerTest {

	public static void main(String[] args) {
		startHttpServer();
	}

	private static void startHttpServer() {
		List<Class<?>> handlers = new ArrayList<Class<?>>();
        handlers.add(TestHandler.class);
        
        IAuthHandler authHandle = new MyAuthHandler();

        HttpServerMarshell serverMarshell = new HttpServerMarshell(9000, false, 4, 40, 60000, handlers, authHandle);
        if (serverMarshell.start()) {
            System.out.println("HttpServerMarshell start ok ....");
        } else {
        	System.out.println("HttpServerMarshell start fail, release ....");
        }
	}

	private static class MyAuthHandler implements IAuthHandler {

		public MyAuthHandler() {
			
		}

		@Override
		public boolean doAuth(RoutingContext ctx) {
			return true;
		}

	}

	@App(path = "/api")
    public static class TestHandler implements IServerHandler {
    	
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
    			
    			System.out.println("respond:" + json.toString());
    		} else {
    			json.put(FixHeader.HEADER_RET_CODE,    CONSTS.REVOKE_NOK);
    			json.put(FixHeader.HEADER_RET_INFO,    "HttpServerRequest null.");
    		}
    		
    		HttpUtils.outJsonObject(routeContext, json);
    	}
    	
    }
	
}
