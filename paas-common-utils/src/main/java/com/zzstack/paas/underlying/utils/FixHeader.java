package com.zzstack.paas.underlying.utils;

public class FixHeader {

    public static final String HEADER_RET_CODE = "RET_CODE";
    public static final String HEADER_RET_INFO = "RET_INFO";

    public static final String HEADER_LOCAL_PORT = "LOCAL_PORT";
    public static final String HEADER_REMOTE_IP = "REMOTE_IP";
    public static final String HEADER_REMOTE_PORT = "REMOTE_PORT";

    public static final String HEADER_UUID = "UUID";
    public static final String HEADER_ID = "ID";
    public static final String HEADER_NAME = "NAME";
    public static final String HEADER_EVENT_TS = "EVENT_TS";
    public static final String HEADER_MSG_BODY = "MSG_BODY";
    public static final String HEADER_OP_TYPE = "OP_TYPE";
    public static final String HEADER_SESSION_KEY = "SESSION_KEY";
    public static final String HEADER_LOG_KEY = "LOG_KEY";
    public static final String HEADER_SERVICE_ID = "SERVICE_ID";

    public static final String HEADER_TS = "TS";
    public static final String HEADER_CPU = "CPU";
    public static final String HEADER_MEM = "MEM";
    public static final String HEADER_TOTAL_DISK = "TOTAL_DISK";
    public static final String HEADER_USED_DISK = "USED_DISK";
    public static final String HEADER_USER_USED_DISK = "USER_USED_DISK";
    public static final String HEADER_UNUSED_DISK = "UNUSED_DISK";
    public static final String HEADER_INPUT_BANDWIDTH = "INPUT_BANDWIDTH";
    public static final String HEADER_OUTPUT_BANDWIDTH = "OUTPUT_BANDWIDTH";

    public static final String HEADER_USED = "Used";
    public static final String HEADER_TOTAL = "Total";
    public static final String HEADER_AVAILABLE = "Available";
    public static final String HEADER_VALID_TIMESTAMP = "VALID_TIMESTAMP";

    public static final String HEADER_QUOTA_CODE = "QUOTA_CODE";
    public static final String HEADER_QUOTA_MEAN = "QUOTA_MEAN";
    public static final String HEADER_START_TS = "START_TS";
    public static final String HEADER_END_TS = "END_TS";

    public static final String HEADER_VBROKER_ID = "VBROKER_ID";
    public static final String HEADER_VBROKER_IDS = "VBROKER_IDS";
    public static final String HEADER_VBROKER_NAME = "VBROKER_NAME";
    public static final String HEADER_BROKER_ID = "BROKER_ID";
    public static final String HEADER_BROKER_NAME = "BROKER_NAME";
    public static final String HEADER_BROKER_INFO = "BROKER_INFO";
    public static final String HEADER_HOSTNAME = "HOSTNAME";
    public static final String HEADER_INSTANCE_NAME = "INSTANCE_NAME";
    public static final String HEADER_IP = "IP";
    public static final String HEADER_MGR_PORT = "MGR_PORT";
    public static final String HEADER_MQ_PWD = "MQ_PWD";
    public static final String HEADER_MQ_USER = "MQ_USER";
    public static final String HEADER_OS_PWD = "OS_PWD";
    public static final String HEADER_OS_USER = "OS_USER";
    public static final String HEADER_PORT = "PORT";
    public static final String HEADER_LISTEN_PORT = "LISTEN_PORT";
    public static final String HEADER_STAT_PORT = "STAT_PORT";
    public static final String HEADER_REC_TIME = "REC_TIME";
    public static final String HEADER_SSH_PORT = "SSH_PORT";
    public static final String HEADER_VHOST = "VHOST";
    public static final String HEADER_VIP = "VIP";
    public static final String HEADER_IS_CLUSTER = "IS_CLUSTER";
    public static final String HEADER_IS_WRITABLE = "IS_WRITABLE";
    public static final String HEADER_ROOT_PWD = "ROOT_PWD";
    public static final String HEADER_IS_RUNNING = "IS_RUNNING";
    
    public static final String HEADER_QUEUE_ID = "QUEUE_ID";
    public static final String HEADER_QUEUE_NAME = "QUEUE_NAME";
    public static final String HEADER_IS_DURABLE = "IS_DURABLE";
    public static final String HEADER_GLOBAL_ORDERED = "IS_ORDERED";
    public static final String HEADER_IS_PRIORITY = "IS_PRIORITY";
    public static final String HEADER_QUEUE_TYPE = "QUEUE_TYPE";
    public static final String HEADER_ERL_COOKIE = "ERL_COOKIE";

    public static final String HEADER_MASTER_ID = "MASTER_ID";
    public static final String HEADER_SLAVE_ID = "SLAVE_ID";

    public static final String HEADER_MAIN_TOPIC = "MAIN_TOPIC";
    public static final String HEADER_SUB_TOPIC = "SUB_TOPIC";

    public static final String HEADER_GROUP_ID = "GROUP_ID";
    public static final String HEADER_GROUP_IDS = "GROUP_IDS";
    public static final String HEADER_GROUP_NAME = "GROUP_NAME";
    public static final String HEADER_GROUP_VBROKER_THRESHHOLD = "VBROKER_THRESHHOLD";

    public static final String HEADER_MSG_CNT = "MSG_CNT";
    public static final String HEADER_EVENT_CODE = "EVENT_CODE";

    public static final String HEADER_PRODUCE_RATE = "PRODUCE_RATE";
    public static final String HEADER_CONSUMER_RATE = "CONSUMER_RATE";

    public static final String HEADER_DURABLE = "DURABLE";
    public static final String HEADER_AUTODELETE = "AUTODELETE";
    public static final String HEADER_ARGUMENTS = "ARGUMENTS";
    public static final String HEADER_NODE_NAME = "NODE_NAME";
    public static final String HEADER_MEMARY = "MEMARY";
    public static final String HEADER_PRODUCE_COUNTS = "PRODUCE_COUNTS";
    public static final String HEADER_CONSUMER_COUNTS = "CONSUMER_COUNTS";

    public static final String HEADER_VIP_PORT = "VIP_PORT";
    public static final String HEADER_CLUSTER_NAME = "CLUSTER_NAME";
    public static final String HEADER_CONSUMERS = "CONSUMERS";
    public static final String HEADER_QUEUES = "QUEUES";
    public static final String HEADER_CONNECTIONS = "CONNECTIONS";
    public static final String HEADER_CONNECTIONS_RATE = "CONNECTIONS_RATE";
    public static final String HEADER_NODEINFO_JSONSTR = "NODEINFO_JSONSTR";

    public static final String HEADER_ALARM_CODE = "ALARM_CODE";
    public static final String HEADER_ALARM_DESC = "ALARM_DESC";

