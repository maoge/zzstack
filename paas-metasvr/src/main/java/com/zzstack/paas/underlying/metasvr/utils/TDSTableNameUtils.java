package com.zzstack.paas.underlying.metasvr.utils;

public class TDSTableNameUtils {

    public static String getSTableForHost(String servIP, String tag) {
        return String.format("host_%s_%s", tag, servIP.replace(".", "_"));
    }
    
    public static String getSTableForHost(String servIP, String tag, String type) {
        return String.format("host_%s_%s_%s", tag, servIP.replace(".", "_"), type.replace(".", "_"));
    }

}
