package com.zzstack.paas.underlying.metasvr.alarm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum AlarmType {

    ALARM_NONE               (10000, "alarm-none"),
    ALARM_APP_PROC_DOWN      (10001, "app-process-down"),
    ALARM_DISK_HIGH_WATERMARK(10002, "disk-high-watermark"),
    ALARM_MEM_HIGH_WATERMARK (10003, "memory-high-watermark");

    private final int     code;
    private final String  info;

    private static final Map<Integer, AlarmType> map = new HashMap<Integer, AlarmType>();

    static {
        for (AlarmType s : EnumSet.allOf(AlarmType.class)) {
            map.put(s.code, s);
        }
    }

    private AlarmType(int iCode, String sInfo) {
        code             = iCode;
        info             = sInfo;
    }

    public static AlarmType get(int code){
        return map.get(code);
    }

    public int getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public boolean equals(final AlarmType e) {
        return this.code == e.code;
    }

}
