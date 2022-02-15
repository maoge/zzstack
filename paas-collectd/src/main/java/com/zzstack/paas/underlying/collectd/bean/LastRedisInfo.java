package com.zzstack.paas.underlying.collectd.bean;

public class LastRedisInfo {
    private Long lastCollect;
    private Double lastUsedCpuSys;
    private Double lastUsedCpuUser;

    public Long getLastCollect() {
        return lastCollect;
    }

    public void setLastCollect(Long lastCollect) {
        this.lastCollect = lastCollect;
    }

    public Double getLastUsedCpuSys() {
        return lastUsedCpuSys;
    }

    public void setLastUsedCpuSys(Double lastUsedCpuSys) {
        this.lastUsedCpuSys = lastUsedCpuSys;
    }

    public Double getLastUsedCpuUser() {
        return lastUsedCpuUser;
    }

    public void setLastUsedCpuUser(Double lastUsedCpuUser) {
        this.lastUsedCpuUser = lastUsedCpuUser;
    }
}
