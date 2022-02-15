package com.zzstack.paas.underlying.bench.redispool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.zzstack.paas.underlying.bench.RedisStatistic;
import com.zzstack.paas.underlying.bench.redispool.runner.PushRunner;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.constants.BenchParams;
import com.zzstack.paas.underlying.worker.BenchSkelonton;
import com.zzstack.paas.underlying.worker.TaskRunner;

public class RedisQueuePusher implements BenchSkelonton {
    
    private RedisStatistic stat;
    
    private AtomicLong[] normalCntVec;
    private AtomicLong[] errorCntVec;
    private AtomicLong maxTPS;
    
    private int workerCnt;
    
    private List<TaskRunner> pusherList;
    
    public RedisQueuePusher() {
        workerCnt = BenchParams.getWorkerCnt();
        normalCntVec = new AtomicLong[workerCnt];
        errorCntVec = new AtomicLong[workerCnt];
        for (int i = 0; i < workerCnt; i++) {
            normalCntVec[i] = new AtomicLong(0L);
            errorCntVec[i] = new AtomicLong(0L);
        }
        maxTPS = new AtomicLong(0L);
        stat = new RedisStatistic(maxTPS, normalCntVec, BenchConstants.LOGGER_REDIS_PUSH);
        pusherList = new ArrayList<TaskRunner>();
    }

    @Override
    public void start() {
        for (int i = 0; i < workerCnt; ++i) {
            PushRunner runner = new PushRunner(normalCntVec[i], errorCntVec[i]);
            Thread thread = new Thread(runner);
            thread.start();

            pusherList.add(runner);
        }
    }

    @Override
    public void stop() {
        for (int i = 0; i < workerCnt; ++i) {
            PushRunner runner = (PushRunner) pusherList.get(i);
            runner.StopRunning();
        }
        pusherList.clear();
        
        if (stat != null) {
            stat.StopRunning();
            stat = null;
        }
    }

}
