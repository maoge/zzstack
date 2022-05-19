package com.zzstack.paas.underlying.metasvr.consts;

public class FixDefs {

	public static final int    UNIT_G                             =   1024*1024*1024;
	public static final int    UNIT_M                             =   1024*1024;
	public static final int    UNIT_K                             =   1024;

	public static final long   SSH_CMD_TIMEOUT                    =   15000;

	public static final String SESSION_PREFIX                     =   "session::";
	public static final String LOGIN_TIME                         =   "login_time";
	public static final String EXPIRE_TIME                        =   "expire_time";

	public static final String ACC_NAME                           =   "acc_name";

	public static final int    LOGIN_TTL                          =   3600;
	public static final int    EVENT_EXPIRE_TTL                   =   10000;

	public static final int    STR_NOT_FOUND                      =   -1;

	public static final int    CODE_OK                            =    0;
	public static final int    CODE_NOK                           =   -1;
	public static final int    REVOKE_NOK_QUEUE_EXIST             =   -2;
	public static final int    REVOKE_AUTH_FAIL                   =   -3;
	public static final int    REVOKE_AUTH_IP_LIMIT               =   -4;
	public static final int    SERVICE_NOT_INIT                   =   -5;

	public static final String S_TRUE                             =   "true";
	public static final String S_FALSE                            =   "false";

	public static final String INFO_OK                            =   "OK";
	public static final String STR_TRUE                           =   "1";
	public static final String STR_FALSE                          =   "0";
	
	public static final String STR_PRE_EMBADDED                   =   "5";
	public static final String STR_ALARM                          =   "4";
	public static final String STR_ERROR                          =   "3";
	public static final String STR_WARN                           =   "2";
	public static final String STR_DEPLOYED                       =   "1";
	public static final String STR_SAVED                          =   "0";

	public static final int    OP_TYPE_ADD                        =    1;
	public static final int    OP_TYPE_MOD                        =    2;
	public static final int    OP_TYPE_DEL                        =    3;

	public static final int    TOPO_TYPE_LINK                     =    1;
	public static final int    TOPO_TYPE_CONTAIN                  =    2;

	public static final int    POS_DEFAULT_VALUE                  =   -1;
	
	public static final int    DEFAULT_SEQ_STEP                   =   100;
	
	public static final String DEFAULT_TIDB_ROOT_PASSWD           =   "abcd.1234";

	public static final String MAGIC_KEY                          =   "MAGIC_KEY";
	public static final String LOG_KEY                            =   "LOG_KEY";
	public static final String RET_CODE                           =   "RET_CODE";
	public static final String RET_INFO                           =   "RET_INFO";
	public static final String PASSWD_MIX                         =   "^^||";
	public static final String COUNT                              =   "COUNT";
	public static final String CONTAINER_INDEX                    =   "_CONTAINER";

	public static final String SCHEMA_OBJECT                      =   "object";
	public static final String SCHEMA_ARRAY                       =   "array";
	
	public static final String COLLECTD_PLUGIN                    =   "plugin";
	public static final String COLLECTD_PLUGIN_INSTANCE           =   "plugin_instance";
	public static final String COLLECTD_HOST                      =   "host";
	public static final String COLLECTD_DSTYPES                   =   "dstypes";
	public static final String COLLECTD_TYPE                      =   "type";
	public static final String COLLECTD_TYPE_INSTANCE             =   "type_instance";
	public static final String COLLECTD_TIME                      =   "time";
	public static final String COLLECTD_INTERVAL                  =   "interval";
	public static final String COLLECTD_VALUES                    =   "values";
	
	public static final String PLUGIN_CPU                         =   "cpu";
	public static final String PLUGIN_MEMORY                      =   "memory";
	public static final String PLUGIN_DISK                        =   "disk";
	public static final String PLUGIN_INTERFACE                   =   "interface";
	
	public static final String CPU_USER                           =   "user";
	public static final String CPU_SYSTEM                         =   "system";
	public static final String CPU_WAIT                           =   "wait";
	public static final String CPU_INTERRUPT                      =   "interrupt";
	public static final String CPU_IDLE                           =   "idle";
	public static final String CPU_NICE                           =   "nice";
	public static final String CPU_SOFTIRQ                        =   "softirq";
	public static final String CPU_STEAL                          =   "steal";
	
    public static final String IF_PACKETS                         =   "if_packets";
    public static final String IF_OCTETS                          =   "if_octets";
	public static final String IF_DROPPED                         =   "if_dropped";
	public static final String IF_ERRORS                          =   "if_errors";
	
	public static final String DISK_OPS                           =   "disk_ops";
	public static final String DISK_OCTETS                        =   "disk_octets";
	public static final String DISK_TIME                          =   "disk_time";
	public static final String DISK_IO_TIME                       =   "disk_io_time";
	public static final String DISK_MERGED                        =   "disk_merged";
	
