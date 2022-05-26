package global_factory

import (
	"fmt"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/autocheck/checkerintf"
	"github.com/maoge/paas-metasvr-go/pkg/autocheck/prober"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployer"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployerIntf"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/factory"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/maintainer"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

var (
	DEPLOYER_FACTORY *factory.DeployerFactory = nil
	PROBER_FACTORY   *factory.ProberFactory   = nil
	deployer_barrier sync.Once
)

func InitDeployerFactory() {
	deployer_barrier.Do(func() {
		DEPLOYER_FACTORY = factory.NewDeployerFactory()

		// deployer
		DEPLOYER_FACTORY.DeployerMap[consts.CACHE_REDIS_CLUSTER] = new(deployer.RedisClusterDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.CACHE_REDIS_MASTER_SLAVE] = new(deployer.RedisMasterSlaveDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.MQ_PULSAR] = new(deployer.PulsarDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.MQ_ROCKETMQ] = new(deployer.RocketMQDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.DB_TIDB] = new(deployer.TiDBDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_CLICKHOUSE] = new(deployer.ClickHouseDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_VOLTDB] = new(deployer.VoltDBDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_TDENGINE] = new(deployer.TDEngineDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.DB_YUGABYTEDB] = new(deployer.YugaByteDBDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.STORE_MINIO] = new(deployer.StoreMinioDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.SERVERLESS_APISIX] = new(deployer.ServerlessApisixDeployer)

		DEPLOYER_FACTORY.DeployerMap[consts.SMS_GATEWAY] = new(deployer.SmsGatewayDeployer)
		DEPLOYER_FACTORY.DeployerMap[consts.SMS_QUERY_SERVICE] = new(deployer.SmsQueryDeployer)

		// maintainer
		DEPLOYER_FACTORY.MaintainerMap[consts.SMS_GATEWAY] = new(maintainer.SmsGatewayMaintainer)

		// prober
		PROBER_FACTORY = factory.NewProberFactory()
		PROBER_FACTORY.ProberMap[consts.SMS_GATEWAY] = new(prober.SmsGatewayProber)
	})
}

func GetDeployer(servType string) deployerIntf.ServiceDeployer {
	return DEPLOYER_FACTORY.GetDeployer(servType)
}

func GetMaintainer(servType string) deployerIntf.ServiceMaintainer {
	return DEPLOYER_FACTORY.GetMaintainer(servType)
}

func GetProber(servType string) checkerintf.CmptProber {
	return PROBER_FACTORY.GetProber(servType)
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

func GetServiceProber(instID, servType string, paasResult *result.ResultBean) checkerintf.CmptProber {
	serviceProber := PROBER_FACTORY.GetProber(servType)
	if serviceProber == nil {
		errMsg := fmt.Sprintf("service prober not found, service_id:%s, service_type:%s", instID, servType)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return serviceProber
}
