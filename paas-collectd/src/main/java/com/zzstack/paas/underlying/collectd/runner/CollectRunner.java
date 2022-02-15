package com.zzstack.paas.underlying.collectd.runner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.collectd.global.CollectdGlobalData;
import com.zzstack.paas.underlying.collectd.probe.Prober;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasCollectException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;

import io.vertx.core.json.JsonObject;

public class CollectRunner {

    private static Logger logger = LoggerFactory.getLogger(CollectRunner.class);

    private ScheduledExecutorService taskInventor;
    private Prober prober;

    public CollectRunner(Prober prober) {
        this.prober = prober;

        ActiveCollectRunner runner = new ActiveCollectRunner(this.prober);
        this.taskInventor = Executors.newScheduledThreadPool(2);
        this.taskInventor.scheduleAtFixedRate(runner, CONSTS.PROBER_COLLECT_INTERVAL, CONSTS.PROBER_COLLECT_INTERVAL, TimeUnit.SECONDS);
    }

    public void destroy() {
        if (taskInventor != null) {
            taskInventor.shutdown();
            taskInventor = null;
        }
    }

    private static class ActiveCollectRunner implements Runnable {

        private final Prober prober;

        public ActiveCollectRunner(Prober prober) {
            super();
            this.prober = prober;
        }

        @Override
        public void run() {
            JsonObject topoJson = CollectdGlobalData.get().getServTopo();
            try {
                prober.doCollect(topoJson);
            } catch (PaasCollectException e) {
                logger.error("prober doCollect exception: {}", e.getMessage(), e);
                return;
            }

            try {
                prober.doReport();
            } catch (PaasCollectException | PaasSdkException e) {
                logger.error("prober doReport exception: {}", e.getMessage(), e);
            }
        }

    }

}
