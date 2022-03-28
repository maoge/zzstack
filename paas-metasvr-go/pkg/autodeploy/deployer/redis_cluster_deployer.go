package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type RedisClusterDeployer struct {
}

func (h *RedisClusterDeployer) DeployService(servInstID string, deployFlag string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum,
	isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile bool,
	rmDeployFile bool, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool {
	return true
}
