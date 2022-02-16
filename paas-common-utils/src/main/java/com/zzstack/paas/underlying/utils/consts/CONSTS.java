package com.zzstack.paas.underlying.utils.consts;

public class CONSTS {

    public static final String HTTP_DEFAULT_BIND_IP = "0.0.0.0";
    public static final int HTTP_DEFAULT_PORT = 9090;
    public static final int HTTP_EVENT_LOOP_POOL_SIZE = 4;
    public static final int HTTP_WORKER_POOL_SIZE = 16;
    public static final long HTTP_EVLOOP_TIMEOUT = 3000;
    public static final long HTTP_TASK_TIMEOUE = 10000; // 单个HTTP请求处理超时时间

    public static final String MAGIC_KEY = "MAGIC_KEY";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APP_JSON = "application/json";

    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_POST = "POST";

    public static final int REVOKE_OK = 0;
    public static final int REVOKE_NOK = -1;
    public static final int REVOKE_NOK_QUEUE_EXIST = -2;
    public static final int REVOKE_AUTH_FAIL = -3;
    public static final int REVOKE_AUTH_IP_LIMIT = -4;
    public static final int SERVICE_NOT_INIT = -5;

    public static final String CONF_PATH = "conf";
    public static final String INIT_PROP_FILE = "conf/init";
    public static final String LOG4J_CONF = "conf/log4j";
    public static final String C3P0_PROP_FILE = "conf/c3p0";
    public static final String HAZELCAST_CONF_FILE = "conf/hazelcast.xml";

    public static final String SYS_EVENT_QUEUE = "sys.event";

    public static final long SSH_CMD_TIMEOUT = 60000;
    public static final long SSH_MAKE_TIMEOUT = 100000;
    public static final long READ_BUF_TIMEOUT = 1000;
    public static final String ERR_SSH_TIMEOUT = "exec remote ssh cmd timeout!";

    public static final String COLLECT_DATA_API = "getCollectData";

    public static final String LINE_SEP = System.lineSeparator();
    public static final String LINUX_SHELL_SEP = "\r\n";
    public static final String HOSTS_FILE = "/etc/hosts";
    public static final String BASH_PROFILE = ".bash_profile";
    public static final String NO_SUCH_FILE = "No such file or directory";
    public static final String COMMAND_NOT_FOUND = "command not found";
    public static final String ERR_COMMAND_NOT_FOUND = "command '%s' not found";
    public static final String FILE_DIR_NOT_EXISTS = "No such file or directory";
    public static final String NO_MAPPING_IN_HOSTS = "gethostbyname error!";

    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_NUMBER = "pageNumber";

    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_PAGE_NUMBER = "1";

    public static final String SHELL_MACRO = "#!/bin/sh";
    public static final String SQUARE_BRACKET_LEFT = "[";
    public static final String SQUARE_BRACKET_RIGHT = "]";

    public static final String DEPLOY_SINGLE_SUCCESS_BEGIN_STYLE = "<span style='color:blue;'>";
    public static final String DEPLOY_SINGLE_FAIL_BEGIN_STYLE = "<span style='color:yellow;'>";
    public static final String END_STYLE = "</span>";

    public static final String MQ_DEPLOY_ROOT_PATH = "mq_deploy";
    public static final String MQ_DEPLOY_PATH = "rabbitmq_server-3.7.4";
    public static final long DISK_FREE_LIMIT = 6000000000L;
    public static final float VM_MEMORY_HIGH_WATERMARK = 0.2f;

    public static final String JDK_DEPLOY_PATH = "jdk1.8.0_72";
    public static final String JDK_ROOT_PATH = "%JDK_ROOT_PATH%";
    public static final String PROCESS_FLAG = "%PROCESS_FLAG%";
    public static final String PROXY_ID = "%PROXY_ID%";
    public static final String METASVR_ROOTURL = "%METASVR_ROOTURL%";
    public static final String REDIS_PID_FILE = "%REDIS_PID_FILE%";
    public static final String REDIS_DIR = "%REDIS_DIR%";
    public static final String REDIS_PORT = "%REDIS_PORT%";
    public static final String REDIS_LOG_FILE = "%REDIS_LOG_FILE%";
    public static final String REDIS_MAX_MEM = "%REDIS_MAX_MEM%";
    public static final String REDIS_SLAVEOF = "slaveof";
    public static final String REDIS_SHELL = "redis.sh";

