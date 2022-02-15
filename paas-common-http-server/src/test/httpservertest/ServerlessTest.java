package httpservertest;

import com.zzstack.paas.underlying.httpserver.serverless.ServerlessGatewayRegister;
import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;
import com.zzstack.paas.underlying.utils.bean.SVarObject;

public class ServerlessTest {

    public static void main(String[] args) {
        ServiceData.get().setXApiKey("edd1c9f034335f136f87ad84b625c8f2");
        ServiceData.get().setUpstreamID("paas_metasvr");
        
        ServerlessGatewayRegister register = new ServerlessGatewayRegister("http://172.20.0.171:9080/apisix/admin");
        register.addRoute("_aaa", "paas_metadata", "/paas/bench/aaa", new String[] { "GET" });
        register.addRoute("_bbb", "paas_metadata", "/paas/bench/bbb", new String[] { "GET" });
        
        SVarObject sVar = new SVarObject();
        register.getRoutes(sVar);
        System.out.println(sVar.getVal());
    }

}
