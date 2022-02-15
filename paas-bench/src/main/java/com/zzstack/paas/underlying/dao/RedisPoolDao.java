package com.zzstack.paas.underlying.dao;

import java.util.HashMap;
import java.util.Map;

import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;

import io.vertx.core.json.JsonObject;

public class RedisPoolDao {
    
    private static final String TUPLE_SPLIT = ",";
    private static final String KV_SPLIT = ":";
    
    public static void adjustRedisPoolWeight(String weigth, JsonObject json) {
        Map<String, Integer> weightMap = parseWeight(weigth);
        WeightedRRLoadBalancer balancer = MultiRedissonClient.get(BenchConstants.REDIS_CONF_FILE);
        boolean result = balancer.resetWeight(weightMap);
        json.put("result", result);
        if (!result) {
            json.put("info", "输入权值不合法:权值全为0或存在负数");
        }
    }
    
    private static Map<String, Integer> parseWeight(String weigth) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        if (weigth == null || weigth.isEmpty()) return result;
        
        String[] tuples = weigth.split(TUPLE_SPLIT);
        for (String tuple : tuples) {
            String[] kvPair = tuple.split(KV_SPLIT);
            String k = kvPair[0];
            Integer v = Integer.valueOf(kvPair[1]);
            result.put(k, v);
        }
        
        return result;
    }

}
