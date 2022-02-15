package com.zzstack.paas.underlying.bench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.bench.dbpool.DbPoolBencher;
import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.constants.BenchParams;
import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.singleton.GlobalRes;

public class DbBenchBootStrap {
    
    private static Logger logger = LoggerFactory.getLogger(DbBenchBootStrap.class);

    public static void main(String[] args) {
        loadSingleton();
        
        loadBench();
        
        benchLoop();
        
        destroy();
    }
    
    private static void loadSingleton() {
        BenchParams.init();
        
        ActiveStandbyDBSrcPool.setMyBatisConf("conf/mybatis-rac-conf.xml");
        ActiveStandbyDBSrcPool.setDefaultDBName(BenchConstants.DB_CONF_NAME);
        ActiveStandbyDBSrcPool.get();
        
        GlobalRes.get();
    }
    
    private static void loadBench() {
        DbPoolBencher dbPoolBencher = new DbPoolBencher();
        dbPoolBencher.start();
        GlobalRes.get().setDbPoolBencher(dbPoolBencher);
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
        ActiveStandbyDBSrcPool.destroy();
    }

}
