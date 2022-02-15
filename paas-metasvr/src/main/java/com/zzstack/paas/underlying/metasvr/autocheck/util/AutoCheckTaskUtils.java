package com.zzstack.paas.underlying.metasvr.autocheck.util;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class AutoCheckTaskUtils {
    
    public static String marshell(String servInstId, String servType, long validTimestamp) {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_SERV_INST_ID, servInstId);
        json.put(FixHeader.HEADER_SERV_TYPE, servType);
        json.put(FixHeader.HEADER_VALID_TIMESTAMP, validTimestamp);
        return json.toString();
    }
    
    public static JsonObject unmarshell(byte[] data) {
        String msg = new String(data);
        return new JsonObject(msg);
    }

}