    public static final String HEADER_CLIENT_INFO = "CLIENT_INFO";
    public static final String HEADER_LSNR_ADDR = "LSNR_ADDR";
    public static final String HEADER_CLIENT_TYPE = "CLIENT_TYPE";
    public static final String HEADER_CLNT_IP_AND_PORT = "CLIENT_IP_AND_PORT";
    public static final String HEADER_BKR_IP_AND_PORT = "BROKER_IP_AND_PORT";
    public static final String HEADER_CLIENT_PRO_TPS = "CLIENT_PRO_TPS";
    public static final String HEADER_CLIENT_CON_TPS = "CLIENT_CON_TPS";
    public static final String HEADER_T_PRO_MSG_COUNT = "TOTAL_PRO_MSG_COUNT";
    public static final String HEADER_T_PRO_MSG_BYTES = "TOTAL_PRO_MSG_BYTES";
    public static final String HEADER_T_CON_MSG_COUNT = "TOTAL_CON_MSG_COUNT";
    public static final String HEADER_T_CON_MSG_BYTES = "TOTAL_CON_MSG_BYTES";

    public static final String HEADER_LAST_TIMESTAMP = "LAST_TIMESTAMP";
    public static final String HEADER_CLIENT_INFOS = "CLIENT_INFOS";

    public static final String HEADER_BIND_TYPE = "BIND_TYPE";
    public static final String HEADER_CONSUMER_ID = "CONSUMER_ID";
    public static final String HEADER_SRC_QUEUE = "SRC_QUEUE";
    public static final String HEADER_MAIN_KEY = "MAINKEY";
    public static final String HEADER_SUB_KEY = "SUBKEY";
    public static final String HEADER_REAL_QUEUE = "REAL_QUEUE";
    public static final String HEADER_PERM_QUEUE = "PERM_QUEUE";

    public static final String HEADER_REC_ID = "REC_ID";
    public static final String HEADER_USER_ID = "USER_ID";
    public static final String HEADER_USER_NAME = "USER_NAME";
    public static final String HEADER_LOGIN_PWD = "LOGIN_PWD";
    public static final String HEADER_USER_PWD = "USER_PWD";
    public static final String HEADER_USER_STATUS = "USER_STATUS";
    public static final String HEADER_LINE_STATUS = "LINE_STATUS";
    public static final String HEADER_REC_STATUS = "REC_STATUS";
    public static final String HEADER_REC_PERSON = "REC_PERSON";

    public static final String HEADER_ROLE_ID = "ROLE_ID";
    public static final String HEADER_ROLE_IDS = "ROLE_IDS";
    public static final String HEADER_ROLE_NAME = "ROLE_NAME";
    public static final String HEADER_PARENT_ID = "PARENT_ID";
    public static final String HEADER_ROLE_DEC = "ROLE_DEC";

    public static final String HEADER_ACTIVE_COLL_INFO = "ACTIVE_COLL_INFO";

    public static final String HEADER_API_TYPE = "API_TYPE";
    public static final String HEADER_JSONSTR = "JSON_STR";

    public static final String HEADER_MSG_READY = "messages_ready";
    public static final String HEADER_MSG_UNACK = "messages_unacknowledged";
    public static final String HEADER_QUEEU_TOTAL_MSG = "queue_totals.messages";
    public static final String HEADER_MSG_TOTAL = "msg_total";
    public static final String HEADER_QUEUE_TOTALS = "queue_totals";
    public static final String HEADER_MESSAGES = "messages";

    public static final String HEADER_KEY = "key";
    public static final String HEADER_TIMESTAMP = "TIMESTAMP";
    public static final String HEADER_START_TIMESTAMP = "START_TIMESTAMP";
    public static final String HEADER_END_TIMESTAMP = "END_TIMESTAMP";
    public static final String HEADER_POS = "POS";
    public static final String HEADER_X = "x";
    public static final String HEADER_Y = "y";
    public static final String HEADER_WIDTH = "WIDTH";
    public static final String HEADER_HEIGHT = "HEIGHT";
    public static final String HEADER_ROW = "ROW_";
    public static final String HEADER_COL = "COL_";

    public static final String HEADER_LOGIN_TIME = "LOGIN_TIME";
    public static final String HEADER_MAGIC_KEY = "MAGIC_KEY";
    public static final String HEADER_COOKIE = "Cookie";
    public static final String HEADER_IS_ADMIN = "IS_ADMIN";
    public static final String HEADER_SESSION_TIMEOUT = "SESSION_TIMEOUT";

    public static final String HEADER_BLACK_WHITE_LIST_IP = "IP";
    public static final String HEADER_BLACK_WHITE_REMARKS = "REMARKS";
    public static final String HEADER_BLACK_WHITE_TYPE = "TYPE";

    public static final String HEADER_ATTR_ID = "ATTR_ID";
    public static final String HEADER_ATTR_NAME = "ATTR_NAME";
    public static final String HEADER_ATTR_NAME_CN = "ATTR_NAME_CN";
    public static final String HEADER_AUTO_GEN = "AUTO_GEN";

    public static final String HEADER_INSTANCE_ADDRESS = "INST_ADD";
    public static final String HEADER_CMPT_TYPE = "CMPT_TYPE";
    public static final String HEADER_CMPT_ID = "CMPT_ID";
    public static final String HEADER_CMPT_NAME = "CMPT_NAME";
    public static final String HEADER_CMPT_NAME_CN = "CMPT_NAME_CN";
    public static final String HEADER_IS_NEED_DEPLOY = "IS_NEED_DEPLOY";
    public static final String HEADER_SERV_CLAZZ = "SERV_CLAZZ";
    public static final String HEADER_SERV_TYPE = "SERV_TYPE";
    public static final String HEADER_SERV_ID = "SERV_ID";
    public static final String HEADER_META_SERV_ID = "META_SERV_ID";
    public static final String HEADER_SERV_NAME = "SERV_NAME";
    public static final String HEADER_SUB_SERV_TYPE = "SUB_SERV_TYPE";
    public static final String HEADER_NODE_JSON_TYPE = "NODE_JSON_TYPE";
    public static final String HEADER_SUB_CMPT_ID = "SUB_CMPT_ID";
    public static final String HEADER_VERSION = "VERSION";

