package com.zzstack.paas.underlying.utils.config;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SmsProcessConfig implements IPaasConfig {
    
    public ProcessConfigItems confItems;
    
    public static class ProcessConfigItems {
        
        // 系统属性   production生产环境、 sandbox 测试沙箱环境
        public String system_property = "production";
        
        // 对接方式：database(数据库) mq(提交队列) multi(混合)
        public String service_impl = "multi";

        // 客户参数维护时间
        public long cp_ref_interval = 180000;
        
        // 告警服务地址
        public String warn_svc_url = "";
        
        // 任务池(行业)核心线程数
        public int dat_core_size = 20;
        // 任务池(行业)最大线程数
        public int dat_max_size = 20;
        // 任务池(行业)核心队列大小
        public int dat_queue_size = 1000000;
        
        // 任务池(营销)核心线程数
        public int alt_core_size = 20;
        // 任务池(营销)最大线程数
        public int alt_max_size = 20;
        // 任务池(营销)核心队列大小
        public int alt_queue_size = 1000000;
        
        // 任务池(大营销)核心线程数
        public int tst_core_size = 20;
        // 任务池(大营销)最大线程数
        public int tst_max_size = 20;
        // 任务池(大营销)核心队列大小
        public int tst_queue_size = 1000000;
        
        // 任务池(压力测试)核心线程数
        public int sts_core_size = 20;
        // 任务池(压力测试)最大线程数
        public int sts_max_size = 20;
        // 任务池(压力测试)核心队列大小
        public int sts_queue_size = 1000000;
        
        // 对应MT表中inst_id分区字段
        public int inst_id = 1;
        // 是否启用抽样内容相似度检查
        public String sampling_switch = "off";
        // 抽样内容相似度检查 调度表达式
        public String cron_expression = "0 0/5 * * * ?";
        public int web_console_port = 5003;
        public String mt_queue_clear_expression = "0 0 1 * * ?";
        
        // 决策是否启用(yes:启用 no:停用)
        public String decision_enable = "yes";
        // 是否采集号段 yes/no/database
        public String collect_msi = "database";
        
        public String redis_cluster_cache = "";
        public String redis_cluster_queue = "";
        public String redis_cluster_pfm = "";
        public String redis_cluster_ipnum = "";
        public String oracle_dg_serv = "";
        public String processor = "1";
        
        public String mnp_ali_url = "http://47.98.147.230:3778/mnp/";
        public String mnp_ali_cid = "Customer1BA2265E86";
        public String mnp_ali_passwd = "FD322B94F55FA522ABF1156D0D48CAA0";
        
    }
    
    public static SmsProcessConfig parseFromMeta(String instMeta) {
        JSONObject metaServJson = JSONObject.parseObject(instMeta);
        JSONObject metaJson = metaServJson.getJSONObject(FixHeader.HEADER_SMS_PROCESS);
        SmsProcessConfig serverConf = new SmsProcessConfig();
        ProcessConfigItems confItems = new ProcessConfigItems();
        serverConf.confItems = confItems;
        
        confItems.system_property = metaJson.getString(FixHeader.HEADER_SYSTEM_PROPERTY);
        confItems.service_impl = metaJson.getString(FixHeader.HEADER_SERVICE_IMPL);
        confItems.cp_ref_interval = Long.valueOf(metaJson.getString(FixHeader.HEADER_CP_REF_INTERVAL));
        confItems.warn_svc_url = metaJson.getString(FixHeader.HEADER_WARN_SVC_URL);
        
        confItems.dat_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_CORE_SIZE));
        confItems.dat_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_MAX_SIZE));
        confItems.dat_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_QUEUE_SIZE));
        confItems.alt_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_CORE_SIZE));
        confItems.alt_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_MAX_SIZE));
        confItems.alt_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_QUEUE_SIZE));
        confItems.tst_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_TST_CORE_SIZE));
        confItems.tst_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_TST_MAX_SIZE));
        confItems.tst_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_TST_QUEUE_SIZE));
        confItems.sts_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_STS_CORE_SIZE));
        confItems.sts_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_STS_MAX_SIZE));
        confItems.sts_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_STS_QUEUE_SIZE));
        
        confItems.inst_id = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DB_INST_ID));
        confItems.sampling_switch = metaJson.getString(FixHeader.HEADER_SAMPLING_SWITCH);
        confItems.cron_expression = metaJson.getString(FixHeader.HEADER_CRON_EXPRESSION);
        confItems.web_console_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WEB_CONSOLE_PORT));
        confItems.mt_queue_clear_expression = metaJson.getString(FixHeader.HEADER_MT_QUEUE_CLEAR_EXPRESSION);
        
        confItems.decision_enable = metaJson.getString(FixHeader.HEADER_DECISION_ENABLE);
        confItems.collect_msi = metaJson.getString(FixHeader.HEADER_COLLECT_MSI);
        
        confItems.redis_cluster_cache = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_CACHE);
        confItems.redis_cluster_queue = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_QUEUE);
        confItems.redis_cluster_pfm = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_PFM);
        confItems.redis_cluster_ipnum = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_IPNUM);
        confItems.oracle_dg_serv = metaJson.getString(FixHeader.HEADER_ORACLE_DG_SERV);
        confItems.processor = metaJson.getString(FixHeader.HEADER_PROCESSOR);
        
        confItems.mnp_ali_url = metaJson.getString(FixHeader.HEADER_MNP_ALI_URL);
        confItems.mnp_ali_cid = metaJson.getString(FixHeader.HEADER_MNP_ALI_CID);
        confItems.mnp_ali_passwd = metaJson.getString(FixHeader.HEADER_MNP_ALI_PASSWD);
        
        return serverConf;
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
