package com.zzstack.paas.underlying.httpserver.marshell;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.httpserver.marshell.verticle.VerticalLoader;
import com.zzstack.paas.underlying.httpserver.serverless.ServerlessGatewayRegister;
import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;
import com.zzstack.paas.underlying.httpserver.singleton.ServiceStatInfo;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class HttpServerMarshell {
    
    private static Logger logger = LoggerFactory.getLogger(HttpServerMarshell.class);
    
    private boolean isNeedRegistGW = false;
    private ServerlessGatewayRegister serverlessGatewayRegister = null;
    
    private Vertx vertx;
    
    public HttpServerMarshell(int port, boolean ssl, int evloopSize, int workerSize, long taskTimeOut, List<Class<?>> handlers) {
        this.init(CONSTS.HTTP_DEFAULT_BIND_IP, port, ssl, evloopSize, workerSize, taskTimeOut, handlers, null);
    }
    
    public HttpServerMarshell(int port, boolean ssl, int evloopSize, int workerSize, long taskTimeOut, List<Class<?>> handlers, IAuthHandler authHandle) {
        this.init(CONSTS.HTTP_DEFAULT_BIND_IP, port, ssl, evloopSize, workerSize, taskTimeOut, handlers, authHandle);
    }
    
    public HttpServerMarshell(String ip, int port, boolean ssl, int evloopSize, int workerSize, long taskTimeOut, List<Class<?>> handlers, IAuthHandler authHandle) {
        this.init(ip, port, ssl, evloopSize, workerSize, taskTimeOut, handlers, authHandle);
    }
    
    public HttpServerMarshell(String ip, int port, boolean ssl, int evloopSize, int workerSize, long taskTimeOut,
            List<Class<?>> handlers, IAuthHandler authHandle, String gwAddrs, String upstreamID, String serviceID, String xApiKey) {
        this.init(ip, port, ssl, evloopSize, workerSize, taskTimeOut, handlers, authHandle);
        this.initGW(gwAddrs, upstreamID, serviceID, xApiKey);
    }
    
    public boolean start() {
        ServiceData serviceData = ServiceData.get();
        
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setMaxEventLoopExecuteTime(serviceData.getEvloopTimeOut());
        vertxOptions.setEventLoopPoolSize(serviceData.getEvLoopPoolSize());
        
        DeploymentOptions deployOptions = new DeploymentOptions();
        deployOptions.setThreadingModel(ThreadingModel.WORKER);
        deployOptions.setWorkerPoolName("verticle.worker.pool");
        deployOptions.setWorkerPoolSize(serviceData.getWorkerPoolSize());
        deployOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS);
        deployOptions.setMaxWorkerExecuteTime(serviceData.getTaskTimeOut());
        deployOptions.setInstances(serviceData.getEvLoopPoolSize());
        
        vertx = Vertx.vertx(vertxOptions);
        ServiceData.get().setVertx(vertx);
        ServerHandleRegister.registVertxRoute(vertx, serviceData.getPort(), serviceData.getHandlers(), serviceData.getAuthHandle());
        
        // Future<String> fu = vertx.deployVerticle(VerticalLoader.class.getName(), deployOptions);
        // logger.info("vertx vertical deploy result: {}", fu.result());
        
        // 兼容vertx 3.9.4
        vertx.deployVerticle(VerticalLoader.class.getName(), deployOptions);
        
        if (isNeedRegistGW) {
            if (!ServerHandleRegister.registServerlessGateway()) {
                return false;
            }
        }
        
        return true;
    }
    
    public void destroy() {
        releaseHttpServer();
        releaseSingletons();
    }
    
    private void releaseHttpServer() {
        vertx.close();
        vertx = null;
    }
    
    private void releaseSingletons() {
        ServiceStatInfo.release();
    }
    
    private void init(String ip, int port, boolean ssl, int evloopSize, int workerSize, long taskTimeOut, List<Class<?>> handlers, IAuthHandler authHandle) {
        ServiceData serviceData = ServiceData.get();
        
        serviceData.setIP(ip);
        serviceData.setPort(port);
        serviceData.setSSL(ssl);
        serviceData.setEvLoopPoolSize(evloopSize);
        serviceData.setWorkerPoolSize(workerSize);
        serviceData.setTaskTimeOut(taskTimeOut);
        serviceData.setHandlers(handlers);
        serviceData.setAuthHandle(authHandle);
    }
    
    private void initGW(String gwAddrs, String upstreamID, String serviceID, String xApiKey) {
        if (gwAddrs == null || gwAddrs.isEmpty()) {
            logger.error("gateway address is null ......");
            return;
        }
        
        if (upstreamID == null || upstreamID.isEmpty()) {
            logger.error("upstreamID is null ......");
            return;
        }
        
        if (xApiKey == null || xApiKey.isEmpty()) {
            logger.error("xApiKey is null ......");
            return;
        }
        
        ServiceData.get().setUpstreamID(upstreamID);
        ServiceData.get().setServiceID(serviceID);
        ServiceData.get().setXApiKey(xApiKey);
        
        this.isNeedRegistGW = true;
        this.serverlessGatewayRegister = new ServerlessGatewayRegister(gwAddrs);
        ServiceData.get().setServerlessGatewayRegister(serverlessGatewayRegister);
    }

}
