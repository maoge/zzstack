package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServerlessApisixDeployer struct {
}

func (h *ServerlessApisixDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *ServerlessApisixDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *ServerlessApisixDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *ServerlessApisixDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
