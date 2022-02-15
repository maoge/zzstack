package com.zzstack.paas.underlying.constants;

import com.zzstack.paas.underlying.utils.PropertiesUtils;

public class BenchParams {
    
    private static int workerCnt = 1;
    private static int totalTime = 3000;
    private static int type = 1;
    
    public static void init() {
        PropertiesUtils props = PropertiesUtils.getInstance("test");
        workerCnt = props.getInt("workerCnt");
        totalTime = props.getInt("totalTime");
        type      = props.getInt("type");
    }
    
    public static int getWorkerCnt() {
        return workerCnt;
    }
    
    public static int getTotalTime() {
        return totalTime;
    }
    
    public static int getType() {
        return type;
    }

}
