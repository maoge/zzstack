package com.zzstack.paas.underlying.httpserver.marshell.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Router;

public class VerticalLoader extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(VerticalLoader.class);

    @Override
    public void start() throws Exception {
        super.start();
        
        Router router = ServiceData.get().getRouter();

        String ip = ServiceData.get().getIP();
        int port = ServiceData.get().getPort();
        boolean useSSL = ServiceData.get().isUseSSL();
        
        HttpServerOptions httpServerOpts = new HttpServerOptions();
        httpServerOpts.setSsl(useSSL);
        if (useSSL) {
            PemKeyCertOptions pkCertOpts = new PemKeyCertOptions();
            pkCertOpts.addCertPath("cert/cert.pem");
            pkCertOpts.addKeyPath("cert/key.pem");
            httpServerOpts.setPemKeyCertOptions(pkCertOpts);
        }
        
        HttpServer server = vertx.createHttpServer(httpServerOpts);
        String protocol = useSSL ? "https" : "http";
        
        SocketAddress sockAddr = SocketAddress.inetSocketAddress(port, ip);
        
        server.requestHandler(router).listen(sockAddr, res -> {
            ServiceData.get().setHttpServer(server);
            
            if (res.succeeded()) {
                // registerEventBus();
                logger.info("{} server listen:{}:{} succeeded!", protocol, ip, port);
            } else {
                Throwable t = res.cause();
                logger.error("{} server listen:{}:{} failed, reason:{}", protocol, ip, port, t);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        logger.info("http server stopped!");
    }

}
