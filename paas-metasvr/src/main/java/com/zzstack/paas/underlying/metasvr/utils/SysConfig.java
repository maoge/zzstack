package com.zzstack.paas.underlying.metasvr.utils;

import com.zzstack.paas.underlying.utils.PropertiesUtils;
import com.zzstack.paas.underlying.utils.CryptTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SysConfig {

    private static Logger logger = LoggerFactory.getLogger(SysConfig.class);

    private static final String APP_PROP_FILE = "app";

    private String webApiIP = "127.0.0.1";
    private int webApiPort = 9090;
    private boolean webApiUseSSL = false;

    private boolean serverlessGatewayRegist = false;  // serverless.gateway.regist=false
    private String serverlessGatewayAddress = "http://127.0.0.1:9080";
    private String serverlessGatewayUpstreamID = "paas_metasvr";
    private String serverlessGatewayServiceID = "metasvr"; // serverless.gateway.serviceid=metasvr
    private String serverlessGatewayXAPIKey = "edd1c9f034335f136f87ad84b625c8f2";

    private long vertxOptionMaxEventLoopExecuteTime = 3000L;
    private int vertxEvLoopSize = 4;
    private int vertxWorkerPoolSize = 32;
    private long vertxWokerMaxExecTime = 1500000;

    private boolean raftClusterEnabled = false;
    private String raftClusterNodes = "";
    private String raftSelf = "";
    private int raftHeartbeatPeriodMilliseconds = 10000;
    
    // collect.enabled=true
    // # 定时探测任务生成间隔(ms)
    // collect.interval=10000
    private boolean collectEnabled = false;
    private int collectInterval = 10000;
    
    private String alarmNotifyUrl;
    private boolean alarmNotifyEnabled = true;

    private int threadPoolCoreSize = 20;
    private int threadPoolMaxSize = 40;
    private int threadPoolKeepaliveTime = 3;
    private int threadPoolWorkQueueLen = 1000;

    private String redisCluster = "127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003";
    private boolean redisEncrypt = false;
    private String redisAuth = "";
    private int redisPoolMaxSize = 20;
    private int redisPoolMinSize = 10;
    private int redisMaxWaitMillis = 3000;

    private String eventbusAddress = "127.0.0.1:6650";
    private String eventbusConsumerSubscription = "sub_001";
    private int eventbusExpireTtl = 60000;

    private int alarmTimeWindow = 600000;

    private long passwordExpire = 7776000L;
    private boolean needAuth = true;
    private boolean checkBlackWhiteList = false;

    private String metaDBYamlName = "metadb";
    private String tdYamlName = "tdengine";

    private static SysConfig theInstance = null;
    private static Object mtx = null;

    static {
        mtx = new Object();
    }

    public static SysConfig get() {
        if (theInstance != null)
            return theInstance;

        synchronized (mtx) {
            if (theInstance == null) {
                theInstance = new SysConfig();
            }
        }

        return theInstance;
    }

    private SysConfig() {
        PropertiesUtils props = PropertiesUtils.getInstance(APP_PROP_FILE);

        try {
            this.webApiIP = props.get("web.api.ip", "127.0.0.1");
            this.webApiPort = props.getInt("web.api.port", 9090);
            this.webApiUseSSL = props.getBoolean("web.api.useSSL", false);

            this.serverlessGatewayRegist = props.getBoolean("serverless.gateway.regist", false);
            this.serverlessGatewayAddress = props.get("serverless.gateway.address", "http://127.0.0.1:9080");
            this.serverlessGatewayUpstreamID = props.get("serverless.gateway.upstreamid", "paas_metasvr");
            this.serverlessGatewayServiceID = props.get("serverless.gateway.serviceid", "metasvr");
            this.serverlessGatewayXAPIKey = props.get("serverless.gateway.xapikey", "");

            this.vertxOptionMaxEventLoopExecuteTime = props.getLong("vertx.option.maxEventLoopExecuteTime", 3000L);
            this.vertxEvLoopSize = props.getInt("vertx.evloopsize", 4);
            this.vertxWorkerPoolSize = props.getInt("vertx.workerpoolsize", 32);
            this.vertxWokerMaxExecTime = props.getLong("vertx.woker.maxexectime", 1500000L);

            this.raftClusterEnabled = props.getBoolean("raft.cluster.enabled", false);
            this.raftClusterNodes = props.get("raft.cluster.nodes", "");
            this.raftSelf = props.get("raft.self", "");
            this.raftHeartbeatPeriodMilliseconds = props.getInt("raft.heartbeat.period.milliseconds", 10000);

            this.collectEnabled = props.getBoolean("collect.enabled", false);
            this.collectInterval = props.getInt("collect.interval", 10000);
            
            this.alarmNotifyUrl = props.get("alarm.notify.url");
            this.alarmNotifyEnabled = props.getBoolean("alarm.notify.enabled", true);

            this.threadPoolCoreSize = props.getInt("thread.pool.core.size", 20);
            this.threadPoolMaxSize = props.getInt("thread.pool.max.size", 40);
            this.threadPoolKeepaliveTime = props.getInt("thread.pool.keepalive.time", 3);
            this.threadPoolWorkQueueLen = props.getInt("thread.pool.workqueue.len", 1000);

            this.redisCluster = props.get("redis.cluster", "127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003");
            this.redisEncrypt = props.getBoolean("redis.encrypt", false);
            this.redisAuth = props.get("redis.auth");
            if (redisEncrypt && StringUtils.isNull(redisAuth)) {
                this.redisAuth = CryptTools.decrypt(redisAuth);
            }
            this.redisPoolMaxSize = props.getInt("redis.pool.max.size", 20);
            this.redisPoolMinSize = props.getInt("redis.pool.min.size", 10);
            this.redisMaxWaitMillis = props.getInt("redis.max.wait.millis", 3000);

            this.eventbusAddress = props.get("eventbus.address", "127.0.0.1:6650");
            this.eventbusConsumerSubscription = props.get("eventbus.consumer.subscription", "sub_001");
            this.eventbusExpireTtl = props.getInt("eventbus.expire.ttl", 60000);

            this.alarmTimeWindow = props.getInt("alarm.time.window", 600000);

            this.passwordExpire = props.getLong("password.expire", 7776000L);
            this.needAuth = props.getBoolean("need.auth", true);
            this.checkBlackWhiteList = props.getBoolean("check.blackwhite.list", false);

            this.metaDBYamlName = props.get("metadb.yaml.name", "metadb");
            this.tdYamlName = props.get("td.yaml.name", "conf");

        } catch (Exception e) {
            logger.error("SysConfig init error ......", e);
        }
    }

    public String getWebApiIP() {
        return this.webApiIP;
    }

    public int getWebApiPort() {
        return this.webApiPort;
    }

    public boolean isWebApiUseSSL() {
        return webApiUseSSL;
    }

    public boolean isServerlessGatewayRegist() {
        return serverlessGatewayRegist;
    }

    public void setServerlessGatewayRegist(boolean serverlessGatewayRegist) {
        this.serverlessGatewayRegist = serverlessGatewayRegist;
    }

    public String getServerlessGatewayAddress() {
        return this.serverlessGatewayAddress;
    }

    public String getServerlessGatewayUpstreamID() {
        return this.serverlessGatewayUpstreamID;
    }

    public String getServerlessGatewayServiceID() {
        return this.serverlessGatewayServiceID;
    }

    public String getServerlessGatewayXAPIKey() {
        return this.serverlessGatewayXAPIKey;
    }

    public long getVertxOptionMaxEventLoopExecuteTime() {
        return this.vertxOptionMaxEventLoopExecuteTime;
    }
    
    public long getVertxWokerMaxExecTime() {
        return this.vertxWokerMaxExecTime;
    }

    public int getVertxEvLoopSize() {
        return this.vertxEvLoopSize;
    }

    public int getVertxWorkerPoolSize() {
        return this.vertxWorkerPoolSize;
    }

    public boolean isRaftClusterEnabled() {
        return raftClusterEnabled;
    }

    public String getRaftClusterNodes() {
        return raftClusterNodes;
    }

    public String getRaftSelf() {
        return raftSelf;
    }

    public int getRaftHeartbeatPeriodMilliseconds() {
        return raftHeartbeatPeriodMilliseconds;
    }

    public int getThreadPoolCoreSize() {
        return this.threadPoolCoreSize;
    }

    public int getThreadPoolMaxSize() {
        return this.threadPoolMaxSize;
    }

    public int getThreadPoolKeepaliveTime() {
        return this.threadPoolKeepaliveTime;
    }

    public int getThreadPoolWorkQueueLen() {
        return this.threadPoolWorkQueueLen;
    }

    public String getRedisCluster() {
        return this.redisCluster;
    }

    public String getRedisAuth() {
        return this.redisAuth;
    }

    public int getRedisPoolMaxSize() {
        return this.redisPoolMaxSize;
    }

    public int getRedisPoolMinSize() {
        return this.redisPoolMinSize;
    }

    public int getRedisMaxWaitMillis() {
        return this.redisMaxWaitMillis;
    }

    public String getEventbusAddress() {
        return this.eventbusAddress;
    }

    public String getEventbusConsumerSubscription() {
        return this.eventbusConsumerSubscription;
    }

    public int getEventbusExpireTtl() {
        return eventbusExpireTtl;
    }

    public void setEventbusExpireTtl(int eventbusExpireTtl) {
        this.eventbusExpireTtl = eventbusExpireTtl;
    }

    public int getAlarmTimeWindow() {
        return this.alarmTimeWindow;
    }

    public long getPasswordExpire() {
        return this.passwordExpire;
    }

    public boolean isNeedAuth() {
        return this.needAuth;
    }

    public boolean checkBlackWhiteList() {
        return this.checkBlackWhiteList;
    }

    public String getMetaDBYamlName() {
        return this.metaDBYamlName;
    }

    public String getTDYamlName() {
        return this.tdYamlName;
    }

    public boolean isCollectEnabled() {
        return collectEnabled;
    }

    public int getCollectInterval() {
        return collectInterval;
    }

    public String getAlarmNotifyUrl() {
        return alarmNotifyUrl;
    }

    public boolean isAlarmNotifyEnabled() {
        return alarmNotifyEnabled;
    }

}
