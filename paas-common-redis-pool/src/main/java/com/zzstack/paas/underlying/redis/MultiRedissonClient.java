package com.zzstack.paas.underlying.redis;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.utils.RedissonConfParser;
import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.paas.PaasTopoParser;

public class MultiRedissonClient {
    
    private static Logger logger = LoggerFactory.getLogger(MultiRedissonClient.class);
    
    private static Map<String, WeightedRRLoadBalancer> entryMap;
    
    private static Lock lock = null;
    
    static {
        lock = new ReentrantLock();
        entryMap = new ConcurrentHashMap<String, WeightedRRLoadBalancer>();
    }
    
    public static WeightedRRLoadBalancer get(String name) {
        WeightedRRLoadBalancer balancer = entryMap.get(name);
        if (balancer != null)
            return balancer;
        
        lock.lock();
        try {
            balancer = RedissonConfParser.fromYaml(name);
            entryMap.put(name, balancer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        
        return balancer;
    }
    
    public static WeightedRRLoadBalancer get(String servInstID, String name, String topoStr, Map<String, Object> params)
            throws PaasSdkException {
        
        WeightedRRLoadBalancer balancer = entryMap.get(name);
        if (balancer != null)
            return balancer;

        lock.lock();
        try {
            Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
            CacheRedisHaConf redissonConf = (CacheRedisHaConf) o;
            balancer = RedissonConfParser.fromRedissonConf(redissonConf);
            
            entryMap.put(name, balancer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }

        return balancer;
    }
    
    public static void destroy() {
        lock.lock();
        try {
            Set<Entry<String, WeightedRRLoadBalancer>> entrySet = entryMap.entrySet();
            for (Entry<String, WeightedRRLoadBalancer> entry : entrySet) {
                WeightedRRLoadBalancer balancer = entry.getValue();
                if (balancer != null) {
                    balancer.destroy();
                }
            }

        } finally {
            lock.unlock();
        }
    }

}
