package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	YugaByteDBDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/yugabytedb"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type YugaByteDBDeployer struct {
}

func (h *YugaByteDBDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	// 1. deploy yb-master
	ybMasterContainer := servJson[consts.HEADER_YB_MASTER_CONTAINER].(map[string]interface{})
	ybMasterArr := ybMasterContainer[consts.HEADER_YB_MASTER].([]map[string]interface{})
	masterList := YugaByteDBDeployUtils.GetYbMasterList(ybMasterArr, magicKey, paasResult)
	for _, ybMaster := range ybMasterArr {
		if !YugaByteDBDeployUtils.DeployMaster(ybMaster, version, masterList, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "yb-master deploy failed ......")
			return false
		}
	}

	// 2. deploy yb-tserver
	ybTServerContainer := servJson[consts.HEADER_YB_TSERVER_CONTAINER].(map[string]interface{})
	ybTServerArr := ybTServerContainer[consts.HEADER_YB_TSERVER].([]map[string]interface{})
	for _, ybTServer := range ybTServerArr {
		if !YugaByteDBDeployUtils.DeployTServer(ybTServer, version, masterList, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "yb-tserver deploy failed ......")
			return false
		}
	}

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}
	return true
}

func (h *YugaByteDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	// 1. undeploy yb-tserver
	ybTServerContainer := servJson[consts.HEADER_YB_TSERVER_CONTAINER].(map[string]interface{})
	ybTServerArr := ybTServerContainer[consts.HEADER_YB_TSERVER].([]map[string]interface{})
	for _, ybTServer := range ybTServerArr {
		if !YugaByteDBDeployUtils.UndeployTServer(ybTServer, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "yb-tserver undeploy failed ......")
			return false
		}
	}

	// 2. undeploy yb-master
	ybMasterContainer := servJson[consts.HEADER_YB_MASTER_CONTAINER].(map[string]interface{})
	ybMasterArr := ybMasterContainer[consts.HEADER_YB_MASTER].([]map[string]interface{})
	for _, ybMaster := range ybMasterArr {
		if !YugaByteDBDeployUtils.UndeployMaster(ybMaster, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "yb-master undeploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

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
