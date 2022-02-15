package com.zzstack.paas.underlying.metasvr.bean.collectd;

import com.zzstack.paas.underlying.metasvr.dataservice.dao.HostStatisticDao;

public class CollectdMemory implements ICollectd {

    private String servIP;
    private long   ts;
    
    private float  used;
    private float  free;
    private float  buffered;
    private float  cached;
    private float  slabUnrecl;
    private float  slabRecl;
    
    public CollectdMemory(String servIP, long ts, float used, float free, float buffered, float cached,
            float slabUnrecl, float slabRecl) {
        super();
        this.servIP = servIP;
        this.ts = ts;
        this.used = used;
        this.free = free;
        this.buffered = buffered;
        this.cached = cached;
        this.slabUnrecl = slabUnrecl;
        this.slabRecl = slabRecl;
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

    public float getUsed() {
        return used;
    }

    public void setUsed(float used) {
        this.used = used;
    }

    public float getFree() {
        return free;
    }

    public void setFree(float free) {
        this.free = free;
    }

    public float getBuffered() {
        return buffered;
    }

    public void setBuffered(float buffered) {
        this.buffered = buffered;
    }

    public float getCached() {
        return cached;
    }

    public void setCached(float cached) {
        this.cached = cached;
    }

    public float getSlabUnrecl() {
        return slabUnrecl;
    }

    public void setSlabUnrecl(float slabUnrecl) {
        this.slabUnrecl = slabUnrecl;
    }

    public float getSlabRecl() {
        return slabRecl;
    }

    public void setSlabRecl(float slabRecl) {
        this.slabRecl = slabRecl;
    }

    @Override
    public String toString() {
        return "CollectdMemory [servIP=" + servIP + ", ts=" + ts + ", used=" + used + ", free=" + free + ", buffered="
                + buffered + ", cached=" + cached + ", slabUnrecl=" + slabUnrecl + ", slabRecl=" + slabRecl + "]";
    }

    @Override
    public void storeData() {
        HostStatisticDao.saveMemory(servIP, ts, used, free, buffered, cached, slabUnrecl, slabRecl);
    }

}
