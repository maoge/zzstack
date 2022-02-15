package com.zzstack.paas.underlying.collect;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleRunner implements Runnable {
    
    private static Logger logger = LoggerFactory.getLogger(ScheduleRunner.class);

    private final List<RedisClusterProbe> redisClusterProbes;

    public ScheduleRunner(List<RedisClusterProbe> probes) {
        this.redisClusterProbes = probes;
    }

    @Override
    public void run() {
        for (RedisClusterProbe probe : redisClusterProbes) {
            try {
                probe.collectClusterInstantOps();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
