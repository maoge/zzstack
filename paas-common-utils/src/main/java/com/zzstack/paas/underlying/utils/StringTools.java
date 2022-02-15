package com.zzstack.paas.underlying.utils;

public class StringTools {
    
    public static boolean notNullAndEmpty(String s) {
        return s != null && !s.isEmpty();
    }
    
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

}
