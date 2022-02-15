package com.zzstack.paas.underlying.metasvr.utils;

public class CacheKeyUtils {
    
    public static final String SESSION_KEY_PREFIX = "session:";
    
    public static String getRedisSessionKey(String accName) {
        return String.format("%s%s", SESSION_KEY_PREFIX, accName);
    }

}
