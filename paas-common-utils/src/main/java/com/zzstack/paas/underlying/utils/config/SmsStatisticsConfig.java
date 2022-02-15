package com.zzstack.paas.underlying.utils.config;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SmsStatisticsConfig implements IPaasConfig {
    public ConfigItems confItems;

    public static class ConfigItems {
        // 系统属性   production生产环境、 sandbox 测试沙箱环境
        public String systemProperty = "production";
        //#web控制台命令接收端口
        public int webconsolePort = 5003;
        //内部状态报告接收端口
        public int internalPort = 5001;
        //敏感词刷新间隔时间(毫秒)
        public int swRefInterval = 3000000;
        //告警服务地址
        public String warnSvcUrl = "http\\://47.105.34.55\\:9083/receiver/receiveWarningMessage";
        public int datCoreSize = 24;
        public int datMaxSize = 24;
        public int datQueueSize = 10000000;
        public int altCoreSize = 24;
        public int altMaxSize = 24;
        public int altQueueSize = 10000000;
        //定时处理内容抽样相似度统计任务开关
        public String samplingSwitch = "off";
        //定时处理内容抽样相似度统计任务
        public String cronExpression = "0 3/5 * * * ?";
        //定时清除 最大短信任务处理进程数(smsprocess)
        public int maxSmsTaskProc = 20;
        public String redis_cluster_cache = "";
        public String redis_cluster_queue = "";
        public String oracle_dg_serv = "";


    }

    public static SmsStatisticsConfig parseFromMeta(String instMeta) {

        JSONObject metaServJson = JSONObject.parseObject(instMeta);
        JSONObject metaJson = metaServJson.getJSONObject(FixHeader.HEADER_SMS_STATS);
        SmsStatisticsConfig smsStatisticsConfig = new SmsStatisticsConfig();
        ConfigItems confItems = new ConfigItems();
        smsStatisticsConfig.confItems = confItems;
        confItems.systemProperty = metaJson.getString(FixHeader.HEADER_SYSTEM_PROPERTY);
        confItems.webconsolePort = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WEB_CONSOLE_PORT));
        confItems.internalPort = Integer.valueOf(metaJson.getString(FixHeader.HEADER_INTERNAL_PORT));
        confItems.swRefInterval = Integer.valueOf(metaJson.getString(FixHeader.HEADER_SW_REF_INTERVAL));
        confItems.warnSvcUrl = metaJson.getString(FixHeader.HEADER_WARN_SVC_URL);
        confItems.datCoreSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_CORE_SIZE));
        confItems.datMaxSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_MAX_SIZE));
        confItems.datQueueSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_QUEUE_SIZE));
        confItems.altCoreSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_CORE_SIZE));
        confItems.altMaxSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_MAX_SIZE));
        confItems.altQueueSize = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_QUEUE_SIZE));
        confItems.samplingSwitch = metaJson.getString(FixHeader.HEADER_SAMPLING_SWITCH);
        confItems.cronExpression = metaJson.getString(FixHeader.HEADER_CRON_EXPRESSION);
        confItems.maxSmsTaskProc = Integer.valueOf(metaJson.getString(FixHeader.HEADER_MAX_SMS_TASK_PROC));
        confItems.redis_cluster_cache = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_CACHE);
        confItems.redis_cluster_queue = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_QUEUE);
        confItems.oracle_dg_serv = metaJson.getString(FixHeader.HEADER_ORACLE_DG_SERV);
        return smsStatisticsConfig;

    }

    @Override
    public String getServClazzType() {
        return CONSTS.SERV_CLAZZ_SMS;
    }

    @Override
    public String getDBType() {
        return "";
    }
}
