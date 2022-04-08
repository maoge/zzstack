package redis

import (
	"fmt"
	"strings"
	"time"

	goredis "github.com/go-redis/redis/v7"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type RedisPool struct {
	clusterClient *goredis.ClusterClient

	Addr               string
	Password           string
	MaxActive          int
	MaxIdle            int
	IdleTimeout        time.Duration
	IdleCheckFrequency time.Duration
	DialTimeout        time.Duration
	ReadTimeout        time.Duration
	WriteTimeout       time.Duration
}

func NewRedisPool() *RedisPool {
	pool := &RedisPool{
		Addr:               config.META_SVR_CONFIG.RedisCluster,
		Password:           config.META_SVR_CONFIG.RedisAuth,
		MaxActive:          config.META_SVR_CONFIG.RedisPoolMaxSize,
		MaxIdle:            config.META_SVR_CONFIG.RedisPoolMinSize,
		IdleTimeout:        config.META_SVR_CONFIG.RedisIdleTimeout,
		IdleCheckFrequency: config.META_SVR_CONFIG.RedisIdleCheckFrequency,
		DialTimeout:        config.META_SVR_CONFIG.RedisDialTimeout,
		ReadTimeout:        config.META_SVR_CONFIG.RedisReadTimeout,
		WriteTimeout:       config.META_SVR_CONFIG.RedisWriteTimeout,
	}

	pool.Init()
	return pool
}

func (redisPool *RedisPool) Init() {
	addrArr := strings.Split(redisPool.Addr, ",")

	clusterOptions := &goredis.ClusterOptions{
		Addrs:          addrArr,
		ReadOnly:       true,
		RouteByLatency: true,
		RouteRandomly:  true,
		Password:       redisPool.Password,

		PoolSize:           redisPool.MaxActive,
		MinIdleConns:       redisPool.MaxIdle,
		MaxConnAge:         redisPool.IdleTimeout,
		PoolTimeout:        0,
		IdleTimeout:        redisPool.IdleTimeout,
		IdleCheckFrequency: redisPool.IdleCheckFrequency,
		DialTimeout:        redisPool.DialTimeout,  // 设置连接超时
		ReadTimeout:        redisPool.ReadTimeout,  // 设置读取超时
		WriteTimeout:       redisPool.WriteTimeout, // 设置写入超时
	}

	redisPool.clusterClient = goredis.NewClusterClient(clusterOptions)
	time.Sleep(time.Duration(20) * time.Millisecond)

	cmd := redisPool.clusterClient.Do("ping")
	res, err := cmd.Result()
	if err == nil {
		info := fmt.Sprintf("ping -> %v, redis cluster: {%v} init OK", res, redisPool.Addr)
		utils.LOGGER.Info(info)
	} else {
		panic(err)
	}
}

func (redisPool *RedisPool) Release() {
	if redisPool.clusterClient != nil {
		redisPool.clusterClient.Close()
	}
}

func (redisPool *RedisPool) GetClusterClient() *goredis.ClusterClient {
	return redisPool.clusterClient
}
