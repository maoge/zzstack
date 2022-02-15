package com.zzstack.paas.underlying.metasvr.startup;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.metasvr.iaas.Dashboard;
import com.zzstack.paas.underlying.metasvr.service.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.threadpool.WorkerPool;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;

public class Bootstrap {

    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        bootstrap();
    }

    public static void bootstrap() {
        bootSingleInstance();
        bootMicroService();
    }

    private static void bootSingleInstance() {
        SysConfig.get();
        MetaSvrGlobalRes.get();
        Dashboard.get();
        WorkerPool.get();
    }

    private static void release() {
        MetaSvrGlobalRes.release();
        WorkerPool.release();
        ActiveStandbyDBSrcPool.destroy();
    }

    private static void bootMicroService() {
        List<Class<?>> handlers = new ArrayList<Class<?>>();
        handlers.add(BenchHandler.class);
        handlers.add(MetaDataHander.class);
        handlers.add(AutoDeployHandler.class);
        handlers.add(AccountHandler.class);
        handlers.add(StatisticHandler.class);
        handlers.add(AlarmHandler.class);

        SysConfig sysConfig = SysConfig.get();
        HttpServerMarshell serverMarshell = null;
        IAuthHandler authHandle = new MetaSvrAuthHandler();
        if (sysConfig.isServerlessGatewayRegist()) {
            serverMarshell = new HttpServerMarshell(sysConfig.getWebApiIP(), sysConfig.getWebApiPort(),
                sysConfig.isWebApiUseSSL(), sysConfig.getVertxEvLoopSize(), sysConfig.getVertxWorkerPoolSize(),
                sysConfig.getVertxWokerMaxExecTime(), handlers, authHandle,
                sysConfig.getServerlessGatewayAddress(), sysConfig.getServerlessGatewayUpstreamID(), sysConfig.getServerlessGatewayServiceID(),
                sysConfig.getServerlessGatewayXAPIKey());
        } else {
            serverMarshell = new HttpServerMarshell(sysConfig.getWebApiPort(),
                sysConfig.isWebApiUseSSL(), sysConfig.getVertxEvLoopSize(), sysConfig.getVertxWorkerPoolSize(),
                sysConfig.getVertxWokerMaxExecTime(), handlers, authHandle);
        }
        MetaSvrGlobalRes.get().setHttpServerMarshell(serverMarshell);

        if (!serverMarshell.start()) {
            logger.error("HttpServerMarshell start fail, release ......");
            release();
        }
    }

}
