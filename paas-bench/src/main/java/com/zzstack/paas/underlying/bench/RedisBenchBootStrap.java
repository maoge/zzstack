package com.zzstack.paas.underlying.bench;

import com.zzstack.paas.underlying.bench.redispool.RedisQueuePoper;
import com.zzstack.paas.underlying.bench.redispool.RedisQueuePusher;
import com.zzstack.paas.underlying.collect.RedisInfoCollector;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.constants.BenchParams;
import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.singleton.GlobalRes;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisBenchBootStrap {

    private static Logger logger = LoggerFactory.getLogger(RedisBenchBootStrap.class);
    private static String pushUrl = "http://172.16.2.44:9090/paas/statistic/saveRedisInfo";
    private static long collectInterval = 5000L;

    public static void main(String[] args) throws PaasSdkException {
        loadSingleton();
        loadBench();
        benchLoop();
        destroy();
    }

    private static void loadSingleton() throws PaasSdkException {
        BenchParams.init();
//        RedisInfoCollector.get();
//        RedisInfoCollector.getFromConfig(pushUrl, "de97ac03-e960-451c-a0ff-e60059b57a31", "cc1bab3a-e553-449e-9507-74a21ac5d444", collectInterval);
        RedisInfoCollector.getFromConfig(pushUrl, "cc1bab3a-e553-449e-9507-74a21ac5d444", "cc1bab3a-e553-449e-9507-74a21ac5d444", collectInterval);
        GlobalRes.get();
        MultiRedissonClient.get(BenchConstants.REDIS_CONF_FILE);
    }

    private static void loadBench() {
        RedisQueuePusher redisPushBencher = new RedisQueuePusher();
        redisPushBencher.start();
        GlobalRes.get().setRedisPushBencher(redisPushBencher);
        RedisQueuePoper redisPopBencher = new RedisQueuePoper();
        redisPopBencher.start();
        GlobalRes.get().setRedisPopBencher(redisPopBencher);
    }

    private static void benchLoop() {
        long start = System.currentTimeMillis();
        long totalDiff = 0L;
        long totalTime = BenchParams.getTotalTime();

        while (totalDiff < totalTime) {
            long curr = System.currentTimeMillis();
            totalDiff = (curr - start) / 1000L;

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static void destroy() {
        GlobalRes.destroy();
        MultiRedissonClient.destroy();
    }

}
