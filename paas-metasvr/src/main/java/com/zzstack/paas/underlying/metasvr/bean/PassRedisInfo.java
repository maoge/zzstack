package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.FixHeader;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * 应用的redis的使用监控信息
 */
public class PassRedisInfo extends BeanMapper {
    /**
     * 时间戳
     */
    private Long ts;
    /**
     * 角色名,master\slave\cluster
     */
    private String role;
    /**
     * 组件实例ID或者组件集群ID
     */
    private String instantId;
    /**
     * 已连接客户端的数量（不包括通过从属服务器连接的客户端）
     */
    private Integer connectedClients;
    /**
     * 由 Redis 分配器分配的内存总量，以字节（byte）为单位
     */
    private Long usedMemory;
    /**
     * Redis可以使用的最大内存总量，以字节（byte）为单位
     */
    private Long maxMemory;
    /**
     * 每秒操作数
     */
    private Integer instantaneousOpsPerSec;
    /**
     * 输入带宽
     */
    private Double instantaneousInputKbps;
    /**
     * 输出带宽
     */
    private Double instantaneousOutputKbps;
    /**
     * 全量同步的次数
     */
    private Integer syncFull;
    /**
     * 运行以来过期的 key 的数量
     */
    private Long expiredKeys;
    /**
     * 运行以来删除过的key的数量
     */
    private Long evictedKeys;
    /**
     * 命中key 的次数
     */
    private Long keyspaceHits;
    /**
     * 没命中key 的次数
     */
    private Long keyspaceMisses;
    /**
     * 指令在 核心态（Kernel Mode）所消耗的CPU时间
     */
    private Double usedCpuSys;
    /**
     * 指令在 用户态（User Mode）所消耗的CPU时间
     */
    private Double usedCpuUser;

    public PassRedisInfo() {
        super();
    }