    public static final String HEADER_INST_ID = "INST_ID";
    public static final String HEADER_INST_ID_LIST = "INST_ID_LIST";
    public static final String HEADER_SERV_INST_ID = "SERV_INST_ID";
    public static final String HEADER_QUEUE_SERV_INST_ID = "QUEUE_SERV_INST_ID";
    public static final String HEADER_DB_SERV_INST_ID = "DB_SERV_INST_ID";
    public static final String HEADER_DASHBOARD_PORT = "DASHBOARD_PORT";
    public static final String HEADER_CONTROL_PORT = "CONTROL_PORT";
    public static final String HEADER_IS_PRODUCT = "IS_PRODUCT";
    public static final String HEADER_SSH_ID = "SSH_ID";
    public static final String HEADER_SSH_NAME = "SSH_NAME";
    public static final String HEADER_SSH_PWD = "SSH_PWD";
    public static final String HEADER_OLD_PWD = "OLD_PWD";
    public static final String HEADER_IS_DEPLOYED = "IS_DEPLOYED";
    public static final String HEADER_PRE_EMBADDED = "PRE_EMBADDED";
    public static final String HEADER_CREATE_TIME = "CREATE_TIME";
    public static final String HEADER_USER = "USER";
    public static final String HEADER_PASSWORD = "PASSWORD";
    public static final String HEADER_ATTR_VALUE = "ATTR_VALUE";
    public static final String HEADER_INST_ID1 = "INST_ID1";
    public static final String HEADER_INST_ID2 = "INST_ID2";
    public static final String HEADER_TOPO_TYPE = "TOPO_TYPE";
    public static final String HEADER_COLLECTD = "COLLECTD";
    public static final String HEADER_ROCKETMQ_CONSOLE = "ROCKETMQ_CONSOLE";
    public static final String HEADER_RPC_BIND_PORT = "RPC_BIND_PORT";
    public static final String HEADER_WEBSERVER_PORT = "WEBSERVER_PORT";
    public static final String HEADER_DURABLE_WAL_WRITE = "DURABLE_WAL_WRITE";
    public static final String HEADER_ENABLE_LOAD_BALANCING = "ENABLE_LOAD_BALANCING";
    public static final String HEADER_MAX_CLOCK_SKEW_USEC = "MAX_CLOCK_SKEW_USEC";
    public static final String HEADER_REPLICATION_FACTOR = "REPLICATION_FACTOR";
    public static final String HEADER_YB_NUM_SHARDS_PER_TSERVER = "YB_NUM_SHARDS_PER_TSERVER";
    public static final String HEADER_YSQL_NUM_SHARDS_PER_TSERVER = "YSQL_NUM_SHARDS_PER_TSERVER";
    public static final String HEADER_PLACEMENT_CLOUD = "PLACEMENT_CLOUD";
    public static final String HEADER_PLACEMENT_ZONE = "PLACEMENT_ZONE";
    public static final String HEADER_PLACEMENT_REGION = "PLACEMENT_REGION";
    public static final String HEADER_CDC_WAL_RETENTION_TIME_SECS = "CDC_WAL_RETENTION_TIME_SECS";
    public static final String HEADER_PGSQL_PROXY_BIND_PORT = "PGSQL_PROXY_BIND_PORT";
    public static final String HEADER_PGSQL_PROXY_WEBSERVER_PORT = "PGSQL_PROXY_WEBSERVER_PORT";
    public static final String HEADER_CQL_PROXY_BIND_PORT = "CQL_PROXY_BIND_PORT";
    public static final String HEADER_CQL_PROXY_WEBSERVER_PORT = "CQL_PROXY_WEBSERVER_PORT";
    public static final String HEADER_YSQL_MAX_CONNECTIONS = "YSQL_MAX_CONNECTIONS";
    public static final String HEADER_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC = "ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC";
    public static final String HEADER_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH = "ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH";
    public static final String HEADER_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO = "ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO";
    public static final String HEADER_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC = "TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC";
    public static final String HEADER_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC = "REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC";

    public static final String HEADER_REDIS_SERV_CLUSTER_CONTAINER = "REDIS_SERV_CLUSTER_CONTAINER";
    public static final String HEADER_REDIS_SERV_MS_CONTAINER = "REDIS_SERV_MS_CONTAINER";
    public static final String HEADER_HOST_CONTAINER = "HOST_CONTAINER";
    public static final String HEADER_REDIS_NODE_CONTAINER = "REDIS_NODE_CONTAINER";
    public static final String HEADER_HOST_NODE_CONTAINER = "HOST_NODE_CONTAINER";
    public static final String HEADER_REDIS_PROXY_CONTAINER = "REDIS_PROXY_CONTAINER";
    public static final String HEADER_REDIS_NODE = "REDIS_NODE";
    public static final String HEADER_HOST_NODE = "HOST_NODE";
    public static final String HEADER_REDIS_PROXY = "REDIS_PROXY";
    public static final String HEADER_MAX_CONN = "MAX_CONN";
    public static final String HEADER_MAX_MEMORY = "MAX_MEMORY";
    public static final String HEADER_PROXY_THREADS = "PROXY_THREADS";
    public static final String HEADER_NODE_CONN_POOL_SIZE = "NODE_CONN_POOL_SIZE";
    public static final String HEADER_CLIENT_PORT = "CLIENT_PORT";
    public static final String HEADER_ZK_CLIENT_PORT1 = "ZK_CLIENT_PORT1";
    public static final String HEADER_ZK_CLIENT_PORT2 = "ZK_CLIENT_PORT2";
    public static final String HEADER_ADMIN_PORT = "ADMIN_PORT";
    public static final String HEADER_PEER_PORT = "PEER_PORT";
    public static final String HEADER_FILE_ID = "FILE_ID";
    public static final String HEADER_HOST_ID = "HOST_ID";
    public static final String HEADER_FILE_NAME = "FILE_NAME";
    public static final String HEADER_FILE_DIR = "FILE_DIR";
    public static final String HEADER_POS_X = "POS_X";
    public static final String HEADER_POS_Y = "POS_Y";
    public static final String HEADER_IP_ADDRESS = "IP_ADDRESS";

    public static final String HEADER_APISIX_CONTAINER = "APISIX_CONTAINER";
    public static final String HEADER_APISIX_SERVER = "APISIX_SERVER";
    public static final String HEADER_ETCD_CONTAINER = "ETCD_CONTAINER";
    public static final String HEADER_ETCD = "ETCD";
    public static final String HEADER_CLIENT_URLS_PORT = "CLIENT_URLS_PORT";
    public static final String HEADER_PEER_URLS_PORT = "PEER_URLS_PORT";
    public static final String HEADER_ETCD_CLUSTER_ADDR = "ETCD_CLUSTER_ADDR";
    public static final String HEADER_ETCD_IP = "ETCD_IP";
    public static final String HEADER_CLUSTER_TOKEN = "CLUSTER_TOKEN";
    public static final String HEADER_ETCD_ADDR_LIST = "ETCD_ADDR_LIST";
    public static final String HEADER_SSL_PORT = "SSL_PORT";
    public static final String HEADER_INST_ID_MD5 = "INST_ID_MD5";
    public static final int ETCD_PRODUCT_ENV_MIN_NODES = 3;

    public static final String HEADER_BROKER_IP = "BROKER_IP";
    public static final String HEADER_ROCKETMQ_SERV_CONTAINER = "ROCKETMQ_SERV_CONTAINER";
    public static final String HEADER_ROCKETMQ_VBROKER_CONTAINER = "ROCKETMQ_VBROKER_CONTAINER";
    public static final String HEADER_ROCKETMQ_NAMESRV_CONTAINER = "ROCKETMQ_NAMESRV_CONTAINER";
    public static final String HEADER_ROCKETMQ_VBROKER = "ROCKETMQ_VBROKER";
    public static final String HEADER_ROCKETMQ_BROKER = "ROCKETMQ_BROKER";
    public static final String HEADER_ROCKETMQ_NAMESRV = "ROCKETMQ_NAMESRV";

    public static final String HEADER_TIDB_SERV_CONTAINER = "TIDB_SERV_CONTAINER";
    public static final String HEADER_PD_SERVER_CONTAINER = "PD_SERVER_CONTAINER";
    public static final String HEADER_TIKV_SERVER_CONTAINER = "TIKV_SERVER_CONTAINER";
    public static final String HEADER_TIDB_SERVER_CONTAINER = "TIDB_SERVER_CONTAINER";
    public static final String HEADER_TIDB_SERVER = "TIDB_SERVER";
    public static final String HEADER_TIKV_SERVER = "TIKV_SERVER";
    public static final String HEADER_PD_SERVER = "PD_SERVER";
    public static final String HEADER_DASHBOARD_PROXY = "DASHBOARD_PROXY";

