package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.FixHeader;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * 应用的jvm的使用监控信息
 */
public class PassJvmInfo extends BeanMapper {
    /**
     * 时间戳
     */
    private Long ts;
    /**
     * 组件实例ID
     */
    private String instantId;
    /**
     * java 版本
     */
    private String javaVersion;
    /**
     * GC指标-YGC总次数
     */
    private Integer gcYoungGcCount;
    /**
     * GC指标-YGC总时间
     */
    private Integer gcYoungGcTime;
    /**
     * GC指标-FGC总次数
     */
    private Integer gcFullGcCount;
    /**
     * GC指标-FGC总时间
     */
    private Integer gcFullGcTime;
    /**
     * 线程指标-Daemon线程总量
     */
    private Integer threadDaemonThreadCount;
    /**
     * 线程指标-当前线程总量
     */
    private Integer threadCount;
    /**
     * 线程指标-获取线程数量峰值
     */
    private Integer threadPeakThreadCount;
    /**
     * 线程指标-死锁线程总量
     */
    private Integer threadDeadLockedThreadCount;
    /**
     * 内存指标-EdenSpace
     * 初始内存大小（字节）
     */
    private Long memEdenInit;
    /**
     * 内存指标-EdenSpace
     * 当前使用内存大小（字节）
     */
    private Long memEdenUsed;
    /**
     * 内存指标-EdenSpace
     * 已经申请分配的内存大小（字节）
     */
    private Long memEdenCommitted;
    /**
     * 内存指标-EdenSpace
     * 最大内存大小（字节）
     */
    private Long memEdenMax;
    /**
     * 内存指标-EdenSpace
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memEdenUsedPercent;
    /**
     * 内存指标-SurvivorSpace
     * 初始内存大小（字节）
     */
    private Long memSurvivorInit;
    /**
     * 内存指标-SurvivorSpace
     * 当前使用内存大小（字节）
     */
    private Long memSurvivorUsed;
    /**
     * 内存指标-SurvivorSpace
     * 已经申请分配的内存大小（字节）
     */
    private Long memSurvivorCommitted;
    /**
     * 内存指标-SurvivorSpace
     * 最大内存大小（字节）
     */
    private Long memSurvivorMax;
    /**
     * 内存指标-SurvivorSpace
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memSurvivorUsedPercent;
    /**
     * 内存指标-OldGeneration
     * 初始内存大小（字节）
     */
    private Long memOldInit;
    /**
     * 内存指标-OldGeneration
     * 当前使用内存大小（字节）
     */
    private Long memOldUsed;
    /**
     * 内存指标-OldGeneration
     * 已经申请分配的内存大小（字节）
     */
    private Long memOldCommitted;
    /**
     * 内存指标-OldGeneration
     * 最大内存大小（字节）
     */
    private Long memOldMax;
    /**
     * 内存指标-OldGeneration
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memOldUsedPercent;
    /**
     * 内存指标-PermGeneration
     * 初始内存大小（字节）
     */
    private Long memPermInit;
    /**
     * 内存指标-PermGeneration
     * 当前使用内存大小（字节）
     */
    private Long memPermUsed;
    /**
     * 内存指标-PermGeneration
     * 已经申请分配的内存大小（字节）
     */
    private Long memPermCommitted;
    /**
     * 内存指标-PermGeneration
     * 最大内存大小（字节）
     */
    private Long memPermMax;
    /**
     * 内存指标-PermGeneration
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memPermUsedPercent;
    /**
     * 内存指标-CodeCache
     * 初始内存大小（字节）
     */
    private Long memCodeInit;
    /**
     * 内存指标-CodeCache
     * 当前使用内存大小（字节）
     */
    private Long memCodeUsed;
    /**
     * 内存指标-CodeCache
     * 已经申请分配的内存大小（字节）
     */
    private Long memCodeCommitted;
    /**
     * 内存指标-CodeCache
     * 最大内存大小（字节）
     */
    private Long memCodeMax;
    /**
     * 内存指标-CodeCache
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memCodeUsedPercent;
    /**
     * 内存指标-Heap
     * 初始内存大小（字节）
     */
    private Long memHeapInit;
    /**
     * 内存指标-Heap
     * 当前使用内存大小（字节）
     */
    private Long memHeapUsed;
    /**
     * 内存指标-Heap
     * 已经申请分配的内存大小（字节）
     */
    private Long memHeapCommitted;
    /**
     * 内存指标-Heap
     * 最大内存大小（字节）
     */
    private Long memHeapMax;
    /**
     * 内存指标-Heap
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memHeapUsedPercent;
    /**
     * 内存指标-NoneHeap
     * 初始内存大小（字节）
     */
    private Long memNoHeapInit;
    /**
     * 内存指标-NoneHeap
     * 当前使用内存大小（字节）
     */
    private Long memNoHeapUsed;
    /**
     * 内存指标-NoneHeap
     * 已经申请分配的内存大小（字节）
     */
    private Long memNoHeapCommitted;
    /**
     * 内存指标-NoneHeap
     * 最大内存大小（字节）
     */
    private Long memNoHeapMax;
    /**
     * 内存指标-NoneHeap
     * 已经申请分配内存与最大内存大小的百分比
     */
    private Double memNoHeapUsedPercent;

