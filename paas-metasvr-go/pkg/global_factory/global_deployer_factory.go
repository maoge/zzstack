package global_factory

import (
	"fmt"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployer"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployerIntf"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/factory"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

var (
	DEPLOYER_FACTORY *factory.DeployerFactory
	deployer_barrier sync.Once
)

func InitDeployerFactory() {
	deployer_barrier.Do(func() {
		DEPLOYER_FACTORY = factory.NewDeployerFactory()

		DEPLOYER_FACTORY.DeployerMap[consts.CACHE_REDIS_CLUSTER] = new(deployer.RedisClusterDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.CACHE_REDIS_MASTER_SLAVE] = new(deployer.RedisMasterSlaveDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.MQ_PULSAR] = new(deployer.PulsarDeployer)
		// DEPLOYER_FACTORY.DeployerMap[consts.MQ_ROCKETMQ] = new(deployer.RocketMQDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.DB_TIDB] = new(deployer.TiDBDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_CLICKHOUSE] = new(deployer.ClickHouseDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_VOLTDB] = new(deployer.VoltDBDeployer)
	})
}

func GetDeployer(servType string) deployerIntf.ServiceDeployer {
	return DEPLOYER_FACTORY.GetDeployer(servType)
}

func GetMaintainer(servType string) deployerIntf.ServiceMaintainer {
	return DEPLOYER_FACTORY.GetMaintainer(servType)
}

func GetServiceDeployer(instID, servType string, paasResult *result.ResultBean) deployerIntf.ServiceDeployer {
	serviceDeployer := DEPLOYER_FACTORY.GetDeployer(servType)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, servType)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return serviceDeployer
}

func GetServiceMaintainer(instID, servType string, paasResult *result.ResultBean) deployerIntf.ServiceMaintainer {
	serviceMaintainer := DEPLOYER_FACTORY.GetMaintainer(servType)
	if serviceMaintainer == nil {
		errMsg := fmt.Sprintf("service maintainer not found, service_id:%s, service_type:%s", instID, servType)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return serviceMaintainer
}
