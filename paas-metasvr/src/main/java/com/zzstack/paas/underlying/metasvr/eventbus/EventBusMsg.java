package com.zzstack.paas.underlying.metasvr.eventbus;

import org.apache.pulsar.client.api.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;

public class EventBusMsg {

    private static Logger logger = LoggerFactory.getLogger(EventBusMsg.class.getName());

    public static void publishEvent(EventBean evBean) {
        if (evBean == null) {
            logger.error("evBean is null ......");
            return;
        }

        String msg = evBean.asJsonString();
        logger.debug("EventBus public messages:{}", msg);

        EventProc.procWriteOperLog(evBean);

        Producer<byte[]> evBusSender = MetaSvrGlobalRes.get().getProducer();
        if (evBusSender != null) {
            evBusSender.sendAsync(msg.getBytes());
        } else {
            logger.info("event bus not ready!");
        }
    }

}