    public static final String MEM_USED                           =   "used";
    public static final String MEM_FREE                           =   "free";
    public static final String MEM_BUFFERED                       =   "buffered";
    public static final String MEM_CACHED                         =   "cached";
    public static final String MEM_SLAB_UNRECL                    =   "slab_unrecl";
    public static final String MEM_SLAB_RECL                      =   "slab_recl";
    
	public static final String ACC_CREATE_SUCC                    =   "账户创建成功";
	public static final String ACC_PASSWD_NOT_ALLOW_EMPTY         =   "密码不能为空";
	public static final String ACC_NAME_NOT_ALLOW_EMPTY           =   "用户名不能为空";
	public static final String ACC_PHONE_NOT_ALLOW_EMPTY          =   "手机号不能为空";
	public static final String ACC_MAIL_NOT_ALLOW_EMPTY           =   "邮箱不能为空";
	public static final String ACC_MAIL_REGX_NOT_MATCH            =   "邮箱正则校验不符合规则";
	public static final String ACC_PHONE_REGX_NOT_MATCH           =   "手机号码正则校验不符合规则";
	public static final String ACC_NAME_EXISTS                    =   "用户名已存在";
	public static final String ACC_PHONE_EXISTS                   =   "手机号码已存在";
	public static final String ACC_MAIL_EXISTS                    =   "邮箱已存在";
	public static final String ACC_LOGIN_SUCC                     =   "账户密码认证通过";
	public static final String ACC_LOGIN_FAIL                     =   "账户密码认证未通过";
	public static final String ACC_CHECK_SUCC                     =   "账户信息校验通过";
	public static final String ACC_LOCAL_SESSION_EXPIRED          =   "本地登录状态已过期";
	public static final String ACC_MAGIC_KEY_NOT_EQUAL            =   "token校验失败,请重新登录";
	public static final String ACC_MAGIC_KEY_EXPIRED              =   "token已过期，请重新登录";

	public static final String ERR_DB                             =   "db error";
	public static final String ERR_NETWORK                        =   "io network error";
	public static final String ERR_JSONNODE_ILLEGAL               =   "json node illegal";
	public static final String ERR_OP_TYPE                        =   "OP_TYPE error";
	public static final String ERR_METADATA_NOT_FOUND             =   "meta data not found ......";
	public static final String ERR_SERVICE_NOT_FOUND              =   "service not found ......";
	public static final String ERR_SERVICE_ALLREADY_DEPLOYED      =   "service all ready deployed ......";
	public static final String ERR_SERVICE_NOT_DEPLOYED           =   "service not deployed ......";
	public static final String ERR_INSTANCE_ALLREADY_DEPLOYED     =   "instance all ready deployed ......";
	public static final String ERR_INSTANCE_NOT_DEPLOYED          =   "instance not deployed ......";
	public static final String ERR_INTERNAL                       =   "internal error ......";
	public static final String ERR_INSTANCE_NOT_FOUND             =   "instance not found ......";
	public static final String ERR_CMPT_NOT_FOUND                 =   "component not found ......";
	public static final String ERR_NO_DEL_DEPLOYED_SERV           =   "cannot delete deployed service ......";
	public static final String ERR_SERVER_NOT_NULL                =   "server contains sub ssh, can not delete ......";
	public static final String ERR_SSH_IS_USING                   =   "ssh resource is using, can not delete ......";
	public static final String ERR_SSH_EXEC_TIMEOUT               =   "ssh2 exec timeout ......";
	public static final String ERR_HOSTINFO_NOT_COMPLETE          =   "host info not complete ......";
	public static final String ERR_SSH_TIMEOUT                    =   "exec remote ssh cmd timeout!";
	public static final String SSH_JSCH_INFO_NOT_SET              =   "JschUserInfo not set ......";
	public static final String ERR_EXIST_MULTI_MASTER_NODE        =   "multi master node exists ......";
	public static final String ERR_ETCD_NODE_REQUIRED_CLUSTER     =   "product env must least 3 node ......";
	public static final String ERR_ETCD_NODE_LESS_THAN_ONE        =   "find etcd node less than one, but dev or test env must single or cluster";
	public static final String ERR_APISIX_NODE_LESS_THAN_ONE      =   "find apisix node less than one, but must single or cluster";
	public static final String ERR_APISIX_NODE_INSTANT_NOT_FOUND  =   " not find apisix node by instantId";

