package com.zzstack.paas.underlying.collectd.probe;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.collectd.bean.LastRedisInfo;
import com.zzstack.paas.underlying.collectd.global.CollectdGlobalData;
import com.zzstack.paas.underlying.sdk.PaasSDK;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.JedisUtil;
import com.zzstack.paas.underlying.utils.exception.PaasCollectException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisClusterProber implements Prober {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterProber.class);
    private JedisCluster jedisCluster = null;
    private final Map<String, LastRedisInfo> redisClusterCpuMap = new HashMap<>();

    private JsonObject jsonToReport = null;

    @Override
    public void doCollect(JsonObject topoJson) {
        JsonObject redisClusterServJson = topoJson.getJsonObject(FixHeader.HEADER_REDIS_SERV_CLUSTER_CONTAINER);
        String clusterId = redisClusterServJson.getString(FixHeader.HEADER_INST_ID);
        JsonObject redisNodeContainJson = redisClusterServJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeJson = redisNodeContainJson.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        StringBuilder clusterAddrStr = new StringBuilder();
        Map<String, String> instMap = new HashMap<>();
        redisNodeJson.forEach(redisNode -> {
            JsonObject redisNodeObj = (JsonObject) redisNode;
            String instId = redisNodeObj.getString(FixHeader.HEADER_INST_ID);
            String hostAndPort = String.format("%s%s%s", redisNodeObj.getString(FixHeader.HEADER_IP), ":", redisNodeObj.getString(FixHeader.HEADER_PORT));
            clusterAddrStr.append(hostAndPort).append(",");
            instMap.put(hostAndPort, instId);
        });
        String clusterAddr = clusterAddrStr.toString();
        if (clusterAddr.endsWith(",")) {
            clusterAddr = clusterAddr.substring(0, clusterAddr.length() - 1);
        }
        
        try {
            JedisUtil.setRedisConnPoolMin(1);
            JedisUtil.setRedisConnPoolMax(1);
            jedisCluster = JedisUtil.getPool(clusterAddr);
            
            if (jedisCluster != null) {
                collectClusterInfo(clusterId, instMap);
            }
        } catch (PaasSdkException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (jedisCluster != null) {
                jedisCluster.close();
                jedisCluster = null;
            }
        }
    }

    @Override
    public void doReport() throws PaasSdkException {
        if (jsonToReport != null) {
            PaasSDK paasSdk = CollectdGlobalData.get().getSdk();
            Map<String, String> postHeads = new HashMap<>();
            postHeads.put("CONTENT-TYPE", "application/json");
            paasSdk.postCollectData("/paas/statistic/saveRedisInfo", postHeads, jsonToReport.toString());
        }
    }

    @Override
    public void doAlarm() throws PaasCollectException {
        // TODO Auto-generated method stub

    }

    @Override
    public void doRecover() throws PaasCollectException {
        // TODO Auto-generated method stub

    }

    public void collectClusterInfo(String clusterId, Map<String, String> instMap) throws PaasSdkException {
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        // 组装redis监控数据
        JsonObject redisInfo = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        long currentStamp = System.currentTimeMillis();
        Map<String, Integer> intMap = new HashMap<>();
        Map<String, Long> longMap = new HashMap<>();
        Map<String, BigDecimal> bigDecimalMap = new HashMap<>();
        for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
            // redis list operates on master node
            try (Jedis jedis = entry.getValue().getResource()) {
                String info = jedis.info();
                String instId = instMap.get(entry.getKey());
                // 提取字符串中redis的关键指标
                infoTransformRedis(info, currentStamp, instId, jsonArray, intMap, longMap, bigDecimalMap);
            }
        }
        int roundingMode = BigDecimal.ROUND_DOWN;
        BigDecimal bigDecimalZero = BigDecimal.ZERO;
        JSONObject redis = new JSONObject();
        redis.put(FixHeader.HEADER_TS, currentStamp);
        // 集群ID
        redis.put(FixHeader.HEADER_INST_ID, clusterId);
        redis.put(FixHeader.HEADER_ROLE, "cluster");
        redis.put(FixHeader.HEADER_CONNECTED_CLIENTS, intMap.getOrDefault(FixHeader.HEADER_CONNECTED_CLIENTS, 0));
        redis.put(FixHeader.HEADER_USED_MEMORY, longMap.getOrDefault(FixHeader.HEADER_USED_MEMORY, 0L));
        redis.put(FixHeader.HEADER_MAXMEMORY, longMap.getOrDefault(FixHeader.HEADER_MAXMEMORY, 0L));
        redis.put(FixHeader.HEADER_INSTANTANEOUS_OPS_PER_SEC, intMap.getOrDefault(FixHeader.HEADER_INSTANTANEOUS_OPS_PER_SEC, 0));
        double inputKbps = bigDecimalMap.getOrDefault(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS, bigDecimalZero)
                .setScale(2, roundingMode).doubleValue();
        redis.put(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS, inputKbps);
        double outputKbps = bigDecimalMap.getOrDefault(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS, bigDecimalZero)
                .setScale(2, roundingMode).doubleValue();
        redis.put(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS, outputKbps);
        redis.put(FixHeader.HEADER_SYNC_FULL, intMap.getOrDefault(FixHeader.HEADER_SYNC_FULL, 0));
        redis.put(FixHeader.HEADER_EXPIRED_KEYS, longMap.getOrDefault(FixHeader.HEADER_EXPIRED_KEYS, 0L));
        redis.put(FixHeader.HEADER_EVICTED_KEYS, longMap.getOrDefault(FixHeader.HEADER_EVICTED_KEYS, 0L));
        redis.put(FixHeader.HEADER_KEYSPACE_HITS, longMap.getOrDefault(FixHeader.HEADER_KEYSPACE_HITS, 0L));
        redis.put(FixHeader.HEADER_KEYSPACE_MISSES, longMap.getOrDefault(FixHeader.HEADER_KEYSPACE_MISSES, 0L));
        int nodesSize = clusterNodes.size();
        double usedCpuSys = 0.0d;
        double usedCpuUser = 0.0d;
        if (nodesSize > 0) {
            usedCpuSys = bigDecimalMap.getOrDefault(FixHeader.HEADER_USED_CPU_SYS, bigDecimalZero)
                    .divide(BigDecimal.valueOf(nodesSize), roundingMode).setScale(2, roundingMode).doubleValue();
            usedCpuUser = bigDecimalMap.getOrDefault(FixHeader.HEADER_USED_CPU_USER, bigDecimalZero)
                    .divide(BigDecimal.valueOf(nodesSize), roundingMode).setScale(2, roundingMode).doubleValue();
        }
        redis.put(FixHeader.HEADER_USED_CPU_SYS, usedCpuSys);
        redis.put(FixHeader.HEADER_USED_CPU_USER, usedCpuUser);
        jsonArray.add(redis);
        redisInfo.put(FixHeader.HEADER_REDIS_INFO_JSON_ARRAY, jsonArray);
        jsonToReport = new JsonObject();
        jsonToReport.put(FixHeader.HEADER_REDIS_DATA, redisInfo);
    }

    private void infoTransformRedis(String info, long currentStamp, String instId, JsonArray jsonArray,
                                    Map<String, Integer> intMap, Map<String, Long> longMap,
                                    Map<String, BigDecimal> bigDecimalMap) {
        JSONObject redis = new JSONObject();
        redis.put(FixHeader.HEADER_TS, currentStamp);
        // 应用的实例ID
        redis.put(FixHeader.HEADER_INST_ID, instId);
        String role = getParamValue(info, "role");
        redis.put(FixHeader.HEADER_ROLE, role);
        String connectedClients = getParamValue(info, "connected_clients");
        redis.put(FixHeader.HEADER_CONNECTED_CLIENTS, connectedClients);
        String usedMemory = getParamValue(info, "used_memory");
        redis.put(FixHeader.HEADER_USED_MEMORY, usedMemory);
        String maxmemory = getParamValue(info, "maxmemory");
        redis.put(FixHeader.HEADER_MAXMEMORY, maxmemory);
        String instantaneousOpsPerSec = getParamValue(info, "instantaneous_ops_per_sec");
        redis.put(FixHeader.HEADER_INSTANTANEOUS_OPS_PER_SEC, instantaneousOpsPerSec);
        String instantaneousInputKbps = getParamValue(info, "instantaneous_input_kbps");
        redis.put(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS, instantaneousInputKbps);
        String instantaneousOutputKbps = getParamValue(info, "instantaneous_output_kbps");
        redis.put(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS, instantaneousOutputKbps);
        String syncFull = getParamValue(info, "sync_full");
        redis.put(FixHeader.HEADER_SYNC_FULL, syncFull);
        String expiredKeys = getParamValue(info, "expired_keys");
        redis.put(FixHeader.HEADER_EXPIRED_KEYS, expiredKeys);
        String evictedKeys = getParamValue(info, "evicted_keys");
        redis.put(FixHeader.HEADER_EVICTED_KEYS, evictedKeys);
        String keyspaceHits = getParamValue(info, "keyspace_hits");
        redis.put(FixHeader.HEADER_KEYSPACE_HITS, keyspaceHits);
        String keyspaceMisses = getParamValue(info, "keyspace_misses");
        redis.put(FixHeader.HEADER_KEYSPACE_MISSES, keyspaceMisses);
        // 需要做些特殊的处理,默认redis中收集的指标是时长 计算公式 (used_cpu_sys_now-used_cpu_sys_before)/(now-before)*100
        LastRedisInfo lastRedisInfo = redisClusterCpuMap.get(instId);
        String usedCpuSysStr = getParamValue(info, "used_cpu_sys");
        if (StringUtils.isBlank(usedCpuSysStr)) {
            usedCpuSysStr = "0.0";
        }
        String usedCpuUserStr = getParamValue(info, "used_cpu_user");
        if (StringUtils.isBlank(usedCpuUserStr)) {
            usedCpuUserStr = "0.0";
        }
        double usedCpuSys = 0.0d;
        double usedCpuUser = 0.0d;
        if (lastRedisInfo == null) {
            if (StringUtils.isNotBlank(usedCpuSysStr) && StringUtils.isNotBlank(usedCpuUserStr)) {
                lastRedisInfo = new LastRedisInfo();
                lastRedisInfo.setLastCollect(currentStamp);
                lastRedisInfo.setLastUsedCpuSys(Double.parseDouble(usedCpuSysStr));
                lastRedisInfo.setLastUsedCpuUser(Double.parseDouble(usedCpuUserStr));
            }
            redis.put(FixHeader.HEADER_USED_CPU_SYS, String.valueOf(usedCpuSys));
            redis.put(FixHeader.HEADER_USED_CPU_USER, String.valueOf(usedCpuUser));
        } else {
            Long lastCollect = lastRedisInfo.getLastCollect();
            Double lastUsedCpuSys = lastRedisInfo.getLastUsedCpuSys();
            Double lastUsedCpuUser = lastRedisInfo.getLastUsedCpuUser();
            long consumeTime = (currentStamp - lastCollect) / 1000;
            Double usedCpuSysd = Double.parseDouble(usedCpuSysStr);
            double consumeUsedCpuSys = usedCpuSysd - lastUsedCpuSys;
            BigDecimal usedCpuSysB = BigDecimal.ZERO;
            if (consumeUsedCpuSys > 0) {
                usedCpuSys = (consumeUsedCpuSys / consumeTime) * 100;
                usedCpuSysB = BigDecimal.valueOf(usedCpuSys).setScale(3, RoundingMode.DOWN);
            }
            Double usedCpuUserd = Double.parseDouble(usedCpuUserStr);
            double consumeUsedCpuUser = Double.parseDouble(usedCpuUserStr) - lastUsedCpuUser;
            BigDecimal usedCpuUserB = BigDecimal.ZERO;;
            if (consumeUsedCpuUser > 0) {
                usedCpuUser = (consumeUsedCpuUser / consumeTime) * 100;
                usedCpuUserB = BigDecimal.valueOf(usedCpuUser).setScale(3, RoundingMode.DOWN);
            }
            redis.put(FixHeader.HEADER_USED_CPU_SYS, usedCpuSysB.toString());
            redis.put(FixHeader.HEADER_USED_CPU_USER, usedCpuUserB.toString());
            lastRedisInfo.setLastCollect(currentStamp);
            lastRedisInfo.setLastUsedCpuSys(usedCpuSysd);
            lastRedisInfo.setLastUsedCpuUser(usedCpuUserd);
        }
        if (lastRedisInfo != null) {
            redisClusterCpuMap.put(instId, lastRedisInfo);
        }
        jsonArray.add(redis);
        if (StringUtils.isNotBlank(connectedClients)) {
            intMap.merge(FixHeader.HEADER_CONNECTED_CLIENTS, Integer.parseInt(connectedClients), Integer::sum);
        }
        if (StringUtils.isNotBlank(usedMemory)) {
            longMap.merge(FixHeader.HEADER_USED_MEMORY, Long.parseLong(usedMemory), Long::sum);
        }
        if (StringUtils.isNotBlank(maxmemory)) {
            longMap.merge(FixHeader.HEADER_MAXMEMORY, Long.parseLong(maxmemory), Long::sum);
        }
        if (StringUtils.isNotBlank(instantaneousInputKbps)) {
            bigDecimalMap.merge(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS,
                    BigDecimal.valueOf(Double.parseDouble(instantaneousInputKbps)), BigDecimal::add);
        }
        if (StringUtils.isNotBlank(instantaneousOutputKbps)) {
            bigDecimalMap.merge(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS,
                    BigDecimal.valueOf(Double.parseDouble(instantaneousOutputKbps)), BigDecimal::add);
        }
        if (StringUtils.isNotBlank(syncFull)) {
            intMap.merge(FixHeader.HEADER_SYNC_FULL, Integer.parseInt(syncFull), Integer::sum);
        }
        if (StringUtils.isNotBlank(expiredKeys)) {
            longMap.merge(FixHeader.HEADER_EXPIRED_KEYS, Long.parseLong(expiredKeys), Long::sum);
        }
        if (StringUtils.isNotBlank(evictedKeys)) {
            longMap.merge(FixHeader.HEADER_EVICTED_KEYS, Long.parseLong(evictedKeys), Long::sum);
        }
        if (StringUtils.isNotBlank(keyspaceHits)) {
            longMap.merge(FixHeader.HEADER_KEYSPACE_HITS, Long.parseLong(keyspaceHits), Long::sum);
        }
        if (StringUtils.isNotBlank(keyspaceMisses)) {
            longMap.merge(FixHeader.HEADER_KEYSPACE_MISSES, Long.parseLong(keyspaceMisses), Long::sum);
        }
        bigDecimalMap.merge(FixHeader.HEADER_USED_CPU_SYS,BigDecimal.valueOf(usedCpuSys), BigDecimal::add);
        bigDecimalMap.merge(FixHeader.HEADER_USED_CPU_USER,BigDecimal.valueOf(usedCpuUser), BigDecimal::add);
    }

    private static String getParamValue(String originStr, String matchStr) {
        Pattern pattern = Pattern.compile(matchStr + ":(\\S+)\r\n");
        Matcher matcher = pattern.matcher(originStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}
