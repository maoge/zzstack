package config

import (
	"fmt"
	"os"
	"time"

	ini "gopkg.in/ini.v1"
)

type MetaSvrConfig struct {
	GoMaxPorc int `json:"go_max_porc,omitempty"`

	WebApiAddress string `json:"web_api_address,omitempty"`
	WebApiUseSSL  bool   `json:"web_api_use_ssl,omitempty"`

	ServerlessGatewayRegist     bool   `json:"serverless_gateway_regist,omitempty"`
	ServerlessGatewayAddress    string `json:"serverless_gateway_address,omitempty"`
	ServerlessGatewayUpstreamId string `json:"serverless_gateway_upstream_id,omitempty"`
	ServerlessGatewayServiceId  string `json:"serverless_gateway_service_id,omitempty"`
	ServerlessGatewayXapiKey    string `json:"serverless_gateway_xapi_key,omitempty"`

	AlarmNotifyUrl     string `json:"alarm_notify_url,omitempty"`
	AlarmNotifyEnabled bool   `json:"alarm_notify_enabled,omitempty"`

	ThreadPoolCoreSize int `json:"thread_pool_core_size,omitempty"`
	ThreadPoolMaxSize  int `json:"thread_pool_max_size,omitempty"`

	EventbusAddress              string `json:"eventbus_address,omitempty"`
	EventbusConsumerSubscription string `json:"eventbus_consumer_subscription,omitempty"`
	EventbusExpireTtl            int    `json:"eventbus_expire_ttl,omitempty"`

	AlarmTimeWindow int `json:"alarm_time_window,omitempty"`

	PasswordExpire      int  `json:"password_expire,omitempty"`
	NeedAuth            bool `json:"need_auth,omitempty"`
	CheckBlackwhiteList bool `json:"check_blackwhite_list,omitempty"`

	RaftClusterEnabled bool `json:"raft_cluster_enabled,omitempty"`
	CollectEnabled     bool `json:"collect_enabled,omitempty"`
	CollectInterval    int  `json:"collect_interval,omitempty"`

	RedisCluster            string        `json:"redis_cluster,omitempty"`
	RedisAuth               string        `json:"redis_auth,omitempty"`
	RedisPoolMaxSize        int           `json:"redis_pool_max_size,omitempty"`
	RedisPoolMinSize        int           `json:"redis_pool_min_size,omitempty"`
	RedisIdleTimeout        time.Duration `json:"redis_idle_timeout,omitempty"`
	RedisIdleCheckFrequency time.Duration `json:"redis_idle_check_frequency,omitempty"`
	RedisDialTimeout        time.Duration `json:"redis_dial_timeout,omitempty"`
	RedisReadTimeout        time.Duration `json:"redis_read_timeout,omitempty"`
	RedisWriteTimeout       time.Duration `json:"redis_write_timeout,omitempty"`

	MetadbYamlName string `json:"metadb_yaml_name,omitempty"`
	TDYamlName     string `json:"td_yaml_name,omitempty"`
}

