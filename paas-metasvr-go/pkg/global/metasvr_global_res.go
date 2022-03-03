package global

import (
	"runtime"
	"sync"

	goredis "github.com/go-redis/redis/v7"
	"github.com/maoge/paas-metasvr-go/pkg/config"

	"github.com/maoge/paas-metasvr-go/pkg/db/pool"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/redis"
)

var GLOBAL_RES MetaSvrGlobalRes

type MetaSvrGlobalRes struct {
	Mut    sync.Mutex
	Config config.MetaSvrConfig

	DbYaml    config.DbYaml
	ldbDbPool pool.LdbDbPool
	redisPool redis.RedisPool

	cmptMeta *meta.CmptMeta
}

func (g *MetaSvrGlobalRes) Init() {
	g.initConf()

	runtime.GOMAXPROCS(g.Config.GoMaxPorc)

	g.initDBPool()
	g.initRedisPool()
	g.initCmptMeta()
}

func (g *MetaSvrGlobalRes) GetDbPool() *pool.DbPool {
	return g.ldbDbPool.GetDbPool()
}

func (g *MetaSvrGlobalRes) GetRedisClusterClient() *goredis.ClusterClient {
	return g.redisPool.GetClusterClient()
}

func (g *MetaSvrGlobalRes) initConf() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	g.Config = *config.NewConfig()
}

func (g *MetaSvrGlobalRes) initCmptMeta() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	g.cmptMeta = meta.NewCmptMeta()
}

func (g *MetaSvrGlobalRes) initDBPool() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	g.DbYaml.Load(g.Config.MetadbYamlName)
	g.ldbDbPool.Init(&g.DbYaml)
}

func (g *MetaSvrGlobalRes) initRedisPool() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	g.redisPool = redis.RedisPool{
		Addr:               g.Config.RedisCluster,
		Password:           g.Config.RedisAuth,
		MaxActive:          g.Config.RedisPoolMaxSize,
		MaxIdle:            g.Config.RedisPoolMinSize,
		IdleTimeout:        g.Config.RedisIdleTimeout,
		IdleCheckFrequency: g.Config.RedisIdleCheckFrequency,
		DialTimeout:        g.Config.RedisDialTimeout,
		ReadTimeout:        g.Config.RedisReadTimeout,
		WriteTimeout:       g.Config.RedisWriteTimeout,
	}
	g.redisPool.Init()
}
