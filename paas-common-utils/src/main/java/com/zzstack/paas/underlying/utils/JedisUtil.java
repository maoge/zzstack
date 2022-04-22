package com.zzstack.paas.underlying.utils;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.*;

public class JedisUtil {

    private static final String REDIS_ADDR_SPLIT = ",";
    private static int MAX_ATTEMPTS = 5;
    private static int REDIS_CONN_POOL_MAX = 20;
    private static int REDIS_CONN_POOL_MIN = 10;
    private static long REDIS_MAX_WAIT_MILLIS = 3000;

	public static JedisCluster getPool(String clusterAddr) {
		if (clusterAddr == null || clusterAddr.isEmpty())
			return null;

		Set<HostAndPort> nodes = new HashSet<>();
		String[] addrArr = clusterAddr.split(REDIS_ADDR_SPLIT);
		for (String addr : addrArr) {
			HostAndPort hostAndPort = parseAddr(addr);
			nodes.add(hostAndPort);
		}

		GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<Connection>();
		poolConfig.setMaxTotal(REDIS_CONN_POOL_MAX);
		poolConfig.setMinIdle(REDIS_CONN_POOL_MIN);
		poolConfig.setMaxIdle(REDIS_CONN_POOL_MIN);
		poolConfig.setMaxWait(Duration.ofMillis(REDIS_MAX_WAIT_MILLIS));
		poolConfig.setTestOnBorrow(false);
		poolConfig.setTestOnReturn(false);

		return new JedisCluster(nodes, poolConfig);
	}
	
    public static JedisCluster getPool(String clusterAddr, String auth, int maxPoolSize, int minPoolSize, int maxWaitMills) {
        if (clusterAddr == null || clusterAddr.isEmpty())
            return null;

        Set<HostAndPort> nodes = new HashSet<>();
        String[] addrArr = clusterAddr.split(REDIS_ADDR_SPLIT);
        for (String addr : addrArr) {
            HostAndPort hostAndPort = parseAddr(addr);
            nodes.add(hostAndPort);
        }

        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<Connection>();
        poolConfig.setMaxTotal(maxPoolSize);
        poolConfig.setMinIdle(minPoolSize);
        poolConfig.setMaxIdle(minPoolSize);
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMills));
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);

        if (auth != null && !auth.isEmpty()) {
            return new JedisCluster(nodes, maxWaitMills, maxWaitMills, MAX_ATTEMPTS, auth, poolConfig);
        } else {
            return new JedisCluster(nodes, maxWaitMills, maxWaitMills, MAX_ATTEMPTS, poolConfig);
        }
    }
	
	public static Jedis getPool(String host, int port) {
	    return new Jedis(host, port);
	}

	public static void setRedisConnPoolMax(int connPoolMax) {
	    if (connPoolMax > 0)
	        REDIS_CONN_POOL_MAX = connPoolMax;
	}

	public static void setRedisConnPoolMin(int connPoolMin) {
	    if (connPoolMin > 0)
	        REDIS_CONN_POOL_MIN = connPoolMin;
	}

	public static void setMaxWaitMillis(long maxWaitMillis) {
	    if (maxWaitMillis > 0)
	        REDIS_MAX_WAIT_MILLIS = maxWaitMillis;
	}

	private static HostAndPort parseAddr(String addr) {
		if (addr == null || addr.isEmpty())
			return null;
		String[] splits = addr.split(":");

		return new HostAndPort(splits[0], Integer.parseInt(splits[1]));
	}

}
