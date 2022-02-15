package com.zzstack.paas.underlying.utils.jvm;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

public class JVMGCUtils {

    private static GarbageCollectorMXBean youngGC;
    private static GarbageCollectorMXBean fullGC;

    private static long lastComputeTS = System.currentTimeMillis();

    private static long lastYoungGCCount = 0L;
    private static long lastYoungGCTime = 0L;

    private static long lastFullGCCount = 0L;
    private static long lastFullGCTime = 0L;

    // private static final String GC_YOUNG_GC_COUNT = "GcYoungGcCount";
    // private static final String GC_YOUNG_GC_TIME = "GcYoungGcTime";

    // private static final String GC_FULL_GC_COUNT = "GcFullGcCount";
    // private static final String GC_FULL_GC_TIME = "GcFullGcTime";


    static {
        List<GarbageCollectorMXBean> gcMxBeanList = ManagementFactory.getGarbageCollectorMXBeans();
        for (final GarbageCollectorMXBean gcMxBean : gcMxBeanList) {
            String gcName = gcMxBean.getName();
            if (gcName == null) {
                continue;
            }

            //G1 Old Generation
            //Garbage collection optimized for short pausetimes Old Collector
            //Garbage collection optimized for throughput Old Collector
            //Garbage collection optimized for deterministic pausetimes Old Collector
            //G1 Young Generation
            //Garbage collection optimized for short pausetimes Young Collector
            //Garbage collection optimized for throughput Young Collector
            //Garbage collection optimized for deterministic pausetimes Young Collector
            if (fullGC == null &&
                    (gcName.endsWith("Old Generation")
                            || "ConcurrentMarkSweep".equals(gcName)
                            || "MarkSweepCompact".equals(gcName)
                            || "PS MarkSweep".equals(gcName))
            ) {
                fullGC = gcMxBean;
            } else if (youngGC == null &&
                    (gcName.endsWith("Young Generation")
                            || "ParNew".equals(gcName)
                            || "Copy".equals(gcName)
                            || "PS Scavenge".equals(gcName))
            ) {
                youngGC = gcMxBean;
            }
        }
    }

    // YGC名称
    public static String getYoungGcName() {
        return youngGC == null ? "" : youngGC.getName();
    }

    // YGC总次数
    public static long getYoungGcCollectionCount() {
        return youngGC == null ? 0 : youngGC.getCollectionCount();
    }

    // YGC总时间
    public static long getYoungGcCollectionTime() {
        return youngGC == null ? 0 : youngGC.getCollectionTime();
    }

    // FGC名称
    public static String getFullGCName() {
        return fullGC == null ? "" : fullGC.getName();
    }

    // FGC总次数
    public static long getFullGcCollectionCount() {
        return fullGC == null ? 0 : fullGC.getCollectionCount();
    }

    // FGC总时间
    public static long getFullGcCollectionTime() {
        return fullGC == null ? 0 : fullGC.getCollectionTime();
    }

    public static void getJvmGcInfo(JSONObject jvm) {
        long currYoungGcCount = getYoungGcCollectionCount();
        long currYoungGcTime = getYoungGcCollectionTime();
        long currFullGcCount = getFullGcCollectionCount();
        long currFullGcTime = getFullGcCollectionTime();
        long currTs = System.currentTimeMillis();
        long time = currTs - lastComputeTS;
        if (time > 0) {
            long youngGcAvgCount = (currYoungGcCount - lastYoungGCCount) * 1000 / time;
            long youngGcAvgTime = (currYoungGcTime - lastYoungGCTime) * 1000 / time;
            long fullGcAvgCount = (currFullGcCount - lastFullGCCount) * 1000 / time;
            long fullGcAvgTime = (currFullGcTime - lastFullGCTime) * 1000 / time;
            jvm.put(FixHeader.HEADER_GC_YOUNG_GC_COUNT, youngGcAvgCount);
            jvm.put(FixHeader.HEADER_GC_YOUNG_GC_TIME, youngGcAvgTime);
            jvm.put(FixHeader.HEADER_GC_FULL_GC_COUNT, fullGcAvgCount);
            jvm.put(FixHeader.HEADER_GC_FULL_GC_TIME, fullGcAvgTime);
        } else {
            jvm.put(FixHeader.HEADER_GC_YOUNG_GC_COUNT, 0);
            jvm.put(FixHeader.HEADER_GC_YOUNG_GC_TIME, 0);
            jvm.put(FixHeader.HEADER_GC_FULL_GC_COUNT, 0);
            jvm.put(FixHeader.HEADER_GC_FULL_GC_TIME, 0);
        }
        lastComputeTS = currTs;
        lastYoungGCCount = currYoungGcCount;
        lastYoungGCTime = currYoungGcTime;
        lastFullGCCount = currFullGcCount;
        lastFullGCTime = currFullGcTime;
    }

}