    public static final int POS_DEFAULT_VALUE = -1;

    public static final long STAT_COMPTE_INTERVAL = 1000L;
    public static final long DBPOOL_CHECK_INTERVAL = 6000L;
    public static final long DEPLOY_CHECK_INTERVAL = 500L;
    public static final long TIKV_STATE_CHECK_INTERVAL = 500L;

    public static final String LINE_END = "\n";
    public static final String LINE_BLACK_SLASH = "\\\\";
    public static final String HTML_LINE_END = "<br/>";

    public static final String PATH_SPLIT = "/";
    public static final String METASVR_ADDR_SPLIT = ",";
    public static final String PATH_COMMA = ",";

    public static final String AUTO_GEN_Y = "1";
    public static final String AUTO_GEN_N = "0";

    public static final int SSH_PORT_DEFAULT = 22;
    public static final int FIX_HEAD_LEN = 10;
    public static final int FIX_PREHEAD_LEN = 6;
    public static final byte[] PRE_HEAD = { '$', 'H', 'E', 'A', 'D', ':' };

    public static final int OP_TYPE_ADD = 1;
    public static final int OP_TYPE_MOD = 2;
    public static final int OP_TYPE_DEL = 3;

    public static final String NOT_DEPLOYED = "0";
    public static final String DEPLOYED = "1";

    public static final String NOT_NEED_DEPLOY = "0";
    public static final String NEED_DEPLOY = "1";

    public static final int TOPO_TYPE_LINK = 1;
    public static final int TOPO_TYPE_CONTAIN = 2;

    public static final boolean IS_PWD_EXPIRE = false;

    public static final String MQ_DEFAULT_USER = "mq";
    public static final String MQ_DEFAULT_PWD = "ibsp_mq@123321";
    public static final String MQ_DEFAULT_VHOST = "/";
    public static final long MQ_DEPLOY_MAXTIME = 60000l;
    public static final int MQ_MAX_QUEUE_PRIORITY = 10;
    public static final int MQ_DEFAULT_QUEUE_PRIORITY = 0;
    public static final int MQ_HA_SYNC_BATCH_SIZE = 500;
    public static final int MQ_VHOST_MAX_CONNS = 4096;
    public static final int MQ_VHOST_MAX_QUEUES = 2048;
    public static final int STUCK_NO_OPER_MESSAGE = 1000000; // 超过100W不进行拉起broker操作
    public static final int CMD_CHANNEL_ID = 1;
    public static final int SEND_CHANNEL_ID = 2;
    public static final int REV_CHANNEL_START = 3;

    public static final long ASYNC_CALL_TIMEOUT = 2000L;

    public static final long SESSION_TTL = 3600000;

    public static final String CACHE_REDIS_CLUSTER_TEMP_FILE = "CacheRedisClusterTemplate.yaml";
    public static final String CACHE_REDIS_MASTER_SLAVE_TEMP_FILE = "CacheRedisMSTemplate.yaml";
    public static final String CACHE_REDIS_HA_CLUSTER_TEMP_FILE = "CacheRedisHaTemplate.yaml";

    public static final String DB_TIDB_TEMP_FILE = "DBTemplate.yaml";
    public static final String DB_TDENGINE_TEMP_FILE = "DBTemplate.yaml";
    public static final String DB_VOLTDB_TEMP_FILE = "DBTemplate.yaml";
    public static final String DB_CLICKHOUSE_TEMP_FILE = "DBTemplate.yaml";
    public static final String DB_ORACLE_DG_TEMP_FILE = "DBTemplate.yaml";
    public static final String MQ_ROCKETMQ_TEMP_FILE = "RocketMqTemplate.yaml";
    public static final String MQ_PULSAR_TEMP_FILE = "PulsarTemplate.yaml";

