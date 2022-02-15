package com.zzstack.paas.underlying.metasvr.eventbus;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.annotation.KVPair;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class EventBean {

    private static Logger logger = LoggerFactory.getLogger(EventBean.class.getName());

    @KVPair(key = FixHeader.HEADER_EVENT_CODE, val = "")
    private EventType evType;

    @KVPair(key = FixHeader.HEADER_META_SERV_ID, val = "")
    private String metaServID;

    @KVPair(key = FixHeader.HEADER_EVENT_TS, val = "")
    private long eventTs;

    @KVPair(key = FixHeader.HEADER_MSG_BODY, val = "")
    private String msgBody;
    
    @KVPair(key = FixHeader.HEADER_MAGIC_KEY, val = "")
    private String magicKey;

    private static Class<?> CLAZZ;

    static {
        CLAZZ = EventBean.class;
    }

    public EventBean() {
        evType = EventType.EVENT_NONE;
        msgBody = "";

        this.eventTs = System.currentTimeMillis();
        this.metaServID = MetaSvrGlobalRes.get().getMetaServId();
    }

    public EventBean(EventType evType, String msgBody, String key) {
        this.evType = evType;
        this.metaServID = MetaSvrGlobalRes.get().getMetaServId();
        this.eventTs = System.currentTimeMillis();
        this.msgBody = msgBody;
        this.magicKey = key;
    }

    public EventType getEvType() {
        return evType;
    }

    public void setEvType(EventType evType) {
        this.evType = evType;
    }

    public String getMetaServID() {
        return metaServID;
    }

    public void setMetaServID(String metaServID) {
        this.metaServID = metaServID;
    }

    public long getEventTs() {
        return eventTs;
    }

    public void setEventTs(long eventTs) {
        this.eventTs = eventTs;
    }

    public String getMsgBody() {
        return msgBody;
    }

    public void setMsgBody(String msgBody) {
        this.msgBody = msgBody;
    }

    public String getMagicKey() {
        return magicKey;
    }

    public void setMagicKey(String magicKey) {
        this.magicKey = magicKey;
    }

    public String asJsonString() {
        JsonObject jsonObj = new JsonObject();

        Field[] fields = CLAZZ.getDeclaredFields();
        for (Field field : fields) {
            KVPair kv = field.getAnnotation(KVPair.class);
            if (kv == null)
                continue;

            boolean error = false;
            Object obj = null;

            try {
                field.setAccessible(true);
                obj = field.get(this);
                if (obj == null)
                    continue;

                if (obj instanceof EventType) {
                    obj = ((EventType) obj).getCode();
                } else if (obj instanceof String) {
                    if (((String) obj).isEmpty())
                        continue;
                }

                if (obj != null) {
                    String key = kv.key();
                    jsonObj.put(key, obj);
                }
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage(), e);
                error = true;
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
                error = true;
            } finally {
                if (error)
                    continue;
            }
        }

        return jsonObj.toString();
    }

}
