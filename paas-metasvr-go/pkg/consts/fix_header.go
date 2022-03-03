package consts

const (
	HEADER_RET_CODE = "RET_CODE"
	HEADER_RET_INFO = "RET_INFO"

	HEADER_LOCAL_PORT  = "LOCAL_PORT"
	HEADER_REMOTE_IP   = "REMOTE_IP"
	HEADER_REMOTE_PORT = "REMOTE_PORT"

	HEADER_UUID        = "UUID"
	HEADER_ID          = "ID"
	HEADER_NAME        = "NAME"
	HEADER_EVENT_TS    = "EVENT_TS"
	HEADER_MSG_BODY    = "MSG_BODY"
	HEADER_SESSION_KEY = "SESSION_KEY"
	HEADER_LOG_KEY     = "LOG_KEY"
	HEADER_SERVICE_ID  = "SERVICE_ID"

	HEADER_TS               = "TS"
	HEADER_CPU              = "CPU"
	HEADER_MEM              = "MEM"
	HEADER_TOTAL_DISK       = "TOTAL_DISK"
	HEADER_USED_DISK        = "USED_DISK"
	HEADER_USER_USED_DISK   = "USER_USED_DISK"
	HEADER_UNUSED_DISK      = "UNUSED_DISK"
	HEADER_INPUT_BANDWIDTH  = "INPUT_BANDWIDTH"
	HEADER_OP_TYPE          = "OP_TYPE"
	HEADER_OUTPUT_BANDWIDTH = "OUTPUT_BANDWIDTH"

	HEADER_USED            = "Used"
	HEADER_TOTAL           = "Total"
	HEADER_AVAILABLE       = "Available"
	HEADER_VALID_TIMESTAMP = "VALID_TIMESTAMP"

	HEADER_QUOTA_CODE = "QUOTA_CODE"
	HEADER_QUOTA_MEAN = "QUOTA_MEAN"
	HEADER_START_TS   = "START_TS"
	HEADER_END_TS     = "END_TS"

	HEADER_VBROKER_ID    = "VBROKER_ID"
	HEADER_VBROKER_IDS   = "VBROKER_IDS"
	HEADER_VBROKER_NAME  = "VBROKER_NAME"
	HEADER_BROKER_ID     = "BROKER_ID"
	HEADER_BROKER_NAME   = "BROKER_NAME"
	HEADER_BROKER_INFO   = "BROKER_INFO"
	HEADER_HOSTNAME      = "HOSTNAME"
	HEADER_INSTANCE_NAME = "INSTANCE_NAME"
	HEADER_IP            = "IP"
	HEADER_MGR_PORT      = "MGR_PORT"
	HEADER_MQ_PWD        = "MQ_PWD"
	HEADER_MQ_USER       = "MQ_USER"
	HEADER_OS_PWD        = "OS_PWD"
	HEADER_OS_USER       = "OS_USER"
	HEADER_PORT          = "PORT"
	HEADER_LISTEN_PORT   = "LISTEN_PORT"
	HEADER_STAT_PORT     = "STAT_PORT"
	HEADER_REC_TIME      = "REC_TIME"
	HEADER_SSH_PORT      = "SSH_PORT"
	HEADER_VHOST         = "VHOST"
	HEADER_VIP           = "VIP"
	HEADER_IS_CLUSTER    = "IS_CLUSTER"
	HEADER_IS_WRITABLE   = "IS_WRITABLE"
	HEADER_ROOT_PWD      = "ROOT_PWD"
	HEADER_IS_RUNNING    = "IS_RUNNING"

	HEADER_QUEUE_ID       = "QUEUE_ID"
	HEADER_QUEUE_NAME     = "QUEUE_NAME"
	HEADER_IS_DURABLE     = "IS_DURABLE"
	HEADER_GLOBAL_ORDERED = "IS_ORDERED"
	HEADER_IS_PRIORITY    = "IS_PRIORITY"
	HEADER_QUEUE_TYPE     = "QUEUE_TYPE"
	HEADER_ERL_COOKIE     = "ERL_COOKIE"

	HEADER_MASTER_ID = "MASTER_ID"
	HEADER_SLAVE_ID  = "SLAVE_ID"

	HEADER_MAIN_TOPIC = "MAIN_TOPIC"
	HEADER_SUB_TOPIC  = "SUB_TOPIC"

	HEADER_GROUP_ID                 = "GROUP_ID"
	HEADER_GROUP_IDS                = "GROUP_IDS"
	HEADER_GROUP_NAME               = "GROUP_NAME"
	HEADER_GROUP_VBROKER_THRESHHOLD = "VBROKER_THRESHHOLD"

	HEADER_MSG_CNT    = "MSG_CNT"
	HEADER_EVENT_CODE = "EVENT_CODE"

	HEADER_PRODUCE_RATE  = "PRODUCE_RATE"
	HEADER_CONSUMER_RATE = "CONSUMER_RATE"

	HEADER_DURABLE         = "DURABLE"
	HEADER_AUTODELETE      = "AUTODELETE"
	HEADER_ARGUMENTS       = "ARGUMENTS"
	HEADER_NODE_NAME       = "NODE_NAME"
	HEADER_MEMARY          = "MEMARY"
	HEADER_PRODUCE_COUNTS  = "PRODUCE_COUNTS"
	HEADER_CONSUMER_COUNTS = "CONSUMER_COUNTS"

	HEADER_VIP_PORT         = "VIP_PORT"
	HEADER_CLUSTER_NAME     = "CLUSTER_NAME"
	HEADER_CONSUMERS        = "CONSUMERS"
	HEADER_QUEUES           = "QUEUES"
	HEADER_CONNECTIONS      = "CONNECTIONS"
	HEADER_CONNECTIONS_RATE = "CONNECTIONS_RATE"
	HEADER_NODEINFO_JSONSTR = "NODEINFO_JSONSTR"

	HEADER_ALARM_CODE = "ALARM_CODE"
	HEADER_ALARM_DESC = "ALARM_DESC"

	HEADER_CLIENT_INFO      = "CLIENT_INFO"
	HEADER_LSNR_ADDR        = "LSNR_ADDR"
	HEADER_CLIENT_TYPE      = "CLIENT_TYPE"
	HEADER_CLNT_IP_AND_PORT = "CLIENT_IP_AND_PORT"
	HEADER_BKR_IP_AND_PORT  = "BROKER_IP_AND_PORT"
	HEADER_CLIENT_PRO_TPS   = "CLIENT_PRO_TPS"
	HEADER_CLIENT_CON_TPS   = "CLIENT_CON_TPS"
	HEADER_T_PRO_MSG_COUNT  = "TOTAL_PRO_MSG_COUNT"
	HEADER_T_PRO_MSG_BYTES  = "TOTAL_PRO_MSG_BYTES"
	HEADER_T_CON_MSG_COUNT  = "TOTAL_CON_MSG_COUNT"
	HEADER_T_CON_MSG_BYTES  = "TOTAL_CON_MSG_BYTES"

	HEADER_LAST_TIMESTAMP = "LAST_TIMESTAMP"
	HEADER_CLIENT_INFOS   = "CLIENT_INFOS"

	HEADER_BIND_TYPE   = "BIND_TYPE"
	HEADER_CONSUMER_ID = "CONSUMER_ID"
	HEADER_SRC_QUEUE   = "SRC_QUEUE"
	HEADER_MAIN_KEY    = "MAINKEY"
	HEADER_SUB_KEY     = "SUBKEY"
	HEADER_REAL_QUEUE  = "REAL_QUEUE"
	HEADER_PERM_QUEUE  = "PERM_QUEUE"

	HEADER_REC_ID      = "REC_ID"
	HEADER_USER_ID     = "USER_ID"
	HEADER_USER_NAME   = "USER_NAME"
	HEADER_LOGIN_PWD   = "LOGIN_PWD"
	HEADER_USER_PWD    = "USER_PWD"
	HEADER_USER_STATUS = "USER_STATUS"
	HEADER_LINE_STATUS = "LINE_STATUS"
	HEADER_REC_STATUS  = "REC_STATUS"
	HEADER_REC_PERSON  = "REC_PERSON"

	HEADER_ROLE_ID   = "ROLE_ID"
	HEADER_ROLE_IDS  = "ROLE_IDS"
	HEADER_ROLE_NAME = "ROLE_NAME"
	HEADER_PARENT_ID = "PARENT_ID"
	HEADER_ROLE_DEC  = "ROLE_DEC"

	HEADER_ACTIVE_COLL_INFO = "ACTIVE_COLL_INFO"

	HEADER_API_TYPE = "API_TYPE"
	HEADER_JSONSTR  = "JSON_STR"

	HEADER_MSG_READY       = "messages_ready"
	HEADER_MSG_UNACK       = "messages_unacknowledged"
	HEADER_QUEEU_TOTAL_MSG = "queue_totals.messages"
	HEADER_MSG_TOTAL       = "msg_total"
	HEADER_QUEUE_TOTALS    = "queue_totals"
	HEADER_MESSAGES        = "messages"

	HEADER_KEY             = "key"
	HEADER_TIMESTAMP       = "TIMESTAMP"
	HEADER_START_TIMESTAMP = "START_TIMESTAMP"
	HEADER_END_TIMESTAMP   = "END_TIMESTAMP"
	HEADER_POS             = "POS"
	HEADER_X               = "x"
	HEADER_Y               = "y"
	HEADER_WIDTH           = "WIDTH"
	HEADER_HEIGHT          = "HEIGHT"
	HEADER_ROW             = "ROW_"
	HEADER_COL             = "COL_"

	HEADER_LOGIN_TIME      = "LOGIN_TIME"
	HEADER_MAGIC_KEY       = "MAGIC_KEY"
	HEADER_COOKIE          = "Cookie"
	HEADER_IS_ADMIN        = "IS_ADMIN"
	HEADER_SESSION_TIMEOUT = "SESSION_TIMEOUT"

	HEADER_BLACK_WHITE_LIST_IP = "IP"
	HEADER_BLACK_WHITE_REMARKS = "REMARKS"
	HEADER_BLACK_WHITE_TYPE    = "TYPE"

	HEADER_ATTR_ID      = "ATTR_ID"
	HEADER_ATTR_NAME    = "ATTR_NAME"
	HEADER_ATTR_NAME_CN = "ATTR_NAME_CN"
	HEADER_AUTO_GEN     = "AUTO_GEN"

	HEADER_INSTANCE_ADDRESS = "INST_ADD"
	HEADER_CMPT_TYPE        = "CMPT_TYPE"
	HEADER_CMPT_ID          = "CMPT_ID"
	HEADER_CMPT_NAME        = "CMPT_NAME"
	HEADER_CMPT_NAME_CN     = "CMPT_NAME_CN"
	HEADER_IS_NEED_DEPLOY   = "IS_NEED_DEPLOY"
	HEADER_SERV_CLAZZ       = "SERV_CLAZZ"
	HEADER_SERV_TYPE        = "SERV_TYPE"
	HEADER_SERV_ID          = "SERV_ID"
	HEADER_META_SERV_ID     = "META_SERV_ID"
	HEADER_SERV_NAME        = "SERV_NAME"
	HEADER_SUB_SERV_TYPE    = "SUB_SERV_TYPE"
	HEADER_NODE_JSON_TYPE   = "NODE_JSON_TYPE"
	HEADER_SUB_CMPT_ID      = "SUB_CMPT_ID"
	HEADER_VERSION          = "VERSION"

	HEADER_INST_ID                                        = "INST_ID"
	HEADER_INST_ID_LIST                                   = "INST_ID_LIST"
	HEADER_SERV_INST_ID                                   = "SERV_INST_ID"
	HEADER_QUEUE_SERV_INST_ID                             = "QUEUE_SERV_INST_ID"
	HEADER_DB_SERV_INST_ID                                = "DB_SERV_INST_ID"
	HEADER_DASHBOARD_PORT                                 = "DASHBOARD_PORT"
	HEADER_CONTROL_PORT                                   = "CONTROL_PORT"
	HEADER_IS_PRODUCT                                     = "IS_PRODUCT"
	HEADER_SSH_ID                                         = "SSH_ID"
	HEADER_SSH_NAME                                       = "SSH_NAME"
	HEADER_SSH_PWD                                        = "SSH_PWD"
	HEADER_OLD_PWD                                        = "OLD_PWD"
	HEADER_IS_DEPLOYED                                    = "IS_DEPLOYED"
	HEADER_PRE_EMBADDED                                   = "PRE_EMBADDED"
	HEADER_CREATE_TIME                                    = "CREATE_TIME"
	HEADER_USER                                           = "USER"
	HEADER_PASSWORD                                       = "PASSWORD"
	HEADER_ATTR_VALUE                                     = "ATTR_VALUE"
	HEADER_INST_ID1                                       = "INST_ID1"
	HEADER_INST_ID2                                       = "INST_ID2"
	HEADER_TOPO_TYPE                                      = "TOPO_TYPE"
	HEADER_COLLECTD                                       = "COLLECTD"
	HEADER_ROCKETMQ_CONSOLE                               = "ROCKETMQ_CONSOLE"
	HEADER_RPC_BIND_PORT                                  = "RPC_BIND_PORT"
	HEADER_WEBSERVER_PORT                                 = "WEBSERVER_PORT"
	HEADER_DURABLE_WAL_WRITE                              = "DURABLE_WAL_WRITE"
	HEADER_ENABLE_LOAD_BALANCING                          = "ENABLE_LOAD_BALANCING"
	HEADER_MAX_CLOCK_SKEW_USEC                            = "MAX_CLOCK_SKEW_USEC"
	HEADER_REPLICATION_FACTOR                             = "REPLICATION_FACTOR"
	HEADER_YB_NUM_SHARDS_PER_TSERVER                      = "YB_NUM_SHARDS_PER_TSERVER"
	HEADER_YSQL_NUM_SHARDS_PER_TSERVER                    = "YSQL_NUM_SHARDS_PER_TSERVER"
	HEADER_PLACEMENT_CLOUD                                = "PLACEMENT_CLOUD"
	HEADER_PLACEMENT_ZONE                                 = "PLACEMENT_ZONE"
	HEADER_PLACEMENT_REGION                               = "PLACEMENT_REGION"
	HEADER_CDC_WAL_RETENTION_TIME_SECS                    = "CDC_WAL_RETENTION_TIME_SECS"
	HEADER_PGSQL_PROXY_BIND_PORT                          = "PGSQL_PROXY_BIND_PORT"
	HEADER_PGSQL_PROXY_WEBSERVER_PORT                     = "PGSQL_PROXY_WEBSERVER_PORT"
	HEADER_CQL_PROXY_BIND_PORT                            = "CQL_PROXY_BIND_PORT"
	HEADER_CQL_PROXY_WEBSERVER_PORT                       = "CQL_PROXY_WEBSERVER_PORT"
	HEADER_YSQL_MAX_CONNECTIONS                           = "YSQL_MAX_CONNECTIONS"
	HEADER_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC = "ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC"
	HEADER_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH   = "ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH"
	HEADER_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO        = "ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO"
	HEADER_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC       = "TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC"
	HEADER_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC      = "REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC"

	HEADER_REDIS_SERV_CLUSTER_CONTAINER = "REDIS_SERV_CLUSTER_CONTAINER"
	HEADER_REDIS_SERV_MS_CONTAINER      = "REDIS_SERV_MS_CONTAINER"
	HEADER_HOST_CONTAINER               = "HOST_CONTAINER"
	HEADER_REDIS_NODE_CONTAINER         = "REDIS_NODE_CONTAINER"
	HEADER_HOST_NODE_CONTAINER          = "HOST_NODE_CONTAINER"
	HEADER_REDIS_PROXY_CONTAINER        = "REDIS_PROXY_CONTAINER"
	HEADER_REDIS_NODE                   = "REDIS_NODE"
	HEADER_HOST_NODE                    = "HOST_NODE"
	HEADER_REDIS_PROXY                  = "REDIS_PROXY"
	HEADER_MAX_CONN                     = "MAX_CONN"
	HEADER_MAX_MEMORY                   = "MAX_MEMORY"
	HEADER_PROXY_THREADS                = "PROXY_THREADS"
	HEADER_NODE_CONN_POOL_SIZE          = "NODE_CONN_POOL_SIZE"
	HEADER_CLIENT_PORT                  = "CLIENT_PORT"
	HEADER_ZK_CLIENT_PORT1              = "ZK_CLIENT_PORT1"
	HEADER_ZK_CLIENT_PORT2              = "ZK_CLIENT_PORT2"
	HEADER_ADMIN_PORT                   = "ADMIN_PORT"
	HEADER_PEER_PORT                    = "PEER_PORT"
	HEADER_FILE_ID                      = "FILE_ID"
	HEADER_HOST_ID                      = "HOST_ID"
	HEADER_FILE_NAME                    = "FILE_NAME"
	HEADER_FILE_DIR                     = "FILE_DIR"
	HEADER_POS_X                        = "POS_X"
	HEADER_POS_Y                        = "POS_Y"
	HEADER_IP_ADDRESS                   = "IP_ADDRESS"

	HEADER_APISIX_CONTAINER    = "APISIX_CONTAINER"
	HEADER_APISIX_SERVER       = "APISIX_SERVER"
	HEADER_ETCD_CONTAINER      = "ETCD_CONTAINER"
	HEADER_ETCD                = "ETCD"
	HEADER_CLIENT_URLS_PORT    = "CLIENT_URLS_PORT"
	HEADER_PEER_URLS_PORT      = "PEER_URLS_PORT"
	HEADER_ETCD_CLUSTER_ADDR   = "ETCD_CLUSTER_ADDR"
	HEADER_ETCD_IP             = "ETCD_IP"
	HEADER_CLUSTER_TOKEN       = "CLUSTER_TOKEN"
	HEADER_ETCD_ADDR_LIST      = "ETCD_ADDR_LIST"
	HEADER_SSL_PORT            = "SSL_PORT"
	HEADER_INST_ID_MD5         = "INST_ID_MD5"
	ETCD_PRODUCT_ENV_MIN_NODES = 3

	HEADER_BROKER_IP                  = "BROKER_IP"
	HEADER_ROCKETMQ_SERV_CONTAINER    = "ROCKETMQ_SERV_CONTAINER"
	HEADER_ROCKETMQ_VBROKER_CONTAINER = "ROCKETMQ_VBROKER_CONTAINER"
	HEADER_ROCKETMQ_NAMESRV_CONTAINER = "ROCKETMQ_NAMESRV_CONTAINER"
	HEADER_ROCKETMQ_VBROKER           = "ROCKETMQ_VBROKER"
	HEADER_ROCKETMQ_BROKER            = "ROCKETMQ_BROKER"
	HEADER_ROCKETMQ_NAMESRV           = "ROCKETMQ_NAMESRV"

	HEADER_TIDB_SERV_CONTAINER   = "TIDB_SERV_CONTAINER"
	HEADER_PD_SERVER_CONTAINER   = "PD_SERVER_CONTAINER"
	HEADER_TIKV_SERVER_CONTAINER = "TIKV_SERVER_CONTAINER"
	HEADER_TIDB_SERVER_CONTAINER = "TIDB_SERVER_CONTAINER"
	HEADER_TIDB_SERVER           = "TIDB_SERVER"
	HEADER_TIKV_SERVER           = "TIKV_SERVER"
	HEADER_PD_SERVER             = "PD_SERVER"
	HEADER_DASHBOARD_PROXY       = "DASHBOARD_PROXY"

	HEADER_PULSAR_SERV_CONTAINER       = "PULSAR_SERV_CONTAINER"
	HEADER_PULSAR_BROKER_CONTAINER     = "PULSAR_BROKER_CONTAINER"
	HEADER_PULSAR_BOOKKEEPER_CONTAINER = "PULSAR_BOOKKEEPER_CONTAINER"
	HEADER_ZOOKEEPER_CONTAINER         = "ZOOKEEPER_CONTAINER"
	HEADER_PULSAR_MANAGER              = "PULSAR_MANAGER"
	HEADER_PULSAR_BROKER               = "PULSAR_BROKER"
	HEADER_PULSAR_BOOKKEEPER           = "PULSAR_BOOKKEEPER"
	HEADER_ZOOKEEPER                   = "ZOOKEEPER"

	HEADER_PULSAR_MGR_PORT = "PULSAR_MGR_PORT"
	HEADER_HERDDB_PORT     = "HERDDB_PORT"

	HEADER_CLICKHOUSE_SERV_CONTAINER     = "CLICKHOUSE_SERV_CONTAINER"
	HEADER_CLICKHOUSE_REPLICAS_CONTAINER = "CLICKHOUSE_REPLICAS_CONTAINER"
	HEADER_CLICKHOUSE_REPLICAS           = "CLICKHOUSE_REPLICAS"
	HEADER_CLICKHOUSE_SERVER             = "CLICKHOUSE_SERVER"
	HEADER_CLICKHOUSE_EXPORTER           = "CLICKHOUSE_EXPORTER"
	HEADER_INTERNAL_REPLICATION          = "INTERNAL_REPLICATION"

	HEADER_VOLTDB_SERV_CONTAINER = "VOLTDB_SERV_CONTAINER"
	HEADER_VOLTDB_CONTAINER      = "VOLTDB_CONTAINER"
	HEADER_VOLTDB_SERVER         = "VOLTDB_SERVER"
	HEADER_VOLT_CLIENT_PORT      = "VOLT_CLIENT_PORT"
	HEADER_VOLT_ADMIN_PORT       = "VOLT_ADMIN_PORT"
	HEADER_VOLT_WEB_PORT         = "VOLT_WEB_PORT"
	HEADER_VOLT_INTERNAL_PORT    = "VOLT_INTERNAL_PORT"
	HEADER_VOLT_REPLI_PORT       = "VOLT_REPLI_PORT"
	HEADER_VOLT_ZK_PORT          = "VOLT_ZK_PORT"
	HEADER_SITES_PER_HOST        = "SITES_PER_HOST"
	HEADER_KFACTOR               = "KFACTOR"
	HEADER_MEM_LIMIT             = "MEM_LIMIT"
	HEADER_HEARTBEAT_TIMEOUT     = "HEARTBEAT_TIMEOUT"
	HEADER_TEMPTABLES_MAXSIZE    = "TEMPTABLES_MAXSIZE"
	HEADER_ELASTIC_DURATION      = "ELASTIC_DURATION"
	HEADER_ELASTIC_THROUGHPUT    = "ELASTIC_THROUGHPUT"
	HEADER_QUERY_TIMEOUT         = "QUERY_TIMEOUT"
	HEADER_PROCEDURE_LOGINFO     = "PROCEDURE_LOGINFO"
	HEADER_MEM_ALERT             = "MEM_ALERT"

	HEADER_YB_MASTER_CONTAINER  = "YB_MASTER_CONTAINER"
	HEADER_YB_TSERVER_CONTAINER = "YB_TSERVER_CONTAINER"
	HEADER_YB_MASTER            = "YB_MASTER"
	HEADER_YB_TSERVER           = "YB_TSERVER"

	HEADER_PROMETHEUS = "PROMETHEUS"
	HEADER_GRAFANA    = "GRAFANA"

	HEADER_TIDB_JSON             = "TIDB_JSON"
	HEADER_TOPO_JSON             = "TOPO_JSON"
	HEADER_NODE_JSON             = "NODE_JSON"
	HEADER_DB_SERV_CONTAINER     = "DB_SERV_CONTAINER"
	HEADER_DB_SVC_CONTAINER_ID   = "DB_SVC_CONTAINER_ID"
	HEADER_DB_SVC_CONTAINER_NAME = "DB_SVC_CONTAINER_NAME"

	HEADER_DB_TIDB_CONTAINER   = "DB_TIDB_CONTAINER"
	HEADER_TIDB_CONTAINER_ID   = "TIDB_CONTAINER_ID"
	HEADER_TIDB_CONTAINER_NAME = "TIDB_CONTAINER_NAME"
	HEADER_DB_TIDB             = "DB_TIDB"
	HEADER_TIDB_ID             = "TIDB_ID"
	HEADER_TIDB_NAME           = "TIDB_NAME"

	HEADER_DB_TIKV_CONTAINER   = "DB_TIKV_CONTAINER"
	HEADER_TIKV_CONTAINER_ID   = "TIKV_CONTAINER_ID"
	HEADER_TIKV_CONTAINER_NAME = "TIKV_CONTAINER_NAME"
	HEADER_DB_TIKV             = "DB_TIKV"
	HEADER_TIKV_ID             = "TIKV_ID"
	HEADER_TIKV_NAME           = "TIKV_NAME"

	HEADER_DB_PD_CONTAINER   = "DB_PD_CONTAINER"
	HEADER_PD_CONTAINER_ID   = "PD_CONTAINER_ID"
	HEADER_PD_CONTAINER_NAME = "PD_CONTAINER_NAME"
	HEADER_DB_PD             = "DB_PD"
	HEADER_PD_ID             = "PD_ID"
	HEADER_PD_NAME           = "PD_NAME"

	HEADER_TDENGINE_SERV_CONTAINER = "TDENGINE_SERV_CONTAINER"
	HEADER_ARBITRATOR_CONTAINER    = "ARBITRATOR_CONTAINER"
	HEADER_DNODE_CONTAINER         = "DNODE_CONTAINER"
	HEADER_TD_ARBITRATOR           = "TD_ARBITRATOR"
	HEADER_TD_DNODE                = "TD_DNODE"

	HEADER_ORACLE_DG_SERV_CONTAINER = "ORACLE_DG_SERV_CONTAINER"
	HEADER_DG_CONTAINER             = "DG_CONTAINER"
	HEADER_ORCL_INSTANCE            = "ORCL_INSTANCE"

	HEADER_REDIS_HA_CLUSTER_CONTAINER = "REDIS_HA_CLUSTER_CONTAINER"
	HEADER_HA_CONTAINER               = "HA_CONTAINER"

	HEADER_DB_COLLECTD   = "DB_COLLECTD"
	HEADER_COLLECTD_ID   = "COLLECTD_ID"
	HEADER_COLLECTD_NAME = "COLLECTD_NAME"

	HEADER_ATTRIBUTES  = "ATTRS"
	HEADER_DEPLOY_FLAG = "DEPLOY_FLAG"

	HEADER_SQL_STR     = "SQL_STR"
	HEADER_SCHEMA_NAME = "SCHEMA_NAME"

	HEADER_PAGE_SIZE   = "pageSize"
	HEADER_PAGE_NUMBER = "pageNumber"

	HEADER_CLUSTER_ID    = "CLUSTER_ID"
	HEADER_NEW_MASTER_ID = "NEW_MASTER_ID"

	HEADER_SEQ_NAME   = "SEQ_NAME"
	HEADER_SEQ_STEP   = "SEQ_STEP"
	HEADER_START      = "START"
	HEADER_END        = "END"
	HEADER_CURR_VALUE = "CURR_VALUE"

	HEADER_CACHE_PROXY_ID         = "CACHE_PROXY_ID"
	HEADER_CACHE_PROXY_NAME       = "CACHE_PROXY_NAME"
	HEADER_ACCESS_CLIENT_CONNS    = "ACCESS_CLIENT_CONNS"
	HEADER_ACCESS_PROCESS_AVTIME  = "ACCESS_PROCESS_AVTIME"
	HEADER_ACCESS_REDIS_CONNS     = "ACCESS_REDIS_CONNS"
	HEADER_ACCESS_PROCESS_MAXTIME = "ACCESS_PROCESS_MAXTIME"
	HEADER_ACCESS_REQUEST_EXCEPTS = "ACCESS_REQUEST_EXCEPTS"
	HEADER_ACCESS_REQUEST_TPS     = "ACCESS_REQUEST_TPS"

	HEADER_CACHE_NODE_ID   = "CACHE_NODE_ID"
	HEADER_CACHE_NODE_NAME = "CACHE_NODE_NAME"
	HEADER_DB_SIZE         = "DB_SIZE"
	HEADER_MEMORY_TOTAL    = "MEMORY_TOTAL"
	HEADER_PROCESS_TPS     = "PROCESS_TPS"
	HEADER_MEMORY_USED     = "MEMORY_USED"
	HEADER_LINK_STATUS     = "LINK_STATUS"

	HEADER_SERVER_ID   = "SERVER_ID"
	HEADER_SERVER_IP   = "SERVER_IP"
	HEADER_SERVER_NAME = "SERVER_NAME"
	HEADER_SSH_LIST    = "SSH_LIST"

	HEADER_TIDB_QPS                = "QPS"
	HEADER_TIDB_CONNECTION_COUNT   = "CONNECTION_COUNT"
	HEADER_TIDB_STATEMENT_COUNT    = "STATEMENT_COUNT"
	HEADER_TIDB_QUERY_DURATION_99P = "QUERY_DURATION_99PERC"

	HEADER_PD_STORAGE_CAPACITY                 = "STORAGE_CAPACITY"
	HEADER_PD_CURRENT_STORAGE_SIZE             = "CURRENT_STORAGE_SIZE"
	HEADER_PD_LEADER_BALANCE_RATIO             = "LEADER_BALANCE_RATIO"
	HEADER_PD_REGION_BALANCE_RATIO             = "REGION_BALANCE_RATIO"
	HEADER_PD_REGIONS                          = "REGIONS"
	HEADER_PD_COMPLETE_DURATION_SECONDS_99PENC = "COMPLETE_DURATION_SECONDS_99PENC"

	HEADER_TIKV_LEADER_COUNT                = "LEADER_COUNT"
	HEADER_TIKV_REGION_COUNT                = "REGION_COUNT"
	HEADER_TIKV_SCHEEDULER_COMMAND_DURATION = "SCHEEDULER_COMMAND_DURATION"

	HEADER_ACC_ID    = "ACC_ID"
	HEADER_ACC_NAME  = "ACC_NAME"
	HEADER_PHONE_NUM = "PHONE_NUM"
	HEADER_MAIL      = "MAIL"
	HEADER_PASSWD    = "PASSWD"

	HEADER_JVM_INFO_JSON_ARRAY             = "JVM_INFO_JSON_ARRAY"
	HEADER_JVM_DATA                        = "JVM_DATA"
	HEADER_JAVA_VERSION                    = "JAVA_VERSION"
	HEADER_GC_YOUNG_GC_COUNT               = "GC_YOUNG_GC_COUNT"
	HEADER_GC_YOUNG_GC_TIME                = "GC_YOUNG_GC_TIME"
	HEADER_GC_FULL_GC_COUNT                = "GC_FULL_GC_COUNT"
	HEADER_GC_FULL_GC_TIME                 = "GC_FULL_GC_TIME"
	HEADER_THREAD_DAEMON_THREAD_COUNT      = "THREAD_DAEMON_THREAD_COUNT"
	HEADER_THREAD_COUNT                    = "THREAD_THREAD_COUNT"
	HEADER_THREAD_PEEK_THREAD_COUNT        = "THREAD_PEEK_THREAD_COUNT"
	HEADER_THREAD_DEAD_LOCKED_THREAD_COUNT = "THREAD_DEADLOCKED_THREAD_COUNT"
	HEADER_MEM_EDEN_INIT                   = "MEM_EDEN_INIT"
	HEADER_MEM_EDEN_USED                   = "MEM_EDEN_USED"
	HEADER_MEM_EDEN_COMMITTED              = "MEM_EDEN_COMMITTED"
	HEADER_MEM_EDEN_MAX                    = "MEM_EDEN_MAX"
	HEADER_MEM_EDEN_USEDPERCENT            = "MEM_EDEN_USEDPERCENT"
	HEADER_MEM_SURVIVOR_INIT               = "MEM_SURVIVOR_INIT"
	HEADER_MEM_SURVIVOR_USED               = "MEM_SURVIVOR_USED"
	HEADER_MEM_SURVIVOR_COMMITTED          = "MEM_SURVIVOR_COMMITTED"
	HEADER_MEM_SURVIVOR_MAX                = "MEM_SURVIVOR_MAX"
	HEADER_MEM_SURVIVOR_USEDPERCENT        = "MEM_SURVIVOR_USEDPERCENT"
	HEADER_MEM_OLD_INIT                    = "MEM_OLD_INIT"
	HEADER_MEM_OLD_USED                    = "MEM_OLD_USED"
	HEADER_MEM_OLD_COMMITTED               = "MEM_OLD_COMMITTED"
	HEADER_MEM_OLD_MAX                     = "MEM_OLD_MAX"
	HEADER_MEM_OLD_USEDPERCENT             = "MEM_OLD_USEDPERCENT"
	HEADER_MEM_PERM_INIT                   = "MEM_PERM_INIT"
	HEADER_MEM_PERM_USED                   = "MEM_PERM_USED"
	HEADER_MEM_PERM_COMMITTED              = "MEM_PERM_COMMITTED"
	HEADER_MEM_PERM_MAX                    = "MEM_PERM_MAX"
	HEADER_MEM_PERM_USEDPERCENT            = "MEM_PERM_USEDPERCENT"
	HEADER_MEM_CODE_INIT                   = "MEM_CODE_INIT"
	HEADER_MEM_CODE_USED                   = "MEM_CODE_USED"
	HEADER_MEM_CODE_COMMITTED              = "MEM_CODE_COMMITTED"
	HEADER_MEM_CODE_MAX                    = "MEM_CODE_MAX"
	HEADER_MEM_CODE_USEDPERCENT            = "MEM_CODE_USEDPERCENT"
	HEADER_MEM_HEAP_INIT                   = "MEM_HEAP_INIT"
	HEADER_MEM_HEAP_USED                   = "MEM_HEAP_USED"
	HEADER_MEM_HEAP_COMMITTED              = "MEM_HEAP_COMMITTED"
	HEADER_MEM_HEAP_MAX                    = "MEM_HEAP_MAX"
	HEADER_MEM_HEAP_USEDPERCENT            = "MEM_HEAP_USEDPERCENT"
	HEADER_MEM_NOHEAP_INIT                 = "MEM_NOHEAP_INIT"
	HEADER_MEM_NOHEAP_USED                 = "MEM_NOHEAP_USED"
	HEADER_MEM_NOHEAP_COMMITTED            = "MEM_NOHEAP_COMMITTED"
	HEADER_MEM_NOHEAP_MAX                  = "MEM_NOHEAP_MAX"
	HEADER_MEM_NOHEAP_USEDPERCENT          = "MEM_NOHEAP_USEDPERCENT"
	HEADER_STATUS                          = "STATUS"
	HEADER_UPDATE_TIME                     = "UPDATE_TIME"
	HEADER_PSEUDO_DEPLOY_FLAG              = "PSEUDO_DEPLOY_FLAG"

	HEADER_REDIS_INFO_JSON_ARRAY     = "REDIS_INFO_JSON_ARRAY"
	HEADER_HOST_INFO_JSON_ARRAY      = "HOST_INFO_JSON_ARRAY"
	HEADER_REDIS_DATA                = "REIDS_DATA"
	HEADER_HOST_DATA                 = "HOST_DATA"
	HEADER_ROLE                      = "ROLE"
	HEADER_CONNECTED_CLIENTS         = "CONNECTED_CLIENTS"
	HEADER_USED_MEMORY               = "USED_MEMORY"
	HEADER_MAXMEMORY                 = "MAXMEMORY"
	HEADER_INSTANTANEOUS_OPS_PER_SEC = "INSTANTANEOUS_OPS_PER_SEC"
	HEADER_INSTANTANEOUS_INPUT_KBPS  = "INSTANTANEOUS_INPUT_KBPS"
	HEADER_INSTANTANEOUS_OUTPUT_KBPS = "INSTANTANEOUS_OUTPUT_KBPS"
	HEADER_SYNC_FULL                 = "SYNC_FULL"
	HEADER_EXPIRED_KEYS              = "EXPIRED_KEYS"
	HEADER_EVICTED_KEYS              = "EVICTED_KEYS"
	HEADER_KEYSPACE_HITS             = "KEYSPACE_HITS"
	HEADER_KEYSPACE_MISSES           = "KEYSPACE_MISSES"
	HEADER_USED_CPU_SYS              = "USED_CPU_SYS"
	HEADER_USED_CPU_USER             = "USED_CPU_USER"
	HEADER_CPU_IDLE                  = "USED_CPU_IDLE"

	HEADER_DG_NAME          = "DG_NAME"
	HEADER_DECRYPT          = "DECRYPT"
	HEADER_DB_TYPE          = "DB_TYPE"
	HEADER_ACTIVE_DB_TYPE   = "ACTIVE_DB_TYPE"
	HEADER_DB_SOURCE_MODEL  = "DB_SOURCE_MODEL"
	HEADER_MASTER_JDBC_URL  = "MASTER_JDBC_URL"
	HEADER_MASTER_USER_NAME = "MASTER_USER_NAME"
	HEADER_MASTER_PASSWORD  = "MASTER_PASSWORD"
	HEADER_BACKUP_JDBC_URL  = "BACKUP_JDBC_URL"
	HEADER_BACKUP_USER_NAME = "BACKUP_USER_NAME"
	HEADER_BACKUP_PASSWORD  = "BACKUP_PASSWORD"
	HEADER_MIN_IDLE         = "MIN_IDLE"
	HEADER_MAX_ACTIVE       = "MAX_ACTIVE"
	HEADER_VALIDATION_QUERY = "VALIDATION_QUERY"
	HEADER_CONN_TIMEOUT     = "CONN_TIMEOUT"

	HEADER_NODE_TYPE           = "NODE_TYPE"
	HEADER_ORA_LSNR_PORT       = "ORA_LSNR_PORT"
	HEADER_DB_USER             = "DB_USER"
	HEADER_DB_PASSWD           = "DB_PASSWD"
	HEADER_DB_NAME             = "DB_NAME"
	HEADER_SERV_CONTAINER_NAME = "SERV_CONTAINER_NAME"
	HEADER_WEIGHT              = "WEIGHT"

	HEADER_CLIENT_NAME          = "CLIENT_NAME"
	HEADER_SLAVE_MIN_IDLE_SIZE  = "SLAVE_MIN_IDLE_SIZE"
	HEADER_SLAVE_POOL_SIZE      = "SLAVE_POOL_SIZE"
	HEADER_MASTER_MIN_IDLE_SIZE = "MASTER_MIN_IDLE_SIZE"
	HEADER_MASTER_POOL_SIZE     = "MASTER_POOL_SIZE"
	HEADER_READ_MODE            = "READ_MODE"
	HEADER_SERVER_MODE          = "SERVER_MODE"

	HEADER_SYSTEM_PROPERTY                       = "SYSTEM_PROPERTY"
	HEADER_SERVICE_IMPL                          = "SERVICE_IMPL"
	HEADER_MAX_SMS_TASK_PROC                     = "MAX_SMS_TASK_PROC"
	HEADER_BATCH_SAVE_PROCESS                    = "BATCH_SAVE_PROCESS"
	HEADER_BATCHSAVE_PROCESS                     = "BATCHSAVE_PROCESS"
	HEADER_WEB_CONSOLE_PORT                      = "WEB_CONSOLE_PORT"
	HEADER_THREE_CHANNEL_LAST_UPDATE_REPORT_TIME = "THREE_CHANNEL_LAST_UPDATE_REPORT_TIME"
	HEADER_INTERNAL_PORT                         = "INTERNAL_PORT"
	HEADER_SW_REF_INTERVAL                       = "SW_REF_INTERVAL"
	HEADER_WARN_SVC_URL                          = "WARN_SVC_URL"
	HEADER_DAT_CORE_SIZE                         = "DAT_CORE_SIZE"
	HEADER_DAT_MAX_SIZE                          = "DAT_MAX_SIZE"
	HEADER_DAT_QUEUE_SIZE                        = "DAT_QUEUE_SIZE"
	HEADER_ALT_CORE_SIZE                         = "ALT_CORE_SIZE"
	HEADER_ALT_MAX_SIZE                          = "ALT_MAX_SIZE"
	HEADER_ALT_QUEUE_SIZE                        = "ALT_QUEUE_SIZE"
	HEADER_TST_CORE_SIZE                         = "TST_CORE_SIZE"
	HEADER_TST_MAX_SIZE                          = "TST_MAX_SIZE"
	HEADER_TST_QUEUE_SIZE                        = "TST_QUEUE_SIZE"
	HEADER_STS_CORE_SIZE                         = "STS_CORE_SIZE"
	HEADER_STS_MAX_SIZE                          = "STS_MAX_SIZE"
	HEADER_STS_QUEUE_SIZE                        = "STS_QUEUE_SIZE"
	HEADER_SAMPLING_SWITCH                       = "SAMPLING_SWITCH"
	HEADER_CRON_EXPRESSION                       = "CRON_EXPRESSION"
	HEADER_DB_INST_ID                            = "DB_INST_ID"
	HEADER_ES_SERVER                             = "ES_SERVER"
	HEADER_ES_MT_SERVER                          = "ES_MT_SERVER"
	HEADER_MT_QUEUE_CLEAR_EXPRESSION             = "MT_QUEUE_CLEAR_EXPRESSION"

	HEADER_CMPP20_PORT           = "CMPP20_PORT"
	HEADER_CMPP30_PORT           = "CMPP30_PORT"
	HEADER_SGIP12_PORT           = "SGIP12_PORT"
	HEADER_SMPP34_PORT           = "SMPP34_PORT"
	HEADER_SMGP30_PORT           = "SMGP30_PORT"
	HEADER_HTTP_PORT             = "HTTP_PORT"
	HEADER_METRIC_PORT           = "METRIC_PORT"
	HEADER_HTTP_PORT2            = "HTTP_PORT2"
	HEADER_HTTPS_PORT            = "HTTPS_PORT"
	HEADER_TCP_PORT              = "TCP_PORT"
	HEADER_MYSQL_PORT            = "MYSQL_PORT"
	HEADER_EXPORTER_PORT         = "EXPORTER_PORT"
	HEADER_INTERSERVER_HTTP_PORT = "INTERSERVER_HTTP_PORT"

	HEADER_MAX_CONNECTIONS         = "MAX_CONNECTIONS"
	HEADER_MAX_CONCURRENT_QUERIES  = "MAX_CONCURRENT_QUERIES"
	HEADER_MAX_SERVER_MEMORY_USAGE = "MAX_SERVER_MEMORY_USAGE"
	HEADER_MAX_MEMORY_USAGE        = "MAX_MEMORY_USAGE"

	HEADER_MEISHENG_PORT        = "MEISHENG_PORT"
	HEADER_HTTP_GBK_PORT        = "HTTP_GBK_PORT"
	HEADER_WJSX_PORT            = "WJSX_PORT"
	HEADER_JDWS_ADDR            = "JDWS_ADDR"
	HEADER_WEB_SERVICE_ADDR     = "WEB_SERVICE_ADDR"
	HEADER_WEB_SERVICE_TASK_URL = "WEB_SERVICE_TASK_URL"

	HEADER_MO_SCAN_INTERVAL     = "MO_SCAN_INTERVAL"
	HEADER_HTTP_REPORT_INTERVAL = "HTTP_REPORT_INTERVAL"
	HEADER_CP_REF_INTERVAL      = "CP_REF_INTERVAL"
	HEADER_LOCAL_IP             = "LOCAL_IP"
	HEADER_CMPP20_PACKLOG       = "CMPP20_PACKLOG"
	HEADER_CMPP30_PACKLOG       = "CMPP30_PACKLOG"
	HEADER_SMGP_PACKLOG         = "SMGP_PACKLOG"
	HEADER_SGIP_PACKLOG         = "SGIP_PACKLOG"
	HEADER_SMPP_PACKLOG         = "SMPP_PACKLOG"

	HEADER_HTTP_PACKLOG  = "HTTP_PACKLOG"
	HEADER_HTTP2_PACKLOG = "HTTP2_PACKLOG"
	HEADER_HTTPS_PACKLOG = "HTTPS_PACKLOG"

	HEADER_BST_CORE_SIZE      = "BST_CORE_SIZE"
	HEADER_BST_MAX_SIZE       = "BST_MAX_SIZE"
	HEADER_BST_QUEUE_SIZE     = "BST_QUEUE_SIZE"
	HEADER_RPT_QUEUE_SIZE     = "RPT_QUEUE_SIZE"
	HEADER_HTTP_REPORT_PUSH   = "HTTP_REPORT_PUSH"
	HEADER_HTTP2_REPORT_PUSH  = "HTTP2_REPORT_PUSH"
	HEADER_HTTPS_REPORT_PUSH  = "HTTPS_REPORT_PUSH"
	HEADER_SGIP_REPORT_PUSH   = "SGIP_REPORT_PUSH"
	HEADER_ACCT_SERVICE       = "ACCT_SERVICE"
	HEADER_MT_MO_MATCHER_IMPL = "MT_MO_MATCHER_IMPL"
	HEADER_PARSE_RPT_TYPE     = "PARSE_RPT_TYPE"

	HEADER_CMPP_ISMG_ID          = "CMPP_ISMG_ID"
	HEADER_SMGP_ISMG_ID          = "SMGP_ISMG_ID"
	HEADER_COLLECT_MSI           = "COLLECT_MSI"
	HEADER_SPECIAL_REPORT_CUSTID = "SPECIAL_REPORT_CUSTID"
	HEADER_UNIQUE_LINK_URL       = "UNIQUE_LINK_URL"
	HEADER_MAX_REPORT_FETCH      = "MAX_REPORT_FETCH"
	HEADER_NO_REPORT_EXECUTE     = "NO_REPORT_EXECUTE"
	HEADER_DECISION_ENABLE       = "DECISION_ENABLE"
	HEADER_PROMETHEUS_PORT       = "PROMETHEUS_PORT"

	HEADER_META_SVR_URL         = "META_SVR_URL"
	HEADER_META_SVR_USR         = "META_SVR_USR"
	HEADER_META_SVR_PASSWD      = "META_SVR_PASSWD"
	HEADER_COLLECTD_PORT        = "COLLECTD_PORT"
	HEADER_CONSOLE_PORT         = "CONSOLE_PORT"
	HEADER_ROCKETMQ_SERV        = "ROCKETMQ_SERV"
	HEADER_JVM_OPS              = "JVM_OPS"
	HEADER_REDIS_CLUSTER_CACHE  = "REDIS_CLUSTER_CACHE"
	HEADER_REDIS_CLUSTER_QUEUE  = "REDIS_CLUSTER_QUEUE"
	HEADER_REDIS_CLUSTER_PFM    = "REDIS_CLUSTER_PFM"
	HEADER_REDIS_CLUSTER_IPNUM  = "REDIS_CLUSTER_IPNUM"
	HEADER_ORACLE_DG_SERV       = "ORACLE_DG_SERV"
	HEADER_PROCESSOR            = "PROCESSOR"
	HEADER_MNP_ALI_URL          = "MNP_ALI_URL"
	HEADER_MNP_ALI_CID          = "MNP_ALI_CID"
	HEADER_MNP_ALI_PASSWD       = "MNP_ALI_PASSWD"
	HEADER_SMS_EXT_PROTO_SWITCH = "SMS_EXT_PROTO_SWITCH"
	HEADER_SMS_EXT_PROTO_PORT   = "SMS_EXT_PROTO_PORT"

	HEADER_SMS_GATEWAY_SERV_CONTAINER = "SMS_GATEWAY_SERV_CONTAINER"
	HEADER_SMS_SERVER_CONTAINER       = "SMS_SERVER_CONTAINER"
	HEADER_SMS_SERVER_EXT_CONTAINER   = "SMS_SERVER_EXT_CONTAINER"
	HEADER_SMS_PROCESS_CONTAINER      = "SMS_PROCESS_CONTAINER"
	HEADER_SMS_CLIENT_CONTAINER       = "SMS_CLIENT_CONTAINER"
	HEADER_SMS_BATSAVE_CONTAINER      = "SMS_BATSAVE_CONTAINER"
	HEADER_SMS_STATS_CONTAINER        = "SMS_STATS_CONTAINER"
	HEADER_SMS_SERVER                 = "SMS_SERVER"
	HEADER_SMS_SERVER_EXT             = "SMS_SERVER_EXT"
	HEADER_SMS_PROCESS                = "SMS_PROCESS"
	HEADER_SMS_CLIENT                 = "SMS_CLIENT"
	HEADER_SMS_BATSAVE                = "SMS_BATSAVE"
	HEADER_SMS_STATS                  = "SMS_STATS"

	HEADER_SMS_QUERY_SERV_CONTAINER = "SMS_QUERY_SERV_CONTAINER"
	HEADER_NGX_CONTAINER            = "NGX_CONTAINER"
	HEADER_SMS_QUERY_CONTAINER      = "SMS_QUERY_CONTAINER"
	HEADER_NGX                      = "NGX"
	HEADER_SMS_QUERY                = "SMS_QUERY"

	HEADER_INST_ID_A = "INST_ID_A"
	HEADER_INST_ID_B = "INST_ID_B"
	HEADER_WEIGHT_A  = "WEIGHT_A"
	HEADER_WEIGHT_B  = "WEIGHT_B"

	HEADER_TOPIC_NAME               = "TOPIC_NAME"
	HEADER_CONSUME_GROUP            = "CONSUME_GROUP"
	HEADER_DIFF_TOTAL               = "DIFF_TOTAL"
	HEADER_PRODUCE_TOTAL            = "PRODUCE_TOTAL"
	HEADER_PRODUCE_TPS              = "PRODUCE_TPS"
	HEADER_CONSUME_TOTAL            = "CONSUME_TOTAL"
	HEADER_CONSUME_TPS              = "CONSUME_TPS"
	HEADER_ROCKETMQ_INFO_JSON_ARRAY = "ROCKETMQ_INFO_JSON_ARRAY"
	HEADER_ROCKETMQ_DATA            = "ROCKETMQ_DATA"
	HEADER_RELOAD_TYPE              = "RELOAD_TYPE"
	HEADER_ALL                      = "ALL"
	HEADER_META_SERVICE             = "META_SERVICE"
	HEADER_META_ATTR                = "META_ATTR"
	HEADER_META_CMPT                = "META_CMPT"
	HEADER_META_CMPT_ATTR           = "META_CMPT_ATTR"
	HEADER_META_META_INST           = "META_META_INST"
	HEADER_META_TOPO                = "META_TOPO"
	HEADER_META_DEPLOY              = "META_DEPLOY"
	HEADER_META_SERVER_SSH          = "META_SERVER_SSH"
	HEADER_META_CMPT_VERSION        = "META_CMPT_VERSION"

	HEADER_LOG_TYPE         = "LOG_TYPE"
	HEADER_BOOKIE_PORT      = "BOOKIE_PORT"
	HEADER_HTTP_SERVER_PORT = "HTTP_SERVER_PORT"
	HEADER_META_DATA_PORT   = "META_DATA_PORT"
	HEADER_GRPC_PORT        = "GRPC_PORT"
	HEADER_BROKER_PORT      = "BROKER_PORT"
	HEADER_WEB_PORT         = "WEB_PORT"

	HEADER_DEAL_FLAG     = "DEAL_FLAG"
	HEADER_ALARM_TYPE    = "ALARM_TYPE"
	HEADER_ALARM_INFO    = "ALARM_INFO"
	HEADER_ALARM_TIME    = "ALARM_TIME"
	HEADER_DEAL_TIME     = "DEAL_TIME"
	HEADER_DEAL_ACC_NAME = "DEAL_ACC_NAME"
	HEADER_ALARM_ID      = "ALARM_ID"

	HEADER_WORKER_PROCESSES = "WORKER_PROCESSES"
	HEADER_VERTX_PORT       = "VERTX_PORT"
)
