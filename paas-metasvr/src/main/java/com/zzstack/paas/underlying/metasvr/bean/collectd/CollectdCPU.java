package com.zzstack.paas.underlying.metasvr.bean.collectd;

import com.zzstack.paas.underlying.metasvr.dataservice.dao.HostStatisticDao;

public class CollectdCPU implements ICollectd {

    private String servIP;
    private long   ts;
    private float  userCPU;
    private float  systemCPU;
    private float  waitCPU;
    private float  interruptCPU;
    private float  idleCPU;
    private float  niceCPU;
    private float  softirqCPU;
    private float  stealCPU;
    
    public CollectdCPU(String servIP, long ts, float userCPU, float systemCPU, float waitCPU, float interruptCPU,
            float idleCPU, float niceCPU, float softirqCPU, float stealCPU) {
        super();
        this.servIP = servIP;
        this.ts = ts;
        this.userCPU = userCPU;
        this.systemCPU = systemCPU;
        this.waitCPU = waitCPU;
        this.interruptCPU = interruptCPU;
        this.idleCPU = idleCPU;
        this.niceCPU = niceCPU;
        this.softirqCPU = softirqCPU;
        this.stealCPU = stealCPU;
    }

    public String getServIP() {
        return servIP;
    }

    public void setServIP(String servIP) {
        this.servIP = servIP;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    public float getUserCPU() {
        return userCPU;
    }

    public void setUserCPU(float userCPU) {
        this.userCPU = userCPU;
    }

    public float getSystemCPU() {
        return systemCPU;
    }

    public void setSystemCPU(float systemCPU) {
        this.systemCPU = systemCPU;
    }

    public float getWaitCPU() {
        return waitCPU;
    }

    public void setWaitCPU(float waitCPU) {
        this.waitCPU = waitCPU;
    }

    public float getInterruptCPU() {
        return interruptCPU;
    }

    public void setInterruptCPU(float interruptCPU) {
        this.interruptCPU = interruptCPU;
    }

    public float getIdleCPU() {
        return idleCPU;
    }

    public void setIdleCPU(float idleCPU) {
        this.idleCPU = idleCPU;
    }

    public float getNiceCPU() {
        return niceCPU;
    }

    public void setNiceCPU(float niceCPU) {
        this.niceCPU = niceCPU;
    }

    public float getSoftirqCPU() {
        return softirqCPU;
    }

    public void setSoftirqCPU(float softirqCPU) {
        this.softirqCPU = softirqCPU;
    }

    public float getStealCPU() {
        return stealCPU;
    }

    public void setStealCPU(float stealCPU) {
        this.stealCPU = stealCPU;
    }

    @Override
    public String toString() {
        return "CollectdCPU [servIP=" + servIP + ", ts=" + ts + ", userCPU=" + userCPU + ", systemCPU=" + systemCPU
                + ", waitCPU=" + waitCPU + ", interruptCPU=" + interruptCPU + ", idleCPU=" + idleCPU + ", niceCPU="
                + niceCPU + ", softirqCPU=" + softirqCPU + ", stealCPU=" + stealCPU + "]";
    }
    
    @Override
    public void storeData() {
        HostStatisticDao.saveCPU(servIP, ts, userCPU, systemCPU, waitCPU, interruptCPU, idleCPU, niceCPU, softirqCPU, stealCPU);
    }

}
