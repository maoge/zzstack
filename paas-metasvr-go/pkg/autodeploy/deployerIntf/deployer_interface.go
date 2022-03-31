package deployerIntf

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServiceDeployer interface {
	DeployService(servInstID string, deployFlag string, logKey string, magicKey string,
		paasResult *result.ResultBean) bool

	UndeployService(servInstID string, force bool, logKey string, magicKey string,
		paasResult *result.ResultBean) bool

	DeployInstance(servInstID string, instID string, logKey string, magicKey string,
		paasResult *result.ResultBean) bool

	UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
		paasResult *result.ResultBean) bool

	MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum,
		isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile bool,
		rmDeployFile bool, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool
}