    public static final int    COLLECTD_FILE_ID                   =   0;
	public static final int    CACHE_REDIS_SERVER_FILE_ID         =   1;
	public static final int    CACHE_REDIS_PROXY_FILE_ID          =   2;
	public static final int    DB_PD_SERVER_FILE_ID               =   3;
	public static final int    DB_TIKV_SERVER_FILE_ID             =   4;
	public static final int    DB_TIDB_SERVER_FILE_ID             =   5;
	public static final int    MQ_ROCKET_MQ_FILE_ID               =   6;
	public static final int    SERVERLESS_ETCD_FILE_ID            =   7;
	public static final int    SERVERLESS_APISIX_FILE_ID          =   8;
	public static final int    COMMON_TOOLS_JDK_FILE_ID           =   9;
	public static final int    DB_TDENGINE_FILE_ID                =   10;
	public static final int    SERVERLESS_APISIX_LUA_FILE_ID      =   11;
	public static final int    SERVERLESS_APISIX_LUAROCKS_FILE_ID =   12;
	public static final int    SERVERLESS_APISIX_OPENRESTY_FILE_ID=   13;
    public static final int    SMS_SERVER_FILE_ID                 =   14;
    public static final int    SMS_PROCESS_FILE_ID                =   15;
    public static final int    SMS_CLIENT_FILE_ID                 =   16;
    public static final int    SMS_BATSAVE_FILE_ID                =   17;
    public static final int    SMS_STATS_FILE_ID                  =   18;
    public static final int    DB_TIDB_DASHBOARD_PROXY_FILE_ID    =   19;
    public static final int    ZK_FILE_ID                         =   20;
    public static final int    BOOKIE_FILE_ID                     =   21;
    public static final int    PULSAR_FILE_ID                     =   22;
    public static final int    PROMETHEUS_FILE_ID                 =   23;
    public static final int    GRAFANA_FILE_ID                    =   24;
    public static final int    PROMETHEUS_PULSAR_YML_FILE_ID      =   25;
    public static final int    DB_CLICKHOUSE_FILE_ID              =   26;
    public static final int    COMMON_ZK_FILE_ID                  =   27;
    public static final int    PROMETHEUS_CLICKHOUSE_YML_FILE_ID  =   28;
    public static final int    PROMETHEUS_APISIX_YML_FILE_ID      =   29;
    public static final int    ROCKETMQ_CONSOLE_FILE_ID           =   30;
    public static final int    DB_VOLTDB_FILE_ID                  =   31;
    public static final int    PULSAR_MANAGER_FILE_ID             =   32;
    public static final int    DB_YUGABYTEDB_FILE_ID              =   33;
    public static final int    NGX_FILE_ID                        =   34;
    public static final int    SMS_QUERY_SERVER_FILE_ID           =   35;
    public static final int    NGX_SMS_QUERY_CONF_FILE_ID         =   36;
    public static final int    STORE_MINIO_FILE_ID                =   37;

    public static final String PAAS_ROOT                          =   "paas";
    public static final String COMMON_TOOLS_ROOT                  =   "tools";
    public static final String CACHE_REDIS_ROOT                   =   "cache/redis";
    public static final String DB_TIDB_ROOT                       =   "db/tidb";
    public static final String SERVERLESS_ROOT                    =   "serverless";
    public static final String SMS_GATEWAY_ROOT                   =   "sms/sms_gateway";
    public static final String SMS_QUERY_ROOT                     =   "sms/sms_query";
    public static final String COLLECTD_ROOT                      =   "collectd";

    public static final String MQ_ROCKETMQ_ROOT                   =   "mq/rocketmq";
    public static final String MQ_PULSAR_ROOT                     =   "mq/pulsar";
    public static final String DB_TDENGINE_ROOT                   =   "db/tdengine";
    public static final String DB_CLICKHOUSE_ROOT                 =   "db/clickhouse";
    public static final String DB_VOLTDB_ROOT                     =   "db/voltdb";
    public static final String DB_YUGABYTEDB_ROOT                 =   "db/yugabyte";
    public static final String STORE_MINIO_ROOT                   =   "store/minio";

    public static final String CACHE_REDIS_PROXY                  =   "cache_redis_proxy";
    public static final String CACHE_REDIS_PROXY_PREFIX           =   "cache_redis_proxy_";

    public static final String HTML_LIEN_END                      =   "<br/>";
    public static final String DEPLOY_SINGLE_SUCCESS_BEGIN_STYLE  =   "<span style='color:blue;'>";
    public static final String DEPLOY_SINGLE_FAIL_BEGIN_STYLE     =   "<span style='color:yellow;'>";
    public static final String END_STYLE                          =   "</span>";
    
    public static final String JSON_ARRAY_PREFIX                  =   "[";
    public static final String JSON_OBJECT_PREFIX                 =   "{";

