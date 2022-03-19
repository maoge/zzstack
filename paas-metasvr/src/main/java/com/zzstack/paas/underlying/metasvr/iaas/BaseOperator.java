package com.zzstack.paas.underlying.metasvr.iaas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;

import redis.clients.jedis.JedisCluster;

public class BaseOperator {

    public static final int UNIT_GBYTE = 1024 * 1024 * 1024;
    public static final int PRECISION_CPU = 1;
    public static final int PRECISION_MEM = 3;

    public static Map<String, List<CollectdRaw>> splitByPluginInstance(List<CollectdRaw> list) {
        Map<String, List<CollectdRaw>> map = new HashMap<String, List<CollectdRaw>>();
        for (CollectdRaw raw : list) {
            map.computeIfAbsent(raw.getPluginInstance(), v -> new ArrayList<CollectdRaw>()).add(raw);
        }
        
        return map;
    }
    
    public static CollectdRaw getCurrentNodeByType(List<CollectdRaw> list, String type) {
        CollectdRaw collectdRaw = null;
        int size = list.size();
        for (int i = size - 1; i >= 0; --i) {
            CollectdRaw tmp = list.get(i);
            if (tmp.getType().equals(type)) {
                collectdRaw = tmp;
                break;
            }
        }
        
        return collectdRaw;
    }
    
    public static List<CollectdRaw> getCurrentNodeListBySpecifiedPlugin(List<CollectdRaw> list, String plugin) {
        List<CollectdRaw> result = null;
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            CollectdRaw tmp = list.get(i);
            if (tmp.getPlugin().equals(plugin)) {
                if (result == null) {
                    result = new ArrayList<CollectdRaw>();
                }
                
                result.add(tmp);
            }
        }
        
        return result;
    }
    
    public static CollectdRaw getCurrentNodeByTypeInstance(List<CollectdRaw> list, String typeInstance) {
        CollectdRaw result = null;
        int size = list.size();
        for (int i = size - 1; i >= 0; --i) {
            CollectdRaw tmp = list.get(i);
            if (tmp.getTypeInstance().equals(typeInstance)) {
                result = tmp;
                break;
            }
        }
        return result;
    }

    public static List<String> getLastCollectdRawValue(String key, String... fileds) {
        JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
        return jedisClient.hmget(key, fileds);
    }

    public static void setCurrentCollectdRawValue(String key, Map<String, String> hash) {
        JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
        jedisClient.hmset(key, hash);
    }

    public static float getAvgValue(List<Float> list, int precision) {
        if (list == null || list.isEmpty())
            return 0.0f;
        
        float sum = 0.0f;
        for (Float f : list) {
            sum += f;
        }
        
        float avg = sum / list.size();
        return roundFloat(avg, precision);
    }

    public static long getSumValue(List<Long> list) {
        if (list == null || list.isEmpty())
            return 0;
        
        long sum = 0;
        for (long l : list) {
            sum += l;
        }
        
        return sum;
    }

    public static long getDeriveData(List<CollectdRaw> list) {
        int size = list.size();
        if (size < 2)
            return 0;
        
        CollectdRaw item2 = list.get(size - 1);
        CollectdRaw item1 = list.get(size - 2);
        
        long value2 = item2.getValues()[0];
        long value1 = item1.getValues()[0];
        
        return value2 - value1;
    }

    public static long getGaugeData(List<CollectdRaw> list) {
        int size = list.size();
        if (size == 0)
            return 0;
        
        CollectdRaw item = list.get(size - 1);
        return item.getValues()[0];
    }

    public static float roundFloat(float val, int precision) {
        return new BigDecimal(val).setScale(precision, RoundingMode.HALF_UP).floatValue();
    }

    public static float roundDoubleAsFloat(double val, int precision) {
        return new BigDecimal(val).setScale(precision, RoundingMode.HALF_UP).floatValue();
    }

}
