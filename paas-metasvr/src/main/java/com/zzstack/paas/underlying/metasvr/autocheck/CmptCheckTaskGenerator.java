package com.zzstack.paas.underlying.metasvr.autocheck;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.autocheck.probe.CmptProber;
import com.zzstack.paas.underlying.metasvr.autocheck.util.AutoCheckTaskUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.threadpool.WorkerPool;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class CmptCheckTaskGenerator {

    private static Logger logger = LoggerFactory.getLogger("CmptCheckTaskGenerator");

    private ScheduledExecutorService scheduledExecutorService;
    private Runnable taskGenerator;
    
    private PulsarClient pulsarClient = null;
    private Producer<byte[]> producer = null;
    private Consumer<byte[]> consumer = null;
    
    private Thread checkTaskDispatchThread = null;
    private CheckTaskDispatcher checkTaskDispatcher = null;
    
    private static final int DELAY = SysConfig.get().getCollectInterval();
    private static final int PERIOD = SysConfig.get().getCollectInterval();
    
    private static final String SYS_CHECK_TASK_SUBSCRIBE = "SUB_SYS_CHECK";
    private static final int SYS_CHECK_TASK_RECV_QUEUE_SIZE = 1;
    
    public CmptCheckTaskGenerator() {
        init();
    }

    private void init() {
        try {
            String topic = String.format("persistent://public/default/%s", FixDefs.SYS_CHECK_TASK);
            
            pulsarClient = MetaSvrGlobalRes.get().getPulsarClient();
    
            producer = pulsarClient.newProducer()
                    .topic(topic)
                    .batchingMaxPublishDelay(1, TimeUnit.MILLISECONDS)
                    .sendTimeout(1, TimeUnit.SECONDS)
                    .blockIfQueueFull(true)
                    .create();
            
            consumer = pulsarClient.newConsumer()
                    .topic(topic)
                    .subscriptionName(SYS_CHECK_TASK_SUBSCRIBE)
                    .receiverQueueSize(SYS_CHECK_TASK_RECV_QUEUE_SIZE)
                    .ackTimeout(10, TimeUnit.SECONDS)
                    .subscriptionType(SubscriptionType.Shared)
                    .subscribe();
            
            checkTaskDispatcher = new CheckTaskDispatcher(consumer);
            checkTaskDispatchThread = new Thread(checkTaskDispatcher);
            checkTaskDispatchThread.setName("CheckTaskDispatcher");
            checkTaskDispatchThread.setDaemon(true);
            checkTaskDispatchThread.start();
            
            taskGenerator = new CheckTaskGenerator(producer);
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleWithFixedDelay(taskGenerator, DELAY, PERIOD, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            logger.error("CmptCheckTaskGenerator init exception:{}", e.getMessage(), e);
        }
    }
    
    public void destory() {
        try {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdownNow();
                scheduledExecutorService = null;
            }
            
            if (checkTaskDispatcher != null) {
                checkTaskDispatcher.stopRunning();
                checkTaskDispatcher = null;
            }

            if (checkTaskDispatchThread != null) {
                checkTaskDispatchThread.join();
                checkTaskDispatchThread = null;
            }
            
            if (producer != null) {
                producer.close();
                producer = null;
            }
            
            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
            
        } catch (PulsarClientException e) {
            logger.error("release pulsar producer and client error:{}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("CmptCheckTaskGenerator destory error:{}", e.getMessage(), e);
        }
    }
    
    private static class CheckTaskGenerator implements Runnable {
        
        private Producer<byte[]> producer = null;
        
        public CheckTaskGenerator(Producer<byte[]> producer) {
            this.producer = producer;
        }
        
        @Override
        public void run() {
            if (!MetaSvrGlobalRes.get().isCollectEnabled()) {
                return;
            }
            
            MetaSvrGlobalRes meta = MetaSvrGlobalRes.get();
            CmptMeta cmptMeta = meta.getCmptMeta();
            
            Map<String, PaasService> serviceMap = cmptMeta.getMetaServiceMap();
            Set<Entry<String, PaasService>> entrySet = serviceMap.entrySet();
            for (Entry<String, PaasService> entry : entrySet) {
                PaasService paasService = entry.getValue();
                // 服务未部署和伪部署的组件不检测
                if (!paasService.isDeployed() || paasService.getPseudoDeployFlag().equals(CONSTS.DEPLOY_FLAG_PSEUDO))
                    continue;
                
                if (!CmptProberFactory.isProbeReady(paasService.getServType()))
                    continue;
                
                String servInstId = paasService.getInstId();
                String servType = paasService.getServType();
                long validTimestamp = System.currentTimeMillis() + SysConfig.get().getCollectInterval();
                
                String msg = AutoCheckTaskUtils.marshell(servInstId, servType, validTimestamp);
                try {
                    producer.send(msg.getBytes());
                } catch (PulsarClientException e) {
                    logger.error("send check task error:{}", e.getMessage(), e);
                }
            }
        }
        
    }
    
    private static class CheckTaskDispatcher implements Runnable {

        private Consumer<byte[]> consumer = null;
        private volatile boolean bRunning = true;
        private Message<byte[]> msg = null;
        
        public CheckTaskDispatcher(Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }
        
        @Override
        public void run() {
            while (bRunning) {
                
                try {
                    waitForIdle();
                    
                    msg = consumer.receive();
                    if (msg == null)
                        continue;
                    
                    JsonObject json = AutoCheckTaskUtils.unmarshell(msg.getData());
                    String servInstId = json.getString(FixHeader.HEADER_SERV_INST_ID);
                    String servType = json.getString(FixHeader.HEADER_SERV_TYPE);
                    long validTimestamp = json.getLong(FixHeader.HEADER_VALID_TIMESTAMP);
                    
                    // 已经过期的采集事件直接跳过
                    if (System.currentTimeMillis() > validTimestamp)
                        continue;
                    
                    CmptProber prober = CmptProberFactory.getCmptProber(servInstId, servType);
                    if (prober == null)
                        continue;
                    
                    // logger.info("recv:{}", json);
                    
                    CmptStatusChecker checker = new CmptStatusChecker(servInstId, servType, prober);
                    WorkerPool.get().execute(checker);

                } catch (PulsarClientException e) {
                    logger.error("CheckTaskDispatcher consume pulsar msg exception:{}", e.getMessage(), e);
                } finally {
                    try {
                        if (msg != null) {
                            consumer.acknowledge(msg);
                            msg = null;
                        }
                    } catch (PulsarClientException e) {
                        
                    }
                }
                
            }
        }
        
        private void waitForIdle() {
            while (WorkerPool.get().isBusy()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
        
        public void stopRunning() {
            this.bRunning = false;
        }
        
    }

}
