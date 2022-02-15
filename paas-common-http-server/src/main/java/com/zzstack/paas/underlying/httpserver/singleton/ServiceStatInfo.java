package com.zzstack.paas.underlying.httpserver.singleton;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.zzstack.paas.underlying.httpserver.bean.StatBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ServiceStatInfo {

    private StatBean sum;

    private Map<String, StatBean> sub;

    private long privTs;
    private long currTs;

    private ScheduledExecutorService tpsCompteExec;
    private Runnable tpsRunner;

    private static ServiceStatInfo theInstance;
    private static Object mtx = null;

    static {
        mtx = new Object();
    }

    public static ServiceStatInfo get() {
        if (theInstance != null) {
            return theInstance;
        }

        synchronized (mtx) {
            if (theInstance == null) {
                theInstance = new ServiceStatInfo();
            }
        }

        return ServiceStatInfo.theInstance;
    }
    
    public static void release() {
        synchronized (mtx) {
            if (theInstance != null) {
                theInstance.tpsCompteExec.shutdownNow();
                theInstance = null;
            }
        }
    }

    private ServiceStatInfo() {
        sum = new StatBean();
        sub = new ConcurrentHashMap<String, StatBean>();

        this.privTs = System.currentTimeMillis();
        this.currTs = privTs;

        tpsRunner = new TPSCompteRunner(this);
        tpsCompteExec = Executors.newSingleThreadScheduledExecutor();
        tpsCompteExec.scheduleAtFixedRate(tpsRunner, CONSTS.STAT_COMPTE_INTERVAL, CONSTS.STAT_COMPTE_INTERVAL,
                TimeUnit.MILLISECONDS);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (tpsCompteExec != null) {
                    tpsCompteExec.shutdown();
                    tpsCompteExec = null;
                }
            }
        });
    }

    public void inc(String name) {
        sum.incCnt();

        StatBean statBean = sub.get(name);
        if (statBean == null) {
            statBean = new StatBean();
            sub.put(name, statBean);
        }
        statBean.incCnt();
    }

    public void computeTPS() {
        currTs = System.currentTimeMillis();
        long diffTS = currTs - privTs;
        if (diffTS <= 0L)
            return;

        sum.computeTPS(diffTS);// 总的计数

        Set<Entry<String, StatBean>> entrySet = sub.entrySet();// 分支计数
        for (Entry<String, StatBean> entry : entrySet) {
            StatBean statBean = entry.getValue();
            statBean.computeTPS(diffTS);
        }

        privTs = currTs;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.put("sum.tps", sum.getTps()); // 总的tps
        json.put("sum.count", sum.getCurrTotalCnt()); // 总的计数 count

        JsonArray jsonArr = new JsonArray();
        Set<Entry<String, StatBean>> entrySet = sub.entrySet();
        for (Entry<String, StatBean> entry : entrySet) {
            String key = entry.getKey();
            StatBean val = entry.getValue();

            JsonObject subJson = new JsonObject();
            subJson.put(key + ".tps", val.getTps());
            subJson.put(key + ".count", val.getCurrTotalCnt());

            jsonArr.add(subJson); // 分支tps
        }
        json.put("sub info", jsonArr);

        return json;
    }

    private class TPSCompteRunner implements Runnable {

        private ServiceStatInfo serviceStatInfo;

        public TPSCompteRunner(ServiceStatInfo serviceStatInfo) {
            this.serviceStatInfo = serviceStatInfo;
        }

        @Override
        public void run() {
            serviceStatInfo.computeTPS();
        }

    }

}