    public static final String SERV_CLAZZ_CACHE = "CACHE";
    public static final String SERV_CLAZZ_MQ = "MQ";
    public static final String SERV_CLAZZ_DB = "DB";
    public static final String SERV_CLAZZ_SERVERLESS = "SERVERLESS";
    public static final String SERV_CLAZZ_SMS = "SMS";

    public static final String SERV_TYPE_CACHE_REDIS_CLUSTER = "CACHE_REDIS_CLUSTER";
    public static final String SERV_TYPE_CACHE_REDIS_MASTER_SLAVE = "CACHE_REDIS_MASTER_SLAVE";
    public static final String SERV_TYPE_CACHE_REDIS_HA_CLUSTER = "CACHE_REDIS_HA_CLUSTER";

    public static final String SERV_TYPE_SERVERLESS_APISIX = "SERVERLESS_APISIX";

    public static final String SERV_TYPE_MQ_ROCKETMQ = "MQ_ROCKETMQ";
    public static final String SERV_TYPE_MQ_PULSAR = "MQ_PULSAR";

    public static final String SERV_TYPE_DB_TIDB = "DB_TIDB";
    public static final String SERV_TYPE_DB_TDENGINE = "DB_TDENGINE";
    public static final String SERV_TYPE_DB_VOLTDB = "DB_VOLTDB";
    public static final String SERV_TYPE_DB_ORACLE_DG = "DB_ORACLE_DG";
    public static final String SERV_TYPE_DB_CLICKHOUSE = "DB_CLICKHOUSE";
    public static final String SERV_TYPE_DB_YUGABYTEDB = "DB_YUGABYTEDB";

    public static final String SERV_TYPE_SMS_GATEWAY = "SMS_GATEWAY";
    public static final String SERV_TYPE_SMS_QUERY = "SMS_QUERY_SERVICE";

    public static final String SERV_DB_PD = "DB_PD";
    public static final String SERV_DB_TIDB = "DB_TIDB";
    public static final String SERV_DB_TIKV = "DB_TIKV";
    public static final String SERV_COLLECTD = "COLLECTD";

    public static final String SERV_MQ_RABBIT = "MQ_RABBIT";
    public static final String SERV_MQ_ERLANG = "MQ_ERLANG";

    public static final String SERV_CACHE_PROXY = "CACHE_PROXY";
    public static final String SERV_CACHE_NODE = "CACHE_NODE";

    public static final String CLIENT_TYPE_CACHE = "CACHE_CLIENT";
    public static final String CLIENT_TYPE_DB = "DB_CLIENT";
    public static final String CLIENT_TYPE_MQ = "MQ_CLIENT";
    
    public static final String APP_SMS_SERVER = "SMS_SERVER";
    public static final String APP_SMS_PROCESS = "SMS_PROCESS";
    public static final String APP_SMS_CLIENT = "SMS_CLIENT";
    public static final String APP_SMS_BATSAVE = "SMS_BATSAVE";
    public static final String APP_SMS_STATS = "SMS_STATS";

    public static final String FILE_TYPE_JDK = "JDK";

    public static final String SCHEMA_OBJECT = "object";
    public static final String SCHEMA_ARRAY = "array";

