package factory

import (
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployer"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

var (
	DEPLOYER_FACTORY *DeployerFactory
	deployer_barrier sync.Once
)

type DeployerFactory struct {
	deployerMap map[string]deployer.ServiceDeployer
}

func NewDeployerFactory() *DeployerFactory {
	res := new(DeployerFactory)
	res.deployerMap = make(map[string]deployer.ServiceDeployer)

	return res
}

func InitDeployerFactory() {
	deployer_barrier.Do(func() {
		DEPLOYER_FACTORY = NewDeployerFactory()
		DEPLOYER_FACTORY.deployerMap[consts.CACHE_REDIS_CLUSTER] = new(deployer.RedisClusterDeployer)
	})
}

func (h *DeployerFactory) Get(servType string) deployer.ServiceDeployer {
	return h.deployerMap[servType]
}

func GetDeployer(servType string) deployer.ServiceDeployer {
	return DEPLOYER_FACTORY.Get(servType)
}
