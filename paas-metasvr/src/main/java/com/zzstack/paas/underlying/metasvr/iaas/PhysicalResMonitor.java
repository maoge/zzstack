package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdPushData;
import com.zzstack.paas.underlying.metasvr.threadpool.WorkerPool;

public class PhysicalResMonitor {
    
    private static Logger logger = LoggerFactory.getLogger(PhysicalResMonitor.class);

    private ArrayBlockingQueue<CollectdPushData> collectdQueue = null;
    
    private Thread collectdProcessThread = null;
    private CollectdDataDispatcher collectdDataDispatcher = null;
    
    private static final int COLLECTD_QUEUE_CAPACITY = 4000;
    
    public PhysicalResMonitor() {
        collectdQueue = new ArrayBlockingQueue<CollectdPushData>(COLLECTD_QUEUE_CAPACITY);
        
        collectdDataDispatcher = new CollectdDataDispatcher(collectdQueue);
        collectdProcessThread = new Thread(collectdDataDispatcher);
        collectdProcessThread.setName("CollectdDataProcessorThread");
        collectdProcessThread.start();
    }
    
    public void release() {
        if (collectdDataDispatcher != null) {
            collectdDataDispatcher.stopRunning();
            collectdProcessThread = null;
        }
    }
    
    public void push(CollectdPushData data) {
        try {
            collectdQueue.put(data);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static class CollectdDataDispatcher implements Runnable {
        
        private ArrayBlockingQueue<CollectdPushData> collectdQueue = null;
        private volatile boolean running = true;
        
        public CollectdDataDispatcher(ArrayBlockingQueue<CollectdPushData> collectdQueue) {
            this.collectdQueue = collectdQueue;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    CollectdPushData collectdData = collectdQueue.take();
                    if (collectdData == null)
                        continue;
                    
                    String servIP = collectdData.getServIP();
                    String body   = collectdData.getData();
                    
                    CollectdDataProcessor collectdDataProcessor = new CollectdDataProcessor(body, servIP);
                    WorkerPool.get().execute(collectdDataProcessor);
                    
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        
        public void stopRunning() {
            running = false;
        }
        
    }

    private static final class CollectdDataProcessor implements Runnable {

        private String servIP;
        private String body;
        
        public CollectdDataProcessor(String body, String servIP) {
            this.body = body;
            this.servIP = servIP;
        }
        
        @Override
        public void run() {
            try {
                CollectdDataMarshell.process(body, servIP);
            } catch (Exception e) {
                logger.error("CollectdDataProcessor error:{}", e.getMessage(), e);
            }
        }
        
    }

}
