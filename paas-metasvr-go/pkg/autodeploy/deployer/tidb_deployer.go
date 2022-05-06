package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type TiDBDeployer struct {
}

func (h *TiDBDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	if !deployutils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	if deployutils.IsServiceDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	version := serv.VERSION

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})
	pdContainer := servJson[consts.HEADER_PD_SERVER_CONTAINER].(map[string]interface{})

	return true
}

func (h *TiDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *TiDBDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *TiDBDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
