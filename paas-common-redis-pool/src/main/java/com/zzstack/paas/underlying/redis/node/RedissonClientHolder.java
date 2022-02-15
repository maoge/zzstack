package com.zzstack.paas.underlying.redis.node;

import org.redisson.api.RedissonClient;

import com.zzstack.paas.underlying.redis.loadbalance.Holder;

public class RedissonClientHolder implements Holder {
    
    private RedissonClient redissonClient = null;
    
    private String id;
    private int weight;
    
    public RedissonClientHolder() {
        super();
    }
    
    public RedissonClientHolder(RedissonClient redissonClient, String id, int weight) {
        super();
        this.redissonClient = redissonClient;
        this.id = id;
        this.weight = weight;
    }
    
    public RedissonClientHolder(String topoStr) {
        this.id = "";
        this.weight = 100;
        
        
        
    }

    @Override
    public boolean isAvalable() {
        return true;
    }

    @Override
    public String id() {
        return id;
    }
    
    public RedissonClient getRedissonClient() {
        return this.redissonClient;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    @Override
    public void destroy() {
        if (redissonClient != null) {
            redissonClient.shutdown();
            redissonClient = null;
        }
    }

}
