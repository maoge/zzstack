package com.zzstack.paas.underlying.utils.paas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zzstack.paas.underlying.utils.config.RocketMqConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.config.CacheRedisClusterConf;
import com.zzstack.paas.underlying.utils.config.CacheRedisHaConf;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.config.DBConfig.DBNode;
import com.zzstack.paas.underlying.utils.config.PulsarConf;
import com.zzstack.paas.underlying.utils.config.RedisNodes;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException.SdkErrInfo;

public class PaasTopoParser {
    
    private static Logger logger = LoggerFactory.getLogger(PaasTopoParser.class);
    
    private static Map<String, String> DB_DRIVER_MAP = null;
    private static Map<String, String> JDBC_URL_FMT_MAP = null;
    
    static {
        DB_DRIVER_MAP = new HashMap<String, String>();
        
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_ORCL,          CONSTS.DB_DRIVER_ORCL);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_MYSQL,         CONSTS.DB_DRIVER_MYSQL);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_PG,            CONSTS.DB_DRIVER_PG);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_TIDB,          CONSTS.DB_DRIVER_MYSQL);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_VOLTDB,        CONSTS.DB_DRIVER_VOLTDB);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_TDENGINE,      CONSTS.DB_DRIVER_TDENGINE);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_CLICKHOUSE,    CONSTS.DB_DRIVER_CLICKHOUSE);
        
        JDBC_URL_FMT_MAP = new HashMap<String, String>();
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_ORCL,       CONSTS.JDBC_URL_FMT_ORCL);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_MYSQL,      CONSTS.JDBC_URL_FMT_MYSQL);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_PG,         CONSTS.JDBC_URL_FMT_PG);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_TIDB,       CONSTS.JDBC_URL_FMT_MYSQL);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_VOLTDB,     CONSTS.JDBC_URL_FMT_VOLTDB);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_TDENGINE,   CONSTS.JDBC_URL_FMT_TDENGINE);
        JDBC_URL_FMT_MAP.put(CONSTS.DB_TYPE_CLICKHOUSE, CONSTS.JDBC_URL_FMT_CLICKHOUSE);
    }
    
    public static Object parseServiceTopo(String servTopo, Map<String, Object> params) throws PaasSdkException {
        JSONObject servTopoJson = JSONObject.parseObject(servTopo);
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        
        Object res = null;
        switch (servType) {
        case CONSTS.SERV_TYPE_DB_ORACLE_DG:
            res = parseDBOrcl(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_DB_TIDB:
            res = parseTiDB(servTopoJson, params);
            break;
            
        case CONSTS.SERV_TYPE_DB_VOLTDB:
            res = parseVoltDB(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_DB_TDENGINE:
            res = parseTDEngine(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_DB_CLICKHOUSE:
            res = parseClickHouse(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_CACHE_REDIS_HA_CLUSTER:
            res = parseCacheRedisHaCluster(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_CACHE_REDIS_CLUSTER:
            res = parseCacheRedisCluster(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_CACHE_REDIS_MASTER_SLAVE:
            break;
        
        case CONSTS.SERV_TYPE_MQ_ROCKETMQ:
            res = parseRocketMq(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_MQ_PULSAR:
            res = parsePulsar(servTopoJson, params);
            break;
        
        case CONSTS.SERV_TYPE_SERVERLESS_APISIX:
            break;
        
        default:
            logger.error("parse service topo fail ......");
            throw new PaasSdkException(SdkErrInfo.e80040002);
        }
        
        return res;
    }

    public static RocketMqConf parseRocketMq(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        RocketMqConf conf = (RocketMqConf) ServiceConfigTemplate.getConfigTemplate(servType);
        
        JSONObject rocketmqServ = servTopoJson.getJSONObject(FixHeader.HEADER_ROCKETMQ_SERV_CONTAINER);
        JSONObject rocketMqNameSrvContainer = rocketmqServ.getJSONObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JSONArray rocketMqNameSrv = rocketMqNameSrvContainer.getJSONArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);
        String namesrvAddr = "";
        for (int i = 0; i < rocketMqNameSrv.size(); i++) {
            JSONObject jsonRokectMq = rocketMqNameSrv.getJSONObject(i);
            String servIp = jsonRokectMq.getString(FixHeader.HEADER_IP);
            String servPort = jsonRokectMq.getString(FixHeader.HEADER_LISTEN_PORT);

            if (i > 0)
                namesrvAddr += ":";
            
            String mqNode = String.format("%s:%s", servIp, servPort);
            namesrvAddr += mqNode;
        }

        if (namesrvAddr == null || "".equals(namesrvAddr)) {
            throw new PaasSdkException(SdkErrInfo.e80050001);
        }

        conf.rockConf.nameSrvUrl = namesrvAddr;

        return conf;
    }

    public static PulsarConf parsePulsar(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        PulsarConf conf = (PulsarConf) ServiceConfigTemplate.getConfigTemplate(servType);
        
        JSONObject pulsarServ = servTopoJson.getJSONObject(FixHeader.HEADER_PULSAR_SERV_CONTAINER);
        JSONObject pulsarBrokerContainer = pulsarServ.getJSONObject(FixHeader.HEADER_PULSAR_BROKER_CONTAINER);
        JSONArray pulsarBrokerArr = pulsarBrokerContainer.getJSONArray(FixHeader.HEADER_PULSAR_BROKER);
        String pulsarBrokerList = "";
        for (int i = 0; i < pulsarBrokerArr.size(); i++) {
            JSONObject jsonRokectMq = pulsarBrokerArr.getJSONObject(i);
            String brokerIp = jsonRokectMq.getString(FixHeader.HEADER_IP);
            String brokerPort = jsonRokectMq.getString(FixHeader.HEADER_BROKER_PORT);

            if (i > 0)
                pulsarBrokerList += ",";
            
            String brokerNode = String.format("%s:%s", brokerIp, brokerPort);
            pulsarBrokerList += brokerNode;
        }

        if (pulsarBrokerList == null || "".equals(pulsarBrokerList)) {
            throw new PaasSdkException(SdkErrInfo.e80050001);
        }

        pulsarBrokerList = String.format("pulsar://%s", pulsarBrokerList); 
        conf.brokerConf.brokerAddr = pulsarBrokerList;

        return conf;
    }

    public static DBConfig parseDBOrcl(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        DBConfig conf = (DBConfig) ServiceConfigTemplate.getConfigTemplate(servType);
        
        // replace params:
        // ----------------------------------------------
        // 从servTopoJson中取DG_CONTAINER.DG_NAME = %DG_NAME%的ORCL_INSTANCE属性值替换如下参数:
        // 
        // decrypt: %DECRYPT%
        // dbType: "%DB_TYPE%"
        // dbSourceModel: "%DB_SOURCE_MODEL%"
        // 
        // initialSize: %MIN_IDLE%
        // minIdle: %MIN_IDLE%
        // maxActive: %MAX_ACTIVE%
        // validationQuery: "%VALIDATION_QUERY%"
        // ----------------------------------------------
        
        String dgName = (String) params.get(FixHeader.HEADER_DG_NAME);
        JSONObject servContainer = servTopoJson.getJSONObject(FixHeader.HEADER_ORACLE_DG_SERV_CONTAINER);
        JSONArray dbContainer = servContainer.getJSONArray(FixHeader.HEADER_DG_CONTAINER);
        JSONArray orclInstArr = getSpecifiedDG(dbContainer, dgName);
        String activeDBType = getSpecifiedDGActiveDBType(dbContainer, dgName);
        
        if (orclInstArr == null || orclInstArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺少指定的dgName:{}", servTopoJson.toString(), dgName);
            throw new PaasSdkException(SdkErrInfo.e80040003);
        }
        
        String dbType = CONSTS.DB_TYPE_ORCL;
        conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        conf.jdbc.dbType = dbType;
        conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        conf.jdbc.activeDBType = activeDBType;
        String urlFmt = JDBC_URL_FMT_MAP.get(dbType);
        
        List<DBNode> masterNodes = new ArrayList<DBNode>();
        List<DBNode> backupNodes = new ArrayList<DBNode>();
        int size = orclInstArr.size();
        
        for (int i = 0; i < size; ++i) {
            JSONObject orclInst = orclInstArr.getJSONObject(i);
            
            String ip = orclInst.getString(FixHeader.HEADER_IP);
            String nodeType = orclInst.getString(FixHeader.HEADER_NODE_TYPE);
            
            String orclLsnrPort = orclInst.getString(FixHeader.HEADER_ORA_LSNR_PORT);
            String dbUser = orclInst.getString(FixHeader.HEADER_DB_USER);
            String dbPasswd = orclInst.getString(FixHeader.HEADER_DB_PASSWD);
            String dbName = orclInst.getString(FixHeader.HEADER_DB_NAME);
            
            // JDBC_URL_FMT_ORCL = "jdbc:oracle:thin:@%s:%s:%s"
            String jdbcUrl = String.format(urlFmt, ip, orclLsnrPort, dbName);
            DBNode dbNode = new DBNode();
            dbNode.url = jdbcUrl;
            dbNode.username = dbUser;
            dbNode.password = dbPasswd;
        
            if (nodeType.equals(CONSTS.NODE_TYPE_MASTER)) {
                masterNodes.add(dbNode);
            } else {
                backupNodes.add(dbNode);
            }
        }
        
        conf.jdbc.masterDBSources.nodes = masterNodes;
        conf.jdbc.backupDBSources.nodes = backupNodes;
        
        conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        
        conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);
        
        return conf;
    }
    
    public static DBConfig parseTiDB(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        DBConfig conf = (DBConfig) ServiceConfigTemplate.getConfigTemplate(servType);
        
        // replace params:
        // ----------------------------------------------
        // 从servTopoJson中取属性值替换如下参数:
        // 
        // decrypt: %DECRYPT%
        // dbType: "%DB_TYPE%"
        // dbSourceModel: "%DB_SOURCE_MODEL%"
        // 
        // initialSize: %MIN_IDLE%
        // minIdle: %MIN_IDLE%
        // maxActive: %MAX_ACTIVE%
        // validationQuery: "%VALIDATION_QUERY%"
        // ----------------------------------------------
        
        JSONObject servContainer = servTopoJson.getJSONObject(FixHeader.HEADER_TIDB_SERV_CONTAINER);
        JSONObject tidbServContainer = servContainer.getJSONObject(FixHeader.HEADER_TIDB_SERVER_CONTAINER);
        JSONArray tidbServArr = tidbServContainer.getJSONArray(FixHeader.HEADER_TIDB_SERVER);
        
        if (tidbServArr == null || tidbServArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺失TIDB_SERVER节点", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80041001);
        }
        
        String dbType = CONSTS.DB_TYPE_TIDB;
        conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        conf.jdbc.dbType = dbType;
        conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        conf.jdbc.activeDBType = CONSTS.ACTIVE_DB_TYPE_MASTER;
        
        List<DBNode> masterNodes = new ArrayList<DBNode>();
        
        String dbUser = (String) params.get(FixHeader.HEADER_DB_USER);
        String dbPasswd = (String) params.get(FixHeader.HEADER_DB_PASSWD);
        String dbName = (String) params.get(FixHeader.HEADER_DB_NAME);
        String urlFmt = JDBC_URL_FMT_MAP.get(dbType);
        
        for (int i = 0; i < tidbServArr.size(); ++i) {
            JSONObject node = tidbServArr.getJSONObject(i);
            
            String ip = node.getString(FixHeader.HEADER_IP);
            String port = node.getString(FixHeader.HEADER_PORT);
            
            // JDBC_URL_FMT_MYSQL = "jdbc:mysql://%s:%s/%s?useSSL=false";
            String jdbcUrl = String.format(urlFmt, ip, port, dbName);
            
            DBNode dbNode = new DBNode();
            dbNode.url = jdbcUrl;
            dbNode.username = dbUser;
            dbNode.password = dbPasswd;
        
            masterNodes.add(dbNode);
        }
        
        conf.jdbc.masterDBSources.nodes = masterNodes;
        conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        
        conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);
        
        return conf;
    }
    
    public static DBConfig parseVoltDB(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        DBConfig conf = (DBConfig) ServiceConfigTemplate.getConfigTemplate(servType);
        
        JSONObject servContainer = servTopoJson.getJSONObject(FixHeader.HEADER_VOLTDB_SERV_CONTAINER);
        JSONObject voltdbContainer = servContainer.getJSONObject(FixHeader.HEADER_VOLTDB_CONTAINER);
        JSONArray voltdbArr = voltdbContainer.getJSONArray(FixHeader.HEADER_VOLTDB_SERVER);
        
        if (voltdbArr == null || voltdbArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺失VOLTDB_SERVER节点", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80042001);
        }
        
        String dbType = CONSTS.DB_TYPE_VOLTDB;
        conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        conf.jdbc.dbType = dbType;
        conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        conf.jdbc.activeDBType = CONSTS.ACTIVE_DB_TYPE_MASTER;
        
        List<DBNode> masterNodes = new ArrayList<DBNode>();
        
        String dbUser = (String) params.get(FixHeader.HEADER_DB_USER);
        String dbPasswd = (String) params.get(FixHeader.HEADER_DB_PASSWD);
        String dbName = (String) params.get(FixHeader.HEADER_DB_NAME);
        StringBuilder addrs = new StringBuilder();
        
        for (int i = 0; i < voltdbArr.size(); ++i) {
            JSONObject node = voltdbArr.getJSONObject(i);
            
            String ip = node.getString(FixHeader.HEADER_IP);
            String port = node.getString(FixHeader.HEADER_VOLT_CLIENT_PORT);
            String addr = String.format("%s:%s", ip, port);
            
            if (addrs.length() > 0) {
                addrs.append(",");
            }
            
            addrs.append(addr);
        }
        
        // String.format("jdbc:voltdb://%s:%s,%s:%s?autoreconnect=true", "192.168.1.190", "21212", "192.168.1.190", "21212");
        // JDBC_URL_FMT_VOLTDB = "jdbc:voltdb://%s?autoreconnect=true";
        String urlFmt = JDBC_URL_FMT_MAP.get(dbType);
        String jdbcUrl = String.format(urlFmt, addrs.toString(), dbName);
        
        DBNode dbNode = new DBNode();
        dbNode.url = jdbcUrl;
        dbNode.username = dbUser;
        dbNode.password = dbPasswd;
        masterNodes.add(dbNode);
        
        conf.jdbc.masterDBSources.nodes = masterNodes;
        conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        
        conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);
        
        return conf;
    }
    
    public static DBConfig parseTDEngine(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        DBConfig conf = (DBConfig) ServiceConfigTemplate.getConfigTemplate(servType);
        
        JSONObject servContainer = servTopoJson.getJSONObject(FixHeader.HEADER_TDENGINE_SERV_CONTAINER);
        JSONObject dnodeContainer = servContainer.getJSONObject(FixHeader.HEADER_DNODE_CONTAINER);
        JSONArray dnodeArr = dnodeContainer.getJSONArray(FixHeader.HEADER_TD_DNODE);
        
        if (dnodeArr == null || dnodeArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺失TD_DNODE节点", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80043001);
        }
        
        String dbType = CONSTS.DB_TYPE_TDENGINE;
        conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        conf.jdbc.dbType = dbType;
        conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        conf.jdbc.activeDBType = CONSTS.ACTIVE_DB_TYPE_MASTER;
        
        List<DBNode> masterNodes = new ArrayList<DBNode>();
        
        String dbUser = (String) params.get(FixHeader.HEADER_DB_USER);
        String dbPasswd = (String) params.get(FixHeader.HEADER_DB_PASSWD);
        String dbName = (String) params.get(FixHeader.HEADER_DB_NAME);
        String urlFmt = JDBC_URL_FMT_MAP.get(dbType);
        
        for (int i = 0; i < dnodeArr.size(); ++i) {
            JSONObject node = dnodeArr.getJSONObject(i);
            
            String ip = node.getString(FixHeader.HEADER_IP);
            String port = node.getString(FixHeader.HEADER_PORT);
            
            // JDBC_URL_FMT_TDENGINE = "jdbc:TAOS://%s:%s/%s"
            String jdbcUrl = String.format(urlFmt, ip, port, dbName);
            
            DBNode dbNode = new DBNode();
            dbNode.url = jdbcUrl;
            dbNode.username = dbUser;
            dbNode.password = dbPasswd;
        
            masterNodes.add(dbNode);
        }
        
        conf.jdbc.masterDBSources.nodes = masterNodes;
        conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);
        
        return conf;
    }
    
    public static DBConfig parseClickHouse(JSONObject servTopoJson, Map<String, Object> params) throws PaasSdkException {
        String servType  = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        DBConfig conf = (DBConfig) ServiceConfigTemplate.getConfigTemplate(servType);
        
        // replace params:
        // ----------------------------------------------
        // 从servTopoJson中取属性值替换如下参数:
        // 
        // decrypt: %DECRYPT%
        // dbType: "%DB_TYPE%"
        // dbSourceModel: "%DB_SOURCE_MODEL%"
        // 
        // initialSize: %MIN_IDLE%
        // minIdle: %MIN_IDLE%
        // maxActive: %MAX_ACTIVE%
        // validationQuery: "%VALIDATION_QUERY%"
        // ----------------------------------------------
        
        JSONObject servContainer = servTopoJson.getJSONObject(FixHeader.HEADER_CLICKHOUSE_SERV_CONTAINER);
        JSONObject replicasContainer = servContainer.getJSONObject(FixHeader.HEADER_CLICKHOUSE_REPLICAS_CONTAINER);
        JSONArray replicasArr = replicasContainer.getJSONArray(FixHeader.HEADER_CLICKHOUSE_REPLICAS);
        
        if (replicasArr == null || replicasArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺失CLICKHOUSE_REPLICAS节点", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80044001);
        }
        
        String dbType = CONSTS.DB_TYPE_CLICKHOUSE;
        conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        conf.jdbc.dbType = dbType;
        conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        conf.jdbc.activeDBType = CONSTS.ACTIVE_DB_TYPE_MASTER;
        
        List<DBNode> masterNodes = new ArrayList<DBNode>();
        
        String dbUser = (String) params.get(FixHeader.HEADER_DB_USER);
        String dbPasswd = (String) params.get(FixHeader.HEADER_DB_PASSWD);
        String dbName = (String) params.get(FixHeader.HEADER_DB_NAME);
        String urlFmt = JDBC_URL_FMT_MAP.get(dbType);
        
        for (int i = 0; i < replicasArr.size(); ++i) {
            JSONObject replicas = replicasArr.getJSONObject(i);
            
            JSONArray clickhouseArr = replicas.getJSONArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            for (int j = 0; j < clickhouseArr.size(); ++j) {
                JSONObject node = clickhouseArr.getJSONObject(j);
                
                String ip = node.getString(FixHeader.HEADER_IP);
                String port = node.getString(FixHeader.HEADER_TCP_PORT);
                // JDBC_URL_FMT_CLICKHOUSE = "jdbc:clickhouse://%s:%s/%s"
                String jdbcUrl = String.format(urlFmt, ip, port, dbName);
                
                DBNode dbNode = new DBNode();
                dbNode.url = jdbcUrl;
                dbNode.username = dbUser;
                dbNode.password = dbPasswd;
            
                masterNodes.add(dbNode);
            }
        }
        
        conf.jdbc.masterDBSources.nodes = masterNodes;
        conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);
        
        return conf;
    }
    
    public static CacheRedisHaConf parseCacheRedisHaCluster(JSONObject servTopoJson, Map<String, Object> params)
            throws PaasSdkException {
        
        String servType = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        CacheRedisHaConf conf = (CacheRedisHaConf) ServiceConfigTemplate.getConfigTemplate(servType);
        
        JSONObject haServContainer = servTopoJson.getJSONObject("REDIS_HA_CLUSTER_CONTAINER");
        JSONArray haArr = haServContainer.getJSONArray("HA_CONTAINER");
        if (haArr == null || haArr.isEmpty()) {
            logger.error("servTopoJson:{}, 缺少指定的HA_CONTAINER", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80020002);
        }
        
        int redisServSize = haArr.size();
        if (redisServSize != 2) {
            // redis A/B容灾集群节点数为2
            logger.error("servTopoJson:{}, 缺少指定的HA_CONTAINER", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80020002);
        }
        for (int i = 0; i < redisServSize; ++i) {
            JSONObject item = haArr.getJSONObject(i);
            int weight = Integer.valueOf(item.getString(FixHeader.HEADER_WEIGHT));
            String clusterName = item.getString(FixHeader.HEADER_SERV_CONTAINER_NAME);
            
            JSONObject redisClusterServObj = item.getJSONObject(FixHeader.HEADER_REDIS_SERV_CLUSTER_CONTAINER);
            JSONObject redisNodeContainerObj = redisClusterServObj.getJSONObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
            JSONArray redisNodeArr = redisNodeContainerObj.getJSONArray(FixHeader.HEADER_REDIS_NODE);
            int redisNodeArrSize = redisNodeArr.size();
            if (redisNodeArrSize <= 0) {
                logger.error("servTopoJson:{}, 缺少指定的REDIS_NODE", servTopoJson.toString());
                throw new PaasSdkException(SdkErrInfo.e80020003);
            }
            
            // params need replace:
            // clientName: %CLIENT_NAME%
            // slaveConnectionMinimumIdleSize: %SLAVE_MIN_IDLE_SIZE%
            // slaveConnectionPoolSize: %SLAVE_POOL_SIZE%
            // masterConnectionMinimumIdleSize: %MASTER_MIN_IDLE_SIZE%
            // masterConnectionPoolSize: %MASTER_POOL_SIZE%
            // readMode: "READ_MODE"
            // serverMode: %SERVER_MODE%
            
            // %ADDRS_A%
            // %WEIGHT_A%
            
            // %ADDRS_B%
            // %WEIGHT_B%
            
            String clientName = (String) params.get(FixHeader.HEADER_CLIENT_NAME);
            Integer slaveMinIdle = (Integer) params.get(FixHeader.HEADER_SLAVE_MIN_IDLE_SIZE);
            Integer slavePoolSize = (Integer) params.get(FixHeader.HEADER_SLAVE_POOL_SIZE);
            Integer masterMinIdle = (Integer) params.get(FixHeader.HEADER_MASTER_MIN_IDLE_SIZE);
            Integer masterPoolSize = (Integer) params.get(FixHeader.HEADER_MASTER_POOL_SIZE);
            String readMode = (String) params.get(FixHeader.HEADER_READ_MODE);
            String serverMode = (String) params.get(FixHeader.HEADER_SERVER_MODE);
            
            conf.redisConfig.clientName = (clientName != null) ? clientName : "";
            conf.redisConfig.slaveConnectionMinimumIdleSize = (slaveMinIdle != null) ? slaveMinIdle : CONSTS.REDIS_DEFAULT_SLAVE_MIN_IDLE_SIZE;
            conf.redisConfig.slaveConnectionPoolSize = (slavePoolSize != null) ? slavePoolSize : CONSTS.REDIS_DEFAULT_SLAVE_POOL_SIZE;
            conf.redisConfig.masterConnectionMinimumIdleSize = (masterMinIdle != null) ? masterMinIdle : CONSTS.REDIS_DEFAULT_MASTER_MIN_IDLE_SIZE;
            conf.redisConfig.masterConnectionPoolSize = (masterPoolSize != null) ? masterPoolSize : CONSTS.REDIS_DEFAULT_MASTER_POOL_SIZE;
            conf.redisConfig.readMode = readMode;
            conf.redisConfig.serverMode = serverMode;
            
            RedisNodes redisNodes = new RedisNodes();
            redisNodes.id = clusterName;
            redisNodes.weight = weight;
            
            String[] nodeAddresses = new String[redisNodeArrSize];
            for (int idx = 0; idx < redisNodeArrSize; ++idx) {
                JSONObject redisNode = redisNodeArr.getJSONObject(idx);
                String port = redisNode.getString(FixHeader.HEADER_PORT);
                String ip = redisNode.getString(FixHeader.HEADER_IP);
                
                String addr = String.format(CONSTS.REDIS_URL_FMT, ip, port);
                nodeAddresses[idx] = addr;
            }
            redisNodes.nodeAddresses = nodeAddresses;
            
            if (clusterName.equals(CONSTS.HA_QUEUE_REDIS_CLUSTER_A)) {
                conf.redisConfig.serverA = redisNodes;
            } else {
                conf.redisConfig.serverB = redisNodes;
            }
        }
        
        return conf;
    }
    
    public static CacheRedisClusterConf parseCacheRedisCluster(JSONObject servTopoJson, Map<String, Object> params)
            throws PaasSdkException {
        
        String servType = servTopoJson.getString(FixHeader.HEADER_SERV_TYPE);
        CacheRedisClusterConf conf = (CacheRedisClusterConf) ServiceConfigTemplate.getConfigTemplate(servType);

        JSONObject redisClusterServObj = servTopoJson.getJSONObject(FixHeader.HEADER_REDIS_SERV_CLUSTER_CONTAINER);
        JSONObject redisNodeContainerObj = redisClusterServObj.getJSONObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JSONArray redisNodeArr = redisNodeContainerObj.getJSONArray(FixHeader.HEADER_REDIS_NODE);
        int redisNodeArrSize = redisNodeArr.size();
        if (redisNodeArrSize <= 0) {
            logger.error("servTopoJson:{}, 缺少指定的REDIS_NODE", servTopoJson.toString());
            throw new PaasSdkException(SdkErrInfo.e80020003);
        }

        // params need replace:
        // clientName: %CLIENT_NAME%
        // slaveConnectionMinimumIdleSize: %SLAVE_MIN_IDLE_SIZE%
        // slaveConnectionPoolSize: %SLAVE_POOL_SIZE%
        // masterConnectionMinimumIdleSize: %MASTER_MIN_IDLE_SIZE%
        // masterConnectionPoolSize: %MASTER_POOL_SIZE%
        // readMode: "READ_MODE"
        // serverMode: %SERVER_MODE%

        // %ADDRS_A%
        // %WEIGHT_A%

        // %ADDRS_B%
        // %WEIGHT_B%

        String clientName = (String) params.get(FixHeader.HEADER_CLIENT_NAME);
        Integer slaveMinIdle = (Integer) params.get(FixHeader.HEADER_SLAVE_MIN_IDLE_SIZE);
        Integer slavePoolSize = (Integer) params.get(FixHeader.HEADER_SLAVE_POOL_SIZE);
        Integer masterMinIdle = (Integer) params.get(FixHeader.HEADER_MASTER_MIN_IDLE_SIZE);
        Integer masterPoolSize = (Integer) params.get(FixHeader.HEADER_MASTER_POOL_SIZE);
        String readMode = (String) params.get(FixHeader.HEADER_READ_MODE);
        String serverMode = (String) params.get(FixHeader.HEADER_SERVER_MODE);

        conf.redisConfig.clientName = (clientName != null) ? clientName : "";
        conf.redisConfig.slaveConnectionMinimumIdleSize = (slaveMinIdle != null) ? slaveMinIdle
                : CONSTS.REDIS_DEFAULT_SLAVE_MIN_IDLE_SIZE;
        conf.redisConfig.slaveConnectionPoolSize = (slavePoolSize != null) ? slavePoolSize
                : CONSTS.REDIS_DEFAULT_SLAVE_POOL_SIZE;
        conf.redisConfig.masterConnectionMinimumIdleSize = (masterMinIdle != null) ? masterMinIdle
                : CONSTS.REDIS_DEFAULT_MASTER_MIN_IDLE_SIZE;
        conf.redisConfig.masterConnectionPoolSize = (masterPoolSize != null) ? masterPoolSize
                : CONSTS.REDIS_DEFAULT_MASTER_POOL_SIZE;
        conf.redisConfig.readMode = readMode;
        conf.redisConfig.serverMode = serverMode;

        RedisNodes redisNodes = new RedisNodes();

        String[] nodeAddresses = new String[redisNodeArrSize];
        for (int idx = 0; idx < redisNodeArrSize; ++idx) {
            JSONObject redisNode = redisNodeArr.getJSONObject(idx);
            String port = redisNode.getString(FixHeader.HEADER_PORT);
            String ip = redisNode.getString(FixHeader.HEADER_IP);

            String addr = String.format(CONSTS.REDIS_URL_FMT, ip, port);
            nodeAddresses[idx] = addr;
        }
        redisNodes.nodeAddresses = nodeAddresses;
        conf.redisConfig.server = redisNodes;

        return conf;
    }
    
    private static JSONArray getSpecifiedDG(JSONArray dbContainer, String dgName) {
        JSONArray res = null;
        Iterator<Object> it = dbContainer.iterator();
        while (it.hasNext()) {
            JSONObject item = (JSONObject) it.next();
            if (item.getString("DG_NAME").equals(dgName)) {
                res = item.getJSONArray("ORCL_INSTANCE");
                break;
            }
        }
        
        return res;
    }
    
    private static String getSpecifiedDGActiveDBType(JSONArray dbContainer, String dgName) {
        String res = CONSTS.ACTIVE_DB_TYPE_MASTER;
        Iterator<Object> it = dbContainer.iterator();
        while (it.hasNext()) {
            JSONObject item = (JSONObject) it.next();
            if (item.getString("DG_NAME").equals(dgName)) {
                res = item.getString("ACTIVE_DB_TYPE");
                break;
            }
        }
        
        return res;
    }

}
