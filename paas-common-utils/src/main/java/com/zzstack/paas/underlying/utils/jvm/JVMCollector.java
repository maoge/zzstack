package com.zzstack.paas.underlying.utils.jvm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class JVMCollector {

    private final static Logger logger = LoggerFactory.getLogger("JVMCollector");

    private static volatile JVMCollector instance;

    private String id = ""; // 进程唯一标志：ip_进程类别_唯一id
    private String pushUrl = ""; // 推送地址,多个地址用","分隔
    private long interval = 1; // 采集间隔，单位(s)
    private JVMProbe jvmProbe;
    private final ScheduledExecutorService collectScheduledler;
    private static final long COLLECT_INTERVAL = 10L;
    private static final String ID = "Id";

    private static Map<String, String> POST_HEADS;

    static {
        POST_HEADS = new HashMap<>();
        POST_HEADS.put("CONTENT-TYPE", "application/json");
    }

    private JVMCollector(String id, String pushUrl, long interval) {

        this.id = id;
        if (interval > 0) {
            this.interval = interval;
        } else {
            this.interval = COLLECT_INTERVAL;
        }

        collectScheduledler = Executors.newSingleThreadScheduledExecutor();
        jvmProbe = new JVMProbe(id, pushUrl);
        collectScheduledler.scheduleAtFixedRate(jvmProbe, interval, interval, TimeUnit.SECONDS);

    }

    public static JVMCollector getInstance(String id, String pushUrl, long interval) {
        if (instance == null) {
            synchronized (JVMCollector.class) {
                if (instance == null) {
                    instance = new JVMCollector(id, pushUrl, interval);
                }
            }
        }
        return instance;
    }

    public void shutdown() {
        synchronized (JVMCollector.class) {
            if (collectScheduledler != null) {
                collectScheduledler.shutdown();
            }
        }
    }

    //
    // JVM Probe指标
    // Memory: Heap | NoneHeap
    // 指标维度: 最大值、已经分配值、当前使用值、已经分配与最大值比率
    // Heap: Eden Space | Survivor Space | Old Gen | Metaspace | Code Cache
    // NoneHeap
    //
    // GC: YGC | FGC
    // 指标维度: 时间、次数
    //
    // Thread:
    // 指标维度: 当前线程总数、死锁线程总数
    //
    private static class JVMProbe implements Runnable {

        private final String id;
        private final List<String> pushAddrs;
        private int addrIdx = 0;
        private final int addrCnt;

        public JVMProbe(String id, String pushUrl) {
            this.id = id;

            String[] addrs = pushUrl.split(",");
            addrCnt = addrs.length;
            pushAddrs = new ArrayList<>(addrCnt);
            for (String addr : addrs) {
                if (addr == null || addr.isEmpty()) {
                    continue;
                }
                pushAddrs.add(addr);
            }
        }

        @Override
        public void run() {
            JSONArray jsonArray = new JSONArray();
            JSONObject jvm = new JSONObject();
            jvm.put(FixHeader.HEADER_TS, System.currentTimeMillis());
            // 应用的实例ID
            jvm.put(FixHeader.HEADER_INST_ID, id);
            String javaVersion = JVMInfoUtils.getJavaVersion();
            jvm.put(FixHeader.HEADER_JAVA_VERSION, javaVersion);
            // 内存
            JVMMemoryUtils.getEdenSpaceInfo(jvm);
            JVMMemoryUtils.getSurvivorSpaceInfo(jvm);
            JVMMemoryUtils.getOldGenInfo(jvm);
            JVMMemoryUtils.getPermGenInfo(jvm);
            JVMMemoryUtils.getCodeCacheInfo(jvm);
            JVMMemoryUtils.getHeapInfo(jvm);
            JVMMemoryUtils.getNoneHeapInfo(jvm);
            // GC
            JVMGCUtils.getJvmGcInfo(jvm);
            // 线程
            JVMThreadUtils.getJvmThreadInfo(jvm);
            jsonArray.add(jvm);
            // 组装jvm监控数据
            JSONObject jvmInfo = new JSONObject();
            jvmInfo.put(FixHeader.HEADER_JVM_INFO_JSON_ARRAY, jsonArray);
            JSONObject root = new JSONObject();
            root.put(FixHeader.HEADER_JVM_DATA, jvmInfo);
            // logger.info(root.toJSONString());
            if (!pushAddrs.isEmpty()) {
                int idx = getAddrIdx();
                String addr = pushAddrs.get(idx);
                SVarObject var = new SVarObject();
                try {
                    if (!HttpCommonTools.postData(addr, POST_HEADS, root.toJSONString(), var)) {
                        logger.error("上传JVM收集信息异常:{}", var.getVal());
                    }
                } catch (IOException e) {
                    logger.error("上传JVM收集信息异常:{}", e.getMessage(), e);
                }
            }
        }

        private int getAddrIdx() {
            if (addrCnt <= 1) {
                return 0;
            }
            return (addrIdx++) % addrCnt;
        }
    }

}
