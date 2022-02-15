package com.zzstack.paas.underlying.bench.redispool.runner;

import java.util.concurrent.atomic.AtomicLong;

import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.node.RedissonClientHolder;
import com.zzstack.paas.underlying.worker.TaskRunner;

public class PushRunner extends TaskRunner {
    
    private static Logger logger = LoggerFactory.getLogger(PushRunner.class);

    public PushRunner(AtomicLong normalCnt, AtomicLong errorCnt) {
        super(normalCnt, errorCnt);
    }

    @Override
    public void run() {
        WeightedRRLoadBalancer balancer = MultiRedissonClient.get(BenchConstants.REDIS_CONF_FILE);
        byte[] msgBytes = "aaaaaaaaabbbbbbbbbcccccccc".getBytes();
        int cnt = 0;
        
        while (bRunning) {
            RedissonClientHolder redissonClientHolder = (RedissonClientHolder) balancer.select();
            RedissonClient redissonClient = redissonClientHolder.getRedissonClient();
            String identifier = String.format(BenchConstants.QUEUE_FMT, cnt++ % 100);
            
            try {
                RDeque<byte[]> deque = redissonClient.getDeque(identifier);
                deque.addFirst(msgBytes);
                normalCnt.incrementAndGet();
            } catch (Exception e) {
                errorCnt.incrementAndGet();
                logger.error(e.getMessage(), e);
            }
        }
    }

}