    public static final String APISIX_CONFIG                      =   "config.yaml";
    public static final String APISIX_APISIX                      =   "apisix";
    public static final String APISIX_DASHBOARD_CONF              =   "conf.yaml";
    public static final String APISIX_LUA                         =   "lua";
    public static final String APISIX_LUAROCKS                    =   "luarocks";
    public static final String APISIX_OPENRESTY                   =   "openresty";
    public static final String APISIX_LUAROCKS_CONFIG             =   "config-5.1.lua";
    public static final String CONF_CONTROL_PORT                  =   "%CONTROL_PORT%";
    public static final String CONF_APISIX_IP                     =   "%APISIX_IP%";
    public static final String CONF_ETCD_ADDR_LIST                =   "%ETCD_ADDR_LIST%";
    public static final String CONF_HTTP_PORT                     =   "%HTTP_PORT%";
    public static final String CONF_TCP_PORT                      =   "%TCP_PORT%";
    public static final String CONF_MYSQL_PORT                    =   "%MYSQL_PORT%";
    public static final String CONF_INTERSERVER_HTTP_PORT         =   "%INTERSERVER_HTTP_PORT%";
    public static final String CONF_LISTEN_HOST                   =   "%LISTEN_HOST%";
    public static final String CONF_MAX_CONNECTIONS               =   "%MAX_CONNECTIONS%";
    public static final String CONF_MAX_CONCURRENT_QUERIES        =   "%MAX_CONCURRENT_QUERIES%";
    public static final String CONF_MAX_SERVER_MEMORY_USAGE       =   "%MAX_SERVER_MEMORY_USAGE%";
    public static final String CONF_CLICKHOUSE_SHARDS             =   "%CLICKHOUSE_SHARDS%";
    public static final String CONF_ZK_NODES                      =   "%ZK_NODES%";
    public static final String CONF_SHARD_ID                      =   "%SHARD_ID%";
    public static final String CONF_REPLICA_ID                    =   "%REPLICA_ID%";
    public static final String CONF_MAX_MEMORY_USAGE              =   "%MAX_MEMORY_USAGE%";
    public static final String CONF_SSL_PORT                      =   "%SSL_PORT%";
    public static final String CONF_INST_ID_MD5                   =   "%INST_ID_MD5%";
    public static final String CONF_OPENRESTY_HOME                =   "%OPENRESTY_HOME%";
    public static final String CONF_APISIX_HOME                   =   "%APISIX_HOME%";
    public static final String CONF_DASHBOARD_IP                  =   "%DASHBOARD_IP%";
    public static final String CONF_DASHBOARD_PORT                =   "%DASHBOARD_PORT%";
    public static final String CONF_ETCD_ADDR                     =   "%ETCD_ADDR%";
	public static final String CONF_APISIX_ROOT                   =   "%APISIX_ROOT%";
	public static final String CONF_APISIX_LUA                    =   "%APISIX_LUA%";
	public static final String CONF_APISIX_OPENRESTY              =   "%APISIX_OPENRESTY%";
	public static final String CONF_NAME                          =   "%NAME%";
	public static final String CONF_LISTEN_CLIENT_URLS            =   "%LISTEN_CLIENT_URLS%";
	public static final String CONF_LISTEN_PEER_URLS              =   "%LISTEN_PEER_URLS%";
	public static final String CONF_ADVERTISE_CLIENT_URLS         =   "%ADVERTISE_CLIENT_URLS%";
	public static final String CONF_MINIO_REGION                  =   "%MINIO_REGION%";
	public static final String CONF_MINIO_USER                    =   "%MINIO_USER%";
	public static final String CONF_MINIO_PASSWD                  =   "%MINIO_PASSWD%";
	public static final String CONF_MINIO_BROWSER                 =   "%BROWSER%";
	public static final String CONF_ADDRESS                       =   "%ADDRESS%";
	public static final String CONF_CONSOLE_ADDRESS               =   "%CONSOLE_ADDRESS%";
	public static final String CONF_ENDPOINTS                     =   "%ENDPOINTS%";
	
	public static final String VOLTDB_ADMIN_NAME                  =   "admin";
	public static final String VOLTDB_ADMIN_PWD                   =   "admin.1234";

