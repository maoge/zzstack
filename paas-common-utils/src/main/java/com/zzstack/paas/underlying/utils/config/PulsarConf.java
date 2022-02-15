package com.zzstack.paas.underlying.utils.config;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class PulsarConf implements IPaasConfig {

    public BrokerConf brokerConf;

    public static class BrokerConf {

        public String brokerAddr = "";

    }
    
    @Override
    public String getServClazzType() {
        return CONSTS.SERV_CLAZZ_MQ;
    }

    @Override
    public String getDBType() {
        return "";
    }
    
}
