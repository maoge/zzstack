package com.zzstack.paas.underlying.collect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.sdk.PaasSDK;
import com.zzstack.paas.underlying.utils.YamlParser;
import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf.RedisHaConfig;
import com.zzstack.paas.underlying.utils.config.RedisNodes;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RedisInfoCollector {

    private static final long COLLECT_INTERVAL = 5000L;

    private final ScheduledExecutorService collectScheduler;
    private Runnable scheduleRunner;
    private List<RedisClusterProbe> redisClusterProbes;
    private static ReentrantLock lock;
    private static RedisInfoCollector theInstance;

    static {
        lock = new ReentrantLock();
    }

    public static RedisInfoCollector get() {
        if (theInstance != null) {
            return theInstance;
        }
        lock.lock();
        try {
            if (theInstance != null) {
                return theInstance;
            } else {
                theInstance = new RedisInfoCollector();
            }
        } finally {
            lock.unlock();
        }
        return theInstance;
    }

    public static RedisInfoCollector getFromConfig(String pushUrl, String clusterAInstId, String clusterBInstId,
                                                   long collectInterval) throws PaasSdkException {
        if (theInstance != null) {
            return theInstance;
        }
        lock.lock();
        try {
            if (theInstance != null) {
                return theInstance;
            } else {
                theInstance = new RedisInfoCollector(pushUrl, clusterAInstId, clusterBInstId, collectInterval);
            }
        } finally {
            lock.unlock();
        }
        return theInstance;
    }

    private RedisInfoCollector() {
        init();
        this.scheduleRunner = new ScheduleRunner(this.redisClusterProbes);
        this.collectScheduler = Executors.newSingleThreadScheduledExecutor();
        this.collectScheduler.scheduleAtFixedRate(this.scheduleRunner, COLLECT_INTERVAL, COLLECT_INTERVAL, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(collectScheduler::shutdown));
    }

    private RedisInfoCollector(String pushUrl, String clusterAInstId, String clusterBInstId, long collectInterval) throws PaasSdkException {
        init(pushUrl, clusterAInstId, clusterBInstId);
        this.scheduleRunner = new ScheduleRunner(this.redisClusterProbes);
        this.collectScheduler = Executors.newSingleThreadScheduledExecutor();
        this.collectScheduler.scheduleAtFixedRate(this.scheduleRunner, COLLECT_INTERVAL, collectInterval, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(collectScheduler::shutdown));
    }

    private void init() {
        String yamlFile = String.format("conf/%s.yaml", BenchConstants.REDIS_CONF_FILE);
        YamlParser parser = new YamlParser(yamlFile);
        CacheRedisHaConf redissonConf = (CacheRedisHaConf) parser.parseObject(CacheRedisHaConf.class);
        RedisHaConfig redisConf = redissonConf.redisConfig;
        RedisNodes serverA = redisConf.serverA;
        RedisNodes serverB = redisConf.serverB;
        RedisClusterProbe redisClusterProbeA = new RedisClusterProbe(serverA);
        RedisClusterProbe redisClusterProbeB = new RedisClusterProbe(serverB);
        redisClusterProbes = new ArrayList<>();
        redisClusterProbes.add(redisClusterProbeA);
        redisClusterProbes.add(redisClusterProbeB);
    }

    private void init(String pushUrl, String clusterAInstId, String clusterBInstId) throws PaasSdkException {
        String metasvrUrls = "http://172.16.2.44:9090";
        String user = "";
        String passwd = "";
        PaasSDK paasSDK = new PaasSDK(metasvrUrls, user, passwd);
        String yamlFile = String.format("conf/%s.yaml", BenchConstants.REDIS_CONF_FILE);
        YamlParser parser = new YamlParser(yamlFile);
        CacheRedisHaConf redissonConf = (CacheRedisHaConf) parser.parseObject(CacheRedisHaConf.class);
        RedisHaConfig redisConf = redissonConf.redisConfig;
        RedisNodes serverA = redisConf.serverA;
        String paasServiceATopo = paasSDK.loadPaasInstance(clusterAInstId);
        JSONObject cacheRedisClusterConfA = JSON.parseObject(paasServiceATopo);
        JSONArray redisNodesAJsonArray = cacheRedisClusterConfA.getJSONObject("REDIS_SERV_CLUSTER_CONTAINER")
                .getJSONObject("REDIS_NODE_CONTAINER").getJSONArray("REDIS_NODE");
        String[] redisNodesA = new String[redisNodesAJsonArray.size()];
        Map<String, String> instMapA = new HashMap<>();
        for (int i = 0; i < redisNodesAJsonArray.size(); i++) {
            JSONObject jsb = (JSONObject) redisNodesAJsonArray.get(i);
            String redisAddr = "redis://" + jsb.get("IP") + ":" + jsb.get("PORT");
            redisNodesA[i] = redisAddr;
            instMapA.put(String.format("%s:%s", jsb.get("IP"), jsb.get("PORT")), String.valueOf(jsb.get("INST_ID")));
        }
        serverA.setNodeAddresses(redisNodesA);
        serverA.setInstMap(instMapA);
        serverA.instId = clusterAInstId;
        String paasServiceBTopo = paasSDK.loadPaasInstance(clusterBInstId);
        JSONObject cacheRedisClusterConfB = JSON.parseObject(paasServiceBTopo);
        JSONArray redisNodesBJsonArray = cacheRedisClusterConfB.getJSONObject("REDIS_SERV_CLUSTER_CONTAINER")
                .getJSONObject("REDIS_NODE_CONTAINER").getJSONArray("REDIS_NODE");
        String[] redisNodesB = new String[redisNodesBJsonArray.size()];
        Map<String, String> instMapB = new HashMap<>();
        RedisNodes serverB = redisConf.serverB;
        for (int j = 0; j < redisNodesBJsonArray.size(); j++) {
            JSONObject jsb = (JSONObject) redisNodesBJsonArray.get(j);
            String redisBddr = "redis://" + jsb.get("IP") + ":" + jsb.get("PORT");
            redisNodesB[j] = redisBddr;
            instMapB.put(String.format("%s:%s", jsb.get("IP"), jsb.get("PORT")), String.valueOf(jsb.get("INST_ID")));
        }
        serverB.setNodeAddresses(redisNodesB);
        serverB.setInstMap(instMapB);
        serverB.instId = clusterBInstId;
        RedisClusterProbe redisClusterProbeA = new RedisClusterProbe(serverA, pushUrl);
        RedisClusterProbe redisClusterProbeB = new RedisClusterProbe(serverB, pushUrl);
        redisClusterProbes = new ArrayList<>();
        redisClusterProbes.add(redisClusterProbeA);
        redisClusterProbes.add(redisClusterProbeB);
    }


    public static void destroy() {
        if (theInstance == null) {
            return;
        }
        lock.lock();
        try {
            for (RedisClusterProbe probe : theInstance.redisClusterProbes) {
                probe.destroy();
            }
            theInstance.redisClusterProbes.clear();
            theInstance = null;
        } finally {
            lock.unlock();
        }
    }

    public String getCollectInfo() {
        StringBuilder sb = new StringBuilder();
        for (RedisClusterProbe probe : redisClusterProbes) {
            String info = String.format("%s instant_ops:%d", probe.getClusterName(), probe.getOps());
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(info);
        }
        return sb.toString();
    }

}
