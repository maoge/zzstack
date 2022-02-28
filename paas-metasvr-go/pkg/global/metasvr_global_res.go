package global

import (
	"runtime"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/redis"
)

var GLOBAL_RES MetaSvrGlobalRes

type MetaSvrGlobalRes struct {
	Mut    sync.Mutex
	Config config.MetaSvrConfig

	DbYaml    config.DbYaml
	LdbDbPool db.LdbDbPool
	RedisPool redis.RedisPool
}

func (global *MetaSvrGlobalRes) Init() {
	global.initConf()

	runtime.GOMAXPROCS(global.Config.GoMaxPorc)

	global.initDBPool()
	global.initRedisPool()
}

func (global *MetaSvrGlobalRes) initConf() {
	global.Mut.Lock()
	defer global.Mut.Unlock()

	global.Config = *config.NewConfig()
}

func (global *MetaSvrGlobalRes) initDBPool() {
	global.Mut.Lock()
	defer global.Mut.Unlock()

	global.DbYaml.Load(global.Config.MetadbYamlName)
	global.LdbDbPool.Init(&global.DbYaml)
}

func (global *MetaSvrGlobalRes) initRedisPool() {
	global.Mut.Lock()
	defer global.Mut.Unlock()

	global.RedisPool = redis.RedisPool{
		Addr:               global.Config.RedisCluster,
		Password:           global.Config.RedisAuth,
		MaxActive:          global.Config.RedisPoolMaxSize,
		MaxIdle:            global.Config.RedisPoolMinSize,
		IdleTimeout:        global.Config.RedisIdleTimeout,
		IdleCheckFrequency: global.Config.RedisIdleCheckFrequency,
		DialTimeout:        global.Config.RedisDialTimeout,
		ReadTimeout:        global.Config.RedisReadTimeout,
		WriteTimeout:       global.Config.RedisWriteTimeout,
	}
	global.RedisPool.Init()
}
