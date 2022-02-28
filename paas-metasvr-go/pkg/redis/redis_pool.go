package redis

import (
	"log"
	"strings"
	"time"

	goredis "github.com/go-redis/redis/v7"
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

	cmd := redisPool.clusterClient.Do("ping")
	res, err := cmd.Result()
	if err == nil {
		log.Printf("ping -> %v, redis cluster: {%v} init OK", res, redisPool.Addr)
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
