package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type SmsQueryDeployer struct {
}

func (h *SmsQueryDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	// servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	// if !ok {
	// 	return false
	// }
	// serv := meta.CMPT_META.GetService(servInstID)

	// mod is_deployed flag and local cache
	// return DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult)
	return true
}

func (h *SmsQueryDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	// servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	// if !ok {
	// 	return false
	// }

	// update deploy flag and local cache
	// return DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult)
	return true
}

func (h *SmsQueryDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	// servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	// if !ok {
	// 	return false
	// }

	// inst := meta.CMPT_META.GetInstance(instID)
	// instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	// deployResult := false

	// switch instCmpt.CMPT_NAME {

	// default:
	// 	break
	// }

	// DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	// return deployResult
	return true
}

func (h *SmsQueryDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	// servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	// if !ok {
	// 	return false
	// }

	// inst := meta.CMPT_META.GetInstance(instID)
	// instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	// undeployResult := false

	// switch instCmpt.CMPT_NAME {

	// default:
	// 	break
	// }

	// DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	// return true

	return true
}