    public static final String ERR_PARAM_INCOMPLETE = "parameter incomplete ......";
    public static final String ERR_TIDB_CONTAINER_META = "tidb container component meta error ......";
    public static final String ERR_JSON_SCHEME_VALI_ERR = "json schema validation fail ......";
    public static final String ERR_SCHEMA_FILE_NOT_EXIST = "schema file not exist ......";
    public static final String ERR_METADATA_NOT_FOUND = "meta data not found ......";
    public static final String ERR_JSONNODE_NOT_COMPLETE = "json node not complete ......";
    public static final String ERR_SERV_TYPE_NOT_FOUND = "service type not found ......";
    public static final String ERR_HOSTINFO_NOT_COMPLETE = "host info not complete ......";
    public static final String ERR_DEPLOY_CONF_MISS = "deploy file config missing ......";
    public static final String ERR_SSH_CONNECT_FAIL = "ssh connect fail ......";
    public static final String ERR_EXEC_SHELL_FAIL = "exec shell fail ......";
    public static final String ERR_DEPLOY_ERL_FAIL = "deploy erlang fail ......";
    public static final String ERR_SET_HOSTS_FAIL = "set hosts fail ......";
    public static final String ERR_MQ_PORT_ISUSED = "MQ port is used ......";
    public static final String ERR_MQ_NO_BROKER_FUND = "no broker found ......";
    public static final String ERR_DEPLOY_MQ_FAIL = "deploy mq fail ......";
    public static final String ERR_UNDEPLOY_MQ_FAIL = "undeploy mq fail ......";
    public static final String ERR_SET_HOST = "set hosts fail ......";
    public static final String ERR_SET_ERLCOOKIE = "set erlang cookie fail ......";
    public static final String ERR_RABBITMQ_PORT_USED = "rabbitmq port is used ......";
    public static final String ERR_RABBITMQ_MGR_PORT_USED = "rabbitmq manage port is used ......";
    public static final String ERR_PORT_USED_IN_DB = "port is used in db ......";
    public static final String ERR_PWD_INCORRECT = "login passwd incorrect ......";
    public static final String ERR_PWD_EXPIRE = "login passwd expired ......";
    public static final String ERR_PUT_SESSION = "put session fail ......";
    public static final String ERR_ACCOUNT_NOT_EXISTS = "account not exists ......";
    public static final String ERR_PAAS_SERVICE_INIT_FAIL = "paas service init fail ......";

    // TIDB consts
    public static final String ERR_FIND_TIDB_SERVER_ERROR = "no available tidb server for serv_id ";
    public static final String ERR_FIND_PD_SERVER_ERROR = "no available pd server for serv_id ";
    public static final String ERR_CONNECT_TIDB_SERVER_ERROR = "connect to tidb server failed ......";
    public static final String ERR_NO_PD_TO_JOIN = "no available pd server to join ......";

    public static final String PD_API_STORES = "/pd/api/v1/stores";

    public static final String PD_DELETE_MEMBER_SUCC = "Success!";
    public static final String PD_DELETE_STORE_SUCC = "Success!";
    public static final String TIKV_OFFLINE_STATUS = "Offline";
    public static final String TIKV_TOMBSTONE_STATUS = "Tombstone";
    public static final String TIKV_METRIC_ADDRESS = "%TIKV_METRIC_ADDRESS%";
    public static final String TIKV_METRIC_JOB = "%TIKV_METRIC_JOB%";
    public static final String TIKV_TOML = "tikv.toml";

    public static final int MIN_PD_NUMBER = 3;
    public static final int MIN_TIKV_NUMBER = 3;
    public static final int MIN_TIDB_NUMBER = 2;

    public static final int MAX_CACHE_SLOT = 16383;

    // rabbit
    public static final String NOT_CLUSTER = "0";
    public static final String CLUSTER = "1";
    public static final String IS_CLUSTER = "1";
    public static final String NOT_DURABLE = "0";
    public static final String DURABLE = "1";
    public static final String TYPE_QUEUE = "1";
    public static final String TYPE_TOPIC = "2";
    public static final String NOT_WRITABLE = "0";
    public static final String WRITABLE = "1";
    public static final String NOT_VALABLE = "-1";
    public static final String NOT_RUNNING = "0";
    public static final String RUNNING = "1";
    public static final String BIND_TYPE_PERM = "1";
    public static final String BIND_TYPE_WILD = "2";
    public static final String NOT_GLOBAL_ORDERED = "0";
    public static final String GLOBAL_ORDERED = "1";
    public static final String NOT_PRIORITY = "0";
    public static final String PRIORITY = "1";

    public static final String HTTP_STR = "http://";
    public static final String COLON = ":";
    public static final String NAME_SPLIT = "@";
    public static final String RABBIT_MRG_API = "api";

