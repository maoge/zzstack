package com.zzstack.paas.underlying.utils.jvm;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Properties;

public class JVMInfoUtils {

    static private RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    static private ClassLoadingMXBean classLoad = ManagementFactory.getClassLoadingMXBean();

    // 可能为null
    static private CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();
    static private Properties properties = System.getProperties();

    /**
     * 获取JVM进程PID
     *
     * @return
     */
    public static String getPID() {
        String pid = System.getProperty("pid");
        if (pid == null) {
            String name = runtime.getName();
            if (name != null) {
                pid = name.split("@")[0];
                System.setProperty("pid", pid);
            }
        }
        return pid;
    }

    /**
     * 获取JVM规范名称
     *
     * @return
     */
    static public String getJVMSpecName() {
        return runtime.getSpecName();
    }

    /**
     * 获取JVM规范运营商
     *
     * @return
     */
    static public String getJVMSpecVendor() {
        return runtime.getSpecVendor();
    }

    /**
     * 获取JVM规范版本（如：1.7）
     *
     * @return
     */
    static public String getJVMSpecVersion() {
        return runtime.getSpecVersion();
    }

    /**
     * 获取JVM名称
     *
     * @return
     */
    static public String getJVMName() {
        return runtime.getVmName();
    }

    /**
     * 获取Java的运行环境版本（如：1.7.0_67）
     *
     * @return
     */
    static public String getJavaVersion() {
        return getSystemProperty("java.version");
    }

    /**
     * 获取JVM运营商
     *
     * @return
     */
    static public String getJVMVendor() {
        return runtime.getVmVendor();
    }

    /**
     * 获取JVM实现版本（如：25.102-b14）
     *
     * @return
     */
    static public String getJVMVersion() {
        return runtime.getVmVersion();
    }

    /**
     * 获取JVM启动时间
     *
     * @return
     */
    static public long getJVMStartTimeMs() {
        return runtime.getStartTime();
    }

    /**
     * 获取JVM运行时间
     *
     * @return
     */
    static public long getJVMUpTimeMs() {
        return runtime.getUptime();
    }

    /**
     * 获取JVM当前加载类总量
     *
     * @return
     */
    static public long getJVMLoadedClassCount() {
        return classLoad.getLoadedClassCount();
    }

    /**
     * 获取JVM已卸载类总量
     *
     * @return
     */
    static public long getJVMUnLoadedClassCount() {
        return classLoad.getUnloadedClassCount();
    }

    /**
     * 获取JVM从启动到现在加载类总量
     *
     * @return
     */
    static public long getJVMTotalLoadedClassCount() {
        return classLoad.getTotalLoadedClassCount();
    }

    /**
     * 获取JIT编译器名称
     *
     * @return
     */
    static public String getJITName() {
        return null == compilation ? "" : compilation.getName();
    }

    /**
     * 获取JIT总编译时间
     *
     * @return
     */
    static public long getJITTimeMs() {
        if (null != compilation && compilation.isCompilationTimeMonitoringSupported()) {
            return compilation.getTotalCompilationTime();
        }
        return -1;
    }

    /**
     * 获取指定key的属性值
     *
     * @param key
     * @return
     */
    static public String getSystemProperty(String key) {
        return properties.getProperty(key);
    }

    static public Properties getSystemProperty() {
        return properties;
    }

}
