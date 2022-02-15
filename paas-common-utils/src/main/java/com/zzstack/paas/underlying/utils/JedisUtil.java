package com.zzstack.paas.underlying.utils;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.*;

public class JedisUtil {

    private static final String REDIS_ADDR_SPLIT = ",";
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

		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(REDIS_CONN_POOL_MAX);
		poolConfig.setMaxIdle(REDIS_CONN_POOL_MIN);
		poolConfig.setMaxWaitMillis(REDIS_MAX_WAIT_MILLIS);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(false);

		return new JedisCluster(nodes, poolConfig);
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
