package com.zzstack.paas.underlying.collectd;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.collectd.constants.CollectdConstants;
import com.zzstack.paas.underlying.collectd.global.CollectdGlobalData;
import com.zzstack.paas.underlying.collectd.handler.CollectdHanlder;
import com.zzstack.paas.underlying.collectd.probe.Prober;
import com.zzstack.paas.underlying.collectd.probe.RedisClusterProber;
import com.zzstack.paas.underlying.collectd.probe.RedisMSProber;
import com.zzstack.paas.underlying.collectd.probe.RocketMQProber;
import com.zzstack.paas.underlying.collectd.runner.CollectRunner;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;

import io.vertx.core.json.JsonObject;

public class Bootstrap {
    
    public static Logger logger = LoggerFactory.getLogger(Bootstrap.class);
    
    public static void main(String[] args) {
        init();
        
        // 1. init http
        initHttpServer();
        
        // 2. init prober
        initProber();
    }
    
    private static void init() {
        try {
            CollectdConstants.init();
            
            CollectdGlobalData.get();
        } catch (PaasSdkException e) {
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }
    }
    
    private static void initHttpServer() {
        List<Class<?>> handlers = new ArrayList<Class<?>>();
        handlers.add(CollectdHanlder.class);
        
        boolean isWebApiUseSSL = false;
        int vertxEvLoopSize = 1;
        int vertxWorkerPoolSize = 4;
        long maxEventLoopExecuteTime = 10000L;
        
        HttpServerMarshell serverMarshell = new HttpServerMarshell(CollectdConstants.collectdPort, isWebApiUseSSL,
                vertxEvLoopSize, vertxWorkerPoolSize, maxEventLoopExecuteTime, handlers);
        if (!serverMarshell.start()) {
            logger.error("HttpServerMarshell start fail, release ......");
            release();
        } else {
            CollectdGlobalData.get().setServerMarshell(serverMarshell);
        }
    }
    
    private static void initProber() {
        JsonObject servTopo = CollectdGlobalData.get().getServTopo();
        String servType = servTopo.getString(FixHeader.HEADER_SERV_TYPE);
        
        Prober prober = null;
        
        switch (servType) {
        case CONSTS.SERV_TYPE_MQ_ROCKETMQ:
            prober = new RocketMQProber();
            break;
        
        case CONSTS.SERV_TYPE_MQ_PULSAR:
            break;
        
        case CONSTS.SERV_TYPE_CACHE_REDIS_CLUSTER:
            prober = new RedisClusterProber();
            break;
        
        case CONSTS.SERV_TYPE_CACHE_REDIS_MASTER_SLAVE:
            prober = new RedisMSProber();
            break;
        
        case CONSTS.SERV_TYPE_DB_TIDB:
            break;
        
        case CONSTS.SERV_TYPE_DB_TDENGINE:
            break;
        
        case CONSTS.SERV_TYPE_DB_VOLTDB:
            break;
        
        case CONSTS.SERV_TYPE_SERVERLESS_APISIX:
            break;
        
        case CONSTS.SERV_TYPE_SMS_GATEWAY:
            break;
        
        default:
            break;
        }
        
        CollectRunner collectRunner = new CollectRunner(prober);
        CollectdGlobalData.get().setCollectRunner(collectRunner);
    }
    
    private static void release() {
        CollectdGlobalData.destroy();
    }

}
