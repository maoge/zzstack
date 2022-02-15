package httpservertest;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;

public class HttpServerTest {
    
    public static void main(String[] args) {
        List<Class<?>> handlers = new ArrayList<Class<?>>();
        handlers.add(TestHandler.class);
        
        HttpServerMarshell serverMarshell = new HttpServerMarshell(9090, false, 4, 16, 15000, handlers, null);
        serverMarshell.start();
    }

}