    public static final String HEADER_PULSAR_SERV_CONTAINER = "PULSAR_SERV_CONTAINER";
    public static final String HEADER_PULSAR_BROKER_CONTAINER = "PULSAR_BROKER_CONTAINER";
    public static final String HEADER_PULSAR_BOOKKEEPER_CONTAINER = "PULSAR_BOOKKEEPER_CONTAINER";
    public static final String HEADER_ZOOKEEPER_CONTAINER = "ZOOKEEPER_CONTAINER";
    public static final String HEADER_PULSAR_MANAGER = "PULSAR_MANAGER";
    public static final String HEADER_PULSAR_BROKER = "PULSAR_BROKER";
    public static final String HEADER_PULSAR_BOOKKEEPER = "PULSAR_BOOKKEEPER";
    public static final String HEADER_ZOOKEEPER = "ZOOKEEPER";
    
    public static final String HEADER_PULSAR_MGR_PORT = "PULSAR_MGR_PORT";
    public static final String HEADER_HERDDB_PORT = "HERDDB_PORT";
    
    public static final String HEADER_CLICKHOUSE_SERV_CONTAINER = "CLICKHOUSE_SERV_CONTAINER";
    public static final String HEADER_CLICKHOUSE_REPLICAS_CONTAINER = "CLICKHOUSE_REPLICAS_CONTAINER";
    public static final String HEADER_CLICKHOUSE_REPLICAS = "CLICKHOUSE_REPLICAS";
    public static final String HEADER_CLICKHOUSE_SERVER = "CLICKHOUSE_SERVER";
    public static final String HEADER_CLICKHOUSE_EXPORTER = "CLICKHOUSE_EXPORTER";
    public static final String HEADER_INTERNAL_REPLICATION = "INTERNAL_REPLICATION";
    
    public static final String HEADER_VOLTDB_SERV_CONTAINER = "VOLTDB_SERV_CONTAINER";
    public static final String HEADER_VOLTDB_CONTAINER = "VOLTDB_CONTAINER";
    public static final String HEADER_VOLTDB_SERVER = "VOLTDB_SERVER";
    public static final String HEADER_VOLT_CLIENT_PORT = "VOLT_CLIENT_PORT";
    public static final String HEADER_VOLT_ADMIN_PORT = "VOLT_ADMIN_PORT";
    public static final String HEADER_VOLT_WEB_PORT = "VOLT_WEB_PORT";
    public static final String HEADER_VOLT_INTERNAL_PORT = "VOLT_INTERNAL_PORT";
    public static final String HEADER_VOLT_REPLI_PORT = "VOLT_REPLI_PORT";
    public static final String HEADER_VOLT_ZK_PORT = "VOLT_ZK_PORT";
    public static final String HEADER_SITES_PER_HOST = "SITES_PER_HOST";
    public static final String HEADER_KFACTOR = "KFACTOR";
    public static final String HEADER_MEM_LIMIT = "MEM_LIMIT";
    public static final String HEADER_HEARTBEAT_TIMEOUT = "HEARTBEAT_TIMEOUT";
    public static final String HEADER_TEMPTABLES_MAXSIZE = "TEMPTABLES_MAXSIZE";
    public static final String HEADER_ELASTIC_DURATION = "ELASTIC_DURATION";
    public static final String HEADER_ELASTIC_THROUGHPUT = "ELASTIC_THROUGHPUT";
    public static final String HEADER_QUERY_TIMEOUT = "QUERY_TIMEOUT";
    public static final String HEADER_PROCEDURE_LOGINFO = "PROCEDURE_LOGINFO";
    public static final String HEADER_MEM_ALERT = "MEM_ALERT";
    
    public static final String HEADER_YB_MASTER_CONTAINER = "YB_MASTER_CONTAINER";
    public static final String HEADER_YB_TSERVER_CONTAINER = "YB_TSERVER_CONTAINER";
    public static final String HEADER_YB_MASTER = "YB_MASTER";
    public static final String HEADER_YB_TSERVER = "YB_TSERVER";
    
    public static final String HEADER_PROMETHEUS = "PROMETHEUS";
    public static final String HEADER_GRAFANA = "GRAFANA";

    public static final String HEADER_TIDB_JSON = "TIDB_JSON";
    public static final String HEADER_TOPO_JSON = "TOPO_JSON";
    public static final String HEADER_NODE_JSON = "NODE_JSON";
    public static final String HEADER_DB_SERV_CONTAINER = "DB_SERV_CONTAINER";
    public static final String HEADER_DB_SVC_CONTAINER_ID = "DB_SVC_CONTAINER_ID";
    public static final String HEADER_DB_SVC_CONTAINER_NAME = "DB_SVC_CONTAINER_NAME";

    public static final String HEADER_DB_TIDB_CONTAINER = "DB_TIDB_CONTAINER";
    public static final String HEADER_TIDB_CONTAINER_ID = "TIDB_CONTAINER_ID";
    public static final String HEADER_TIDB_CONTAINER_NAME = "TIDB_CONTAINER_NAME";
    public static final String HEADER_DB_TIDB = "DB_TIDB";
    public static final String HEADER_TIDB_ID = "TIDB_ID";
    public static final String HEADER_TIDB_NAME = "TIDB_NAME";

    public static final String HEADER_DB_TIKV_CONTAINER = "DB_TIKV_CONTAINER";
    public static final String HEADER_TIKV_CONTAINER_ID = "TIKV_CONTAINER_ID";
    public static final String HEADER_TIKV_CONTAINER_NAME = "TIKV_CONTAINER_NAME";
    public static final String HEADER_DB_TIKV = "DB_TIKV";
    public static final String HEADER_TIKV_ID = "TIKV_ID";
    public static final String HEADER_TIKV_NAME = "TIKV_NAME";

    public static final String HEADER_DB_PD_CONTAINER = "DB_PD_CONTAINER";
    public static final String HEADER_PD_CONTAINER_ID = "PD_CONTAINER_ID";
    public static final String HEADER_PD_CONTAINER_NAME = "PD_CONTAINER_NAME";
    public static final String HEADER_DB_PD = "DB_PD";
    public static final String HEADER_PD_ID = "PD_ID";
    public static final String HEADER_PD_NAME = "PD_NAME";

    public static final String HEADER_TDENGINE_SERV_CONTAINER = "TDENGINE_SERV_CONTAINER";
    public static final String HEADER_ARBITRATOR_CONTAINER = "ARBITRATOR_CONTAINER";
    public static final String HEADER_DNODE_CONTAINER = "DNODE_CONTAINER";
    public static final String HEADER_TD_ARBITRATOR = "TD_ARBITRATOR";
    public static final String HEADER_TD_DNODE = "TD_DNODE";

    public static final String HEADER_ORACLE_DG_SERV_CONTAINER = "ORACLE_DG_SERV_CONTAINER";
    public static final String HEADER_DG_CONTAINER = "DG_CONTAINER";
    public static final String HEADER_ORCL_INSTANCE = "ORCL_INSTANCE";