    public static final String ERR_CREATE_QUEUE_ON_DB = "create queue/topic error on db .....";
    public static final String ERR_QUEUE_EXISTS = "queue/topic allready exists .....";
    public static final String ERR_QUEUE_TYPE_ERROR = "queue/topic type is [1,2]";
    public static final String ERR_DURABLE_TYPE_ERROR = "durable type is [0,1]";
    public static final String ERR_ORDERED_TYPE_ERROR = "ordered type is [0,1]";
    public static final String ERR_PRIORITY_TYPE_ERROR = "priority type is [0,1]";
    public static final String ERR_QUEUE_ALLREADY_DEPLOYED = "queue/topic allready deployed ......";
    public static final String ERR_QUEUE_NOT_EXISTS = "queue/topic not exists .....";
    public static final String ERR_TOPIC_BINDED = "topic has been binded, please unbind first ......";
    public static final String ERR_DECLARE_ON_BROKER = "declare queue/topic error on broker ......";
    public static final String ERR_DELETE_FROM_RABBIT = "delete error on rabbit ......";
    public static final String ERR_DELETE_FROM_DB = "delete error on database ......";
    public static final String ERR_FETCH_SEQ_SQL_NOT_MATCH = "fetch sequence sql not match ......";
    public static final String ERR_SEQ_STEP_ILLEGAL = "sequence step illegal, must >= 1 ......";
    public static final String ERR_SEQ_NOT_EXISTS = "sequence not exists ......";

    public static final int UNIT_G = 1024 * 1024 * 1024;
    public static final int UNIT_M = 1024 * 1024;
    public static final int UNIT_K = 1024;

    public static final String SESSION_PREFIX = "session::";
    public static final String LOGIN_TIME = "login_time";
    public static final String EXPIRE_TIME = "expire_time";

    public static final String ACC_NAME = "acc_name";

    public static final int LOGIN_TTL = 3600;
    public static final int EVENT_EXPIRE_TTL = 10000;

    public static final int STR_NOT_FOUND = -1;

    public static final String INFO_OK = "OK";
    public static final String STR_TRUE = "1";
    public static final String STR_FALSE = "0";
    
    public static final String STR_ALARM = "4";
    public static final String STR_ERROR = "3";
    public static final String STR_WARN = "2";
    public static final String STR_DEPLOYED = "1";
    public static final String STR_SAVED = "0";

    public static final String LOG_KEY = "LOG_KEY";
    public static final String PASSWD_MIX = "^^||";
    public static final String COUNT = "COUNT";
    public static final String CONTAINER_INDEX = "_CONTAINER";

    public static final String ACC_CREATE_SUCC = "账户创建成功";
    public static final String ACC_PASSWD_NOT_ALLOW_EMPTY = "密码不能为空";
    public static final String ACC_NAME_NOT_ALLOW_EMPTY = "用户名不能为空";
    public static final String ACC_PHONE_NOT_ALLOW_EMPTY = "手机号不能为空";
    public static final String ACC_MAIL_NOT_ALLOW_EMPTY = "邮箱不能为空";
    public static final String ACC_MAIL_REGX_NOT_MATCH = "邮箱正则校验不符合规则";
    public static final String ACC_PHONE_REGX_NOT_MATCH = "手机号码正则校验不符合规则";
    public static final String ACC_NAME_EXISTS = "用户名已存在";
    public static final String ACC_PHONE_EXISTS = "手机号码已存在";
    public static final String ACC_MAIL_EXISTS = "邮箱已存在";
    public static final String ACC_LOGIN_SUCC = "账户密码认证通过";
    public static final String ACC_LOGIN_FAIL = "账户密码认证未通过";
    public static final String ACC_CHECK_SUCC = "账户信息校验通过";
    public static final String ACC_LOCAL_SESSION_EXPIRED = "本地登录状态已过期";
    public static final String ACC_MAGIC_KEY_NOT_EQUAL = "token校验失败,请重新登录";
    public static final String ACC_MAGIC_KEY_EXPIRED = "token已过期，请重新登录";

