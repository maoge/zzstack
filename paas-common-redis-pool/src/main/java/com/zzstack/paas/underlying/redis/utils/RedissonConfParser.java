package com.zzstack.paas.underlying.redis.utils;

import java.util.ArrayList;
import java.util.List;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.codec.KryoCodec;
import org.redisson.codec.LZ4Codec;
import org.redisson.codec.SerializationCodec;
import org.redisson.codec.SnappyCodecV2;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.redisson.connection.balancer.RandomLoadBalancer;
import org.redisson.connection.balancer.RoundRobinLoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.redis.loadbalance.Node;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.node.RedissonClientHolder;
import com.zzstack.paas.underlying.utils.YamlParser;
import com.zzstack.paas.underlying.utils.config.CacheRedisClusterConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisMSConf;
import com.zzstack.paas.underlying.utils.config.RedisNodes;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class RedissonConfParser {
    
    private static Logger logger = LoggerFactory.getLogger(RedissonConfParser.class);
    
    public static WeightedRRLoadBalancer fromYaml(String yamlName) {
        String yamlFile = String.format("conf/%s.yaml", yamlName);
        YamlParser parser = new YamlParser(yamlFile);
        CacheRedisHaConf redissonConf = (CacheRedisHaConf) parser.parseObject(CacheRedisHaConf.class);
        return marshell(redissonConf);
    }
    
    public static WeightedRRLoadBalancer fromRedissonConf(CacheRedisHaConf redissonConf) {
        return marshell(redissonConf);
    }
    
    public static RedissonClientHolder fromRedissonConf(CacheRedisClusterConf redissonConf) {
        return parseRedisCluster(redissonConf);
    }
    
    public static RedissonClientHolder fromRedissionConf(CacheRedisMSConf redissionConf) {
        return parseRedisMS(redissionConf);
    }
    
    private static WeightedRRLoadBalancer marshell(CacheRedisHaConf redissonConf) {
        List<Node> nodes = parse(redissonConf);
        ServerMode mode = translateMode(redissonConf.redisConfig.serverMode);
        return new WeightedRRLoadBalancer(nodes, mode);
    }

    public static List<Node> parse(CacheRedisHaConf redissonConf) {
        CacheRedisHaConf.RedisHaConfig redisConf = redissonConf.redisConfig;
        RedisNodes serverA = redisConf.serverA;
        RedisNodes serverB = redisConf.serverB;

        List<Node> result = new ArrayList<Node>();

        String serverMode = redisConf.serverMode;
        if (serverMode.equals(CONSTS.REDIS_SERVER_MODE_CLUSTER)) {
            RedissonClientHolder clusterA = marshellRedissonClient(redisConf, serverA);
            RedissonClientHolder clusterB = marshellRedissonClient(redisConf, serverB);
            
            if (clusterA == null || clusterB == null) {
                logger.error("serverA or serverB miss nodeAddresses");
                if (clusterA != null) clusterA.destroy();
                if (clusterB != null) clusterB.destroy();
                
                return null;
            }
            
            Node nodeA = new Node(clusterA, clusterA.getWeight(), clusterA.id());
            Node nodeB = new Node(clusterB, clusterB.getWeight(), clusterB.id());

            result.add(nodeA);
            result.add(nodeB);
        } else if (serverMode.equals(CONSTS.REDIS_SERVER_MODE_PROXY)) {
            // TODO
        } else if (serverMode.equals(CONSTS.REDIS_SERVER_MODE_MS)) {
            
        }

        return result;
    }
    
    public static RedissonClientHolder parseRedisCluster(CacheRedisClusterConf redissonConf) {
        RedissonClientHolder cluster = null;
        
        CacheRedisClusterConf.RedisConfig redisConf = redissonConf.redisConfig;
        RedisNodes server = redisConf.server;
        String serverMode = redisConf.serverMode;
        
        if (serverMode.equals(CONSTS.REDIS_SERVER_MODE_CLUSTER)) {
            cluster = marshellRedissonClient(redisConf, server);
            if (cluster == null) {
                logger.error("serverA or serverB miss nodeAddresses");
                return null;
            }
        } else if (serverMode.equals(CONSTS.REDIS_SERVER_MODE_PROXY)) {
            // TODO
        }

        return cluster;
    }
    
    public static RedissonClientHolder parseRedisMS(CacheRedisMSConf redissionConf) {
        RedissonClientHolder cluster = null;
        
        CacheRedisMSConf.RedisConfig redisConf = redissionConf.redisConfig;
        RedisNodes master = redisConf.master;
        RedisNodes slave = redisConf.slave;
        
        cluster = marshellRedissonClient(redisConf, master, slave);
        
        return cluster;
    }
    
    private static RedissonClientHolder marshellRedissonClient(CacheRedisMSConf.RedisConfig redisConf, RedisNodes master, RedisNodes slave) {
        if ((master != null && master.nodeAddresses != null)
                || (slave != null && slave.nodeAddresses != null)) {
            Config config = new Config();
            switch (redisConf.codec) {
            case "ByteArrayCodec":
                config.setCodec(new ByteArrayCodec());
                break;
            case "JsonJacksonCodec":
                config.setCodec(new JsonJacksonCodec());
                break;
            case "Kryo5Codec":
                config.setCodec(new Kryo5Codec());
                break;
            case "KryoCodec":
                config.setCodec(new KryoCodec());
                break;
            case "LZ4Codec":
                config.setCodec(new LZ4Codec());
                break;
            case "SerializationCodec":
                config.setCodec(new SerializationCodec());
                break;
            case "SnappyCodecV2":
                config.setCodec(new SnappyCodecV2());
                break;
            case "StringCodec":
                config.setCodec(new StringCodec());
                break;
            default:
                break;
            }
            
            MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers();
            if (master != null && master.nodeAddresses != null) {
                masterSlaveServersConfig.setMasterAddress(master.nodeAddresses[0]);
            }
            
            if (slave != null && slave.nodeAddresses != null) {
                for (String slaveAddr : slave.nodeAddresses) {
                    masterSlaveServersConfig.addSlaveAddress(slaveAddr);
                }
            }
            
            masterSlaveServersConfig.setRetryInterval(redisConf.scanInterval);
            
            switch (redisConf.loadBalancer) {
            case "RoundRobinLoadBalancer":
            case "WeightedRoundRobinBalancer":
                masterSlaveServersConfig.setLoadBalancer(new RoundRobinLoadBalancer());
                break;
            case "RandomLoadBalancer":
                masterSlaveServersConfig.setLoadBalancer(new RandomLoadBalancer());
                break;
            default:
                break;
            }
            
            // BaseConfig
            masterSlaveServersConfig.setIdleConnectionTimeout(redisConf.idleConnectionTimeout);
            masterSlaveServersConfig.setConnectTimeout(redisConf.connectTimeout);
            masterSlaveServersConfig.setTimeout(redisConf.timeout);
            masterSlaveServersConfig.setRetryAttempts(redisConf.retryAttempts);
            masterSlaveServersConfig.setRetryInterval(redisConf.retryInterval);
            masterSlaveServersConfig.setPassword(redisConf.password);
            masterSlaveServersConfig.setSubscriptionsPerConnection(redisConf.subscriptionsPerConnection);
            masterSlaveServersConfig.setClientName(redisConf.clientName);
            
            // BaseMasterSlaveServersConfig
            masterSlaveServersConfig.setSlaveConnectionMinimumIdleSize(redisConf.slaveConnectionMinimumIdleSize);
            masterSlaveServersConfig.setSlaveConnectionPoolSize(redisConf.slaveConnectionPoolSize);
            
            masterSlaveServersConfig.setFailedSlaveReconnectionInterval(redisConf.failedSlaveReconnectionInterval);
            // masterSlaveServersConfig.setFailedSlaveCheckInterval(redisConf.failedSlaveCheckInterval);
            
            masterSlaveServersConfig.setMasterConnectionMinimumIdleSize(redisConf.masterConnectionMinimumIdleSize);
            masterSlaveServersConfig.setMasterConnectionPoolSize(redisConf.masterConnectionPoolSize);
            
            switch (redisConf.readMode) {
            case "SLAVE":
                masterSlaveServersConfig.setReadMode(ReadMode.SLAVE);
                break;
            case "MASTER":
                masterSlaveServersConfig.setReadMode(ReadMode.MASTER);
                break;
            case "MASTER_SLAVE":
                masterSlaveServersConfig.setReadMode(ReadMode.MASTER_SLAVE);
                break;
            default:
                break;
            }
            
            switch (redisConf.subscriptionMode) {
            case "SLAVE":
                masterSlaveServersConfig.setSubscriptionMode(SubscriptionMode.SLAVE);
                break;
            case "MASTER":
                masterSlaveServersConfig.setSubscriptionMode(SubscriptionMode.MASTER);
                break;
            default:
                break;
            }
            masterSlaveServersConfig.setSubscriptionConnectionMinimumIdleSize(redisConf.subscriptionConnectionMinimumIdleSize);
            masterSlaveServersConfig.setSubscriptionConnectionPoolSize(redisConf.subscriptionConnectionPoolSize);
            
            RedissonClient redissonClient = Redisson.create(config);
            RedissonClientHolder redisClientHolder = new RedissonClientHolder(redissonClient, master.id, 100);

            return redisClientHolder;
        }

        return null;
    }

    private static RedissonClientHolder marshellRedissonClient(CacheRedisHaConf.RedisHaConfig redisConf, RedisNodes server) {
        if (server.nodeAddresses != null) {
            Config config = new Config();
            switch (redisConf.codec) {
            case "ByteArrayCodec":
                config.setCodec(new ByteArrayCodec());
                break;
            case "JsonJacksonCodec":
                config.setCodec(new JsonJacksonCodec());
                break;
            case "Kryo5Codec":
                config.setCodec(new Kryo5Codec());
                break;
            case "KryoCodec":
                config.setCodec(new KryoCodec());
                break;
            case "LZ4Codec":
                config.setCodec(new LZ4Codec());
                break;
            case "SerializationCodec":
                config.setCodec(new SerializationCodec());
                break;
            case "SnappyCodecV2":
                config.setCodec(new SnappyCodecV2());
                break;
            case "StringCodec":
                config.setCodec(new StringCodec());
                break;
            default:
                break;
            }
            
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            for (String address : server.nodeAddresses) {
                clusterServersConfig.addNodeAddress(address);
            }
            clusterServersConfig.setScanInterval(redisConf.scanInterval);
            
            switch (redisConf.loadBalancer) {
            case "RoundRobinLoadBalancer":
            case "WeightedRoundRobinBalancer":
                clusterServersConfig.setLoadBalancer(new RoundRobinLoadBalancer());
                break;
            case "RandomLoadBalancer":
                clusterServersConfig.setLoadBalancer(new RandomLoadBalancer());
                break;
            default:
                break;
            }
            
            // BaseConfig
            clusterServersConfig.setIdleConnectionTimeout(redisConf.idleConnectionTimeout);
            clusterServersConfig.setConnectTimeout(redisConf.connectTimeout);
            clusterServersConfig.setTimeout(redisConf.timeout);
            clusterServersConfig.setRetryAttempts(redisConf.retryAttempts);
            clusterServersConfig.setRetryInterval(redisConf.retryInterval);
            clusterServersConfig.setPassword(redisConf.password);
            clusterServersConfig.setSubscriptionsPerConnection(redisConf.subscriptionsPerConnection);
            clusterServersConfig.setClientName(redisConf.clientName);
            
            // BaseMasterSlaveServersConfig
            clusterServersConfig.setSlaveConnectionMinimumIdleSize(redisConf.slaveConnectionMinimumIdleSize);
            clusterServersConfig.setSlaveConnectionPoolSize(redisConf.slaveConnectionPoolSize);
            
            clusterServersConfig.setFailedSlaveReconnectionInterval(redisConf.failedSlaveReconnectionInterval);
            // clusterServersConfig.setFailedSlaveCheckInterval(redisConf.failedSlaveCheckInterval);
            
            clusterServersConfig.setMasterConnectionMinimumIdleSize(redisConf.masterConnectionMinimumIdleSize);
            clusterServersConfig.setMasterConnectionPoolSize(redisConf.masterConnectionPoolSize);
            
            switch (redisConf.readMode) {
            case "SLAVE":
                clusterServersConfig.setReadMode(ReadMode.SLAVE);
                break;
            case "MASTER":
                clusterServersConfig.setReadMode(ReadMode.MASTER);
                break;
            case "MASTER_SLAVE":
                clusterServersConfig.setReadMode(ReadMode.MASTER_SLAVE);
                break;
            default:
                break;
            }
            
            switch (redisConf.subscriptionMode) {
            case "SLAVE":
                clusterServersConfig.setSubscriptionMode(SubscriptionMode.SLAVE);
                break;
            case "MASTER":
                clusterServersConfig.setSubscriptionMode(SubscriptionMode.MASTER);
                break;
            default:
                break;
            }
            clusterServersConfig.setSubscriptionConnectionMinimumIdleSize(redisConf.subscriptionConnectionMinimumIdleSize);
            clusterServersConfig.setSubscriptionConnectionPoolSize(redisConf.subscriptionConnectionPoolSize);
            
            RedissonClient redissonClient = Redisson.create(config);
            RedissonClientHolder redisClientHolder = new RedissonClientHolder(redissonClient, server.id, server.weight);

            return redisClientHolder;
        }

        return null;
    }
    
    private static RedissonClientHolder marshellRedissonClient(CacheRedisClusterConf.RedisConfig redisConf, RedisNodes server) {
        if (server.nodeAddresses != null) {
            Config config = new Config();
            switch (redisConf.codec) {
            case "ByteArrayCodec":
                config.setCodec(new ByteArrayCodec());
                break;
            case "JsonJacksonCodec":
                config.setCodec(new JsonJacksonCodec());
                break;
            case "Kryo5Codec":
                config.setCodec(new Kryo5Codec());
                break;
            case "KryoCodec":
                config.setCodec(new KryoCodec());
                break;
            case "LZ4Codec":
                config.setCodec(new LZ4Codec());
                break;
            case "SerializationCodec":
                config.setCodec(new SerializationCodec());
                break;
            case "SnappyCodecV2":
                config.setCodec(new SnappyCodecV2());
                break;
            case "StringCodec":
                config.setCodec(new StringCodec());
                break;
            default:
                break;
            }
            
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            for (String address : server.nodeAddresses) {
                clusterServersConfig.addNodeAddress(address);
            }
            clusterServersConfig.setScanInterval(redisConf.scanInterval);
            
            switch (redisConf.loadBalancer) {
            case "RoundRobinLoadBalancer":
            case "WeightedRoundRobinBalancer":
                clusterServersConfig.setLoadBalancer(new RoundRobinLoadBalancer());
                break;
            case "RandomLoadBalancer":
                clusterServersConfig.setLoadBalancer(new RandomLoadBalancer());
                break;
            default:
                break;
            }
            
            // BaseConfig
            clusterServersConfig.setIdleConnectionTimeout(redisConf.idleConnectionTimeout);
            clusterServersConfig.setConnectTimeout(redisConf.connectTimeout);
            clusterServersConfig.setTimeout(redisConf.timeout);
            clusterServersConfig.setRetryAttempts(redisConf.retryAttempts);
            clusterServersConfig.setRetryInterval(redisConf.retryInterval);
            clusterServersConfig.setPassword(redisConf.password);
            clusterServersConfig.setSubscriptionsPerConnection(redisConf.subscriptionsPerConnection);
            clusterServersConfig.setClientName(redisConf.clientName);
            
            // BaseMasterSlaveServersConfig
            clusterServersConfig.setSlaveConnectionMinimumIdleSize(redisConf.slaveConnectionMinimumIdleSize);
            clusterServersConfig.setSlaveConnectionPoolSize(redisConf.slaveConnectionPoolSize);
            
            clusterServersConfig.setFailedSlaveReconnectionInterval(redisConf.failedSlaveReconnectionInterval);
            // clusterServersConfig.setFailedSlaveCheckInterval(redisConf.failedSlaveCheckInterval);
            
            clusterServersConfig.setMasterConnectionMinimumIdleSize(redisConf.masterConnectionMinimumIdleSize);
            clusterServersConfig.setMasterConnectionPoolSize(redisConf.masterConnectionPoolSize);
            
            switch (redisConf.readMode) {
            case "SLAVE":
                clusterServersConfig.setReadMode(ReadMode.SLAVE);
                break;
            case "MASTER":
                clusterServersConfig.setReadMode(ReadMode.MASTER);
                break;
            case "MASTER_SLAVE":
                clusterServersConfig.setReadMode(ReadMode.MASTER_SLAVE);
                break;
            default:
                break;
            }
            
            switch (redisConf.subscriptionMode) {
            case "SLAVE":
                clusterServersConfig.setSubscriptionMode(SubscriptionMode.SLAVE);
                break;
            case "MASTER":
                clusterServersConfig.setSubscriptionMode(SubscriptionMode.MASTER);
                break;
            default:
                break;
            }
            clusterServersConfig.setSubscriptionConnectionMinimumIdleSize(redisConf.subscriptionConnectionMinimumIdleSize);
            clusterServersConfig.setSubscriptionConnectionPoolSize(redisConf.subscriptionConnectionPoolSize);
            
            RedissonClient redissonClient = Redisson.create(config);
            RedissonClientHolder redisClientHolder = new RedissonClientHolder(redissonClient, server.id, server.weight);

            return redisClientHolder;
        }

        return null;
    }
    
    private static ServerMode translateMode(String serverMode) {
        switch (serverMode) {
        case CONSTS.REDIS_SERVER_MODE_CLUSTER:
            return ServerMode.CLUSTER;
        case CONSTS.REDIS_SERVER_MODE_PROXY:
            return ServerMode.PROXY;
        default:
            return ServerMode.DEFAULT;
        }
    }

}