    public static final String HEADER_REDIS_HA_CLUSTER_CONTAINER = "REDIS_HA_CLUSTER_CONTAINER";
    public static final String HEADER_HA_CONTAINER = "HA_CONTAINER";

    public static final String HEADER_DB_COLLECTD = "DB_COLLECTD";
    public static final String HEADER_COLLECTD_ID = "COLLECTD_ID";
    public static final String HEADER_COLLECTD_NAME = "COLLECTD_NAME";

    public static final String HEADER_ATTRIBUTES = "ATTRS";
    public static final String HEADER_DEPLOY_FLAG = "DEPLOY_FLAG";

    public static final String HEADER_SQL_STR = "SQL_STR";
    public static final String HEADER_SCHEMA_NAME = "SCHEMA_NAME";

    public static final String HEADER_PAGE_SIZE = "pageSize";
    public static final String HEADER_PAGE_NUMBER = "pageNumber";

    public static final String HEADER_CLUSTER_ID = "CLUSTER_ID";
    public static final String HEADER_NEW_MASTER_ID = "NEW_MASTER_ID";

    public static final String HEADER_SEQ_NAME = "SEQ_NAME";
    public static final String HEADER_SEQ_STEP = "SEQ_STEP";
    public static final String HEADER_START = "START";
    public static final String HEADER_END = "END";
    public static final String HEADER_CURR_VALUE = "CURR_VALUE";

    public static final String HEADER_CACHE_PROXY_ID = "CACHE_PROXY_ID";
    public static final String HEADER_CACHE_PROXY_NAME = "CACHE_PROXY_NAME";
    public static final String HEADER_ACCESS_CLIENT_CONNS = "ACCESS_CLIENT_CONNS";
    public static final String HEADER_ACCESS_PROCESS_AVTIME = "ACCESS_PROCESS_AVTIME";
    public static final String HEADER_ACCESS_REDIS_CONNS = "ACCESS_REDIS_CONNS";
    public static final String HEADER_ACCESS_PROCESS_MAXTIME = "ACCESS_PROCESS_MAXTIME";
    public static final String HEADER_ACCESS_REQUEST_EXCEPTS = "ACCESS_REQUEST_EXCEPTS";
    public static final String HEADER_ACCESS_REQUEST_TPS = "ACCESS_REQUEST_TPS";

    public static final String HEADER_CACHE_NODE_ID = "CACHE_NODE_ID";
    public static final String HEADER_CACHE_NODE_NAME = "CACHE_NODE_NAME";
    public static final String HEADER_DB_SIZE = "DB_SIZE";
    public static final String HEADER_MEMORY_TOTAL = "MEMORY_TOTAL";
    public static final String HEADER_PROCESS_TPS = "PROCESS_TPS";
    public static final String HEADER_MEMORY_USED = "MEMORY_USED";
    public static final String HEADER_LINK_STATUS = "LINK_STATUS";

    public static final String HEADER_SERVER_ID = "SERVER_ID";
    public static final String HEADER_SERVER_IP = "SERVER_IP";
    public static final String HEADER_SERVER_NAME = "SERVER_NAME";
    public static final String HEADER_SSH_LIST = "SSH_LIST";

    public static final String HEADER_TIDB_QPS = "QPS";
    public static final String HEADER_TIDB_CONNECTION_COUNT = "CONNECTION_COUNT";
    public static final String HEADER_TIDB_STATEMENT_COUNT = "STATEMENT_COUNT";
    public static final String HEADER_TIDB_QUERY_DURATION_99P = "QUERY_DURATION_99PERC";

    public static final String HEADER_PD_STORAGE_CAPACITY = "STORAGE_CAPACITY";
    public static final String HEADER_PD_CURRENT_STORAGE_SIZE = "CURRENT_STORAGE_SIZE";
    public static final String HEADER_PD_LEADER_BALANCE_RATIO = "LEADER_BALANCE_RATIO";
    public static final String HEADER_PD_REGION_BALANCE_RATIO = "REGION_BALANCE_RATIO";
    public static final String HEADER_PD_REGIONS = "REGIONS";
    public static final String HEADER_PD_COMPLETE_DURATION_SECONDS_99PENC = "COMPLETE_DURATION_SECONDS_99PENC";

    public static final String HEADER_TIKV_LEADER_COUNT = "LEADER_COUNT";
    public static final String HEADER_TIKV_REGION_COUNT = "REGION_COUNT";
    public static final String HEADER_TIKV_SCHEEDULER_COMMAND_DURATION = "SCHEEDULER_COMMAND_DURATION";

    public static final String HEADER_ACC_ID = "ACC_ID";
    public static final String HEADER_ACC_NAME = "ACC_NAME";
    public static final String HEADER_PHONE_NUM = "PHONE_NUM";
    public static final String HEADER_MAIL = "MAIL";
    public static final String HEADER_PASSWD = "PASSWD";

