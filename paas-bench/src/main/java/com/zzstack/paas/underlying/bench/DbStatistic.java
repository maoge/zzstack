package com.zzstack.paas.underlying.bench;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.constants.BenchConstants;

public class DbStatistic {
    
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

    public DbStatistic(AtomicLong maxTPS, AtomicLong[] normalCntVec, String tag) {
        this.maxTPS = maxTPS;
        this.normalCntVec = normalCntVec;
        this.lastTotalCnt = 0L;
        this.tag = tag;

        this.begTS = System.currentTimeMillis();
        this.lastTS = this.begTS;

        this.statRunner = new DbStatRunner(this);
        this.statRunnerExec = Executors.newSingleThreadScheduledExecutor();
        this.statRunnerExec.scheduleAtFixedRate(this.statRunner, STAT_INTERVAL, STAT_INTERVAL, TimeUnit.MILLISECONDS);

        this.statPrinter = new DbStatPrinter(this);
        this.statPrintExec = Executors.newSingleThreadScheduledExecutor();
        this.statPrintExec.scheduleAtFixedRate(this.statPrinter, PRINT_INTERVAL, PRINT_INTERVAL, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                DbStatistic.this.statRunnerExec.shutdown();
                DbStatistic.this.statPrintExec.shutdown();
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
        String statInfo = String.format(
                "%s Statistic runs for:%d seconds, total processed:%d, Last TPS:%d, Avg TPS:%d, Max TPS:%d",
                tag, Long.valueOf((this.lastTS - this.begTS) / 1000L), Long.valueOf(this.lastTotalCnt),
                Long.valueOf(this.lastTPS), Long.valueOf(this.avgTPS), Long.valueOf(this.maxTPS.get()));
        logger.info(BenchConstants.LOGGER_SPLIT);
        logger.info(statInfo);
        logger.info(BenchConstants.LOGGER_SPLIT);
    }

    private static class DbStatRunner implements Runnable {
        private DbStatistic statistic;

        public DbStatRunner(DbStatistic statistic) {
            this.statistic = statistic;
        }

        public void run() {
            this.statistic.computeStatInfo();
        }
    }

    private static class DbStatPrinter implements Runnable {
        private DbStatistic statistic;

        public DbStatPrinter(DbStatistic statistic) {
            this.statistic = statistic;
        }

        public void run() {
            this.statistic.printStatInfo();
        }
    }

}
