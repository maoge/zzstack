package com.zzstack.paas.underlying.utils.config;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class CacheRedisMSConf implements IPaasConfig {
    
    public RedisConfig redisConfig;
    
    public static class RedisConfig {
        
        public int idleConnectionTimeout = 10000;
        public int connectTimeout = 10000;
        public int timeout = 3000;
        public int retryAttempts = 3;
        public int retryInterval = 1500;
        public int failedSlaveReconnectionInterval = 3000;
        public int failedSlaveCheckInterval = 60000;
        public String password = "";
        
        public int subscriptionsPerConnection = 5;
        public String clientName = "";
        
        public String loadBalancer;
        
        public String subscriptionMode;
        public int subscriptionConnectionMinimumIdleSize = 1;
        public int subscriptionConnectionPoolSize = 1;
        
        public int slaveConnectionMinimumIdleSize = 10;
        public int slaveConnectionPoolSize = 20;
        
        public int masterConnectionMinimumIdleSize = 10;
        public int masterConnectionPoolSize = 20;

        public String readMode = "MASTER_SLAVE";
        
        public int scanInterval = 1000;
        public String codec = "ByteArrayCodec";
        
        public String transportMode = "NIO";
        
        public String serverMode = "CLUSTER";
        
        public RedisNodes server;

    }
    
    @Override
    public String getServClazzType() {
        return CONSTS.SERV_CLAZZ_CACHE;
    }

    @Override
    public String getDBType() {
        return "";
    }
    
}
