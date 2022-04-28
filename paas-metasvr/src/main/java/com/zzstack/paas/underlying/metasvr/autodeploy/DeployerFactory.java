package com.zzstack.paas.underlying.metasvr.autodeploy;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.ApiSixDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.MinioDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.ClickHouseDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.OracleDgDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.PulsarDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.RedisClusterDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.RedisHaClusterDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.RedisMSDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.RocketMqDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.SMSGatewayDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.SMSQueryServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.TDengineDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.TiDBDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.VoltDBDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.deployer.YugaByteDBDeployer;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class DeployerFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeployerFactory.class);

    private static final Map<String, Class<? extends ServiceDeployer>> DEPLOYER_MAP;

    static {
        DEPLOYER_MAP = new HashMap<>();
        DEPLOYER_MAP.put(FixDefs.CACHE_REDIS_CLUSTER,      RedisClusterDeployer.class);
        DEPLOYER_MAP.put(FixDefs.CACHE_REDIS_MASTER_SLAVE, RedisMSDeployer.class);
        DEPLOYER_MAP.put(FixDefs.CACHE_REDIS_HA_CLUSTER,   RedisHaClusterDeployer.class);
        
        DEPLOYER_MAP.put(FixDefs.MQ_ROCKETMQ,              RocketMqDeployer.class);
        DEPLOYER_MAP.put(FixDefs.MQ_PULSAR,                PulsarDeployer.class);
        
        DEPLOYER_MAP.put(FixDefs.DB_TDENGINE,              TDengineDeployer.class);
        DEPLOYER_MAP.put(FixDefs.DB_ORACLE_DG,             OracleDgDeployer.class);
        DEPLOYER_MAP.put(FixDefs.DB_TIDB,                  TiDBDeployer.class);
        DEPLOYER_MAP.put(FixDefs.DB_CLICKHOUSE,            ClickHouseDeployer.class);
        DEPLOYER_MAP.put(FixDefs.DB_VOLTDB,                VoltDBDeployer.class);
        DEPLOYER_MAP.put(FixDefs.DB_YUGABYTEDB,            YugaByteDBDeployer.class);
        
        DEPLOYER_MAP.put(FixDefs.SERVERLESS_APISIX,        ApiSixDeployer.class);
        DEPLOYER_MAP.put(FixDefs.STORE_MINIO,              MinioDeployer.class);
        
        DEPLOYER_MAP.put(FixDefs.SMS_GATEWAY,              SMSGatewayDeployer.class);
        DEPLOYER_MAP.put(FixDefs.SMS_QUERY_SERVICE,        SMSQueryServiceDeployer.class);
    }

    public static ServiceDeployer getDeployer(final String servType, final String logKey) {
        Class<? extends ServiceDeployer> clazz = DEPLOYER_MAP.get(servType);
        ServiceDeployer servDeployer = null;
        if (clazz != null) {
            try {
                servDeployer = (ServiceDeployer) clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                String errInfo = String.format("%s %s", servType, CONSTS.ERR_SERVICE_DEPLOYER_INIT_ERROR);
                logger.error("{}, {}", errInfo, e.getMessage(), e);
                DeployLog.pubFailLog(logKey, CONSTS.ERR_SERVICE_NOT_FOUND);
            }
        } else {
            String errInfo = String.format("deployer for %s not found ......", servType);
            DeployLog.pubFailLog(logKey, errInfo);
            logger.error("{}", errInfo);
        }

        return servDeployer;
    }

}