	public static final String REDIS_CONF                         =   "redis.conf";
	public static final String PROXY_CONF                         =   "proxy.conf";
	public static final String CONF_SERV_IP                       =   "%SERV_IP%";
	public static final String CONF_SERV_PORT                     =   "%SERV_PORT%";
	public static final String CONF_PID_FILE                      =   "%PID_FILE%";
	public static final String CONF_LOG_FILE                      =   "%LOG_FILE%";
	public static final String CONF_MAX_CONN                      =   "%MAX_CONN%";
	public static final String CONF_PROXY_THREADS                 =   "%PROXY_THREADS%";
	public static final String CONF_MAX_MEMORY                    =   "%MAX_MEMORY%";
	public static final String CONF_APPENDONLY_FILENAME           =   "%APPENDONLY_FILENAME%";
	public static final String REDIS_CLUSTER_CONF_FILENAME        =   "%REDIS_CLUSTER_CONF_FILENAME%";
	public static final String CONF_CLUSTER_ENABLED               =   "%CLUSTER_ENABLED%";
	public static final String CONF_ROCKET_HOME                   =   "%ROCKETMQ_HOME%";
	public static final String CONF_KV_CONFIG_PATH                =   "%KV_CONFIG_PATH%";
	public static final String CONF_LISTEN_PORT                   =   "%LISTEN_PORT%";
	public static final String CONF_BROKER_CLUSTER_NAME           =   "%BROKER_CLUSTER_NAME%";
	public static final String CONF_BROKER_NAME                   =   "%BROKER_NAME%";
	public static final String CONF_BROKER_ID                     =   "%BROKER_ID%";
	public static final String CONF_NAMESRV_ADDR                  =   "%NAMESRV_ADDR%";
	public static final String CONF_BROKER_IP                     =   "%BROKER_IP%";
	public static final String CONF_STORE_ROOT                    =   "%STORE_ROOT%";
	public static final String CONF_COMMIT_LOG_PATH               =   "%COMMIT_LOG_PATH%";
	public static final String CONF_CONSUME_QUEUE_PATH            =   "%CONSUME_QUEUE_PATH%";
	public static final String CONF_INDEX_PATH                    =   "%INDEX_PATH%";
	public static final String CONF_CHECKPOINT_PATH               =   "%CHECKPOINT_PATH%";
	public static final String CONF_ABORT_FILE_PATH               =   "%ABORT_FILE_PATH%";
	public static final String CONF_BROKER_ROLE                   =   "%BROKER_ROLE%";
	public static final String CONF_FLUSH_DISK_TYPE               =   "%FLUSH_DISK_TYPE%";
    public static final String CONF_UUID                          =   "%UUID%";
    public static final String CONF_META_SVR_URL                  =   "%META_SVR_URL%";
    public static final String CONF_META_SVR_USR                  =   "%META_SVR_USR%";
    public static final String CONF_META_SVR_PASSWD               =   "%META_SVR_PASSWD%";
    public static final String CONF_ROCKETMQ_SERV                 =   "%ROCKETMQ_SERV%";
    public static final String CONF_PROCESSOR                     =   "%PROCESSOR%";
    public static final String CONF_JVM_OPS                       =   "%JVM_OPS%";
    public static final String CONF_SERV_INST_ID                  =   "%SERV_INST_ID%";
    public static final String CONF_COLLECTD_PORT                 =   "%COLLECTD_PORT%";
    public static final String CONF_CONSOLE_PORT                  =   "%CONSOLE_PORT%";
    public static final String CONF_ES_SERVER                     =   "%ES_SERVER%";
    public static final String CONF_ES_MT_SERVER                  =   "%ES_MT_SERVER%";

	public static final String CONF_CLUSTER_NODES                 =   "%CLUSTER_NODES%";
	public static final String CONF_CLUSTER_TOKEN                 =   "%CLUSTER_TOKEN%";
	public static final String CONF_CONN_POOL_SIZE                =   "%CONN_POOL_SIZE%";
	public static final String CONF_PASSWORD                      =   "%PASSWORD%";
	public static final String CONF_FIRSTEP                       =   "%FIRSTEP%";
	public static final String CONF_FQDN                          =   "%FQDN%";
	public static final String CONF_PORT                          =   "%PORT%";
	public static final String CONF_ARBITRATOR_ADDR               =   "%ARBITRATOR_ADDR%";
	public static final String CONF_SCRAPE_URI                    =   "%SCRAPE_URI%";
	public static final String CONF_TELEMETRY_ADDRESS             =   "%TELEMETRY_ADDRESS%";
	public static final String CONF_CLICKHOUSE_USER               =   "%CLICKHOUSE_USER%";
	public static final String CONF_CLICKHOUSE_PASSWORD           =   "%CLICKHOUSE_PASSWORD%";

	public static final String START_SHELL                        =   "start.sh";
	public static final String STOP_SHELL                         =   "stop.sh";
	public static final String STOP_NOAUTH_SHELL                  =   "stop_noauth.sh";
	
	public static final String ARBITRATOR_START_SHELL             =   "arbitrator_start.sh";
	public static final String ARBITRATOR_STOP_SHELL              =   "arbitrator_stop.sh";
	public static final String TAOSD_START_SHELL                  =   "taosd_start.sh";
	public static final String TAOSD_STOP_SHELL                   =   "taosd_stop.sh";
	
	public static final String PROMETHEUS_PULSAR_YML              =   "prometheus_pulsar.yml";
	public static final String PROMETHEUS_CLICKHOUSE_YML          =   "prometheus_clickhouse.yml";
	public static final String PROMETHEUS_APISIX_YML              =   "prometheus_apisix.yml";
	public static final String PROMETHEUS_YML                     =   "prometheus.yml";
	public static final String NGX_SMS_QUERY_CONF                 =   "nginx_sms_query.conf";
	public static final String NGX_CONF                           =   "nginx.conf";
	public static final String SHELL_MACRO                        =   "#! /bin/sh";
	public static final String ZZSOFT_REDIS_PASSWD                =   "zzsoft.1234";
	public static final String LINE_SEP                           =   "\n";
	public static final String TAR_GZ_SURFIX                      =   ".tar.gz";
	public static final String TAR_ZXVF                           =   "-zxvf";
	public static final int    CHECK_PORT_RETRY                   =   100;

