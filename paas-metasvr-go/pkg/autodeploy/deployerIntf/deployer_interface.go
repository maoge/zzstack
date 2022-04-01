package deployerIntf

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServiceDeployer interface {
	DeployService(servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool

	UndeployService(servInstID string, force bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	DeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) bool

	UndeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) bool

	MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile, rmDeployFile, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool
}
