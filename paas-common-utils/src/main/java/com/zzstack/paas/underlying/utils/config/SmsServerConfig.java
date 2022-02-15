package com.zzstack.paas.underlying.utils.config;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SmsServerConfig implements IPaasConfig {
    
    public ConfigItems confItems;
    
    public static class ConfigItems {
        // 系统属性   production生产环境、 sandbox 测试沙箱环境
        public String system_property = "production";
        
        // 缓存接入类型：1.redis 2.memcache
        // app.cache.type=1
        // app.memcachedAddr=192.168.128.51\:11211

        // process进程最大数(全部)
        public int max_sms_task_proc = 1;

        // batsave进程最大数(单台)
        public int batch_save_process = 1;

        // web控制台命令接收端口
        public int web_console_port = 5003;

        // 各个通信协议接收端口
        public int cmpp20_port = 8855;
        public int cmpp30_port = 8856;
        public int sgip12_port = 8857;
        public int smpp34_port = 8858;
        public int smgp30_port = 8859;
        public int http_port = 8860;
        public int http_port2 = 8861;
        public int https_port = 8862;

        // 美圣
        public int meisheng_port = 8863;
        // 中国人保
        public int http_gbk_port = 8865;
        // 天津电信
        public int wjsx_port = 9010;
        // 京东
        public String jdws_addr = "http://127.0.0.1:9011/jdserver";

        // WEB
        public String web_service_addr = "http://192.168.128.50:9012/SmsWebService.asmx";
        public String web_service_task_url = "http://192.168.128.77:8088/jersey/clientWeb/sendSms";


        // 扫描上行表间隔时间(毫秒)
        public long mo_scan_interval = 3000;

        // 发送HTTP协议状态报告间隔时间(毫秒)
        public long http_report_interval = 500;

        // 敏感词刷新间隔时间(毫秒)
        public long sw_ref_interval = 180000;

        // 客户参数刷新时间(毫秒)
        public long cp_ref_interval = 180000;
        // 本平台ip地址
        public String local_ip = "127.0.0.1";

        // cmpp20包日志功能
        public String cmpp20_packlog = "no";

        // sgip包日志功能
        public String cmpp30_packlog = "no";

        // smgp包日志功能
        public String smgp_packlog = "no";

        // sgip包日志功能
        public String sgip_packlog = "no";

        // http包日志功能
        public String http_packlog = "no";

        // http2包日志功能
        public String http2_packlog = "no";

        // https包日志功能
        public String https_packlog = "no";

        // 告警服务地址
        public String warn_svc_url = "";

        // mt提交任务线程池核心数
        public int dat_core_size = 16;
        // mt提交任务线程池最大数
        public int dat_max_size = 16;
        // mt提交任务队列大小
        public int dat_queue_size = 1000000;

        public int alt_core_size = 16;
        public int alt_max_size = 16;
        public int alt_queue_size = 1000000;

        // 状态报告推送状态线程池核心数
        public int bst_core_size = 16;
        // 状态报告推送状态线程池最大数
        public int bst_max_size = 16;
        // 状态报告推送状态任务队列大小
        public int bst_queue_size = 1000000;

        // 状态报告推送更新队列大小
        public int rpt_queue_size = 1000000;

        // 是否开启 维护HTTP状态报告推送线程
        public String http_report_push = "yes";
        // 是否开启 维护HTTP2状态报告推送线程
        public String http2_report_push = "yes";
        // 是否开始 维护HTTPS状态报告推送线程
        public String https_report_push = "yes";
        // 是否开启 维护SGIP Client 线程
        public String sgip_report_push = "no";

        // 是否启动帐户中心服务线程
        public String acct_service = "yes";

        // cmpp ismg id
        public int cmpp_ismg_id = 59150;

        // smgp and http ismg id
        public String smgp_ismg_id = "591050";

        // 是否采集号段 yes/no/database
        public String collect_msi = "yes";

        // 特殊用户状态报告推送
        public String special_report_custid = "";

        public String unique_link_url = "http://192.168.128.69:8081/store/short_url/generate/analysis";

        // redis每次拉取状态报告最大条数
        public int max_report_fetch = 100;

        // 已发送状态包再次发送拦截是否执行
        public String no_report_execute = "no";

        // 决策是否启用(yes:启用 no:停用)
        public String decision_enable = "no";

        // prometheus采集server的端口
        public int prometheus_port = 10010;
        
        public String redis_cluster_cache = "";
        public String redis_cluster_queue = "";
        public String oracle_dg_serv = "";
        
        public boolean ext_proto_switch = false;  // 扩展协议开关, 默认关闭
        public int ext_proto_port = 9988;         // 扩展协议开启时, 默认使用的端口
    }
    
    public static SmsServerConfig parseFromMeta(String instMeta) {
        JSONObject metaServJson = JSONObject.parseObject(instMeta);
        boolean isExist = metaServJson.containsKey(FixHeader.HEADER_SMS_SERVER);
        JSONObject metaJson = isExist ? metaServJson.getJSONObject(FixHeader.HEADER_SMS_SERVER)
                : metaServJson.getJSONObject(FixHeader.HEADER_SMS_SERVER_EXT);
            
        SmsServerConfig serverConf = new SmsServerConfig();
        ConfigItems confItems = new ConfigItems();
        serverConf.confItems = confItems;
        
        confItems.system_property = metaJson.getString(FixHeader.HEADER_SYSTEM_PROPERTY);
        confItems.max_sms_task_proc = Integer.valueOf(metaJson.getString(FixHeader.HEADER_MAX_SMS_TASK_PROC));
        confItems.batch_save_process = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BATCH_SAVE_PROCESS));
        confItems.web_console_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WEB_CONSOLE_PORT));
        
        confItems.cmpp20_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_CMPP20_PORT));
        confItems.cmpp30_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_CMPP30_PORT));
        confItems.sgip12_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_SGIP12_PORT));
        confItems.smpp34_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_SMPP34_PORT));
        confItems.smgp30_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_SMGP30_PORT));
        confItems.http_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_HTTP_PORT));
        confItems.http_port2 = Integer.valueOf(metaJson.getString(FixHeader.HEADER_HTTP_PORT2));
        confItems.https_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_HTTPS_PORT));

        confItems.meisheng_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_MEISHENG_PORT));
        confItems.http_gbk_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_HTTP_GBK_PORT));
        confItems.wjsx_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_WJSX_PORT));
        confItems.jdws_addr = metaJson.getString(FixHeader.HEADER_JDWS_ADDR);
        confItems.web_service_addr = metaJson.getString(FixHeader.HEADER_WEB_SERVICE_ADDR);
        confItems.web_service_task_url = metaJson.getString(FixHeader.HEADER_WEB_SERVICE_TASK_URL);

        confItems.mo_scan_interval = Long.valueOf(metaJson.getString(FixHeader.HEADER_MO_SCAN_INTERVAL));
        confItems.http_report_interval = Long.valueOf(metaJson.getString(FixHeader.HEADER_HTTP_REPORT_INTERVAL));
        confItems.sw_ref_interval = Long.valueOf(metaJson.getString(FixHeader.HEADER_SW_REF_INTERVAL));
        confItems.cp_ref_interval = Long.valueOf(metaJson.getString(FixHeader.HEADER_CP_REF_INTERVAL));
        
        confItems.local_ip = metaJson.getString(FixHeader.HEADER_LOCAL_IP);
        confItems.cmpp20_packlog = metaJson.getString(FixHeader.HEADER_CMPP20_PACKLOG);
        confItems.cmpp30_packlog = metaJson.getString(FixHeader.HEADER_CMPP30_PACKLOG);
        confItems.smgp_packlog = metaJson.getString(FixHeader.HEADER_SMGP_PACKLOG);
        confItems.sgip_packlog = metaJson.getString(FixHeader.HEADER_SGIP_PACKLOG);
        confItems.http_packlog = metaJson.getString(FixHeader.HEADER_HTTP_PACKLOG);
        confItems.http2_packlog = metaJson.getString(FixHeader.HEADER_HTTP2_PACKLOG);
        confItems.https_packlog = metaJson.getString(FixHeader.HEADER_HTTPS_PACKLOG);
        confItems.warn_svc_url = metaJson.getString(FixHeader.HEADER_WARN_SVC_URL);
        
        confItems.dat_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_CORE_SIZE));
        confItems.dat_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_MAX_SIZE));
        confItems.dat_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_DAT_QUEUE_SIZE));
        confItems.alt_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_CORE_SIZE));
        confItems.alt_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_MAX_SIZE));
        confItems.alt_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_ALT_QUEUE_SIZE));
        confItems.bst_core_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_CORE_SIZE));
        confItems.bst_max_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_MAX_SIZE));
        confItems.bst_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_BST_QUEUE_SIZE));
        confItems.rpt_queue_size = Integer.valueOf(metaJson.getString(FixHeader.HEADER_RPT_QUEUE_SIZE));
        
        confItems.http_report_push = metaJson.getString(FixHeader.HEADER_HTTP_REPORT_PUSH);
        confItems.http2_report_push = metaJson.getString(FixHeader.HEADER_HTTP2_REPORT_PUSH);
        confItems.https_report_push = metaJson.getString(FixHeader.HEADER_HTTPS_REPORT_PUSH);
        confItems.sgip_report_push = metaJson.getString(FixHeader.HEADER_SGIP_REPORT_PUSH);
        confItems.acct_service = metaJson.getString(FixHeader.HEADER_ACCT_SERVICE);
        
        confItems.cmpp_ismg_id = Integer.valueOf(metaJson.getString(FixHeader.HEADER_CMPP_ISMG_ID));
        confItems.smgp_ismg_id = metaJson.getString(FixHeader.HEADER_SMGP_ISMG_ID);
        confItems.collect_msi = metaJson.getString(FixHeader.HEADER_COLLECT_MSI);
        confItems.special_report_custid = metaJson.getString(FixHeader.HEADER_SPECIAL_REPORT_CUSTID);
        confItems.unique_link_url = metaJson.getString(FixHeader.HEADER_UNIQUE_LINK_URL);
        
        confItems.max_report_fetch = Integer.valueOf(metaJson.getString(FixHeader.HEADER_MAX_REPORT_FETCH));
        confItems.no_report_execute = metaJson.getString(FixHeader.HEADER_NO_REPORT_EXECUTE);
        confItems.decision_enable = metaJson.getString(FixHeader.HEADER_DECISION_ENABLE);
        confItems.prometheus_port = Integer.valueOf(metaJson.getString(FixHeader.HEADER_PROMETHEUS_PORT));
        
        confItems.redis_cluster_cache = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_CACHE);
        confItems.redis_cluster_queue = metaJson.getString(FixHeader.HEADER_REDIS_CLUSTER_QUEUE);
        confItems.oracle_dg_serv = metaJson.getString(FixHeader.HEADER_ORACLE_DG_SERV);
        
        if (metaJson.containsKey(FixHeader.HEADER_SMS_EXT_PROTO_SWITCH))
            confItems.ext_proto_switch = metaJson.getBooleanValue(FixHeader.HEADER_SMS_EXT_PROTO_SWITCH);
        
        if (metaJson.containsKey(FixHeader.HEADER_SMS_EXT_PROTO_PORT))
            confItems.ext_proto_port = metaJson.getIntValue(FixHeader.HEADER_SMS_EXT_PROTO_PORT);
        
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