    public static final String HEADER_JVM_INFO_JSON_ARRAY = "JVM_INFO_JSON_ARRAY";
    public static final String HEADER_JVM_DATA = "JVM_DATA";
    public static final String HEADER_JAVA_VERSION = "JAVA_VERSION";
    public static final String HEADER_GC_YOUNG_GC_COUNT = "GC_YOUNG_GC_COUNT";
    public static final String HEADER_GC_YOUNG_GC_TIME = "GC_YOUNG_GC_TIME";
    public static final String HEADER_GC_FULL_GC_COUNT = "GC_FULL_GC_COUNT";
    public static final String HEADER_GC_FULL_GC_TIME = "GC_FULL_GC_TIME";
    public static final String HEADER_THREAD_DAEMON_THREAD_COUNT = "THREAD_DAEMON_THREAD_COUNT";
    public static final String HEADER_THREAD_COUNT = "THREAD_THREAD_COUNT";
    public static final String HEADER_THREAD_PEEK_THREAD_COUNT = "THREAD_PEEK_THREAD_COUNT";
    public static final String HEADER_THREAD_DEAD_LOCKED_THREAD_COUNT = "THREAD_DEADLOCKED_THREAD_COUNT";
    public static final String HEADER_MEM_EDEN_INIT = "MEM_EDEN_INIT";
    public static final String HEADER_MEM_EDEN_USED = "MEM_EDEN_USED";
    public static final String HEADER_MEM_EDEN_COMMITTED = "MEM_EDEN_COMMITTED";
    public static final String HEADER_MEM_EDEN_MAX = "MEM_EDEN_MAX";
    public static final String HEADER_MEM_EDEN_USEDPERCENT = "MEM_EDEN_USEDPERCENT";
    public static final String HEADER_MEM_SURVIVOR_INIT = "MEM_SURVIVOR_INIT";
    public static final String HEADER_MEM_SURVIVOR_USED = "MEM_SURVIVOR_USED";
    public static final String HEADER_MEM_SURVIVOR_COMMITTED = "MEM_SURVIVOR_COMMITTED";
    public static final String HEADER_MEM_SURVIVOR_MAX = "MEM_SURVIVOR_MAX";
    public static final String HEADER_MEM_SURVIVOR_USEDPERCENT = "MEM_SURVIVOR_USEDPERCENT";
    public static final String HEADER_MEM_OLD_INIT = "MEM_OLD_INIT";
    public static final String HEADER_MEM_OLD_USED = "MEM_OLD_USED";
    public static final String HEADER_MEM_OLD_COMMITTED = "MEM_OLD_COMMITTED";
    public static final String HEADER_MEM_OLD_MAX = "MEM_OLD_MAX";
    public static final String HEADER_MEM_OLD_USEDPERCENT = "MEM_OLD_USEDPERCENT";
    public static final String HEADER_MEM_PERM_INIT = "MEM_PERM_INIT";
    public static final String HEADER_MEM_PERM_USED = "MEM_PERM_USED";
    public static final String HEADER_MEM_PERM_COMMITTED = "MEM_PERM_COMMITTED";
    public static final String HEADER_MEM_PERM_MAX = "MEM_PERM_MAX";
    public static final String HEADER_MEM_PERM_USEDPERCENT = "MEM_PERM_USEDPERCENT";
    public static final String HEADER_MEM_CODE_INIT = "MEM_CODE_INIT";
    public static final String HEADER_MEM_CODE_USED = "MEM_CODE_USED";
    public static final String HEADER_MEM_CODE_COMMITTED = "MEM_CODE_COMMITTED";
    public static final String HEADER_MEM_CODE_MAX = "MEM_CODE_MAX";
    public static final String HEADER_MEM_CODE_USEDPERCENT = "MEM_CODE_USEDPERCENT";
    public static final String HEADER_MEM_HEAP_INIT = "MEM_HEAP_INIT";
    public static final String HEADER_MEM_HEAP_USED = "MEM_HEAP_USED";
    public static final String HEADER_MEM_HEAP_COMMITTED = "MEM_HEAP_COMMITTED";
    public static final String HEADER_MEM_HEAP_MAX = "MEM_HEAP_MAX";
    public static final String HEADER_MEM_HEAP_USEDPERCENT = "MEM_HEAP_USEDPERCENT";
    public static final String HEADER_MEM_NOHEAP_INIT = "MEM_NOHEAP_INIT";
    public static final String HEADER_MEM_NOHEAP_USED = "MEM_NOHEAP_USED";
    public static final String HEADER_MEM_NOHEAP_COMMITTED = "MEM_NOHEAP_COMMITTED";
    public static final String HEADER_MEM_NOHEAP_MAX = "MEM_NOHEAP_MAX";
    public static final String HEADER_MEM_NOHEAP_USEDPERCENT = "MEM_NOHEAP_USEDPERCENT";
    public static final String HEADER_STATUS = "STATUS";
    public static final String HEADER_UPDATE_TIME = "UPDATE_TIME";
    public static final String HEADER_PSEUDO_DEPLOY_FLAG = "PSEUDO_DEPLOY_FLAG";

    public static final String HEADER_REDIS_INFO_JSON_ARRAY = "REDIS_INFO_JSON_ARRAY";
    public static final String HEADER_HOST_INFO_JSON_ARRAY = "HOST_INFO_JSON_ARRAY";
    public static final String HEADER_REDIS_DATA = "REIDS_DATA";
    public static final String HEADER_HOST_DATA = "HOST_DATA";
    public static final String HEADER_ROLE = "ROLE";
    public static final String HEADER_CONNECTED_CLIENTS = "CONNECTED_CLIENTS";
    public static final String HEADER_USED_MEMORY = "USED_MEMORY";
    public static final String HEADER_MAXMEMORY= "MAXMEMORY";
    public static final String HEADER_INSTANTANEOUS_OPS_PER_SEC = "INSTANTANEOUS_OPS_PER_SEC";
    public static final String HEADER_INSTANTANEOUS_INPUT_KBPS = "INSTANTANEOUS_INPUT_KBPS";
    public static final String HEADER_INSTANTANEOUS_OUTPUT_KBPS = "INSTANTANEOUS_OUTPUT_KBPS";
    public static final String HEADER_SYNC_FULL = "SYNC_FULL";
    public static final String HEADER_EXPIRED_KEYS= "EXPIRED_KEYS";
    public static final String HEADER_EVICTED_KEYS = "EVICTED_KEYS";
    public static final String HEADER_KEYSPACE_HITS = "KEYSPACE_HITS";
    public static final String HEADER_KEYSPACE_MISSES = "KEYSPACE_MISSES";
    public static final String HEADER_USED_CPU_SYS = "USED_CPU_SYS";
    public static final String HEADER_USED_CPU_USER = "USED_CPU_USER";
    public static final String HEADER_CPU_IDLE = "USED_CPU_IDLE";

    public static final String HEADER_DG_NAME = "DG_NAME";
    public static final String HEADER_DECRYPT = "DECRYPT";
    public static final String HEADER_DB_TYPE = "DB_TYPE";
    public static final String HEADER_ACTIVE_DB_TYPE = "ACTIVE_DB_TYPE";
    public static final String HEADER_DB_SOURCE_MODEL = "DB_SOURCE_MODEL";
    public static final String HEADER_MASTER_JDBC_URL = "MASTER_JDBC_URL";
    public static final String HEADER_MASTER_USER_NAME = "MASTER_USER_NAME";
    public static final String HEADER_MASTER_PASSWORD = "MASTER_PASSWORD";
    public static final String HEADER_BACKUP_JDBC_URL = "BACKUP_JDBC_URL";
    public static final String HEADER_BACKUP_USER_NAME = "BACKUP_USER_NAME";
    public static final String HEADER_BACKUP_PASSWORD = "BACKUP_PASSWORD";
    public static final String HEADER_MIN_IDLE = "MIN_IDLE";
    public static final String HEADER_MAX_ACTIVE = "MAX_ACTIVE";
    public static final String HEADER_VALIDATION_QUERY = "VALIDATION_QUERY";
    public static final String HEADER_CONN_TIMEOUT = "CONN_TIMEOUT";

    public static final String HEADER_NODE_TYPE = "NODE_TYPE";
    public static final String HEADER_ORA_LSNR_PORT = "ORA_LSNR_PORT";
    public static final String HEADER_DB_USER = "DB_USER";
    public static final String HEADER_DB_PASSWD = "DB_PASSWD";
    public static final String HEADER_DB_NAME = "DB_NAME";
    public static final String HEADER_SERV_CONTAINER_NAME = "SERV_CONTAINER_NAME";
    public static final String HEADER_WEIGHT = "WEIGHT";

    public static final String HEADER_CLIENT_NAME = "CLIENT_NAME";
    public static final String HEADER_SLAVE_MIN_IDLE_SIZE = "SLAVE_MIN_IDLE_SIZE";
    public static final String HEADER_SLAVE_POOL_SIZE = "SLAVE_POOL_SIZE";
    public static final String HEADER_MASTER_MIN_IDLE_SIZE = "MASTER_MIN_IDLE_SIZE";
    public static final String HEADER_MASTER_POOL_SIZE = "MASTER_POOL_SIZE";
    public static final String HEADER_READ_MODE = "READ_MODE";
    public static final String HEADER_SERVER_MODE = "SERVER_MODE";

