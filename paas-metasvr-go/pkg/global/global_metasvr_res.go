package global

import (
	"fmt"
	"runtime"
	"sync"
	"time"

	goredis "github.com/go-redis/redis/v7"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/db/pool"
	"github.com/maoge/paas-metasvr-go/pkg/redis"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

var (
	GLOBAL_RES         *MetaSvrGlobalRes = nil
	global_res_barrier sync.Once
)

type MetaSvrGlobalRes struct {
	Mut sync.Mutex

	ldbDbPool *pool.LdbDbPool
	redisPool *redis.RedisPool

	PulsarClient *pulsar.Client

	deployLog *utils.DeployLog
}

func InitGlobalRes() {
	global_res_barrier.Do(func() {
		runtime.GOMAXPROCS(config.META_SVR_CONFIG.GoMaxPorc)

		GLOBAL_RES = new(MetaSvrGlobalRes)
		GLOBAL_RES.initDBPool()
		GLOBAL_RES.initRedisPool()
		GLOBAL_RES.initDeployLog()
		GLOBAL_RES.initPulsarClient()
	})
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

func (g *MetaSvrGlobalRes) initPulsarClient() {
	addr := fmt.Sprintf("pulsar://%v", config.META_SVR_CONFIG.EventbusAddress)
	pulsarClient, err := pulsar.NewClient(pulsar.ClientOptions{
		URL:               addr,
		OperationTimeout:  3 * time.Second,
		ConnectionTimeout: 3 * time.Second,
	})

	if err != nil {
		errInfo := fmt.Sprintf("Could not instantiate Pulsar client: %v, error: %v", addr, err.Error())
		utils.LOGGER.Fatal(errInfo)
		return
	}

	g.PulsarClient = &pulsarClient
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
