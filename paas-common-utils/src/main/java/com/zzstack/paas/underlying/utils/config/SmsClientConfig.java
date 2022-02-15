package com.zzstack.paas.underlying.utils.config;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SmsClientConfig implements IPaasConfig {
    
    public ClientConfigItems confItems;
    
    public static class ClientConfigItems {
        
        public int web_console_port = 6000;
        
        // 对接方式：database(数据库) mq(提交队列) multi(混合)
        public String service_impl = "database";
        
        public int batch_save_process = 10;
        
        // cmpp20包日志功能
        public String cmpp20_packlog = "no";

        // sgip包日志功能
        public String cmpp30_packlog = "no";

        // smgp包日志功能
        public String smgp_packlog = "no";

        // sgip包日志功能
        public String sgip_packlog = "no";

        // smpp包日志功能
        public String smpp_packlog = "no";

        // 告警服务地址
        public String warn_svc_url = "";
        
        // 通用处理线程核心线程数
        public int bst_core_size = 50;
        // 通用处理线程最大线程数
        public int bst_max_size = 50;
        // 通用处理线程核心队列大小
        public int bst_queue_size = 50000;
        
        // 上行下行匹配模式: es(es查询),cache(redis-cluster集群查询)
        public String mt_mo_matcher_impl = "es";
        
        public String parse_rpt_type = "1.9.9";
        
        public long three_channel_last_update_report_time = 48 * 60 * 60 * 1000;

        public String redis_cluster_cache = "";
        public String redis_cluster_queue = "";
        public String oracle_dg_serv = "";
        public String processor = "1";
    }
    
    public static SmsClientConfig parseFromMeta(String instMeta) {
        JSONObject metaServJson = JSONObject.parseObject(instMeta);
        JSONObject metaJson = metaServJson.getJSONObject(FixHeader.HEADER_SMS_CLIENT);
        SmsClientConfig clientConf = new SmsClientConfig();
        ClientConfigItems confItems = new ClientConfigItems();
        clientConf.confItems = confItems;
        
        confItems.web_console_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WEB_CONSOLE_PORT));
        confItems.service_impl = metaJson.getString(FixHeader.HEADER_SERVICE_IMPL);
        confItems.batch_save_process = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BATCHSAVE_PROCESS));
        
        confItems.cmpp20_packlog = metaJson.getString(FixHeader.HEADER_CMPP20_PACKLOG);
        confItems.cmpp30_packlog = metaJson.getString(FixHeader.HEADER_CMPP30_PACKLOG);
        confItems.smgp_packlog = metaJson.getString(FixHeader.HEADER_SMGP_PACKLOG);
        confItems.sgip_packlog = metaJson.getString(FixHeader.HEADER_SGIP_PACKLOG);
        confItems.smpp_packlog = metaJson.getString(FixHeader.HEADER_SMPP_PACKLOG);
        
        confItems.warn_svc_url = metaJson.getString(FixHeader.HEADER_WARN_SVC_URL);
        
        confItems.bst_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_CORE_SIZE));
        confItems.bst_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_MAX_SIZE));
        confItems.bst_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_QUEUE_SIZE));
        
        confItems.mt_mo_matcher_impl = metaJson.getString(FixHeader.HEADER_MT_MO_MATCHER_IMPL);
        confItems.parse_rpt_type = metaJson.getString(FixHeader.HEADER_PARSE_RPT_TYPE);
        
        if (metaJson.containsKey(FixHeader.HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME))
            confItems.three_channel_last_update_report_time = Long.valueOf(metaJson.getString(FixHeader.HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME));
        
        confItems.redis_cluster_cache = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_CACHE);
        confItems.redis_cluster_queue = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_QUEUE);
        confItems.oracle_dg_serv = metaJson.getString(FixHeader.HEADER_ORACLE_DG_SERV);
        confItems.processor = metaJson.getString(FixHeader.HEADER_PROCESSOR);
        
        return clientConf;
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
