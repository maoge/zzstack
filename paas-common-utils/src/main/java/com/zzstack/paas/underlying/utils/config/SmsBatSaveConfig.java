package com.zzstack.paas.underlying.utils.config;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SmsBatSaveConfig implements IPaasConfig {
    
    public BatSaveConfigItems confItems;
    
    public static class BatSaveConfigItems {
        
        // 系统属性: production(生产环境), sandbox(测试沙箱环境)
        public String system_property = "production";
        
        // 对接方式：database(数据库) mq(提交队列) multi(混合)
        public String service_impl = "database";
        
        public int cp_ref_interval = 60000;

        public String warn_svc_url = "";

        public int dat_core_size = 20;
        public int dat_max_size = 20;
        public int dat_queue_size = 1000000;
        
        // 对应MT表中inst_id分区字段
        public int inst_id = 1;
        public int web_console_port = 5103;
        
        public long three_channel_last_update_report_time = 48 * 60 * 60 * 1000;
        
        public String redis_cluster_cache = "";
        public String redis_cluster_queue = "";
        public String oracle_dg_serv = "";
        public String processor = "1";
        
    }
    
    public static SmsBatSaveConfig parseFromMeta(String instMeta) {
        JSONObject metaServJson = JSONObject.parseObject(instMeta);
        JSONObject metaJson = metaServJson.getJSONObject(FixHeader.HEADER_SMS_BATSAVE);
        SmsBatSaveConfig batSaveConf = new SmsBatSaveConfig();
        BatSaveConfigItems confItems = new BatSaveConfigItems();
        batSaveConf.confItems = confItems;
        
        confItems.system_property = metaJson.getString(FixHeader.HEADER_SYSTEM_PROPERTY);
        confItems.service_impl = metaJson.getString(FixHeader.HEADER_SERVICE_IMPL);
        confItems.cp_ref_interval = Integer.valueOf(metaJson.getString(FixHeader.HEADER_CP_REF_INTERVAL));
        confItems.warn_svc_url = metaJson.getString(FixHeader.HEADER_WARN_SVC_URL);
        
        confItems.dat_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_CORE_SIZE));
        confItems.dat_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_MAX_SIZE));
        confItems.dat_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_QUEUE_SIZE));

        confItems.inst_id = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DB_INST_ID));
        confItems.web_console_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WEB_CONSOLE_PORT));
        
        if (metaJson.containsKey(FixHeader.HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME))
            confItems.three_channel_last_update_report_time = Long.valueOf(metaJson.getString(FixHeader.HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME));
        
        confItems.redis_cluster_cache = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_CACHE);
        confItems.redis_cluster_queue = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_QUEUE);
        confItems.oracle_dg_serv = metaJson.getString(FixHeader.HEADER_ORACLE_DG_SERV);
        confItems.processor = metaJson.getString(FixHeader.HEADER_PROCESSOR);
        
        return batSaveConf;
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
