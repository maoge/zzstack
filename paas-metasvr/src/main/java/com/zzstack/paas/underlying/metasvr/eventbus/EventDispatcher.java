package com.zzstack.paas.underlying.metasvr.eventbus;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.threadpool.WorkerPool;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;

public class EventDispatcher {

    private static Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private EventConsumer eventRunner = null;
    private Thread eventThread = null;
    
    private PulsarClient pulsarClient = null;
    private Consumer<byte[]> consumer = null;
    private ReentrantLock lock = null;

    public EventDispatcher(PulsarClient pulsarClient) {
        this.lock = new ReentrantLock();
        this.pulsarClient = pulsarClient;
        
        init();
    }
    
    private void init() {
        String topic = String.format("persistent://public/default/%s", FixDefs.SYS_EVENT_TOPIC);
        // String topic = String.format("persistent://%s/%s/%s", PAAS_TENANT, PAAS_NAMESPACE, SYS_EVENT_TOPIC);
        String subscriber = SysConfig.get().getEventbusConsumerSubscription();
        
        // 当有堆积时, 一次接收太多会导致 WorkerPool 队列堆满报错
        int recvQueueSize = SysConfig.get().getThreadPoolCoreSize();
        
        try {
            consumer = pulsarClient.newConsumer()
                    .topic(topic)
                    .subscriptionName(subscriber)
                    .receiverQueueSize(recvQueueSize)
                    .ackTimeout(10, TimeUnit.SECONDS)
                    .subscriptionType(SubscriptionType.Exclusive)
                    .subscribe();
            
            eventRunner = new EventConsumer(consumer);
        } catch (Exception e) {
            logger.error("EventDispatcher init error:{}", e.getMessage(), e);
        }
    }
    
    public void start() {
        lock.lock();
        try {
            eventThread = new Thread(eventRunner);
            eventThread.setName("EventBusDispatcher");
            eventThread.setDaemon(true);
            eventThread.start();
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            if (eventRunner != null) {
                eventRunner.stopRunning();
                eventRunner = null;
            }

            if (eventThread != null) {
                eventThread.join();
                eventThread = null;
            }
            
            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (PulsarClientException e) {
            logger.error("EventDispatcher pulsar consumer close exception:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    private static class EventConsumer implements Runnable {

        private Consumer<byte[]> consumer;
        private volatile boolean bRunning = true;
        private Message<byte[]> msg = null;

        public EventConsumer(Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void run() {
            while (bRunning) {
                try {
                    msg = consumer.receive();
                    if (msg != null) {
                        if (!WorkerPool.get().isBusy()) {
                            SysEventProcessor runner = new SysEventProcessor(new String(msg.getData()));
                            WorkerPool.get().execute(runner);
                        } else {
                            TimeUnit.MILLISECONDS.sleep(10);
                        }
                    }

                } catch (PulsarClientException e) {
                    logger.error(e.getMessage(), e);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
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

        public void stopRunning() {
            this.bRunning = false;
        }
    }

}