	public static final String PATH_SPLIT                         =   "/";
	public static final String PATH_COMMA                         =   ",";

	public static final String CACHE_REDIS                        =   "CACHE_REDIS";
	public static final String CACHE_SCYLLA                       =   "CACHE_SCYLLA";
	public static final String DB_TIDB                            =   "DB_TIDB";
	public static final String DB_BAIKAL                          =   "DB_BAIKAL";
	public static final String DB_COCKROACH                       =   "DB_COCKROACH";
	public static final String DB_YUGABYTEDB                      =   "DB_YUGABYTEDB";
	public static final String DB_DORIS                           =   "DB_DORIS";
	public static final String DB_CLICKHOUSE                      =   "DB_CLICKHOUSE";
	public static final String DB_VOLTDB                          =   "DB_VOLTDB";
	public static final String SMS_GW                             =   "SMS_GW";

	public static final String CMPT_COLLECTD                      =   "COLLECTD";
	
	public static final String CMPT_REDIS_NODE                    =   "REDIS_NODE";
	public static final String CMPT_REDIS_PROXY                   =   "REDIS_PROXY";
	public static final String CMPT_MINIO                         =   "MINIO";

    public static final String CACHE_REDIS_CLUSTER                =   "CACHE_REDIS_CLUSTER";
    public static final String CACHE_REDIS_MASTER_SLAVE           =   "CACHE_REDIS_MASTER_SLAVE";
    public static final String CACHE_REDIS_HA_CLUSTER             =   "CACHE_REDIS_HA_CLUSTER";

	public static final int    REDIS_CLUSTER_REPLICAS             =   1;
	public static final int    REDIS_CLUSTER_TTL_SLOT             =   16384;
	public static final int    REDIS_ROLE_MASTER                  =   1;
	public static final int    REDIS_ROLE_SLAVE                   =   0;
	public static final int    REDIS_ROLE_NONE                    =   -1;
	public static final int    CPU_SLICE                          =   50000;
	public static final int    REDIS_CLUSTER_MIN_MASTER_NODES     =   3;

	public static final String STR_NULL                           =   "";
	public static final String TYPE_REDIS_MASTER_NODE             =   "1";
	public static final String TYPE_REDIS_SLAVE_NODE              =   "0";
	public static final String ATTR_NODE_TYPE                     =   "NODE_TYPE";

