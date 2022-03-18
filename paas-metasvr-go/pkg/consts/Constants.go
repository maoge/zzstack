package consts

import (
	"time"
)

var (
	DB_RECOVER_INTERVAL time.Duration = 3 * time.Second
	INFO_OK                           = "OK"
	STR_TRUE                          = "1"
	STR_FALSE                         = "0"
	STR_ALARM                         = "4"
	STR_ERROR                         = "3"
	STR_WARN                          = "2"
	STR_DEPLOYED                      = "1"
	STR_SAVED                         = "0"
	POS_DEFAULT_VALUE   int           = -1

	ALARM_UNDEALED = "0"
	ALARM_DEALED   = "1"
	ALARM_ALL      = "-1"

	REVOKE_OK              int = 0
	REVOKE_NOK             int = -1
	REVOKE_NOK_QUEUE_EXIST int = -2
	REVOKE_AUTH_FAIL       int = -3
	REVOKE_AUTH_IP_LIMIT   int = -4
	SERVICE_NOT_INIT       int = -5

	SESSION_TTL        int64 = 3600000
	SESSION_KEY_PREFIX       = "session:"

	EVENTBUS_PULSAR = "pulsar"
	PAAS_TENANT     = "paas-tenant"
	PAAS_NAMESPACE  = "paas-namespace"
	SYS_EVENT_TOPIC = "sys-event"
	SYS_CHECK_TASK  = "sys-check-task"

	CACHE_REDIS_CLUSTER_TEMP_FILE      = "CacheRedisClusterTemplate.yaml"
	CACHE_REDIS_MASTER_SLAVE_TEMP_FILE = "CacheRedisMSTemplate.yaml"
	CACHE_REDIS_HA_CLUSTER_TEMP_FILE   = "CacheRedisHaTemplate.yaml"
	DB_TIDB_TEMP_FILE                  = "DBTemplate.yaml"
	DB_TDENGINE_TEMP_FILE              = "DBTemplate.yaml"
	DB_VOLTDB_TEMP_FILE                = "DBTemplate.yaml"
	DB_CLICKHOUSE_TEMP_FILE            = "DBTemplate.yaml"
	DB_ORACLE_DG_TEMP_FILE             = "DBTemplate.yaml"
	MQ_ROCKETMQ_TEMP_FILE              = "RocketMqTemplate.yaml"
	MQ_PULSAR_TEMP_FILE                = "PulsarTemplate.yaml"
	SERV_CLAZZ_CACHE                   = "CACHE"
	SERV_CLAZZ_MQ                      = "MQ"
	SERV_CLAZZ_DB                      = "DB"
	SERV_CLAZZ_SERVERLESS              = "SERVERLESS"
	SERV_CLAZZ_SMS                     = "SMS"
	SERV_TYPE_CACHE_REDIS_CLUSTER      = "CACHE_REDIS_CLUSTER"
	SERV_TYPE_CACHE_REDIS_MASTER_SLAVE = "CACHE_REDIS_MASTER_SLAVE"
	SERV_TYPE_CACHE_REDIS_HA_CLUSTER   = "CACHE_REDIS_HA_CLUSTER"
	SERV_TYPE_SERVERLESS_APISIX        = "SERVERLESS_APISIX"
	SERV_TYPE_MQ_ROCKETMQ              = "MQ_ROCKETMQ"
	SERV_TYPE_MQ_PULSAR                = "MQ_PULSAR"
	SERV_TYPE_DB_TIDB                  = "DB_TIDB"
	SERV_TYPE_DB_TDENGINE              = "DB_TDENGINE"
	SERV_TYPE_DB_VOLTDB                = "DB_VOLTDB"
	SERV_TYPE_DB_ORACLE_DG             = "DB_ORACLE_DG"
	SERV_TYPE_DB_CLICKHOUSE            = "DB_CLICKHOUSE"
	SERV_TYPE_DB_YUGABYTEDB            = "DB_YUGABYTEDB"
	SERV_TYPE_SMS_GATEWAY              = "SMS_GATEWAY"
	SERV_TYPE_SMS_QUERY                = "SMS_QUERY_SERVICE"
	SERV_DB_PD                         = "DB_PD"
	SERV_DB_TIDB                       = "DB_TIDB"
	SERV_DB_TIKV                       = "DB_TIKV"
	SERV_COLLECTD                      = "COLLECTD"
	SERV_MQ_RABBIT                     = "MQ_RABBIT"
	SERV_MQ_ERLANG                     = "MQ_ERLANG"
	SERV_CACHE_PROXY                   = "CACHE_PROXY"
	SERV_CACHE_NODE                    = "CACHE_NODE"
	CLIENT_TYPE_CACHE                  = "CACHE_CLIENT"
	CLIENT_TYPE_DB                     = "DB_CLIENT"
	CLIENT_TYPE_MQ                     = "MQ_CLIENT"
	APP_SMS_SERVER                     = "SMS_SERVER"
	APP_SMS_PROCESS                    = "SMS_PROCESS"
	APP_SMS_CLIENT                     = "SMS_CLIENT"
	APP_SMS_BATSAVE                    = "SMS_BATSAVE"
	APP_SMS_STATS                      = "SMS_STATS"
	FILE_TYPE_JDK                      = "JDK"
	SCHEMA_OBJECT                      = "object"
	SCHEMA_ARRAY                       = "array"
)
