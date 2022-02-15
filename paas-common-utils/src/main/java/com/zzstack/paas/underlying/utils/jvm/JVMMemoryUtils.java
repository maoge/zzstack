package com.zzstack.paas.underlying.utils.jvm;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

public class JVMMemoryUtils {

    private static final MemoryMXBean memoryMXBean;
    private static MemoryPoolMXBean edenSpaceMxBean;
    private static MemoryPoolMXBean survivorSpaceMxBean;
    private static MemoryPoolMXBean oldGenMxBean;
    private static MemoryPoolMXBean permGenMxBean;
    private static MemoryPoolMXBean codeCacheMxBean;

    private static final String USAGE_INIT = "init";
    private static final String USAGE_USED = "used";
    private static final String USAGE_COMMITTED = "committed";
    private static final String USAGE_MAX = "max";
    private static final String USAGE_PERCENT = "usedPercent";

    /**
     * JVM内存区域使用情况。
     * <p>
     * init：初始内存大小（字节）
     * used：当前使用内存大小（字节）
     * committed：已经申请分配的内存大小（字节）
     * max：最大内存大小（字节）
     * usedPercent：已经申请分配内存与最大内存大小的百分比
     */
    static public class JVMMemoryUsage {
        // 初始内存大小（字节）
        private long init;
        // 当前使用内存大小（字节）
        private long used;
        // 已经申请分配的内存大小（字节）
        private long committed;
        // 最大内存大小（字节）
        private long max;
        // 已经申请分配内存与最大内存大小的百分比
        private double usedPercent;

        public JVMMemoryUsage(MemoryUsage memoryUsage) {
            this.setMemoryUsage(memoryUsage);
        }

        public JVMMemoryUsage(long init, long used, long committed, long max) {
            super();
            this.setMemoryUsage(init, used, committed, max);
        }

        private void setMemoryUsage(MemoryUsage memoryUsage) {
            if (memoryUsage != null) {
                this.setMemoryUsage(memoryUsage.getInit(), memoryUsage.getUsed(), memoryUsage.getCommitted(),
                        memoryUsage.getMax());
            } else {
                this.setMemoryUsage(0, 0, 0, 0);
            }
        }

        private void setMemoryUsage(long init, long used, long committed, long max) {
            this.init = init;
            this.used = used;
            this.committed = committed;
            this.max = max;
            if (this.used > 0 && max > 0) {
                this.usedPercent = ((double) used) / max;  // used * Float.valueOf("1.0") / max;
            } else {
                this.usedPercent = 0;
            }
        }

        public long getInit() {
            return init;
        }

        public long getUsed() {
            return used;
        }

        public long getCommitted() {
            return committed;
        }

        public long getMax() {
            return max;
        }

        public double getUsedPercent() {
            return usedPercent;
        }

        @Override
        public String toString() {
            return "init = " + init + "(" + (init >> 10) + "K) " +
                    "used = " + used + "(" + (used >> 10) + "K) " +
                    "committed = " + committed + "(" + (committed >> 10) + "K) " +
                    "max = " + max + "(" + (max >> 10) + "K)" +
                    "usedPercent = " + usedPercent;
        }

        public JSONObject toJsonObejct() {
            JSONObject json = new JSONObject();
            json.put(USAGE_INIT, init);
            json.put(USAGE_USED, used);
            json.put(USAGE_COMMITTED, committed);
            json.put(USAGE_MAX, max);
            json.put(USAGE_PERCENT, usedPercent);
            return json;
        }
    }

