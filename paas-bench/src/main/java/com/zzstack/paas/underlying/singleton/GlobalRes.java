package com.zzstack.paas.underlying.singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.zzstack.paas.underlying.bench.dbpool.DbPoolBencher;
import com.zzstack.paas.underlying.bench.redispool.RedisQueuePoper;
import com.zzstack.paas.underlying.bench.redispool.RedisQueuePusher;
import com.zzstack.paas.underlying.collect.RedisInfoCollector;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.handler.BenchHandler;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;

public class GlobalRes {

    private HttpServerMarshell httpServerMarshell;
    
    private RedisQueuePusher redisPushBencher;
    private RedisQueuePoper redisPopBencher;
    
    private DbPoolBencher dbPoolBencher;

    private static GlobalRes theInstance;
    private static ReentrantLock lock = null;

    static {
        lock = new ReentrantLock();
    }

    public static GlobalRes get() {
        if (theInstance != null) {
            return theInstance;
        }

        try {
            lock.lock();
            if (theInstance != null) {
                return theInstance;
            } else {
                theInstance = new GlobalRes();
            }
        } finally {
            lock.unlock();
        }

        return theInstance;
    }

    private GlobalRes() {
        initData();
    }

    private void initData() {
        List<Class<?>> handlers = new ArrayList<Class<?>>();
        handlers.add(BenchHandler.class);

        httpServerMarshell = new HttpServerMarshell(BenchConstants.SERVER_PORT, BenchConstants.USE_SSL,
                BenchConstants.SERVER_EVENT_GRP_SIZE, BenchConstants.SERVER_WORKER_SIZE, BenchConstants.TASK_TIMEOUT,
                handlers);
        httpServerMarshell.start();
    }

    public static void destroy() {
        if (theInstance.redisPushBencher != null) {
            theInstance.redisPushBencher.stop();
            theInstance.redisPushBencher = null;
        }
        
        if (theInstance.redisPopBencher != null) {
            theInstance.redisPopBencher.stop();
            theInstance.redisPopBencher = null;
        }
        
        if (theInstance.dbPoolBencher != null) {
            theInstance.dbPoolBencher.stop();
            theInstance.dbPoolBencher = null;
        }
        
        if (theInstance.httpServerMarshell != null) {
            theInstance.httpServerMarshell.destroy();
            theInstance.httpServerMarshell = null;
        }
        
        RedisInfoCollector.destroy();
    }

    public RedisQueuePusher getRedisPushBencher() {
        return redisPushBencher;
    }

    public void setRedisPushBencher(RedisQueuePusher redisPushBencher) {
        this.redisPushBencher = redisPushBencher;
    }

    public RedisQueuePoper getRedisPopBencher() {
        return redisPopBencher;
    }

    public void setRedisPopBencher(RedisQueuePoper redisPopBencher) {
        this.redisPopBencher = redisPopBencher;
    }
    
    public DbPoolBencher getDbPoolBencher() {
        return dbPoolBencher;
    }

    public void setDbPoolBencher(DbPoolBencher dbPoolBencher) {
        this.dbPoolBencher = dbPoolBencher;
    }
    
}