    public static final String ERR_DB = "db error";
    public static final String ERR_NETWORK = "io network error";
    public static final String ERR_JSONNODE_ILLEGAL = "json node illegal";
    public static final String ERR_OP_TYPE = "OP_TYPE error";
    public static final String ERR_SERVICE_NOT_FOUND = "service not found ......";
    public static final String ERR_SERVICE_PROBER_INIT_ERROR = "service prober init error ......";
    public static final String ERR_SERVICE_DEPLOYER_INIT_ERROR = "service deployer init error ......";
    public static final String ERR_SERVICE_ALLREADY_DEPLOYED = "service allready deployed ......";
    public static final String ERR_SERVICE_NOT_DEPLOYED = "service not deployed ......";
    public static final String ERR_INSTANCE_ALLREADY_DEPLOYED = "instance allready deployed ......";
    public static final String ERR_INSTANCE_NOT_DEPLOYED = "instance not deployed ......";
    public static final String ERR_INTERNAL = "internal error ......";
    public static final String ERR_INSTANCE_NOT_FOUND = "instance not found ......";
    public static final String ERR_SMS_INSTANCE_NOT_FOUND = "sms service instance not found ......";
    public static final String ERR_DB_INSTANCE_NOT_FOUND = "db service instance not found ......";
    public static final String ERR_CMPT_NOT_FOUND = "component not found ......";
    public static final String ERR_NO_DEL_DEPLOYED_SERV = "cannot delete deployed service ......";
    public static final String ERR_SERVER_NOT_NULL = "server contains sub ssh, can not delete ......";
    public static final String ERR_SSH_IS_USING = "ssh resource is using, can not delete ......";
    public static final String ERR_SSH_LOGIN_FAIL = "ssh login fail ......";
    public static final String ERR_SSH_EXEC_TIMEOUT = "ssh2 exec timeout ......";
    public static final String ERR_CONFIG_TEMPLATE_INIT_ERROR = "paas config template init error ......";
    public static final String ERR_REDIS_QUEUE_NOT_INITIALIZED = "redis A/B cluster queue service not initialized ......";
    public static final String ERR_SMS_SERV_NOT_INITIALIZED = "sms service not initialized ......";
    public static final String ERR_DB_SERV_NOT_INITIALIZED = "db service not initialized ......";
    public static final String ERR_SPECIFIED_DG_NOT_FOUND = "specified dg not found ......";
    public static final String ERR_REDIS_A_OR_B_MISSING = "redis A/B cluster queue A or B missing ......";
    public static final String ERR_AJUST_REDIS_WEIGHT_PARTAIL_OK = "adjust redis weight partial ok ......";
    public static final String ERR_LOAD_SERV_TOPO_FAIL = "load service topo fail ......";
    public static final String ERR_CANNOT_DEL_SERVICE_VERSION = "you cannot delete the remaining 1 version ......";
    public static final String ERR_DUMPLICATE_KEY_OR_DB_EXCEPTION = "dumplicate key or db exception ......";
    public static final String ERR_NOT_TWO_NUM_CLUSTER_ID = "not two num cluster id......";
    public static final String ERR_TOPIC_NAME = "topic name is empty......";
    public static final String ERR_SERVICE_NAME_EXISTS = "service name exists ......";
    public static final String ERR_SERVER_IP_EXISTS = "server ip exists ......";

    public static final String ERR_MISSING_UUID = "缺少UUID环境变量";
    public static final String ERR_MISSING_SERV_INST_ID = "缺少SERV_INST_ID环境变量";
    public static final String ERR_MISSING_META_SVR_URL = "缺少META_SVR_URL环境变量";
    public static final String ERR_MISSING_META_SVR_USR = "缺少META_SVR_USR环境变量";
    public static final String ERR_MISSING_META_SVR_PASSWD = "缺少META_SVR_PASSWD环境变量";
    public static final String ERR_MISSING_COLLECTD_PORT = "缺少COLLECTD_PORT环境变量";
    public static final String ERR_LOAD_PAAS_INSTANCE = "load paas instance meta fail";

