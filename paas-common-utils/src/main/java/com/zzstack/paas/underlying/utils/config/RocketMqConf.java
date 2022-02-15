package com.zzstack.paas.underlying.utils.config;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class RocketMqConf implements IPaasConfig {
    public RocketMqConfig rockConf;

    public static class RocketMqConfig {

        public String nameSrvUrl = "";

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
