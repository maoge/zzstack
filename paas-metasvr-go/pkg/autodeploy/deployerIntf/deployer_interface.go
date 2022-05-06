package deployerIntf

import (
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServiceDeployer interface {
	DeployService(servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool

	UndeployService(servInstID string, force bool, logKey, magicKey string, paasResult *result.ResultBean) bool

	DeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) bool

	UndeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) bool
}
