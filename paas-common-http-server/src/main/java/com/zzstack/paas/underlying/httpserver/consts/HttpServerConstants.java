package com.zzstack.paas.underlying.httpserver.consts;

public class HttpServerConstants {
    
    private static boolean isLogRequestEnable = false;
    
    public static void setLogRequestEnable(boolean logRequestEnable) {
        isLogRequestEnable = logRequestEnable;
    }
    
    public static boolean logRequestEnable() {
        return isLogRequestEnable;
    }

}