    public PassJvmInfo() {
        super();
    }

    public PassJvmInfo(Long ts, String instantId, String javaVersion, Integer gcYoungGcCount, Integer gcYoungGcTime,
                       Integer gcFullGcCount, Integer gcFullGcTime, Integer threadDaemonThreadCount, Integer threadCount,
                       Integer threadPeakThreadCount, Integer threadDeadLockedThreadCount, Long memEdenInit,
                       Long memEdenUsed, Long memEdenCommitted, Long memEdenMax, Double memEdenUsedPercent,
                       Long memSurvivorInit, Long memSurvivorUsed, Long memSurvivorCommitted, Long memSurvivorMax,
                       Double memSurvivorUsedPercent, Long memOldInit, Long memOldUsed, Long memOldCommitted,
                       Long memOldMax, Double memOldUsedPercent, Long memPermInit, Long memPermUsed,
                       Long memPermCommitted, Long memPermMax, Double memPermUsedPercent, Long memCodeInit,
                       Long memCodeUsed, Long memCodeCommitted, Long memCodeMax, Double memCodeUsedPercent,
                       Long memHeapInit, Long memHeapUsed, Long memHeapCommitted, Long memHeapMax,
                       Double memHeapUsedPercent, Long memNoHeapInit, Long memNoHeapUsed, Long memNoHeapCommitted,
                       Long memNoHeapMax, Double memNoHeapUsedPercent) {
        this.ts = ts;
        this.instantId = instantId;
        this.javaVersion = javaVersion;
        this.gcYoungGcCount = gcYoungGcCount;
        this.gcYoungGcTime = gcYoungGcTime;
        this.gcFullGcCount = gcFullGcCount;
        this.gcFullGcTime = gcFullGcTime;
        this.threadDaemonThreadCount = threadDaemonThreadCount;
        this.threadCount = threadCount;
        this.threadPeakThreadCount = threadPeakThreadCount;
        this.threadDeadLockedThreadCount = threadDeadLockedThreadCount;
        this.memEdenInit = memEdenInit;
        this.memEdenUsed = memEdenUsed;
        this.memEdenCommitted = memEdenCommitted;
        this.memEdenMax = memEdenMax;
        this.memEdenUsedPercent = memEdenUsedPercent;
        this.memSurvivorInit = memSurvivorInit;
        this.memSurvivorUsed = memSurvivorUsed;
        this.memSurvivorCommitted = memSurvivorCommitted;
        this.memSurvivorMax = memSurvivorMax;
        this.memSurvivorUsedPercent = memSurvivorUsedPercent;
        this.memOldInit = memOldInit;
        this.memOldUsed = memOldUsed;
        this.memOldCommitted = memOldCommitted;
        this.memOldMax = memOldMax;
        this.memOldUsedPercent = memOldUsedPercent;
        this.memPermInit = memPermInit;
        this.memPermUsed = memPermUsed;
        this.memPermCommitted = memPermCommitted;
        this.memPermMax = memPermMax;
        this.memPermUsedPercent = memPermUsedPercent;
        this.memCodeInit = memCodeInit;
        this.memCodeUsed = memCodeUsed;
        this.memCodeCommitted = memCodeCommitted;
        this.memCodeMax = memCodeMax;
        this.memCodeUsedPercent = memCodeUsedPercent;
        this.memHeapInit = memHeapInit;
        this.memHeapUsed = memHeapUsed;
        this.memHeapCommitted = memHeapCommitted;
        this.memHeapMax = memHeapMax;
        this.memHeapUsedPercent = memHeapUsedPercent;
        this.memNoHeapInit = memNoHeapInit;
        this.memNoHeapUsed = memNoHeapUsed;
        this.memNoHeapCommitted = memNoHeapCommitted;
        this.memNoHeapMax = memNoHeapMax;
        this.memNoHeapUsedPercent = memNoHeapUsedPercent;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getInstantId() {
        return instantId;
    }

    public void setInstantId(String instantId) {
        this.instantId = instantId;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public Integer getGcYoungGcCount() {
        return gcYoungGcCount;
    }

    public void setGcYoungGcCount(Integer gcYoungGcCount) {
        this.gcYoungGcCount = gcYoungGcCount;
    }

    public Integer getGcYoungGcTime() {
        return gcYoungGcTime;
    }

    public void setGcYoungGcTime(Integer gcYoungGcTime) {
        this.gcYoungGcTime = gcYoungGcTime;
    }

    public Integer getGcFullGcCount() {
        return gcFullGcCount;
    }

    public void setGcFullGcCount(Integer gcFullGcCount) {
        this.gcFullGcCount = gcFullGcCount;
    }

    public Integer getGcFullGcTime() {
        return gcFullGcTime;
    }

    public void setGcFullGcTime(Integer gcFullGcTime) {
        this.gcFullGcTime = gcFullGcTime;
    }

    public Integer getThreadDaemonThreadCount() {
        return threadDaemonThreadCount;
    }

    public void setThreadDaemonThreadCount(Integer threadDaemonThreadCount) {
        this.threadDaemonThreadCount = threadDaemonThreadCount;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public Integer getThreadPeakThreadCount() {
        return threadPeakThreadCount;
    }

    public void setThreadPeakThreadCount(Integer threadPeakThreadCount) {
        this.threadPeakThreadCount = threadPeakThreadCount;
    }

    public Integer getThreadDeadLockedThreadCount() {
        return threadDeadLockedThreadCount;
    }

    public void setThreadDeadLockedThreadCount(Integer threadDeadLockedThreadCount) {
        this.threadDeadLockedThreadCount = threadDeadLockedThreadCount;
    }

    public Long getMemEdenInit() {
        return memEdenInit;
    }

    public void setMemEdenInit(Long memEdenInit) {
        this.memEdenInit = memEdenInit;
    }

    public Long getMemEdenUsed() {
        return memEdenUsed;
    }

    public void setMemEdenUsed(Long memEdenUsed) {
        this.memEdenUsed = memEdenUsed;
    }

    public Long getMemEdenCommitted() {
        return memEdenCommitted;
    }

    public void setMemEdenCommitted(Long memEdenCommitted) {
        this.memEdenCommitted = memEdenCommitted;
    }

    public Long getMemEdenMax() {
        return memEdenMax;
    }

    public void setMemEdenMax(Long memEdenMax) {
        this.memEdenMax = memEdenMax;
    }

    public Double getMemEdenUsedPercent() {
        return memEdenUsedPercent;
    }

    public void setMemEdenUsedPercent(Double memEdenUsedPercent) {
        this.memEdenUsedPercent = memEdenUsedPercent;
    }

    public Long getMemSurvivorInit() {
        return memSurvivorInit;
    }

    public void setMemSurvivorInit(Long memSurvivorInit) {
        this.memSurvivorInit = memSurvivorInit;
    }

    public Long getMemSurvivorUsed() {
        return memSurvivorUsed;
    }

    public void setMemSurvivorUsed(Long memSurvivorUsed) {
        this.memSurvivorUsed = memSurvivorUsed;
    }

    public Long getMemSurvivorCommitted() {
        return memSurvivorCommitted;
    }

    public void setMemSurvivorCommitted(Long memSurvivorCommitted) {
        this.memSurvivorCommitted = memSurvivorCommitted;
    }

    public Long getMemSurvivorMax() {
        return memSurvivorMax;
    }

    public void setMemSurvivorMax(Long memSurvivorMax) {
        this.memSurvivorMax = memSurvivorMax;
    }

    public Double getMemSurvivorUsedPercent() {
        return memSurvivorUsedPercent;
    }

    public void setMemSurvivorUsedPercent(Double memSurvivorUsedPercent) {
        this.memSurvivorUsedPercent = memSurvivorUsedPercent;
    }

    public Long getMemOldInit() {
        return memOldInit;
    }

    public void setMemOldInit(Long memOldInit) {
        this.memOldInit = memOldInit;
    }

    public Long getMemOldUsed() {
        return memOldUsed;
    }

    public void setMemOldUsed(Long memOldUsed) {
        this.memOldUsed = memOldUsed;
    }

    public Long getMemOldCommitted() {
        return memOldCommitted;
    }

    public void setMemOldCommitted(Long memOldCommitted) {
        this.memOldCommitted = memOldCommitted;
    }

    public Long getMemOldMax() {
        return memOldMax;
    }

    public void setMemOldMax(Long memOldMax) {
        this.memOldMax = memOldMax;
    }

    public Double getMemOldUsedPercent() {
        return memOldUsedPercent;
    }

    public void setMemOldUsedPercent(Double memOldUsedPercent) {
        this.memOldUsedPercent = memOldUsedPercent;
    }

    public Long getMemPermInit() {
        return memPermInit;
    }

    public void setMemPermInit(Long memPermInit) {
        this.memPermInit = memPermInit;
    }

    public Long getMemPermUsed() {
        return memPermUsed;
    }

    public void setMemPermUsed(Long memPermUsed) {
        this.memPermUsed = memPermUsed;
    }

    public Long getMemPermCommitted() {
        return memPermCommitted;
    }

    public void setMemPermCommitted(Long memPermCommitted) {
        this.memPermCommitted = memPermCommitted;
    }

    public Long getMemPermMax() {
        return memPermMax;
    }

    public void setMemPermMax(Long memPermMax) {
        this.memPermMax = memPermMax;
    }

    public Double getMemPermUsedPercent() {
        return memPermUsedPercent;
    }

    public void setMemPermUsedPercent(Double memPermUsedPercent) {
        this.memPermUsedPercent = memPermUsedPercent;
    }

    public Long getMemCodeInit() {
        return memCodeInit;
    }

    public void setMemCodeInit(Long memCodeInit) {
        this.memCodeInit = memCodeInit;
    }

    public Long getMemCodeUsed() {
        return memCodeUsed;
    }

    public void setMemCodeUsed(Long memCodeUsed) {
        this.memCodeUsed = memCodeUsed;
    }

    public Long getMemCodeCommitted() {
        return memCodeCommitted;
    }

    public void setMemCodeCommitted(Long memCodeCommitted) {
        this.memCodeCommitted = memCodeCommitted;
    }

    public Long getMemCodeMax() {
        return memCodeMax;
    }

    public void setMemCodeMax(Long memCodeMax) {
        this.memCodeMax = memCodeMax;
    }

    public Double getMemCodeUsedPercent() {
        return memCodeUsedPercent;
    }

    public void setMemCodeUsedPercent(Double memCodeUsedPercent) {
        this.memCodeUsedPercent = memCodeUsedPercent;
    }

    public Long getMemHeapInit() {
        return memHeapInit;
    }

    public void setMemHeapInit(Long memHeapInit) {
        this.memHeapInit = memHeapInit;
    }

    public Long getMemHeapUsed() {
        return memHeapUsed;
    }

    public void setMemHeapUsed(Long memHeapUsed) {
        this.memHeapUsed = memHeapUsed;
    }

    public Long getMemHeapCommitted() {
        return memHeapCommitted;
    }

    public void setMemHeapCommitted(Long memHeapCommitted) {
        this.memHeapCommitted = memHeapCommitted;
    }

    public Long getMemHeapMax() {
        return memHeapMax;
    }

    public void setMemHeapMax(Long memHeapMax) {
        this.memHeapMax = memHeapMax;
    }

    public Double getMemHeapUsedPercent() {
        return memHeapUsedPercent;
    }

    public void setMemHeapUsedPercent(Double memHeapUsedPercent) {
        this.memHeapUsedPercent = memHeapUsedPercent;
    }

    public Long getMemNoHeapInit() {
        return memNoHeapInit;
    }

    public void setMemNoHeapInit(Long memNoHeapInit) {
        this.memNoHeapInit = memNoHeapInit;
    }

    public Long getMemNoHeapUsed() {
        return memNoHeapUsed;
    }

    public void setMemNoHeapUsed(Long memNoHeapUsed) {
        this.memNoHeapUsed = memNoHeapUsed;
    }

    public Long getMemNoHeapCommitted() {
        return memNoHeapCommitted;
    }

    public void setMemNoHeapCommitted(Long memNoHeapCommitted) {
        this.memNoHeapCommitted = memNoHeapCommitted;
    }

    public Long getMemNoHeapMax() {
        return memNoHeapMax;
    }

    public void setMemNoHeapMax(Long memNoHeapMax) {
        this.memNoHeapMax = memNoHeapMax;
    }

    public Double getMemNoHeapUsedPercent() {
        return memNoHeapUsedPercent;
    }

    public void setMemNoHeapUsedPercent(Double memNoHeapUsedPercent) {
        this.memNoHeapUsedPercent = memNoHeapUsedPercent;
    }

    public static PassJvmInfo convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty()) {
            return null;
        }
        long ts = getFixDataAsLong(mapper, FixHeader.HEADER_TS);
        String instantId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        String javaVersion = getFixDataAsString(mapper, FixHeader.HEADER_JAVA_VERSION);
        int gcYoungGcCount = getFixDataAsInt(mapper, FixHeader.HEADER_GC_YOUNG_GC_COUNT);
        int gcYoungGcTime = getFixDataAsInt(mapper, FixHeader.HEADER_GC_YOUNG_GC_TIME);
        int gcFullGcCount = getFixDataAsInt(mapper, FixHeader.HEADER_GC_FULL_GC_COUNT);
        int gcFullGcTime = getFixDataAsInt(mapper, FixHeader.HEADER_GC_FULL_GC_TIME);
        int threadDaemonThreadCount = getFixDataAsInt(mapper, FixHeader.HEADER_THREAD_DAEMON_THREAD_COUNT);
        int threadCount = getFixDataAsInt(mapper, FixHeader.HEADER_THREAD_COUNT);
        int threadPeakThreadCount = getFixDataAsInt(mapper, FixHeader.HEADER_THREAD_PEEK_THREAD_COUNT);
        int threadDeadLockedThreadCount = getFixDataAsInt(mapper, FixHeader.HEADER_THREAD_DEAD_LOCKED_THREAD_COUNT);
        long memEdenInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_EDEN_INIT);
        long memEdenUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_EDEN_USED);
        long memEdenCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_EDEN_COMMITTED);
        long memEdenMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_EDEN_MAX);
        double memEdenUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_EDEN_USEDPERCENT);
        long memSurvivorInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_SURVIVOR_INIT);
        long memSurvivorUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_SURVIVOR_USED);
        long memSurvivorCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_SURVIVOR_COMMITTED);
        long memSurvivorMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_SURVIVOR_MAX);
        double memSurvivorUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_SURVIVOR_USEDPERCENT);
        long memOldInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_OLD_INIT);
        long memOldUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_OLD_USED);
        long memOldCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_OLD_COMMITTED);
        long memOldMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_OLD_MAX);
        double memOldUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_OLD_USEDPERCENT);
        long memPermInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_PERM_INIT);
        long memPermUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_PERM_USED);
        long memPermCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_PERM_COMMITTED);
        long memPermMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_PERM_MAX);
        double memPermUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_PERM_USEDPERCENT);
        long memCodeInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_CODE_INIT);
        long memCodeUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_CODE_USED);
        long memCodeCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_CODE_COMMITTED);
        long memCodeMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_CODE_MAX);
        double memCodeUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_CODE_USEDPERCENT);
        long memHeapInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_HEAP_INIT);
        long memHeapUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_HEAP_USED);
        long memHeapCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_HEAP_COMMITTED);
        long memHeapMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_HEAP_MAX);
        double memHeapUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_HEAP_USEDPERCENT);
        long memNoHeapInit = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_NOHEAP_INIT);
        long memNoHeapUsed = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_NOHEAP_USED);
        long memNoHeapCommitted = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_NOHEAP_COMMITTED);
        long memNoHeapMax = getFixDataAsLong(mapper, FixHeader.HEADER_MEM_NOHEAP_MAX);
        double memNoHeapUsedPercent = getFixDataAsDouble(mapper, FixHeader.HEADER_MEM_NOHEAP_USEDPERCENT);
        return new PassJvmInfo(ts, instantId, javaVersion, gcYoungGcCount, gcYoungGcTime, gcFullGcCount, gcFullGcTime,
                threadDaemonThreadCount, threadCount, threadPeakThreadCount, threadDeadLockedThreadCount, memEdenInit,
                memEdenUsed, memEdenCommitted, memEdenMax, memEdenUsedPercent, memSurvivorInit, memSurvivorUsed,
                memSurvivorCommitted, memSurvivorMax, memSurvivorUsedPercent, memOldInit, memOldUsed, memOldCommitted,
                memOldMax, memOldUsedPercent, memPermInit, memPermUsed, memPermCommitted, memPermMax, memPermUsedPercent,
                memCodeInit, memCodeUsed, memCodeCommitted, memCodeMax, memCodeUsedPercent, memHeapInit, memHeapUsed,
                memHeapCommitted, memHeapMax, memHeapUsedPercent, memNoHeapInit, memNoHeapUsed, memNoHeapCommitted,
                memNoHeapMax, memNoHeapUsedPercent);
    }

    public JsonObject toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_TS, this.ts);
        retval.put(FixHeader.HEADER_INST_ID, this.instantId);
        retval.put(FixHeader.HEADER_JAVA_VERSION, this.javaVersion);
        retval.put(FixHeader.HEADER_GC_YOUNG_GC_COUNT, this.gcYoungGcCount);
        retval.put(FixHeader.HEADER_GC_YOUNG_GC_TIME, this.gcYoungGcTime);
        retval.put(FixHeader.HEADER_GC_FULL_GC_COUNT, this.gcFullGcCount);
        retval.put(FixHeader.HEADER_GC_FULL_GC_TIME, this.gcFullGcTime);
        retval.put(FixHeader.HEADER_THREAD_COUNT, this.threadCount);
        retval.put(FixHeader.HEADER_THREAD_DAEMON_THREAD_COUNT, this.threadDaemonThreadCount);
        retval.put(FixHeader.HEADER_THREAD_PEEK_THREAD_COUNT, this.threadPeakThreadCount);
        retval.put(FixHeader.HEADER_THREAD_DEAD_LOCKED_THREAD_COUNT, this.threadDeadLockedThreadCount);
        retval.put(FixHeader.HEADER_MEM_EDEN_INIT, this.memEdenInit);
        retval.put(FixHeader.HEADER_MEM_EDEN_USED, this.memEdenUsed);
        retval.put(FixHeader.HEADER_MEM_EDEN_COMMITTED, this.memEdenCommitted);
        retval.put(FixHeader.HEADER_MEM_EDEN_MAX, this.memEdenMax);
        retval.put(FixHeader.HEADER_MEM_EDEN_USEDPERCENT, this.memEdenUsedPercent);
        retval.put(FixHeader.HEADER_MEM_SURVIVOR_INIT, this.memSurvivorInit);
        retval.put(FixHeader.HEADER_MEM_SURVIVOR_USED, this.memSurvivorUsed);
        retval.put(FixHeader.HEADER_MEM_SURVIVOR_COMMITTED, this.memSurvivorCommitted);
        retval.put(FixHeader.HEADER_MEM_SURVIVOR_MAX, this.memSurvivorMax);
        retval.put(FixHeader.HEADER_MEM_SURVIVOR_USEDPERCENT, this.memSurvivorUsedPercent);
        retval.put(FixHeader.HEADER_MEM_OLD_INIT, this.memOldInit);
        retval.put(FixHeader.HEADER_MEM_OLD_USED, this.memOldUsed);
        retval.put(FixHeader.HEADER_MEM_OLD_COMMITTED, this.memOldCommitted);
        retval.put(FixHeader.HEADER_MEM_OLD_MAX, this.memOldMax);
        retval.put(FixHeader.HEADER_MEM_OLD_USEDPERCENT, this.memOldUsedPercent);
        retval.put(FixHeader.HEADER_MEM_PERM_INIT, this.memPermInit);
        retval.put(FixHeader.HEADER_MEM_PERM_USED, this.memPermUsed);
        retval.put(FixHeader.HEADER_MEM_PERM_COMMITTED, this.memPermCommitted);
        retval.put(FixHeader.HEADER_MEM_PERM_MAX, this.memPermMax);
        retval.put(FixHeader.HEADER_MEM_PERM_USEDPERCENT, this.memPermUsedPercent);
        retval.put(FixHeader.HEADER_MEM_CODE_INIT, this.memCodeInit);
        retval.put(FixHeader.HEADER_MEM_CODE_USED, this.memCodeUsed);
        retval.put(FixHeader.HEADER_MEM_CODE_COMMITTED, this.memCodeCommitted);
        retval.put(FixHeader.HEADER_MEM_CODE_MAX, this.memCodeMax);
        retval.put(FixHeader.HEADER_MEM_CODE_USEDPERCENT, this.memCodeUsedPercent);
        retval.put(FixHeader.HEADER_MEM_HEAP_INIT, this.memHeapInit);
        retval.put(FixHeader.HEADER_MEM_HEAP_USED, this.memHeapUsed);
        retval.put(FixHeader.HEADER_MEM_HEAP_COMMITTED, this.memHeapCommitted);
        retval.put(FixHeader.HEADER_MEM_HEAP_MAX, this.memHeapMax);
        retval.put(FixHeader.HEADER_MEM_HEAP_USEDPERCENT, this.memHeapUsedPercent);
        retval.put(FixHeader.HEADER_MEM_NOHEAP_INIT, this.memNoHeapInit);
        retval.put(FixHeader.HEADER_MEM_NOHEAP_USED, this.memNoHeapUsed);
        retval.put(FixHeader.HEADER_MEM_NOHEAP_COMMITTED, this.memNoHeapCommitted);
        retval.put(FixHeader.HEADER_MEM_NOHEAP_MAX, this.memNoHeapMax);
        retval.put(FixHeader.HEADER_MEM_NOHEAP_USEDPERCENT, this.memNoHeapUsedPercent);
        return retval;
    }

}
