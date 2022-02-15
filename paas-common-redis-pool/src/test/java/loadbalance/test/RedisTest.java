package loadbalance.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.redis.MultiRedissonClient;

public class RedisTest {
    
    private static Logger logger = LoggerFactory.getLogger(RedisTest.class);

    public static void main(String[] args) {
        try {
            MultiRedissonClient.get("redis-cluster-queue");
            
            Thread.sleep(20000);
            
            MultiRedissonClient.destroy();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
