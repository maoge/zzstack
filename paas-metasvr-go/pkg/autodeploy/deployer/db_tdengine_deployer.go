package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type TDEngineDeployer struct {
}

func (h *TDEngineDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *TDEngineDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *TDEngineDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *TDEngineDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
