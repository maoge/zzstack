package com.zzstack.paas.underlying.redis.loadbalance;

public interface Holder {

    boolean isAvalable();
    
    void destroy();

    String id();

}