    static {
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolMXBean> memoryPoolMxBeanList = ManagementFactory.getMemoryPoolMXBeans();
        for (final MemoryPoolMXBean memoryPoolMxBean : memoryPoolMxBeanList) {
            String poolName = memoryPoolMxBean.getName();
            if (poolName == null) {
                continue;
            }
            // 官方JVM(HotSpot)提供的MemoryPoolMXBean
            // JDK1.7/1.8 Eden区内存池名称： "Eden Space" 或 "PS Eden Space"、 “G1 Eden Space”(和垃圾收集器有关)
            // JDK1.7/1.8 Survivor区内存池名称："Survivor Space" 或 "PS Survivor Space"、“G1 Survivor Space”(和垃圾收集器有关)
            // JDK1.7 老区内存池名称： "Tenured Gen", JDK1.8 老区内存池名称："Old Gen" 或 "PS Old Gen"、“G1 Old Gen”(和垃圾收集器有关)
            // JDK1.7 方法/永久区内存池名称： "Perm Gen" 或 "PS Perm Gen"(和垃圾收集器有关), JDK1.8 方法/永久区内存池名称："Metaspace"(注意：不在堆内存中)
            // JDK1.7/1.8 CodeCache区内存池名称： "Code Cache"
            if (edenSpaceMxBean == null && poolName.endsWith("Eden Space")) {
                edenSpaceMxBean = memoryPoolMxBean;
            } else if (survivorSpaceMxBean == null && poolName.endsWith("Survivor Space")) {
                survivorSpaceMxBean = memoryPoolMxBean;
            } else if (oldGenMxBean == null && (poolName.endsWith("Tenured Gen") || poolName.endsWith("Old Gen"))) {
                oldGenMxBean = memoryPoolMxBean;
            } else if (permGenMxBean == null && (poolName.endsWith("Perm Gen") || poolName.endsWith("Metaspace"))) {
                permGenMxBean = memoryPoolMxBean;
            } else if (codeCacheMxBean == null && poolName.endsWith("Code Cache")) {
                codeCacheMxBean = memoryPoolMxBean;
            }
        }
    }

    public static void getEdenSpaceInfo(JSONObject jvm) {
        JVMMemoryUsage edenMemUsage = getEdenSpaceMemoryUsage();
        jvm.put(FixHeader.HEADER_MEM_EDEN_INIT, edenMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_EDEN_USED, edenMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_EDEN_COMMITTED, edenMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_EDEN_MAX, edenMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_EDEN_USEDPERCENT, edenMemUsage.getUsedPercent());
    }

    public static void getSurvivorSpaceInfo(JSONObject jvm) {
        JVMMemoryUsage survivorMemUsage = getSurvivorSpaceMemoryUsage();
        jvm.put(FixHeader.HEADER_MEM_SURVIVOR_INIT, survivorMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_SURVIVOR_USED, survivorMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_SURVIVOR_COMMITTED, survivorMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_SURVIVOR_MAX, survivorMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_SURVIVOR_USEDPERCENT, survivorMemUsage.getUsedPercent());
    }

    public static void getOldGenInfo(JSONObject jvm) {
        JVMMemoryUsage oldGenMemUsage = getOldGenMemoryUsage();
        jvm.put(FixHeader.HEADER_MEM_OLD_INIT, oldGenMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_OLD_USED, oldGenMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_OLD_COMMITTED, oldGenMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_OLD_MAX, oldGenMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_OLD_USEDPERCENT, oldGenMemUsage.getUsedPercent());
    }

    public static void getPermGenInfo(JSONObject jvm) {
        JVMMemoryUsage permGenMemUsage = getPermGenMemoryUsage();
        jvm.put(FixHeader.HEADER_MEM_PERM_INIT, permGenMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_PERM_USED, permGenMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_PERM_COMMITTED, permGenMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_PERM_MAX, permGenMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_PERM_USEDPERCENT, permGenMemUsage.getUsedPercent());
    }

    public static void getCodeCacheInfo(JSONObject jvm) {
        JVMMemoryUsage codeCacheMemUsage = getCodeCacheMemoryUsage();
        jvm.put(FixHeader.HEADER_MEM_CODE_INIT, codeCacheMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_CODE_USED, codeCacheMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_CODE_COMMITTED, codeCacheMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_CODE_MAX, codeCacheMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_CODE_USEDPERCENT, codeCacheMemUsage.getUsedPercent());
    }

    public static void getHeapInfo(JSONObject jvm) {
        JVMMemoryUsage heapMemUsage = getHeapMemoryUsage();
        if (heapMemUsage == null) {
            jvm.put(FixHeader.HEADER_MEM_HEAP_INIT, 0);
            jvm.put(FixHeader.HEADER_MEM_HEAP_USED, 0);
            jvm.put(FixHeader.HEADER_MEM_HEAP_COMMITTED, 0);
            jvm.put(FixHeader.HEADER_MEM_HEAP_MAX, 0);
            jvm.put(FixHeader.HEADER_MEM_HEAP_USEDPERCENT, 0.0d);
            return;
        }
        jvm.put(FixHeader.HEADER_MEM_HEAP_INIT, heapMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_HEAP_USED, heapMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_HEAP_COMMITTED, heapMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_HEAP_MAX, heapMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_HEAP_USEDPERCENT, heapMemUsage.getUsedPercent());
    }