    public static final String SMS_ROOT = "sms_operator/";
    public static final int CACHE_REDIS_SERVER_FILE_ID = 1;
    public static final int CACHE_REDIS_PROXY_FILE_ID = 2;
    public static final int DB_TIDB_PD_FILE_ID = 3;
    public static final int DB_TIDB_TIKV_FILE_ID = 4;
    public static final int DB_TIDB_TIDB_FILE_ID = 5;

    public static final String CACHE_REDIS_ROOT = "cache_redis/";
    public static final String DB_TIDB_ROOT = "db_tidb/";

    public static final String HTML_LIEN_END = "<br/>";

    public static final String REDIS_CONF = "redis.conf";
    public static final String PROXY_CONF = "proxy.conf";
    public static final String CONF_SERV_IP = "%SERV_IP%";
    public static final String CONF_SERV_PORT = "%SERV_PORT%";
    public static final String CONF_PID_FILE = "%PID_FILE%";
    public static final String CONF_LOG_FILE = "%LOG_FILE%";
    public static final String CONF_MAX_CONN = "%MAX_CONN%";
    public static final String CONF_MAX_MEMORY = "%MAX_MEMORY%";
    public static final String CONF_APPENDONLY_FILENAME = "%APPENDONLY_FILENAME%";
    public static final String REDIS_CLUSTER_CONF_FILENAME = "%REDIS_CLUSTER_CONF_FILENAME%";

    public static final String CONF_CLUSTER_NODES = "%CLUSTER_NODES%";
    public static final String CONF_CONN_POOL_SIZE = "%CONN_POOL_SIZE%";
    public static final String CONF_PASSWORD = "%PASSWORD%";

    public static final String STOP_NOAUTH_SHELL = "stop_noauth.sh";
    public static final String ZZSOFT_REDIS_PASSWD = "zzsoft.1234";
    public static final String TAR_GZ_SURFIX = ".tar.gz";
    public static final String ZIP_SURFIX = ".zip";
    public static final int CHECK_PORT_RETRY = 30;

    public static final String CACHE_REDIS = "CACHE_REDIS";
    public static final String CACHE_SCYLLA = "CACHE_SCYLLA";
    public static final String DB_TIDB = "DB_TIDB";
    public static final String DB_BAIKAL = "DB_BAIKAL";
    public static final String DB_COCKROACH = "DB_COCKROACH";
    public static final String DB_YUGABYTE = "DB_YUGABYTE";
    public static final String DB_DORIS = "DB_DORIS";

    public static final String CMPT_REDIS_NODE = "REDIS_NODE";
    public static final String CMPT_REDIS_PROXY = "REDIS_PROXY";

    public static final int REDIS_CLUSTER_REPLICAS = 1;
    public static final int REDIS_CLUSTER_TTL_SLOT = 16384;
    public static final int REDIS_ROLE_MASTER = 1;
    public static final int REDIS_ROLE_SLAVE = 0;
    public static final int REDIS_ROLE_NONE = -1;
    public static final int CPU_SLICE = 50000;

    public static final String DB_TYPE_ORCL = "oracle";
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_TIDB = "mysql";
    public static final String DB_TYPE_PG = "postgresql";
    public static final String DB_TYPE_VOLTDB = "voltdb";
    public static final String DB_TYPE_TDENGINE = "tdengine";
    public static final String DB_TYPE_CLICKHOUSE = "clickhouse";

    public static final String DB_DRIVER_ORCL = "oracle.jdbc.OracleDriver";
    public static final String DB_DRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
    public static final String DB_DRIVER_PG = "org.postgresql.Driver";
    public static final String DB_DRIVER_VOLTDB = "org.voltdb.jdbc.Driver";
    public static final String DB_DRIVER_TDENGINE = "com.taosdata.jdbc.TSDBDriver";
    public static final String DB_DRIVER_CLICKHOUSE = "com.github.housepower.jdbc.ClickHouseDriver";