    public static final String HEADER_SYSTEM_PROPERTY = "SYSTEM_PROPERTY";
    public static final String HEADER_SERVICE_IMPL = "SERVICE_IMPL";
    public static final String HEADER_MAX_SMS_TASK_PROC = "MAX_SMS_TASK_PROC";
    public static final String HEADER_BATCH_SAVE_PROCESS = "BATCH_SAVE_PROCESS";
    public static final String HEADER_BATCHSAVE_PROCESS = "BATCHSAVE_PROCESS";
    public static final String HEADER_WEB_CONSOLE_PORT = "WEB_CONSOLE_PORT";
    public static final String HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME = "THREE_CHANNEL_LAST_UPDATE_REPORT_TIME";
    public static final String HEADER_INTERNAL_PORT = "INTERNAL_PORT";
    public static final String HEADER_SW_REF_INTERVAL = "SW_REF_INTERVAL";
    public static final String HEADER_WARN_SVC_URL = "WARN_SVC_URL";
    public static final String HEADER_DAT_CORE_SIZE = "DAT_CORE_SIZE";
    public static final String HEADER_DAT_MAX_SIZE = "DAT_MAX_SIZE";
    public static final String HEADER_DAT_QUEUE_SIZE = "DAT_QUEUE_SIZE";
    public static final String HEADER_ALT_CORE_SIZE = "ALT_CORE_SIZE";
    public static final String HEADER_ALT_MAX_SIZE = "ALT_MAX_SIZE";
    public static final String HEADER_ALT_QUEUE_SIZE = "ALT_QUEUE_SIZE";
    public static final String HEADER_TST_CORE_SIZE = "TST_CORE_SIZE";
    public static final String HEADER_TST_MAX_SIZE = "TST_MAX_SIZE";
    public static final String HEADER_TST_QUEUE_SIZE = "TST_QUEUE_SIZE";
    public static final String HEADER_STS_CORE_SIZE = "STS_CORE_SIZE";
    public static final String HEADER_STS_MAX_SIZE = "STS_MAX_SIZE";
    public static final String HEADER_STS_QUEUE_SIZE = "STS_QUEUE_SIZE";
    public static final String HEADER_SAMPLING_SWITCH = "SAMPLING_SWITCH";
    public static final String HEADER_CRON_EXPRESSION = "CRON_EXPRESSION";
    public static final String HEADER_DB_INST_ID = "DB_INST_ID";
    public static final String HEADER_ES_SERVER = "ES_SERVER";
    public static final String HEADER_ES_MT_SERVER = "ES_MT_SERVER";
    public static final String HEADER_MT_QUEUE_CLEAR_EXPRESSION = "MT_QUEUE_CLEAR_EXPRESSION";

    public static final String HEADER_CMPP20_PORT = "CMPP20_PORT";
    public static final String HEADER_CMPP30_PORT = "CMPP30_PORT";
    public static final String HEADER_SGIP12_PORT = "SGIP12_PORT";
    public static final String HEADER_SMPP34_PORT = "SMPP34_PORT";
    public static final String HEADER_SMGP30_PORT = "SMGP30_PORT";
    public static final String HEADER_HTTP_PORT = "HTTP_PORT";
    public static final String HEADER_METRIC_PORT = "METRIC_PORT";
    public static final String HEADER_HTTP_PORT2 = "HTTP_PORT2";
    public static final String HEADER_HTTPS_PORT = "HTTPS_PORT";
    public static final String HEADER_TCP_PORT = "TCP_PORT";
    public static final String HEADER_MYSQL_PORT = "MYSQL_PORT";
    public static final String HEADER_EXPORTER_PORT = "EXPORTER_PORT";
    public static final String HEADER_INTERSERVER_HTTP_PORT = "INTERSERVER_HTTP_PORT";

    public static final String HEADER_MAX_CONNECTIONS = "MAX_CONNECTIONS";
    public static final String HEADER_MAX_CONCURRENT_QUERIES = "MAX_CONCURRENT_QUERIES";
    public static final String HEADER_MAX_SERVER_MEMORY_USAGE = "MAX_SERVER_MEMORY_USAGE";
    public static final String HEADER_MAX_MEMORY_USAGE = "MAX_MEMORY_USAGE";

    public static final String HEADER_MEISHENG_PORT = "MEISHENG_PORT";
    public static final String HEADER_HTTP_GBK_PORT = "HTTP_GBK_PORT";
    public static final String HEADER_WJSX_PORT = "WJSX_PORT";
    public static final String HEADER_JDWS_ADDR = "JDWS_ADDR";
    public static final String HEADER_WEB_SERVICE_ADDR = "WEB_SERVICE_ADDR";
    public static final String HEADER_WEB_SERVICE_TASK_URL = "WEB_SERVICE_TASK_URL";

    public static final String HEADER_MO_SCAN_INTERVAL = "MO_SCAN_INTERVAL";
    public static final String HEADER_HTTP_REPORT_INTERVAL = "HTTP_REPORT_INTERVAL";
    public static final String HEADER_CP_REF_INTERVAL = "CP_REF_INTERVAL";
    public static final String HEADER_LOCAL_IP = "LOCAL_IP";
    public static final String HEADER_CMPP20_PACKLOG = "CMPP20_PACKLOG";
    public static final String HEADER_CMPP30_PACKLOG = "CMPP30_PACKLOG";
    public static final String HEADER_SMGP_PACKLOG = "SMGP_PACKLOG";
    public static final String HEADER_SGIP_PACKLOG = "SGIP_PACKLOG";
    public static final String HEADER_SMPP_PACKLOG = "SMPP_PACKLOG";

    public static final String HEADER_HTTP_PACKLOG = "HTTP_PACKLOG";
    public static final String HEADER_HTTP2_PACKLOG = "HTTP2_PACKLOG";
    public static final String HEADER_HTTPS_PACKLOG = "HTTPS_PACKLOG";

    public static final String HEADER_BST_CORE_SIZE = "BST_CORE_SIZE";
    public static final String HEADER_BST_MAX_SIZE = "BST_MAX_SIZE";
    public static final String HEADER_BST_QUEUE_SIZE = "BST_QUEUE_SIZE";
    public static final String HEADER_RPT_QUEUE_SIZE = "RPT_QUEUE_SIZE";
    public static final String HEADER_HTTP_REPORT_PUSH = "HTTP_REPORT_PUSH";
    public static final String HEADER_HTTP2_REPORT_PUSH = "HTTP2_REPORT_PUSH";
    public static final String HEADER_HTTPS_REPORT_PUSH = "HTTPS_REPORT_PUSH";
    public static final String HEADER_SGIP_REPORT_PUSH = "SGIP_REPORT_PUSH";
    public static final String HEADER_ACCT_SERVICE = "ACCT_SERVICE";
    public static final String HEADER_MT_MO_MATCHER_IMPL = "MT_MO_MATCHER_IMPL";
    public static final String HEADER_PARSE_RPT_TYPE = "PARSE_RPT_TYPE";