    public static void getNoneHeapInfo(JSONObject jvm) {
        JVMMemoryUsage noneHeapMemUsage = getNonHeapMemoryUsage();
        if (noneHeapMemUsage == null) {
            jvm.put(FixHeader.HEADER_MEM_NOHEAP_INIT, 0);
            jvm.put(FixHeader.HEADER_MEM_NOHEAP_USED, 0);
            jvm.put(FixHeader.HEADER_MEM_NOHEAP_COMMITTED, 0);
            jvm.put(FixHeader.HEADER_MEM_NOHEAP_MAX, 0);
            jvm.put(FixHeader.HEADER_MEM_NOHEAP_USEDPERCENT, 0.0d);
            return;
        }
        jvm.put(FixHeader.HEADER_MEM_NOHEAP_INIT, noneHeapMemUsage.getInit());
        jvm.put(FixHeader.HEADER_MEM_NOHEAP_USED, noneHeapMemUsage.getUsed());
        jvm.put(FixHeader.HEADER_MEM_NOHEAP_COMMITTED, noneHeapMemUsage.getCommitted());
        jvm.put(FixHeader.HEADER_MEM_NOHEAP_MAX, noneHeapMemUsage.getMax());
        jvm.put(FixHeader.HEADER_MEM_NOHEAP_USEDPERCENT, noneHeapMemUsage.getUsedPercent());
    }

    /**
     * 获取堆内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getHeapMemoryUsage() {
        if (memoryMXBean != null) {
            final MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
            if (usage != null) {
                return new JVMMemoryUsage(usage);
            }
        }
        return null;
    }

    /**
     * 获取堆外内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getNonHeapMemoryUsage() {
        if (memoryMXBean != null) {
            final MemoryUsage usage = memoryMXBean.getNonHeapMemoryUsage();
            if (usage != null) {
                return new JVMMemoryUsage(usage);
            }
        }
        return null;
    }

    /**
     * 获取Eden区内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getEdenSpaceMemoryUsage() {
        return getMemoryPoolUsage(edenSpaceMxBean);
    }

    /**
     * 获取Eden区内存峰值（从启动或上一次重置开始统计），并重置
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getAndResetEdenSpaceMemoryPeakUsage() {
        return getAndResetMemoryPoolPeakUsage(edenSpaceMxBean);
    }

    /**
     * 获取Survivor区内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getSurvivorSpaceMemoryUsage() {
        return getMemoryPoolUsage(survivorSpaceMxBean);
    }

    /**
     * 获取Survivor区内存峰值（从启动或上一次重置开始统计），并重置
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getAndResetSurvivorSpaceMemoryPeakUsage() {
        return getAndResetMemoryPoolPeakUsage(survivorSpaceMxBean);
    }

    /**
     * 获取老区内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getOldGenMemoryUsage() {
        return getMemoryPoolUsage(oldGenMxBean);
    }

    /**
     * 获取老区内存峰值（从启动或上一次重置开始统计），并重置
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getAndResetOldGenMemoryPeakUsage() {
        return getAndResetMemoryPoolPeakUsage(oldGenMxBean);
    }

    /**
     * 获取永久区/方法区内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getPermGenMemoryUsage() {
        return getMemoryPoolUsage(permGenMxBean);
    }

    /**
     * 获取永久区/方法区内存峰值（从启动或上一次重置开始统计），并重置
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getAndResetPermGenMemoryPeakUsage() {
        return getAndResetMemoryPoolPeakUsage(permGenMxBean);
    }

    /**
     * 获取CodeCache区内存情况
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getCodeCacheMemoryUsage() {
        return getMemoryPoolUsage(codeCacheMxBean);
    }

    /**
     * 获取CodeCache区内存峰值（从启动或上一次重置开始统计），并重置
     *
     * @return 不能获取到返回null
     */
    public static JVMMemoryUsage getAndResetCodeCacheMemoryPeakUsage() {
        return getAndResetMemoryPoolPeakUsage(codeCacheMxBean);
    }

    private static JVMMemoryUsage getMemoryPoolUsage(MemoryPoolMXBean memoryPoolMxBean) {
        if (memoryPoolMxBean != null) {
            final MemoryUsage usage = memoryPoolMxBean.getUsage();
            if (usage != null) {
                return new JVMMemoryUsage(usage);
            }
        }
        return null;
    }

    private static JVMMemoryUsage getAndResetMemoryPoolPeakUsage(MemoryPoolMXBean memoryPoolMxBean) {
        if (memoryPoolMxBean != null) {
            final MemoryUsage usage = memoryPoolMxBean.getPeakUsage();
            if (usage != null) {
                memoryPoolMxBean.resetPeakUsage();
                return new JVMMemoryUsage(usage);
            }
        }
        return null;
    }

}
