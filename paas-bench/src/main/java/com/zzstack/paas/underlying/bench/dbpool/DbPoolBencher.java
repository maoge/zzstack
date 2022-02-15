package com.zzstack.paas.underlying.bench.dbpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.zzstack.paas.underlying.bench.DbStatistic;
import com.zzstack.paas.underlying.bench.dbpool.runner.DbRunner;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.constants.BenchParams;
import com.zzstack.paas.underlying.worker.BenchSkelonton;
import com.zzstack.paas.underlying.worker.TaskRunner;

public class DbPoolBencher implements BenchSkelonton {
    
    private DbStatistic stat;
    
    private static AtomicLong[] normalCntVec;
    private static AtomicLong[] errorCntVec;
    private static AtomicLong maxTPS;
    
    private int workerCnt;
    
    private List<TaskRunner> dbRunnerList;
    
    public DbPoolBencher() {
        workerCnt = BenchParams.getWorkerCnt();
        normalCntVec = new AtomicLong[workerCnt];
        errorCntVec = new AtomicLong[workerCnt];
        for (int i = 0; i < workerCnt; i++) {
            normalCntVec[i] = new AtomicLong(0L);
            errorCntVec[i] = new AtomicLong(0L);
        }
        maxTPS = new AtomicLong(0L);
        stat = new DbStatistic(maxTPS, normalCntVec, BenchConstants.LOGGER_DB_BENCH);
        dbRunnerList = new ArrayList<TaskRunner>();
    }

    @Override
    public void start() {
        for (int i = 0; i < workerCnt; ++i) {
            DbRunner runner = new DbRunner(normalCntVec[i], errorCntVec[i]);
            Thread thread = new Thread(runner);
            thread.start();

            dbRunnerList.add(runner);
        }
    }

    @Override
    public void stop() {
        for (int i = 0; i < workerCnt; ++i) {
            DbRunner runner = (DbRunner) dbRunnerList.get(i);
            runner.StopRunning();
        }
        dbRunnerList.clear();
        
        if (stat != null) {
            stat.StopRunning();
            stat = null;
        }
    }
    
}
