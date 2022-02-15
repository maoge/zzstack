package com.zzstack.paas.underlying.metasvr.utils;

public class StringUtils {
    
    public static boolean isNull(Object obj) {
        return (obj == null || "".equals(obj)) ? true : false;
    }

}
