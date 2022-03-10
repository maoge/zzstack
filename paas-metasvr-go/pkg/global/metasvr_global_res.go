package global

import (
	"runtime"
	"sync"

	goredis "github.com/go-redis/redis/v7"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/db/pool"

	// "github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/redis"
)

var GLOBAL_RES MetaSvrGlobalRes

type MetaSvrGlobalRes struct {
	Mut sync.Mutex

	ldbDbPool *pool.LdbDbPool
	redisPool *redis.RedisPool
	// eventbus  eventbus.EventBus
}

func (g *MetaSvrGlobalRes) Init() {
	runtime.GOMAXPROCS(config.META_SVR_CONFIG.GoMaxPorc)

	g.initDBPool()
	g.initRedisPool()
	// g.initEventBus()
}

func (g *MetaSvrGlobalRes) initDBPool() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	var dbYaml config.DbYaml
	dbYaml.Load(config.META_SVR_CONFIG.MetadbYamlName)

	g.ldbDbPool = pool.NewLdbDbPool(&dbYaml)
}

func (g *MetaSvrGlobalRes) initRedisPool() {
	g.Mut.Lock()
	defer g.Mut.Unlock()

	g.redisPool = redis.NewRedisPool()
}

// func (g *MetaSvrGlobalRes) initEventBus() {
// 	g.Mut.Lock()
// 	defer g.Mut.Unlock()

// 	g.eventbus = eventbus.CreateEventBus(consts.EVENTBUS_PULSAR).(eventbus.EventBus)
// }

func (g *MetaSvrGlobalRes) GetDbPool() *pool.DbPool {
	return g.ldbDbPool.GetDbPool()
}

func (g *MetaSvrGlobalRes) GetRedisClusterClient() *goredis.ClusterClient {
	return g.redisPool.GetClusterClient()
}
