package com.zzstack.paas.underlying.collect;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.JedisUtil;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import com.zzstack.paas.underlying.utils.config.RedisNodes;
import com.zzstack.paas.underlying.utils.jvm.JVMMemoryUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedisClusterProbe {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterProbe.class);

    private static String REDIS_PROTOCOL_PREFIX = "redis://";

    private JedisCluster jedisCluster;

    private String clusterName;

    private volatile int ops;
    private int addrIdx = 0;
    private int addrCnt;
    private List<String> pushAddrs;
    private static Map<String, String> POST_HEADS;

    static {
        POST_HEADS = new HashMap<>();
        POST_HEADS.put("CONTENT-TYPE", "application/json");
    }

    public RedisClusterProbe(RedisNodes serverNodes) {
        this.clusterName = serverNodes.id;
        init(serverNodes);
    }

    public RedisClusterProbe(RedisNodes serverNodes, String pushUrl) {
        this.clusterName = serverNodes.id;
        init(serverNodes, pushUrl);
    }

    private void init(RedisNodes serverNodes) {
        String[] addressList = serverNodes.nodeAddresses;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addressList.length; ++i) {
            String address = addressList[i];
            int idx = address.indexOf(REDIS_PROTOCOL_PREFIX);
            String redisAddr = address.substring(idx + REDIS_PROTOCOL_PREFIX.length());
            if (i > 0) {
                sb.append(",");
            }
            sb.append(redisAddr);
        }
        String clusterAddr = sb.toString();
        jedisCluster = JedisUtil.getPool(clusterAddr);
    }

    private void init(RedisNodes serverNodes, String pushUrl) {
        String[] addressList = serverNodes.nodeAddresses;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addressList.length; ++i) {
            String address = addressList[i];
            int idx = address.indexOf(REDIS_PROTOCOL_PREFIX);
            String redisAddr = address.substring(idx + REDIS_PROTOCOL_PREFIX.length());
            if (i > 0) {
                sb.append(",");
            }
            sb.append(redisAddr);
        }
        String clusterAddr = sb.toString();
        jedisCluster = JedisUtil.getPool(clusterAddr);
        String[] addrs = pushUrl.split(",");
        addrCnt = addrs.length;
        pushAddrs = new ArrayList<>(addrCnt);
        for (String addr : addrs) {
            if (addr == null || addr.isEmpty()) {
                continue;
            }
            pushAddrs.add(addr);
        }
        collectClusterInstantInfoFromConfig(serverNodes, pushUrl);
    }

    public void destroy() {
        if (jedisCluster != null) {
            jedisCluster.close();
            jedisCluster = null;
        }
    }

    public int collectClusterInstantOps() {
        int result = 0;
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
            Jedis jedis = entry.getValue().getResource();
            // redis list operates on master node
            try {
                if (!jedis.info("replication").contains("role:slave")) {
                    String infoStats = jedis.info(BenchConstants.REDIS_INFO_STATS);
                    result += getInstantOps(infoStats);
                }
            } finally {
                jedis.close();
            }
        }
        ops = result;
        return result;
    }

    public void collectClusterInstantInfoFromConfig(RedisNodes serverNodes, String pushUrl) {
        Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
        // 组装redis监控数据
        JSONObject redisInfo = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        long currentStamp = System.currentTimeMillis();
        Map<String, Integer> intMap = new HashMap<>();
        Map<String, Long> longMap = new HashMap<>();
        Map<String, BigDecimal> bigDecimalMap = new HashMap<>();
        String clusterId = serverNodes.instId;
        Map<String, String> instMap = serverNodes.getInstMap();
        for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
            // redis list operates on master node
            try (Jedis jedis = entry.getValue().getResource()) {
                String info = jedis.info();
                JSONObject redis = new JSONObject();
                redis.put(FixHeader.HEADER_TS, currentStamp);
                // 应用的实例ID
                redis.put(FixHeader.HEADER_INST_ID, instMap.get(entry.getKey()));
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
                String usedCpuSys = getParamValue(info, "used_cpu_sys");
                redis.put(FixHeader.HEADER_USED_CPU_SYS, usedCpuSys);
                String usedCpuUser = getParamValue(info, "used_cpu_user");
                redis.put(FixHeader.HEADER_USED_CPU_USER, usedCpuUser);
                JVMMemoryUtils.getEdenSpaceInfo(redis);
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
                    bigDecimalMap.merge(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS, BigDecimal.valueOf(Double.parseDouble(instantaneousInputKbps)), BigDecimal::add);
                }
                if (StringUtils.isNotBlank(instantaneousOutputKbps)) {
                    bigDecimalMap.merge(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS, BigDecimal.valueOf(Double.parseDouble(instantaneousOutputKbps)), BigDecimal::add);
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
                if (StringUtils.isNotBlank(usedCpuSys)) {
                    bigDecimalMap.merge(FixHeader.HEADER_USED_CPU_SYS, BigDecimal.valueOf(Double.parseDouble(usedCpuSys)), BigDecimal::add);
                }
                if (StringUtils.isNotBlank(usedCpuUser)) {
                    bigDecimalMap.merge(FixHeader.HEADER_USED_CPU_USER, BigDecimal.valueOf(Double.parseDouble(usedCpuUser)), BigDecimal::add);
                }
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
        JSONObject root = new JSONObject();
        root.put(FixHeader.HEADER_REDIS_DATA, redisInfo);
        // 聚合Redis集群的信息
        if (!pushAddrs.isEmpty()) {
            int idx = getAddrIdx();
            String addr = pushAddrs.get(idx);
            SVarObject var = new SVarObject();
            try {
                if (!HttpCommonTools.postData(addr, POST_HEADS, root.toJSONString(), var)) {
                    logger.error("上传Redis收集信息异常:{}", var.getVal());
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private int getAddrIdx() {
        if (addrCnt <= 1) {
            return 0;
        }
        return (addrIdx++) % addrCnt;
    }

    private static String getParamValue(String originStr, String matchStr) {
        Pattern pattern = Pattern.compile(matchStr + ":(\\S+)\r\n");
        Matcher matcher = pattern.matcher(originStr);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int getInstantOps(String infoStats) {
//      total_connections_received:3661
//      total_commands_processed:16940855
//      instantaneous_ops_per_sec:7578
//      total_net_input_bytes:691658863
//      total_net_output_bytes:1016234804
//      instantaneous_input_kbps:303.32
//      instantaneous_output_kbps:439.26
//      rejected_connections:0
//      sync_full:1
//      sync_partial_ok:0
//      sync_partial_err:1
//      expired_keys:116809
//      expired_stale_perc:0.00
//      expired_time_cap_reached_count:0
//      evicted_keys:0
//      keyspace_hits:26
//      keyspace_misses:31
//      pubsub_channels:0
//      pubsub_patterns:0
//      latest_fork_usec:379
//      migrate_cached_sockets:0
//      slave_expires_tracked_keys:0
//      active_defrag_hits:0
//      active_defrag_misses:0
//      active_defrag_key_hits:0
//      active_defrag_key_misses:0

        int beg = infoStats.indexOf(BenchConstants.REDIS_INFO_INSTANT_OPS);
        int end = infoStats.indexOf(BenchConstants.REDIS_LINE_SEPARATOR, beg);
        String ops = infoStats.substring(beg + BenchConstants.REDIS_INFO_INSTANT_OPS.length(), end);

        int result = 0;
        try {
//            if (clusterName.equals("B")) {
//                System.out.println(ops);
//            }

            result = Integer.parseInt(ops);
        } catch (NumberFormatException e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public String getClusterName() {
        return clusterName;
    }

    public int getOps() {
        return ops;
    }

}
