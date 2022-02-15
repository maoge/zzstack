package com.zzstack.paas.underlying.metasvr.autocheck;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.autocheck.probe.CmptProber;
import com.zzstack.paas.underlying.metasvr.autocheck.probe.SmsGatewayProber;
import com.zzstack.paas.underlying.metasvr.autocheck.probe.SmsQueryServiceProber;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class CmptProberFactory {

    private static final Logger logger = LoggerFactory.getLogger(CmptProberFactory.class);

    private static final Map<String, Class<? extends CmptProber>> PROBER_MAP;
    
    static {
        PROBER_MAP = new HashMap<String, Class<? extends CmptProber>>();
        // PROBER_MAP.put(FixDefs.CACHE_REDIS_CLUSTER, RedisClusterProber.class);
        // PROBER_MAP.put(FixDefs.CACHE_REDIS_MASTER_SLAVE, RedisMSProber.class);
        // PROBER_MAP.put(FixDefs.CACHE_REDIS_HA_CLUSTER, RedisHaProber.class);
        
        // PROBER_MAP.put(FixDefs.MQ_ROCKETMQ, RocketMQProber.class);
        // PROBER_MAP.put(FixDefs.MQ_PULSAR, PulsarProber.class);
        
        // PROBER_MAP.put(FixDefs.DB_TDENGINE, TDEngineProber.class);
        // PROBER_MAP.put(FixDefs.DB_ORACLE_DG, OracleDGProber.class);
        // PROBER_MAP.put(FixDefs.DB_TIDB, TiDBProber.class);
        // PROBER_MAP.put(FixDefs.DB_CLICKHOUSE, ClickHouseProber.class);
        // PROBER_MAP.put(FixDefs.DB_VOLTDB, VoltDBProber.class);
        // PROBER_MAP.put(FixDefs.DB_YUGABYTEDB, YugaByteDBProber.class);
        
        // PROBER_MAP.put(FixDefs.SERVERLESS_APISIX, ApiSixProber.class);
        
        PROBER_MAP.put(FixDefs.SMS_GATEWAY,       SmsGatewayProber.class);
        PROBER_MAP.put(FixDefs.SMS_QUERY_SERVICE, SmsQueryServiceProber.class);
    }

    public static boolean isProbeReady(String servType) {
        return PROBER_MAP.containsKey(servType);
    }
    
    public static CmptProber getCmptProber(final String servInstID, final String servType) {

        Class<? extends CmptProber> clazz = PROBER_MAP.get(servType);
        CmptProber prober = null;
        if (clazz != null) {
            try {
                prober = (CmptProber) clazz.getDeclaredConstructor(String.class, String.class).newInstance(servInstID, servType);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                String errInfo = String.format("%s %s", servType, CONSTS.ERR_SERVICE_PROBER_INIT_ERROR);
                logger.error("{}, {}", errInfo, e.getMessage(), e);
            }
        } else {
            logger.error("prober for {} not found ......", servType);
        }

        return prober;
    }

}
