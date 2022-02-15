package com.zzstack.paas.underlying.utils.paas;

import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisClusterConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisMSConf;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.config.RocketMqConf;
import com.zzstack.paas.underlying.utils.config.PulsarConf;
import com.zzstack.paas.underlying.utils.config.IPaasConfig;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public enum ConfigTemplateEnum {
    
    TP_ORACLE_DG             (CONSTS.SERV_TYPE_DB_ORACLE_DG,             DBConfig.class,       CONSTS.DB_ORACLE_DG_TEMP_FILE),
    TP_TIDB                  (CONSTS.SERV_TYPE_DB_TIDB,                  DBConfig.class,       CONSTS.DB_TIDB_TEMP_FILE),
    TP_TDENGINE              (CONSTS.SERV_TYPE_DB_TDENGINE,              DBConfig.class,       CONSTS.DB_TDENGINE_TEMP_FILE),
    TP_VOLTDB                (CONSTS.SERV_TYPE_DB_VOLTDB,                DBConfig.class,       CONSTS.DB_VOLTDB_TEMP_FILE),
    TP_CLICKHOUSE            (CONSTS.SERV_TYPE_DB_CLICKHOUSE,            DBConfig.class,       CONSTS.DB_CLICKHOUSE_TEMP_FILE),
    
    TP_ROCKET_MQ             (CONSTS.SERV_TYPE_MQ_ROCKETMQ,              RocketMqConf.class,   CONSTS.MQ_ROCKETMQ_TEMP_FILE),
    TP_PULSAR                (CONSTS.SERV_TYPE_MQ_PULSAR,                PulsarConf.class,     CONSTS.MQ_PULSAR_TEMP_FILE),
    
    TP_REDIS_CLUSTER         (CONSTS.SERV_TYPE_CACHE_REDIS_CLUSTER,      CacheRedisClusterConf.class, CONSTS.CACHE_REDIS_CLUSTER_TEMP_FILE),
    TP_REDIS_MASTER_SLAVE    (CONSTS.SERV_TYPE_CACHE_REDIS_MASTER_SLAVE, CacheRedisMSConf.class, CONSTS.CACHE_REDIS_MASTER_SLAVE_TEMP_FILE),
    TP_CACHE_REDIS_HA_CLUSTER(CONSTS.SERV_TYPE_CACHE_REDIS_HA_CLUSTER,   CacheRedisHaConf.class, CONSTS.CACHE_REDIS_HA_CLUSTER_TEMP_FILE);
    
    private String servType;
    private Class<? extends IPaasConfig> configMeta;
    private String templateFile;
    
    private ConfigTemplateEnum(String servType, Class<? extends IPaasConfig> configMeta, String templateFile) {
        this.servType = servType;
        this.configMeta = configMeta;
        this.templateFile = templateFile;
    }

    public String getServType() {
        return servType;
    }

    public void setServType(String servType) {
        this.servType = servType;
    }

    public Class<? extends IPaasConfig> getConfigMeta() {
        return configMeta;
    }

    public void setConfigMeta(Class<? extends IPaasConfig> configMeta) {
        this.configMeta = configMeta;
    }

    public String getTemplateFile() {
        return templateFile;
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

}