	public static final String SERVERLESS_APISIX                  =   "SERVERLESS_APISIX";
	public static final String STORE_MINIO                        =   "STORE_MINIO";
	public static final String MQ_ROCKETMQ                  	  =   "MQ_ROCKETMQ";
	public static final String MQ_PULSAR                          =   "MQ_PULSAR";
    public static final String DB_TDENGINE                        =   "DB_TDENGINE";
    public static final String DB_ORACLE_DG                       =   "DB_ORACLE_DG";
	public static final String SMS_GATEWAY                        =   "SMS_GATEWAY";
	public static final String SMS_QUERY_SERVICE                  =   "SMS_QUERY_SERVICE";
	public static final String COMMON_COLLECTD                    =   "COMMON_COLLECTD";
	public static final String CONF_INST_ID                       =   "%INST_ID%";
	public static final String CONF_CLIENT_URLS                   =   "%CLIENT_URLS%";
	public static final String CONF_PEER_URLS                     =   "%PEER_URLS%";
	public static final String CONF_ADVERTISE_PEER_URLS           =   "%ADVERTISE_PEER_URLS%";
	public static final String CONF_PD_LIST                       =   "%PD_LIST%";
	public static final String CONF_STAT_ADDR                     =   "%STAT_ADDR%";
	public static final String CONF_TIKV_ADDR                     =   "%TIKV_ADDR%";
	public static final String CONF_HOST                          =   "%HOST%";
	public static final String CONF_STAT_HOST                     =   "%STAT_HOST%";
	public static final String CONF_STAT_PORT                     =   "%STAT_PORT%";
	public static final String CONF_DASHBOARD_ADDR                =   "%DASHBOARD_ADDR%";
	public static final String CONF_PD_ADDRESS                    =   "%PD_ADDRESS%";
	public static final String CONF_DATA_DIR                      =   "%DATA_DIR%";
	public static final String CONF_LOG_DIR                       =   "%LOG_DIR%";
	public static final String CONF_CLIENT_PORT                   =   "%CLIENT_PORT%";
	public static final String CONF_CLIENT_ADDRESS                =   "%CLIENT_ADDRESS%";
	public static final String CONF_ADMIN_PORT                    =   "%ADMIN_PORT%";
	public static final String CONF_ZK_SERVER_LIST                =   "%ZK_SERVER_LIST%";
	public static final String CONF_BOOKIE_ID                     =   "%BOOKIE_ID%";
	public static final String CONF_BOOKIE_PORT                   =   "%BOOKIE_PORT%";
	public static final String CONF_ADVERTISED_ADDRESS            =   "%ADVERTISED_ADDRESS%";
	public static final String CONF_HTTP_SERVER_PORT              =   "%HTTP_SERVER_PORT%";
	public static final String CONF_JOURNAL_DIRS                  =   "%JOURNAL_DIRS%";
	public static final String CONF_LEDGER_DIRS                   =   "%LEDGER_DIRS%";
	public static final String CONF_META_DATA_SERVICE_URI         =   "%META_DATA_SERVICE_URI%";
	public static final String CONF_ZK_SERVERS                    =   "%ZK_SERVERS%";
	public static final String CONF_PULSAR_MGR_PORT               =   "%PULSAR_MGR_PORT%";
	public static final String CONF_HERDDB_PORT                   =   "%HERDDB_PORT%";
	public static final String CONF_BOOKIE_LIST                   =   "%BOOKIE_LIST%";
	public static final String CONF_STORE_SERVERS                 =   "%CONF_STORE_SERVERS%";
	public static final String CONF_GRPC_PORT                     =   "%GRPC_PORT%";
	public static final String CONF_BROKER_PORT                   =   "%BROKER_PORT%";
	public static final String CONF_WEB_PORT                      =   "%WEB_PORT%";
	public static final String CONF_BROKER_ADDRESS                =   "%BROKER_ADDRESS%";
	public static final String CONF_CLUSTER_NAME                  =   "%CLUSTER_NAME%";
	public static final String CONF_LISTEN_ADDRESS                =   "%LISTEN_ADDRESS%";
	public static final String CONF_PULSAR_BROKERS                =   "%PULSAR_BROKERS%";
	public static final String CONF_PULSAR_BOOKIES                =   "%PULSAR_BOOKIES%";
	public static final String CONF_PULSAR_ZOOKEEPERS             =   "%PULSAR_ZOOKEEPERS%";
	public static final String CONF_HTTP_ADDR                     =   "%HTTP_ADDR%";
	public static final String CONF_DOMAIN                        =   "%DOMAIN%";
	public static final String CONF_GRAFANA_DIR                   =   "%GRAFANA_DIR%";
	public static final String CONF_PROMETHEUS_YML_FILE           =   "%PROMETHEUS_YML_FILE%";
	public static final String CONF_CLICKHOUSE_EXPORTER_LIST      =   "%CLICKHOUSE_EXPORTER_LIST%";
	public static final String CONF_APISIX_LIST                   =   "%APISIX_LIST%";
	public static final String CONF_SITES_PER_HOST                =   "%SITES_PER_HOST%";
	public static final String CONF_KFACTOR                       =   "%KFACTOR%";
	public static final String CONF_HEARTBEAT_TIMEOUT             =   "%HEARTBEAT_TIMEOUT%";
	public static final String CONF_ADMIN_NAME                    =   "%ADMIN_NAME%";
	public static final String CONF_ADMIN_PWD                     =   "%ADMIN_PWD%";
	public static final String CONF_USER_NAME                     =   "%USER_NAME%";
	public static final String CONF_USER_PASSWORD                 =   "%USER_PASSWORD%";
	public static final String CONF_TEMPTABLES_MAXSIZE            =   "%TEMPTABLES_MAXSIZE%";
	public static final String CONF_ELASTIC_DURATION              =   "%ELASTIC_DURATION%";
	public static final String CONF_ELASTIC_THROUGHPUT            =   "%ELASTIC_THROUGHPUT%";
	public static final String CONF_QUERY_TIMEOUT                 =   "%QUERY_TIMEOUT%";
	public static final String CONF_PROCEDURE_LOGINFO             =   "%PROCEDURE_LOGINFO%";
	public static final String CONF_MEMORYLIMIT_SIZE              =   "%MEMORYLIMIT_SIZE%";
	public static final String CONF_MEMORYLIMIT_ALERT             =   "%MEMORYLIMIT_ALERT%";
	public static final String CONF_HOSTS                         =   "%HOSTS%";
	public static final String CONF_VOLT_CLIENT_PORT              =   "%VOLT_CLIENT_PORT%";
	public static final String CONF_VOLT_ADMIN_PORT               =   "%VOLT_ADMIN_PORT%";
	public static final String CONF_VOLT_WEB_PORT                 =   "%VOLT_WEB_PORT%";
	public static final String CONF_VOLT_INTERNAL_PORT            =   "%VOLT_INTERNAL_PORT%";
	public static final String CONF_VOLT_REPLI_PORT               =   "%VOLT_REPLI_PORT%";
	public static final String CONF_VOLT_ZK_PORT                  =   "%VOLT_ZK_PORT%";
	public static final String CONF_YB_MASTER_ADDR                =   "%YB_MASTER_ADDR%";
	public static final String CONF_FS_DATA_DIRS                  =   "%FS_DATA_DIRS%";
	public static final String CONF_RPC_BIND_ADDR                 =   "%RPC_BIND_ADDR%";
	public static final String CONF_SERV_BROADCAST_ADDR           =   "%SERV_BROADCAST_ADDR%";
	public static final String CONF_WEBSERV_INTERFACE             =   "%WEBSERV_INTERFACE%";
	public static final String CONF_WEBSERVER_PORT                =   "%WEBSERVER_PORT%";
	public static final String CONF_DURABLE_WAL_WRITE             =   "%DURABLE_WAL_WRITE%";
	public static final String CONF_ENABLE_LOAD_BALANCING         =   "%ENABLE_LOAD_BALANCING%";
	public static final String CONF_MAX_CLOCK_SKEW_USEC           =   "%MAX_CLOCK_SKEW_USEC%";
	public static final String CONF_REPLICATION_FACTOR            =   "%REPLICATION_FACTOR%";
	public static final String CONF_YB_NUM_SHARDS_PER_TSERVER     =   "%YB_NUM_SHARDS_PER_TSERVER%";
	public static final String CONF_YSQL_NUM_SHARDS_PER_TSERVER   =   "%YSQL_NUM_SHARDS_PER_TSERVER%";
	public static final String CONF_PLACEMENT_CLOUD               =   "%PLACEMENT_CLOUD%";
	public static final String CONF_PLACEMENT_ZONE                =   "%PLACEMENT_ZONE%";
	public static final String CONF_PLACEMENT_REGION              =   "%PLACEMENT_REGION%";
	public static final String CONF_CDC_WAL_RETENTION_TIME_SECS   =   "%CDC_WAL_RETENTION_TIME_SECS%";
	public static final String CONF_MASTER_ADDRS                  =   "%MASTER_ADDRS%";
	public static final String CONF_PGSQL_PROXY_BIND_ADDR         =   "%PGSQL_PROXY_BIND_ADDR%";
	public static final String CONF_PGSQL_PROXY_WEBSERVER_PORT    =   "%PGSQL_PROXY_WEBSERVER_PORT%";
	public static final String CONF_YSQL_MAX_CONNECTIONS          =   "%YSQL_MAX_CONNECTIONS%";
	public static final String CONF_CQL_PROXY_BIND_ADDR           =   "%CQL_PROXY_BIND_ADDR%";
	public static final String CONF_CQL_PROXY_WEBSERVER_PORT      =   "%CQL_PROXY_WEBSERVER_PORT%";
	public static final String CONF_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC = "%ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC%";
	public static final String CONF_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH = "%ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH%";
	public static final String CONF_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO = "%ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO%";
	public static final String CONF_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC = "%TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC%";
	public static final String CONF_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC = "%REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC%";
	public static final String CONF_WORKER_PROCESSES              =   "%WORKER_PROCESSES%";
	public static final String CONF_SERVER_LIST                   =   "%SERVER_LIST%";

