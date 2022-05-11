package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type YugaByteDBDeployer struct {
}

func (h *YugaByteDBDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *YugaByteDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *YugaByteDBDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *YugaByteDBDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
