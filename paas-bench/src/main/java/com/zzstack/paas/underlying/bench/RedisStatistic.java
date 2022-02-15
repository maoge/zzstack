package com.zzstack.paas.underlying.bench;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.collect.RedisInfoCollector;
import com.zzstack.paas.underlying.constants.BenchConstants;

public class RedisStatistic {

    private static Logger logger = LoggerFactory.getLogger(BenchConstants.LOGGER_BENCH_STAT);

    private AtomicLong maxTPS;
    private AtomicLong[] normalCntVec;
    private ScheduledExecutorService statRunnerExec;
    private Runnable statRunner;
    private ScheduledExecutorService statPrintExec;
    private Runnable statPrinter;
    private long lastTotalCnt;
    private long begTS;
    private long lastTS;
    private long lastTPS;
    private long avgTPS;

    private final String tag;

    private static final long STAT_INTERVAL = 1000L;
    private static final long PRINT_INTERVAL = 5000L;

    public RedisStatistic(AtomicLong maxTPS, AtomicLong[] normalCntVec, String tag) {
        this.maxTPS = maxTPS;
        this.normalCntVec = normalCntVec;
        this.lastTotalCnt = 0L;
        this.tag = tag;

        this.begTS = System.currentTimeMillis();
        this.lastTS = this.begTS;

        this.statRunner = new RedisStatRunner(this);
        this.statRunnerExec = Executors.newSingleThreadScheduledExecutor();
        this.statRunnerExec.scheduleAtFixedRate(this.statRunner, STAT_INTERVAL, STAT_INTERVAL, TimeUnit.MILLISECONDS);

        this.statPrinter = new RedisStatPrinter(this);
        this.statPrintExec = Executors.newSingleThreadScheduledExecutor();
        this.statPrintExec.scheduleAtFixedRate(this.statPrinter, PRINT_INTERVAL, PRINT_INTERVAL, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                RedisStatistic.this.statRunnerExec.shutdown();
                RedisStatistic.this.statPrintExec.shutdown();
            }
        });
    }

    public void StopRunning() {
        if (!this.statRunnerExec.isShutdown()) {
            this.statRunnerExec.shutdown();
        }

        if (!this.statPrintExec.isShutdown()) {
            this.statPrintExec.shutdown();
        }

        computeStatInfo();
        printStatInfo();
    }

    private long getCurrTotal() {
        long currTotalCnt = 0L;

        for (AtomicLong ai : this.normalCntVec) {
            currTotalCnt += ai.get();
        }

        return currTotalCnt;
    }

    public void computeStatInfo() {
        long currTotalCnt = getCurrTotal();
        long currTS = System.currentTimeMillis();

        long diff = currTotalCnt - this.lastTotalCnt;

        if (currTS > this.lastTS) {
            this.lastTPS = diff * 1000L / (currTS - this.lastTS);
            this.avgTPS = currTotalCnt * 1000L / (currTS - this.begTS);

            if (this.lastTPS > this.maxTPS.get()) {
                this.maxTPS.set(this.lastTPS);
            }

            this.lastTS = currTS;
            this.lastTotalCnt = currTotalCnt;
        }
    }

    public void printStatInfo() {
        RedisInfoCollector redisInfoCollector = RedisInfoCollector.get();

        String statInfo = String.format(
                "%s Statistic runs for:%d seconds, total processed:%d, Last TPS:%d, Avg TPS:%d, Max TPS:%d",
                tag, (this.lastTS - this.begTS) / 1000L, this.lastTotalCnt,this.lastTPS, this.avgTPS, this.maxTPS.get());
        logger.info(BenchConstants.LOGGER_SPLIT);
        logger.info(statInfo);
        logger.info(redisInfoCollector.getCollectInfo());
        logger.info(BenchConstants.LOGGER_SPLIT);
    }

    private static class RedisStatRunner implements Runnable {
        private RedisStatistic statistic;

        public RedisStatRunner(RedisStatistic statistic) {
            this.statistic = statistic;
        }

        @Override
        public void run() {
            this.statistic.computeStatInfo();
        }
    }

    private static class RedisStatPrinter implements Runnable {
        private RedisStatistic statistic;

        public RedisStatPrinter(RedisStatistic statistic) {
            this.statistic = statistic;
        }

        public void run() {
            this.statistic.printStatInfo();
        }
    }

}