    public static final String CLICKHOUSE_INTERNAL_REPLICATION_TRUE  = "1";
    public static final String CLICKHOUSE_INTERNAL_REPLICATION_FALSE = "0";
    public static final int    CLICKHOUSE_DEFAULT_REPLICA_WEIGHT  =   100;
    public static final String CLICKHOUSE_DEFAULT_USER            =   "default";
    public static final String CLICKHOUSE_DEFAULT_PASSWD          =   "abcd.1234";
    
    public static final String RAFT_DATA_DIR                      =   "./data";
    
    public static final String PAAS_TENANT                        =   "paas-tenant";
    public static final String PAAS_NAMESPACE                     =   "paas-namespace";
    public static final String SYS_EVENT_TOPIC                    =   "sys-event";
    public static final String SYS_CHECK_TASK                     =   "sys-check-task";
    
    public static final String SMS_GATEWAY_CMD_PING               =   "PING";
    public static final String SMS_GATEWAY_CMD_PING_RESP          =   "PINGRESP";
    
    public static final String SEQ_ALARM                          =   "SEQ_ALARM";
    public static final String SYS_USER                           =   "sys";
    
    public static final String ALARM_UNDEALED                     =   "0";
    public static final String ALARM_DEALED                       =   "1";
    public static final String ALARM_ALL                          =   "-1";
    
    public static final String ADD_ALARM_EVENT_URI                =   "receiver/addAlarmEvent";
    public static final String HEALTH_CHECK_URI                   =   "healthCheck/test";

}
