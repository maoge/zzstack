package com.zzstack.paas.underlying.metasvr.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DateFmtUtil {
    
    public static final String ISO_DATETIME_FMT = "yyyy-MM-dd HH:mm:ss";

    private static ConcurrentMap<String, SimpleDateFormat> concurrentMap;
    
    static {
        concurrentMap = new ConcurrentHashMap<String, SimpleDateFormat>();
    }

    private DateFmtUtil() {

    }

    public static String format(long ts) {
        SimpleDateFormat formatter = concurrentMap.computeIfAbsent(ISO_DATETIME_FMT, v -> new SimpleDateFormat(ISO_DATETIME_FMT));
        Date date = new Date(ts);
        return formatter.format(date);
    }
    
}