func NewConfig() *MetaSvrConfig {
	cfg, err := ini.Load("./etc/metasvr.ini") //初始化一个cfg
	if err != nil {
		fmt.Printf("Fail to read file: %v", err)
		os.Exit(1)
	}

	goMaxPorc := cfg.Section("System").Key("go_max_porc").MustInt(16)

	webApiAddress := cfg.Section("System").Key("web_api_address").MustString("0.0.0.0:9090")
	webApiUseSSL := cfg.Section("System").Key("web_api_use_ssl").MustBool(false)

	serverlessGatewayRegist := cfg.Section("System").Key("serverless_gateway_regist").MustBool(false)
	serverlessGatewayAddress := cfg.Section("System").Key("serverless_gateway_address").String()
	serverlessGatewayUpstreamId := cfg.Section("System").Key("serverless_gateway_upstream_id").MustString("paas_metasvr")
	serverlessGatewayServiceId := cfg.Section("System").Key("serverless_gateway_service_id").MustString("metasvr")
	serverlessGatewayXapiKey := cfg.Section("System").Key("serverless_gateway_xapi_key").String()

	alarmNotifyUrl := cfg.Section("System").Key("alarm_notify_url").String()
	alarmNotifyEnabled := cfg.Section("System").Key("alarm_notify_enabled").MustBool(true)

	threadPoolCoreSize := cfg.Section("System").Key("thread_pool_core_size").MustInt(20)
	threadPoolMaxSize := cfg.Section("System").Key("thread_pool_max_size").MustInt(40)

	eventbusAddress := cfg.Section("System").Key("eventbus_address").String()
	eventbusConsumerSubscription := cfg.Section("System").Key("eventbus_address").String()
	eventbusExpireTtl := cfg.Section("System").Key("eventbus_address").MustInt(60000)

	alarmTimeWindow := cfg.Section("System").Key("alarm_time_window").MustInt(600000)

	passwordExpire := cfg.Section("System").Key("password_expire").MustInt(7776000)
	needAuth := cfg.Section("System").Key("need_auth").MustBool(true)
	checkBlackwhiteList := cfg.Section("System").Key("check_blackwhite_list").MustBool(false)

	raftClusterEnabled := cfg.Section("Cluster").Key("raft_cluster_enabled").MustBool(false)
	collectEnabled := cfg.Section("Cluster").Key("collect_enabled").MustBool(true)
	collectInterval := cfg.Section("Cluster").Key("collect_interval").MustInt(10000)

	redisCluster := cfg.Section("DataBase").Key("redis_cluster").String()
	redisAuth := cfg.Section("DataBase").Key("redis_auth").String()
	redisPoolMaxSize := cfg.Section("DataBase").Key("redis_pool_max_size").MustInt(20)
	redisPoolMinSize := cfg.Section("DataBase").Key("redis_pool_min_size").MustInt(10)
	redisIdleTimeout := cfg.Section("DataBase").Key("redis_idle_timeout").MustInt(1800)
	redisIdleCheckFrequency := cfg.Section("DataBase").Key("redis_idle_check_frequency").MustInt(10)
	redisDialTimeout := cfg.Section("DataBase").Key("redis_dial_timeout").MustInt(3)
	redisReadTimeout := cfg.Section("DataBase").Key("redis_read_timeout").MustInt(5)
	redisWriteTimeout := cfg.Section("DataBase").Key("redis_write_timeout").MustInt(5)

	metadbYamlName := cfg.Section("DataBase").Key("metadb_yaml_name").MustString("metadb")
	tdYamlName := cfg.Section("DataBase").Key("td_yaml_name").MustString("tdengine")

	return &MetaSvrConfig{
		GoMaxPorc:     goMaxPorc,
		WebApiAddress: webApiAddress,
		WebApiUseSSL:  webApiUseSSL,

		ServerlessGatewayRegist:     serverlessGatewayRegist,
		ServerlessGatewayAddress:    serverlessGatewayAddress,
		ServerlessGatewayUpstreamId: serverlessGatewayUpstreamId,
		ServerlessGatewayServiceId:  serverlessGatewayServiceId,
		ServerlessGatewayXapiKey:    serverlessGatewayXapiKey,

		AlarmNotifyUrl:     alarmNotifyUrl,
		AlarmNotifyEnabled: alarmNotifyEnabled,

		ThreadPoolCoreSize: threadPoolCoreSize,
		ThreadPoolMaxSize:  threadPoolMaxSize,

		EventbusAddress:              eventbusAddress,
		EventbusConsumerSubscription: eventbusConsumerSubscription,
		EventbusExpireTtl:            eventbusExpireTtl,

		AlarmTimeWindow: alarmTimeWindow,

		PasswordExpire:      passwordExpire,
		NeedAuth:            needAuth,
		CheckBlackwhiteList: checkBlackwhiteList,

		RaftClusterEnabled: raftClusterEnabled,
		CollectEnabled:     collectEnabled,
		CollectInterval:    collectInterval,

		RedisCluster:            redisCluster,
		RedisAuth:               redisAuth,
		RedisPoolMaxSize:        redisPoolMaxSize,
		RedisPoolMinSize:        redisPoolMinSize,
		RedisIdleTimeout:        time.Duration(redisIdleTimeout) * time.Millisecond,
		RedisIdleCheckFrequency: time.Duration(redisIdleCheckFrequency) * time.Millisecond,
		RedisDialTimeout:        time.Duration(redisDialTimeout) * time.Millisecond,
		RedisReadTimeout:        time.Duration(redisReadTimeout) * time.Millisecond,
		RedisWriteTimeout:       time.Duration(redisWriteTimeout) * time.Millisecond,

		MetadbYamlName: metadbYamlName,
		TDYamlName:     tdYamlName,
	}
}