    public static final String JDBC_URL_FMT_ORCL = "jdbc:oracle:thin:@%s:%s:%s";
    public static final String JDBC_URL_FMT_MYSQL = "jdbc:mysql://%s:%s/%s?useSSL=false";
    public static final String JDBC_URL_FMT_PG = "jdbc:postgresql://%s:%s/%s";
    public static final String JDBC_URL_FMT_VOLTDB = "jdbc:voltdb://%s?autoreconnect=true";
    public static final String JDBC_URL_FMT_TDENGINE = "jdbc:TAOS://%s:%s/%s";
    public static final String JDBC_URL_FMT_CLICKHOUSE = "jdbc:clickhouse://%s:%s/%s";

    public static final String NODE_TYPE_MASTER = "1";
    public static final String NODE_TYPE_SLAVE = "0";

    public static final String DEPLOY_FLAG_PHYSICAL = "1";  // 物理部署
    public static final String DEPLOY_FLAG_PSEUDO = "2";    // 伪部署

    public static final String TD_JNI_LINUX = "libtaos.so";
    public static final String TD_JNI_WINDOWS = "taos.dll";

    public static final String DB_SOURCE_POOL_DRUID = "Druid";
    public static final String DB_SOURCE_POOL_HIKARI = "Hikari";

    public static final String DB_TEST_SQL_ORACLE = "select 1 from dual";
    public static final String DB_TEST_SQL_MYSQL = "select 1 from dual";
    public static final String DB_TEST_SQL_POSTGRESQL = "select 1";
    public static final String DB_TEST_SQL_TDENGINE = "select 1";
    public static final String DB_TEST_SQL_VOLTDB = "select 1 from dual";
    public static final String DB_TEST_SQL_CLICKHOUSE = "SELECT 1";

    public static final String HA_QUEUE_REDIS_CLUSTER_A = "RedisClusterA";
    public static final String HA_QUEUE_REDIS_CLUSTER_B = "RedisClusterB";

    public static final int REDIS_DEFAULT_SLAVE_MIN_IDLE_SIZE = 10;
    public static final int REDIS_DEFAULT_SLAVE_POOL_SIZE = 20;
    public static final int REDIS_DEFAULT_MASTER_MIN_IDLE_SIZE = 10;
    public static final int REDIS_DEFAULT_MASTER_POOL_SIZE = 20;

    // SLAVE - 只在从服务节点里读取。 MASTER - 只在主服务节点里读取。 MASTER_SLAVE
    public static final String READ_MODE_SLAVE = "SLAVE";
    public static final String READ_MODE_MASTER = "MASTER";
    public static final String READ_MODE_MASTER_SLAVE = "MASTER_SLAVE";

    // serverMode: CLUSTER | PROXY
    public static final String REDIS_SERVER_MODE_PROXY = "PROXY";
    public static final String REDIS_SERVER_MODE_CLUSTER = "CLUSTER";
    public static final String REDIS_SERVER_MODE_MS = "MASTER_SLAVE";

    public static final String REDIS_URL_FMT = "redis://%s:%s";
    public static final String REDIS_CLUSTER_A = "RedisClusterA";
    public static final String REDIS_CLUSTER_B = "RedisClusterB";
    public static final String CMD_ADJUST_REDIS_WEIGHT = "AdjustRedisWeight";
    public static final String CMD_SWITCH_DB_TYPE = "SwitchDBType";
    public static final String SMS_CONSOLE_PASSWD = "21232f297a57a5a743894a0e4a801fc3";

    public static final String REG_VERSION = "%VERSION%";

    // 数据库主从当前生效的是哪个: master | backup
    public static final String ACTIVE_DB_TYPE_MASTER = "master";
    public static final String ACTIVE_DB_TYPE_SLAVE = "backup";
    
    public static final long PROBER_COLLECT_INTERVAL = 10;  // 采集探针定时执行间隔(s)
    
    public static final String LOG_TYPE_INFO = "info";
    public static final String LOG_TYPE_ERROR = "error";
    public static final String LOG_TYPE_DEBUG = "debug";
    public static final String LOG_TYPE_STDOUT = "stdout";
    
    public static final String CLICKHOUSE_DEFAULT_USER = "default";
    public static final String CLICKHOUSE_DEFAULT_PWD = "abcd.1234";

}
