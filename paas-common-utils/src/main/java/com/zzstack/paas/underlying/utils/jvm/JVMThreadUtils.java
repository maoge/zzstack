package com.zzstack.paas.underlying.utils.jvm;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class JVMThreadUtils {

    private static final ThreadMXBean THREAD_MX_BEAN;

    // private static final String THREAD_COUNT = "ThreadCount";
    // private static final String THREAD_DAEMON_THREAD_COUNT = "ThreadDaemonThreadCount";
    // private static final String THREAD_PEAK_THREAD_COUNT = "ThreadPeakThreadCount";
    // private static final String THREAD_DEAD_LOCKED_THREAD_COUNT = "ThreadDeadLockedThreadCount";

    static {
        THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    }

    /**
     * Daemon线程总量
     */
    public static int getDaemonThreadCount() {
        return THREAD_MX_BEAN.getDaemonThreadCount();
    }

    /**
     * 当前线程总量
     */
    public static int getThreadCount() {
        return THREAD_MX_BEAN.getThreadCount();
    }

    /**
     * 获取线程数量峰值（从启动或resetPeakThreadCount()方法重置开始统计）
     */
    public static int getPeakThreadCount() {
        return THREAD_MX_BEAN.getPeakThreadCount();
    }

    /**
     * 获取线程数量峰值（从启动或resetPeakThreadCount()方法重置开始统计），并重置
     */
    public static int getAndResetPeakThreadCount() {
        int count = THREAD_MX_BEAN.getPeakThreadCount();
        resetPeakThreadCount();
        return count;
    }

    /**
     * 重置线程数量峰值.
     */
    public static void resetPeakThreadCount() {
        THREAD_MX_BEAN.resetPeakThreadCount();
    }

    /**
     * 死锁线程总量
     */
    public static int getDeadLockedThreadCount() {
        try {
            long[] deadLockedThreadIds = THREAD_MX_BEAN.findDeadlockedThreads();
            if (deadLockedThreadIds == null) {
                return 0;
            }
            return deadLockedThreadIds.length;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static void getJvmThreadInfo(JSONObject jvm) {
        jvm.put(FixHeader.HEADER_THREAD_DAEMON_THREAD_COUNT, getDaemonThreadCount());
        jvm.put(FixHeader.HEADER_THREAD_COUNT, getThreadCount());
        jvm.put(FixHeader.HEADER_THREAD_PEEK_THREAD_COUNT, getAndResetPeakThreadCount());
        jvm.put(FixHeader.HEADER_THREAD_DEAD_LOCKED_THREAD_COUNT, getDeadLockedThreadCount());
    }

}
