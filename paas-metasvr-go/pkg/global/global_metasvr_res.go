package global

import (
	"runtime"
	"sync"

	goredis "github.com/go-redis/redis/v7"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/db/pool"
	"github.com/maoge/paas-metasvr-go/pkg/redis"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

var GLOBAL_RES MetaSvrGlobalRes

type MetaSvrGlobalRes struct {
	Mut sync.Mutex

	ldbDbPool *pool.LdbDbPool
	redisPool *redis.RedisPool

	deployLog *utils.DeployLog
}

func (g *MetaSvrGlobalRes) Init() {
	runtime.GOMAXPROCS(config.META_SVR_CONFIG.GoMaxPorc)

	g.initDBPool()
	g.initRedisPool()
	g.initDeployLog()
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

func (g *MetaSvrGlobalRes) initDeployLog() {
	g.deployLog = utils.NewDeployLog()
}

func (g *MetaSvrGlobalRes) GetDbPool() *pool.DbPool {
	return g.ldbDbPool.GetDbPool()
}

func (g *MetaSvrGlobalRes) GetRedisClusterClient() *goredis.ClusterClient {
	return g.redisPool.GetClusterClient()
}

func (g *MetaSvrGlobalRes) PubLog(logKey, log string) {
	g.deployLog.PubLog(logKey, log)
}

func (g *MetaSvrGlobalRes) PubSuccessLog(logKey, log string) {
	g.deployLog.PubSuccessLog(logKey, log)
}

func (g *MetaSvrGlobalRes) PubFailLog(logKey, log string) {
	g.deployLog.PubFailLog(logKey, log)
}

func (g *MetaSvrGlobalRes) PubErrorLog(logKey, log string) {
	g.deployLog.PubErrorLog(logKey, log)
}

func (g *MetaSvrGlobalRes) GetDeployLog(logKey string, result *result.ResultBean) {
	g.deployLog.GetLog(logKey, result)
}