    public static final String HEADER_CMPP_ISMG_ID = "CMPP_ISMG_ID";
    public static final String HEADER_SMGP_ISMG_ID = "SMGP_ISMG_ID";
    public static final String HEADER_COLLECT_MSI = "COLLECT_MSI";
    public static final String HEADER_SPECIAL_REPORT_CUSTID = "SPECIAL_REPORT_CUSTID";
    public static final String HEADER_UNIQUE_LINK_URL = "UNIQUE_LINK_URL";
    public static final String HEADER_MAX_REPORT_FETCH = "MAX_REPORT_FETCH";
    public static final String HEADER_NO_REPORT_EXECUTE = "NO_REPORT_EXECUTE";
    public static final String HEADER_DECISION_ENABLE = "DECISION_ENABLE";
    public static final String HEADER_PROMETHEUS_PORT = "PROMETHEUS_PORT";

    public static final String HEADER_META_SVR_URL = "META_SVR_URL";
    public static final String HEADER_META_SVR_USR = "META_SVR_USR";
    public static final String HEADER_META_SVR_PASSWD = "META_SVR_PASSWD";
    public static final String HEADER_COLLECTD_PORT = "COLLECTD_PORT";
    public static final String HEADER_CONSOLE_PORT = "CONSOLE_PORT";
    public static final String HEADER_ROCKETMQ_SERV = "ROCKETMQ_SERV";
    public static final String HEADER_JVM_OPS = "JVM_OPS";
    public static final String HEADER_REDIS_CLUSTER_CACHE = "REDIS_CLUSTER_CACHE";
    public static final String HEADER_REDIS_CLUSTER_QUEUE = "REDIS_CLUSTER_QUEUE";
    public static final String HEADER_REDIS_CLUSTER_PFM = "REDIS_CLUSTER_PFM";
    public static final String HEADER_REDIS_CLUSTER_IPNUM = "REDIS_CLUSTER_IPNUM";
    public static final String HEADER_ORACLE_DG_SERV = "ORACLE_DG_SERV";
    public static final String HEADER_PROCESSOR = "PROCESSOR";
    public static final String HEADER_MNP_ALI_URL = "MNP_ALI_URL";
    public static final String HEADER_MNP_ALI_CID = "MNP_ALI_CID";
    public static final String HEADER_MNP_ALI_PASSWD = "MNP_ALI_PASSWD";
    public static final String HEADER_SMS_EXT_PROTO_SWITCH = "SMS_EXT_PROTO_SWITCH";
    public static final String HEADER_SMS_EXT_PROTO_PORT = "SMS_EXT_PROTO_PORT";

    public static final String HEADER_SMS_GATEWAY_SERV_CONTAINER = "SMS_GATEWAY_SERV_CONTAINER";
    public static final String HEADER_SMS_SERVER_CONTAINER = "SMS_SERVER_CONTAINER";
    public static final String HEADER_SMS_SERVER_EXT_CONTAINER = "SMS_SERVER_EXT_CONTAINER";
    public static final String HEADER_SMS_PROCESS_CONTAINER = "SMS_PROCESS_CONTAINER";
    public static final String HEADER_SMS_CLIENT_CONTAINER = "SMS_CLIENT_CONTAINER";
    public static final String HEADER_SMS_BATSAVE_CONTAINER = "SMS_BATSAVE_CONTAINER";
    public static final String HEADER_SMS_STATS_CONTAINER = "SMS_STATS_CONTAINER";
    public static final String HEADER_SMS_SERVER = "SMS_SERVER";
    public static final String HEADER_SMS_SERVER_EXT = "SMS_SERVER_EXT";
    public static final String HEADER_SMS_PROCESS = "SMS_PROCESS";
    public static final String HEADER_SMS_CLIENT = "SMS_CLIENT";
    public static final String HEADER_SMS_BATSAVE = "SMS_BATSAVE";
    public static final String HEADER_SMS_STATS = "SMS_STATS";

    public static final String HEADER_SMS_QUERY_SERV_CONTAINER = "SMS_QUERY_SERV_CONTAINER";
    public static final String HEADER_NGX_CONTAINER = "NGX_CONTAINER";
    public static final String HEADER_SMS_QUERY_CONTAINER = "SMS_QUERY_CONTAINER";
    public static final String HEADER_NGX = "NGX";
    public static final String HEADER_SMS_QUERY = "SMS_QUERY";

    public static final String HEADER_INST_ID_A = "INST_ID_A";
    public static final String HEADER_INST_ID_B = "INST_ID_B";
    public static final String HEADER_WEIGHT_A = "WEIGHT_A";
    public static final String HEADER_WEIGHT_B = "WEIGHT_B";


    public static final String HEADER_TOPIC_NAME = "TOPIC_NAME";
    public static final String HEADER_CONSUME_GROUP = "CONSUME_GROUP";
    public static final String HEADER_DIFF_TOTAL = "DIFF_TOTAL";
    public static final String HEADER_PRODUCE_TOTAL = "PRODUCE_TOTAL";
    public static final String HEADER_PRODUCE_TPS = "PRODUCE_TPS";
    public static final String HEADER_CONSUME_TOTAL = "CONSUME_TOTAL";
    public static final String HEADER_CONSUME_TPS = "CONSUME_TPS";
    public static final String HEADER_ROCKETMQ_INFO_JSON_ARRAY = "ROCKETMQ_INFO_JSON_ARRAY";
    public static final String HEADER_ROCKETMQ_DATA = "ROCKETMQ_DATA";
    public static final String HEADER_RELOAD_TYPE = "RELOAD_TYPE";
    public static final String HEADER_ALL = "ALL";
    public static final String HEADER_META_SERVICE = "META_SERVICE";
    public static final String HEADER_META_ATTR = "META_ATTR";
    public static final String HEADER_META_CMPT = "META_CMPT";
    public static final String HEADER_META_CMPT_ATTR = "META_CMPT_ATTR";
    public static final String HEADER_META_META_INST = "META_META_INST";
    public static final String HEADER_META_TOPO = "META_TOPO";
    public static final String HEADER_META_DEPLOY = "META_DEPLOY";
    public static final String HEADER_META_SERVER_SSH = "META_SERVER_SSH";
    public static final String HEADER_META_CMPT_VERSION = "META_CMPT_VERSION";

    public static final String HEADER_LOG_TYPE = "LOG_TYPE";
    public static final String HEADER_BOOKIE_PORT = "BOOKIE_PORT";
    public static final String HEADER_HTTP_SERVER_PORT = "HTTP_SERVER_PORT";
    public static final String HEADER_META_DATA_PORT = "META_DATA_PORT";
    public static final String HEADER_GRPC_PORT = "GRPC_PORT";
    public static final String HEADER_BROKER_PORT = "BROKER_PORT";
    public static final String HEADER_WEB_PORT = "WEB_PORT";

    public static final String HEADER_DEAL_FLAG = "DEAL_FLAG";
    public static final String HEADER_ALARM_TYPE = "ALARM_TYPE";
    public static final String HEADER_ALARM_INFO = "ALARM_INFO";
    public static final String HEADER_ALARM_TIME = "ALARM_TIME";
    public static final String HEADER_DEAL_TIME = "DEAL_TIME";
    public static final String HEADER_DEAL_ACC_NAME = "DEAL_ACC_NAME";
    public static final String HEADER_ALARM_ID = "ALARM_ID";

    public static final String HEADER_WORKER_PROCESSES = "WORKER_PROCESSES";
    public static final String HEADER_VERTX_PORT = "VERTX_PORT";
}