    public PassRedisInfo(Long ts, String role, String instantId, Integer connectedClients, Long usedMemory,
                         Long maxMemory, Integer instantaneousOpsPerSec, Double instantaneousInputKbps,
                         Double instantaneousOutputKbps, Integer syncFull, Long expiredKeys, Long evictedKeys,
                         Long keyspaceHits, Long keyspaceMisses, Double usedCpuSys, Double usedCpuUser) {
        this.ts = ts;
        this.role = role;
        this.instantId = instantId;
        this.connectedClients = connectedClients;
        this.usedMemory = usedMemory;
        this.maxMemory = maxMemory;
        this.instantaneousOpsPerSec = instantaneousOpsPerSec;
        this.instantaneousInputKbps = instantaneousInputKbps;
        this.instantaneousOutputKbps = instantaneousOutputKbps;
        this.syncFull = syncFull;
        this.expiredKeys = expiredKeys;
        this.evictedKeys = evictedKeys;
        this.keyspaceHits = keyspaceHits;
        this.keyspaceMisses = keyspaceMisses;
        this.usedCpuSys = usedCpuSys;
        this.usedCpuUser = usedCpuUser;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getInstantId() {
        return instantId;
    }

    public void setInstantId(String instantId) {
        this.instantId = instantId;
    }

    public Integer getConnectedClients() {
        return connectedClients;
    }

    public void setConnectedClients(Integer connectedClients) {
        this.connectedClients = connectedClients;
    }

    public Long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(Long usedMemory) {
        this.usedMemory = usedMemory;
    }

    public Long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(Long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public Integer getInstantaneousOpsPerSec() {
        return instantaneousOpsPerSec;
    }

    public void setInstantaneousOpsPerSec(Integer instantaneousOpsPerSec) {
        this.instantaneousOpsPerSec = instantaneousOpsPerSec;
    }

    public Double getInstantaneousInputKbps() {
        return instantaneousInputKbps;
    }

    public void setInstantaneousInputKbps(Double instantaneousInputKbps) {
        this.instantaneousInputKbps = instantaneousInputKbps;
    }

    public Double getInstantaneousOutputKbps() {
        return instantaneousOutputKbps;
    }

    public void setInstantaneousOutputKbps(Double instantaneousOutputKbps) {
        this.instantaneousOutputKbps = instantaneousOutputKbps;
    }

    public Integer getSyncFull() {
        return syncFull;
    }

    public void setSyncFull(Integer syncFull) {
        this.syncFull = syncFull;
    }

    public Long getExpiredKeys() {
        return expiredKeys;
    }

    public void setExpiredKeys(Long expiredKeys) {
        this.expiredKeys = expiredKeys;
    }

    public Long getEvictedKeys() {
        return evictedKeys;
    }

    public void setEvictedKeys(Long evictedKeys) {
        this.evictedKeys = evictedKeys;
    }

    public Long getKeyspaceHits() {
        return keyspaceHits;
    }

    public void setKeyspaceHits(Long keyspaceHits) {
        this.keyspaceHits = keyspaceHits;
    }

    public Long getKeyspaceMisses() {
        return keyspaceMisses;
    }

    public void setKeyspaceMisses(Long keyspaceMisses) {
        this.keyspaceMisses = keyspaceMisses;
    }

    public Double getUsedCpuSys() {
        return usedCpuSys;
    }

    public void setUsedCpuSys(Double usedCpuSys) {
        this.usedCpuSys = usedCpuSys;
    }

    public Double getUsedCpuUser() {
        return usedCpuUser;
    }

    public void setUsedCpuUser(Double usedCpuUser) {
        this.usedCpuUser = usedCpuUser;
    }

    public static PassRedisInfo convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty()) {
            return null;
        }
        long ts = getFixDataAsLong(mapper, FixHeader.HEADER_TS);
        String role = getFixDataAsString(mapper, FixHeader.HEADER_ROLE);
        String instantId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        int connectedClients = getFixDataAsInt(mapper, FixHeader.HEADER_CONNECTED_CLIENTS);
        long usedMemory = getFixDataAsLong(mapper, FixHeader.HEADER_USED_MEMORY);
        long maxMemory = getFixDataAsLong(mapper, FixHeader.HEADER_MAXMEMORY);
        int instantaneousOpsPerSec = getFixDataAsInt(mapper, FixHeader.HEADER_INSTANTANEOUS_OPS_PER_SEC);
        double instantaneousInputKbps = getFixDataAsDouble(mapper, FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS);
        double instantaneousOutputKbps = getFixDataAsDouble(mapper, FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS);
        int syncFull = getFixDataAsInt(mapper, FixHeader.HEADER_SYNC_FULL);
        long expiredKeys = getFixDataAsLong(mapper, FixHeader.HEADER_EXPIRED_KEYS);
        long evictedKeys = getFixDataAsLong(mapper, FixHeader.HEADER_EVICTED_KEYS);
        long keyspaceHits = getFixDataAsLong(mapper, FixHeader.HEADER_KEYSPACE_HITS);
        long keyspaceMisses = getFixDataAsLong(mapper, FixHeader.HEADER_KEYSPACE_MISSES);
        double usedCpuSys = getFixDataAsDouble(mapper, FixHeader.HEADER_USED_CPU_SYS);
        double usedCpuUser = getFixDataAsDouble(mapper, FixHeader.HEADER_USED_CPU_USER);
        return new PassRedisInfo(ts, role, instantId, connectedClients, usedMemory, maxMemory, instantaneousOpsPerSec,
                instantaneousInputKbps, instantaneousOutputKbps, syncFull, expiredKeys, evictedKeys, keyspaceHits,
                keyspaceMisses, usedCpuSys, usedCpuUser);
    }

    public JsonObject toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_TS, this.ts);
        retval.put(FixHeader.HEADER_ROLE, this.role);
        retval.put(FixHeader.HEADER_INST_ID, this.instantId);
        retval.put(FixHeader.HEADER_CONNECTED_CLIENTS, this.connectedClients);
        retval.put(FixHeader.HEADER_USED_MEMORY, this.usedMemory);
        retval.put(FixHeader.HEADER_MAXMEMORY, this.maxMemory);
        retval.put(FixHeader.HEADER_INSTANTANEOUS_OPS_PER_SEC, this.instantaneousOpsPerSec);
        retval.put(FixHeader.HEADER_INSTANTANEOUS_INPUT_KBPS, this.instantaneousInputKbps);
        retval.put(FixHeader.HEADER_INSTANTANEOUS_OUTPUT_KBPS, this.instantaneousOutputKbps);
        retval.put(FixHeader.HEADER_SYNC_FULL, this.syncFull);
        retval.put(FixHeader.HEADER_EXPIRED_KEYS, this.expiredKeys);
        retval.put(FixHeader.HEADER_EVICTED_KEYS, this.evictedKeys);
        retval.put(FixHeader.HEADER_KEYSPACE_HITS, this.keyspaceHits);
        retval.put(FixHeader.HEADER_KEYSPACE_MISSES, this.keyspaceMisses);
        retval.put(FixHeader.HEADER_USED_CPU_SYS, this.usedCpuSys);
        retval.put(FixHeader.HEADER_USED_CPU_USER, this.usedCpuUser);
        return retval;
    }

}
