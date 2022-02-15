package com.zzstack.paas.underlying.utils.rocketmq.util;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;

public class RocketMQUtil {
    /**
     * @param groupName
     * @param url
     * @return
     */
    public static DefaultMQProducer createProducer(String groupName, String url) {
        DefaultMQProducer producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(url);
        return producer;
    }

    /**
     * Thread shutdown hook for close Reference
     *
     * @param producer current MQProducer
     */
    public static void shutdownHook(final MQProducer producer) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (producer != null) {
                producer.shutdown();
            }
        }));
    }

    /**
     * create DefaultMQPushConsumer
     *
     * @param groupName
     * @param url
     * @return
     */
    public static DefaultMQPushConsumer createConsumer(String groupName, String url) {
        //需要一个consumer group名字作为构造方法的参数，这里为concurrent_consumer
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);
        //同样也要设置NameServer地址
        consumer.setNamesrvAddr(url);
        return consumer;
    }

    /**
     * Thread shutdown hook for close Reference
     *
     * @param consumer current  MQPushConsumer
     */
    public static void shutdownHook(final MQPushConsumer consumer) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (consumer != null) {
                consumer.shutdown();
            }
        }));
    }
}
