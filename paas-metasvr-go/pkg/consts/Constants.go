package consts

import (
	"time"
)

const (
	DB_RECOVER_INTERVAL                time.Duration = 3 * time.Second
	UNIT_G                                           = 1024 * 1024 * 1024
	UNIT_M                                           = 1024 * 1024
	UNIT_K                                           = 1024
	CLICKHOUSE_DEFAULT_REPLICA_WEIGHT                = 100
	PATH_SPLIT                                       = "/"
	METASVR_ADDR_SPLIT                               = ","
	PATH_COMMA                                       = ","
	LINE_SEP                                         = "\n"
	SMS_CONSOLE_PASSWD                               = "21232f297a57a5a743894a0e4a801fc3"
	CMD_ADJUST_REDIS_WEIGHT                          = "AdjustRedisWeight"
	CMD_SWITCH_DB_TYPE                               = "SwitchDBType"
	REDIS_CLUSTER_A                                  = "RedisClusterA"
	REDIS_CLUSTER_B                                  = "RedisClusterB"
	ZZSOFT_REDIS_PASSWD                              = "zzsoft.1234"
	DEFAULT_TIDB_ROOT_PASSWD                         = "abcd.1234"
	CLICKHOUSE_DEFAULT_USER                          = "default"
	CLICKHOUSE_DEFAULT_PASSWD                        = "abcd.1234"
	VOLTDB_ADMIN_NAME                                = "admin"
	VOLTDB_ADMIN_PWD                                 = "admin.1234"
	MINIO_ACCESS_KEY                                 = "zzadmin"
	MINIO_SECRET_KEY                                 = "abcd.1234"
	NOTIFY_RETRY_CNT                   int           = 3
	OP_TYPE_ADD                        int           = 1
	OP_TYPE_MOD                        int           = 2
	OP_TYPE_DEL                        int           = 3
	INFO_OK                                          = "OK"
	STR_DEPLOY                                       = "1"
	STR_UNDEPLOY                                     = "0"
	STR_TRUE                                         = "1"
	STR_FALSE                                        = "0"
	STR_ALARM                                        = "4"
	STR_ERROR                                        = "3"
	STR_WARN                                         = "2"
	STR_DEPLOYED                                     = "1"
	STR_SAVED                                        = "0"
	TOPO_TYPE_LINK                     int           = 1
	TOPO_TYPE_CONTAIN                  int           = 2
	POS_DEFAULT_VALUE                  int           = -1
	DEPLOY_FLAG_PHYSICAL                             = "1" // 物理部署
	DEPLOY_FLAG_PSEUDO                               = "2" // 伪部署
	ALARM_UNDEALED                                   = "0"
	ALARM_DEALED                                     = "1"
	ALARM_ALL                                        = "-1"
	REVOKE_OK                          int           = 0
	REVOKE_NOK                         int           = -1
	REVOKE_NOK_QUEUE_EXIST             int           = -2
	REVOKE_AUTH_FAIL                   int           = -3
	REVOKE_AUTH_IP_LIMIT               int           = -4
	SERVICE_NOT_INIT                   int           = -5
	SESSION_TTL                        int64         = 3600000
	STR_NULL                                         = ""
	TYPE_REDIS_MASTER_NODE                           = "1"
	TYPE_REDIS_SLAVE_NODE                            = "0"
	ATTR_NODE_TYPE                                   = "NODE_TYPE"
	SESSION_KEY_PREFIX                               = "session:"
	EVENTBUS_PULSAR                                  = "pulsar"
	PAAS_TENANT                                      = "paas-tenant"
	PAAS_NAMESPACE                                   = "paas-namespace"
	SYS_EVENT_TOPIC                                  = "sys-event"
	SYS_CHECK_TASK                                   = "sys-check-task"
	CACHE_REDIS_CLUSTER_TEMP_FILE                    = "CacheRedisClusterTemplate.yaml"
	CACHE_REDIS_MASTER_SLAVE_TEMP_FILE               = "CacheRedisMSTemplate.yaml"
	CACHE_REDIS_HA_CLUSTER_TEMP_FILE                 = "CacheRedisHaTemplate.yaml"
	DB_TIDB_TEMP_FILE                                = "DBTemplate.yaml"
	DB_TDENGINE_TEMP_FILE                            = "DBTemplate.yaml"
	DB_VOLTDB_TEMP_FILE                              = "DBTemplate.yaml"
	DB_CLICKHOUSE_TEMP_FILE                          = "DBTemplate.yaml"
	DB_ORACLE_DG_TEMP_FILE                           = "DBTemplate.yaml"
	MQ_ROCKETMQ_TEMP_FILE                            = "RocketMqTemplate.yaml"
	MQ_PULSAR_TEMP_FILE                              = "PulsarTemplate.yaml"
	PROMETHEUS_CLICKHOUSE_YML                        = "prometheus_clickhouse.yml"
	PROMETHEUS_YML                                   = "prometheus.yml"
	PROMETHEUS_PULSAR_YML                            = "prometheus_pulsar.yml"
	PROMETHEUS_APISIX_YML                            = "prometheus_apisix.yml"
	NGX_SMS_QUERY_CONF                               = "nginx_sms_query.conf"
	NGX_CONF                                         = "nginx.conf"
	CONF_CLICKHOUSE_EXPORTER_LIST                    = "%CLICKHOUSE_EXPORTER_LIST%"
	SERV_CLAZZ_CACHE                                 = "CACHE"
	SERV_CLAZZ_MQ                                    = "MQ"
	SERV_CLAZZ_DB                                    = "DB"
	SERV_CLAZZ_SERVERLESS                            = "SERVERLESS"
	SERV_CLAZZ_SMS                                   = "SMS"
	SERV_TYPE_CACHE_REDIS_CLUSTER                    = "CACHE_REDIS_CLUSTER"
	SERV_TYPE_CACHE_REDIS_MASTER_SLAVE               = "CACHE_REDIS_MASTER_SLAVE"
	SERV_TYPE_CACHE_REDIS_HA_CLUSTER                 = "CACHE_REDIS_HA_CLUSTER"
	SERV_TYPE_SERVERLESS_APISIX                      = "SERVERLESS_APISIX"
	SERV_TYPE_STORE_MINIO                            = "STORE_MINIO"
	SERV_TYPE_MQ_ROCKETMQ                            = "MQ_ROCKETMQ"
	SERV_TYPE_MQ_PULSAR                              = "MQ_PULSAR"
	SERV_TYPE_DB_TIDB                                = "DB_TIDB"
	SERV_TYPE_DB_TDENGINE                            = "DB_TDENGINE"
	SERV_TYPE_DB_VOLTDB                              = "DB_VOLTDB"
	SERV_TYPE_DB_ORACLE_DG                           = "DB_ORACLE_DG"
	SERV_TYPE_DB_CLICKHOUSE                          = "DB_CLICKHOUSE"
	SERV_TYPE_DB_YUGABYTEDB                          = "DB_YUGABYTEDB"
	SERV_TYPE_SMS_GATEWAY                            = "SMS_GATEWAY"
	SERV_TYPE_SMS_QUERY                              = "SMS_QUERY_SERVICE"
	SERV_DB_PD                                       = "DB_PD"
	SERV_DB_TIDB                                     = "DB_TIDB"
	SERV_DB_TIKV                                     = "DB_TIKV"
	SERV_COLLECTD                                    = "COLLECTD"
	SERV_MQ_RABBIT                                   = "MQ_RABBIT"
	SERV_MQ_ERLANG                                   = "MQ_ERLANG"
	SERV_CACHE_PROXY                                 = "CACHE_PROXY"
	SERV_CACHE_NODE                                  = "CACHE_NODE"
	CLIENT_TYPE_CACHE                                = "CACHE_CLIENT"
	CLIENT_TYPE_DB                                   = "DB_CLIENT"
	CLIENT_TYPE_MQ                                   = "MQ_CLIENT"
	APP_SMS_SERVER                                   = "SMS_SERVER"
	APP_SMS_PROCESS                                  = "SMS_PROCESS"
	APP_SMS_CLIENT                                   = "SMS_CLIENT"
	APP_SMS_BATSAVE                                  = "SMS_BATSAVE"
	APP_SMS_STATS                                    = "SMS_STATS"
	FILE_TYPE_JDK                                    = "JDK"
	SCHEMA_OBJECT                                    = "object"
	SCHEMA_ARRAY                                     = "array"
	LINE_END                                         = "\n"
	LINE_BLACK_SLASH                                 = "\\\\"
	HTML_LINE_END                                    = "<br/>"
	DEPLOY_SINGLE_SUCCESS_BEGIN_STYLE                = "<span style='color:blue;'>"
	DEPLOY_SINGLE_FAIL_BEGIN_STYLE                   = "<span style='color:yellow;'>"
	END_STYLE                                        = "</span>"
	LINUX_SHELL_SEP                                  = "\r\n"
	PAAS_COLLECTD                                    = "paas-collectd"
	PAAS_ROOT                                        = "paas"
	COMMON_TOOLS_ROOT                                = "tools"
	CACHE_REDIS_ROOT                                 = "cache/redis"
	DB_TIDB_ROOT                                     = "db/tidb"
	SERVERLESS_ROOT                                  = "serverless"
	SMS_GATEWAY_ROOT                                 = "sms/sms_gateway"
	SMS_QUERY_ROOT                                   = "sms/sms_query"
	COLLECTD_ROOT                                    = "collectd"
	MQ_ROCKETMQ_ROOT                                 = "mq/rocketmq"
	MQ_PULSAR_ROOT                                   = "mq/pulsar"
	DB_TDENGINE_ROOT                                 = "db/tdengine"
	DB_CLICKHOUSE_ROOT                               = "db/clickhouse"
	DB_VOLTDB_ROOT                                   = "db/voltdb"
	DB_YUGABYTEDB_ROOT                               = "db/yugabyte"
	STORE_MINIO_ROOT                                 = "store/minio"
	CACHE_REDIS_PROXY                                = "cache_redis_proxy"
	CACHE_REDIS_PROXY_PREFIX                         = "cache_redis_proxy_"
	REDIS_CONF                                       = "redis.conf"
	PROXY_CONF                                       = "proxy.conf"
	CONF_SERV_IP                                     = "%SERV_IP%"
	CONF_SERV_PORT                                   = "%SERV_PORT%"
	CONF_PID_FILE                                    = "%PID_FILE%"
	CONF_LOG_FILE                                    = "%LOG_FILE%"
	CONF_MAX_CONN                                    = "%MAX_CONN%"
	CONF_PROXY_THREADS                               = "%PROXY_THREADS%"
	CONF_MAX_MEMORY                                  = "%MAX_MEMORY%"
	CONF_APPENDONLY_FILENAME                         = "%APPENDONLY_FILENAME%"
	REDIS_CLUSTER_CONF_FILENAME                      = "%REDIS_CLUSTER_CONF_FILENAME%"
	CONF_CLUSTER_ENABLED                             = "%CLUSTER_ENABLED%"
	CONF_ROCKET_HOME                                 = "%ROCKETMQ_HOME%"
	CONF_KV_CONFIG_PATH                              = "%KV_CONFIG_PATH%"
	CONF_LISTEN_PORT                                 = "%LISTEN_PORT%"
	CONF_BROKER_CLUSTER_NAME                         = "%BROKER_CLUSTER_NAME%"
	CONF_BROKER_NAME                                 = "%BROKER_NAME%"
	CONF_BROKER_ID                                   = "%BROKER_ID%"
	CONF_NAMESRV_ADDR                                = "%NAMESRV_ADDR%"
	CONF_BROKER_IP                                   = "%BROKER_IP%"
	CONF_STORE_ROOT                                  = "%STORE_ROOT%"
	CONF_COMMIT_LOG_PATH                             = "%COMMIT_LOG_PATH%"
	CONF_CONSUME_QUEUE_PATH                          = "%CONSUME_QUEUE_PATH%"
	CONF_INDEX_PATH                                  = "%INDEX_PATH%"
	CONF_CHECKPOINT_PATH                             = "%CHECKPOINT_PATH%"
	CONF_ABORT_FILE_PATH                             = "%ABORT_FILE_PATH%"
	CONF_BROKER_ROLE                                 = "%BROKER_ROLE%"
	CONF_FLUSH_DISK_TYPE                             = "%FLUSH_DISK_TYPE%"
	CONF_UUID                                        = "%UUID%"
	CONF_META_SVR_URL                                = "%META_SVR_URL%"
	CONF_META_SVR_USR                                = "%META_SVR_USR%"
	CONF_META_SVR_PASSWD                             = "%META_SVR_PASSWD%"
	CONF_ROCKETMQ_SERV                               = "%ROCKETMQ_SERV%"
	CONF_PROCESSOR                                   = "%PROCESSOR%"
	CONF_JVM_OPS                                     = "%JVM_OPS%"
	CONF_SERV_INST_ID                                = "%SERV_INST_ID%"
	CONF_COLLECTD_PORT                               = "%COLLECTD_PORT%"
	CONF_CONSOLE_PORT                                = "%CONSOLE_PORT%"
	CONF_ES_SERVER                                   = "%ES_SERVER%"
	CONF_ES_MT_SERVER                                = "%ES_MT_SERVER%"
	CONF_CLUSTER_NODES                               = "%CLUSTER_NODES%"
	CONF_CONN_POOL_SIZE                              = "%CONN_POOL_SIZE%"
	CONF_PORT                                        = "%PORT%"
	CONF_DATA_DIR                                    = "%DATA_DIR%"
	CONF_LOG_DIR                                     = "%LOG_DIR%"
	CONF_ADMIN_PORT                                  = "%ADMIN_PORT%"
	CONF_CLIENT_PORT                                 = "%CLIENT_PORT%"
	CONF_CLIENT_ADDRESS                              = "%CLIENT_ADDRESS%"
	CONF_HTTP_PORT                                   = "%HTTP_PORT%"
	CONF_TCP_PORT                                    = "%TCP_PORT%"
	CONF_MYSQL_PORT                                  = "%MYSQL_PORT%"
	CONF_INTERSERVER_HTTP_PORT                       = "%INTERSERVER_HTTP_PORT%"
	CONF_LISTEN_HOST                                 = "%LISTEN_HOST%"
	CONF_MAX_CONNECTIONS                             = "%MAX_CONNECTIONS%"
	CONF_MAX_CONCURRENT_QUERIES                      = "%MAX_CONCURRENT_QUERIES%"
	CONF_MAX_SERVER_MEMORY_USAGE                     = "%MAX_SERVER_MEMORY_USAGE%"
	CONF_CLICKHOUSE_SHARDS                           = "%CLICKHOUSE_SHARDS%"
	CONF_ZK_NODES                                    = "%ZK_NODES%"
	CONF_SHARD_ID                                    = "%SHARD_ID%"
	CONF_REPLICA_ID                                  = "%REPLICA_ID%"
	CONF_MAX_MEMORY_USAGE                            = "%MAX_MEMORY_USAGE%"
	CONF_PASSWORD                                    = "%PASSWORD%"
	CONF_SCRAPE_URI                                  = "%SCRAPE_URI%"
	CONF_TELEMETRY_ADDRESS                           = "%TELEMETRY_ADDRESS%"
	CONF_CLICKHOUSE_USER                             = "%CLICKHOUSE_USER%"
	CONF_CLICKHOUSE_PASSWORD                         = "%CLICKHOUSE_PASSWORD%"
	CONF_LISTEN_ADDRESS                              = "%LISTEN_ADDRESS%"
	CONF_CLUSTER_NAME                                = "%CLUSTER_NAME%"
	CONF_HTTP_ADDR                                   = "%HTTP_ADDR%"
	CONF_DOMAIN                                      = "%DOMAIN%"
	CONF_GRAFANA_DIR                                 = "%GRAFANA_DIR%"
	CONF_DASHBOARD_ADDR                              = "%DASHBOARD_ADDR%"
	CONF_PD_ADDRESS                                  = "%PD_ADDRESS%"
	CONF_DASHBOARD_PORT                              = "%DASHBOARD_PORT%"
	CONF_INST_ID                                     = "%INST_ID%"
	CONF_CLIENT_URLS                                 = "%CLIENT_URLS%"
	CONF_PEER_URLS                                   = "%PEER_URLS%"
	CONF_ADVERTISE_PEER_URLS                         = "%ADVERTISE_PEER_URLS%"
	CONF_PD_LIST                                     = "%PD_LIST%"
	CONF_TIKV_ADDR                                   = "%TIKV_ADDR%"
	CONF_STAT_ADDR                                   = "%STAT_ADDR%"
	CONF_HOST                                        = "%HOST%"
	CONF_STAT_HOST                                   = "%STAT_HOST%"
	CONF_STAT_PORT                                   = "%STAT_PORT%"
	CONF_SITES_PER_HOST                              = "%SITES_PER_HOST%"
	CONF_KFACTOR                                     = "%KFACTOR%"
	CONF_HEARTBEAT_TIMEOUT                           = "%HEARTBEAT_TIMEOUT%"
	CONF_ADMIN_NAME                                  = "%ADMIN_NAME%"
	CONF_ADMIN_PWD                                   = "%ADMIN_PWD%"
	CONF_USER_NAME                                   = "%USER_NAME%"
	CONF_USER_PASSWORD                               = "%USER_PASSWORD%"
	CONF_TEMPTABLES_MAXSIZE                          = "%TEMPTABLES_MAXSIZE%"
	CONF_ELASTIC_DURATION                            = "%ELASTIC_DURATION%"
	CONF_ELASTIC_THROUGHPUT                          = "%ELASTIC_THROUGHPUT%"
	CONF_QUERY_TIMEOUT                               = "%QUERY_TIMEOUT%"
	CONF_PROCEDURE_LOGINFO                           = "%PROCEDURE_LOGINFO%"
	CONF_MEMORYLIMIT_SIZE                            = "%MEMORYLIMIT_SIZE%"
	CONF_MEMORYLIMIT_ALERT                           = "%MEMORYLIMIT_ALERT%"
	CONF_HOSTS                                       = "%HOSTS%"
	CONF_VOLT_CLIENT_PORT                            = "%VOLT_CLIENT_PORT%"
	CONF_VOLT_ADMIN_PORT                             = "%VOLT_ADMIN_PORT%"
	CONF_VOLT_WEB_PORT                               = "%VOLT_WEB_PORT%"
	CONF_VOLT_INTERNAL_PORT                          = "%VOLT_INTERNAL_PORT%"
	CONF_VOLT_REPLI_PORT                             = "%VOLT_REPLI_PORT%"
	CONF_VOLT_ZK_PORT                                = "%VOLT_ZK_PORT%"
	CONF_BOOKIE_ID                                   = "%BOOKIE_ID%"
	CONF_BOOKIE_PORT                                 = "%BOOKIE_PORT%"
	CONF_ADVERTISED_ADDRESS                          = "%ADVERTISED_ADDRESS%"
	CONF_HTTP_SERVER_PORT                            = "%HTTP_SERVER_PORT%"
	CONF_JOURNAL_DIRS                                = "%JOURNAL_DIRS%"
	CONF_LEDGER_DIRS                                 = "%LEDGER_DIRS%"
	CONF_META_DATA_SERVICE_URI                       = "%META_DATA_SERVICE_URI%"
	CONF_ZK_SERVERS                                  = "%ZK_SERVERS%"
	CONF_PULSAR_MGR_PORT                             = "%PULSAR_MGR_PORT%"
	CONF_HERDDB_PORT                                 = "%HERDDB_PORT%"
	CONF_BOOKIE_LIST                                 = "%BOOKIE_LIST%"
	CONF_STORE_SERVERS                               = "%CONF_STORE_SERVERS%"
	CONF_GRPC_PORT                                   = "%GRPC_PORT%"
	CONF_BROKER_PORT                                 = "%BROKER_PORT%"
	CONF_WEB_PORT                                    = "%WEB_PORT%"
	CONF_BROKER_ADDRESS                              = "%BROKER_ADDRESS%"
	CONF_MINIO_BROWSER                               = "%BROWSER%"
	CONF_ADDRESS                                     = "%ADDRESS%"
	CONF_CONSOLE_ADDRESS                             = "%CONSOLE_ADDRESS%"
	CONF_ENDPOINTS                                   = "%ENDPOINTS%"
	CONF_MINIO_REGION                                = "%MINIO_REGION%"
	CONF_MINIO_USER                                  = "%MINIO_USER%"
	CONF_MINIO_PASSWD                                = "%MINIO_PASSWD%"
	CONF_FIRSTEP                                     = "%FIRSTEP%"
	CONF_FQDN                                        = "%FQDN%"
	CONF_ARBITRATOR_ADDR                             = "%ARBITRATOR_ADDR%"
	CONF_END                                         = "%END%"
)
