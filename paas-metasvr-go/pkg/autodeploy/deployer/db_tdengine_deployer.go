package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	CollectdDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	TDengineDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/tdengine"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type TDEngineDeployer struct {
}

func getFirstDnodeAddr(dnode map[string]interface{}) string {
	dnodeID := dnode[consts.HEADER_SSH_ID].(string)
	dnodePort := dnode[consts.HEADER_PORT].(string)
	dnodeSSH := meta.CMPT_META.GetSshById(dnodeID)
	dnodeServIP := dnodeSSH.SERVER_IP

	return fmt.Sprintf("%s:%s", dnodeServIP, dnodePort)
}

func (h *TDEngineDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	if DeployUtils.IsServiceDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	version := serv.VERSION

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})
	arbitratorContainer := servJson[consts.HEADER_ARBITRATOR_CONTAINER].(map[string]interface{})
	tdArbitrator := arbitratorContainer[consts.HEADER_TD_ARBITRATOR].(map[string]interface{})

	// 部署arbitrator
	arbitratorAddr := TDengineDeployUtils.GetArbitratorAddr(tdArbitrator)
	if !TDengineDeployUtils.DeployArbitrator(tdArbitrator, version, logKey, magicKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "arbitrator start failed ......")
		return false
	}

	// 部署dnode服务
	dnodeContainer := servJson[consts.HEADER_DNODE_CONTAINER].(map[string]interface{})
	dnodeArr := dnodeContainer[consts.HEADER_TD_DNODE].([]map[string]interface{})
	firstDNodeAddr := ""
	for idx, dnode := range dnodeArr {
		bIsFirst := false
		if idx == 0 {
			firstDNodeAddr = getFirstDnodeAddr(dnode)
			bIsFirst = true
		}
		if !TDengineDeployUtils.DeployDnode(dnode, bIsFirst, arbitratorAddr, firstDNodeAddr, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "dnode start failed ......")
			return false
		}
	}

	// 部署collectd服务
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !CollectdDeployUtils.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "collectd start failed ......")
				return false
			}
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *TDEngineDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	// 未部署直接退出不往下执行
	if DeployUtils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})

	// 卸载collectd服务
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !CollectdDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "collectd undeploy failed ......")
				return false
			}
		}
	}

	// 卸载dnode
	dnodeContainer := servJson[consts.HEADER_DNODE_CONTAINER].(map[string]interface{})
	dnodeArr := dnodeContainer[consts.HEADER_TD_DNODE].([]map[string]interface{})
	for _, dnode := range dnodeArr {
		if !TDengineDeployUtils.UndeployDnode(dnode, false, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "dnode undeploy failed ......")
			return false
		}
	}

	// 卸载arbitrator
	arbitratorContainer := servJson[consts.HEADER_ARBITRATOR_CONTAINER].(map[string]interface{})
	arbitrator := arbitratorContainer[consts.HEADER_TD_ARBITRATOR].(map[string]interface{})
	if !TDengineDeployUtils.UndeployArbitrator(arbitrator, logKey, magicKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "arbitrator undeploy failed ......")
		return false
	}

	// update t_meta_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *TDEngineDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	version := serv.VERSION
	// 未部署直接退出不往下执行
	if DeployUtils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	servInst := meta.CMPT_META.GetInstance(servInstID)
	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})

	arbitratorContainer := servJson[consts.HEADER_ARBITRATOR_CONTAINER].(map[string]interface{})
	arbitrator := arbitratorContainer[consts.HEADER_TD_ARBITRATOR].(map[string]interface{})

	dnodeContainer := servJson[consts.HEADER_DNODE_CONTAINER].(map[string]interface{})
	dnodeArr := dnodeContainer[consts.HEADER_TD_DNODE].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_TD_ARBITRATOR:
		deployResult = TDengineDeployUtils.DeployArbitrator(arbitrator, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_TD_DNODE:
		dnode := DeployUtils.GetSpecifiedItem(dnodeArr, instID)
		arbitratorAddr := TDengineDeployUtils.GetArbitratorAddr(arbitrator)
		firstDNodeAddr := getFirstDnodeAddr(dnodeArr[0])
		deployResult = TDengineDeployUtils.DeployDnode(dnode, false, arbitratorAddr, firstDNodeAddr, version, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	if deployResult {
		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	} else {
		info := fmt.Sprintf("service inst_id:%s, deploy failed ......", servInstID)
		global.GLOBAL_RES.PubFailLog(logKey, info)
	}

	return true
}

func (h *TDEngineDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	// 未部署直接退出不往下执行
	if DeployUtils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	servInst := meta.CMPT_META.GetInstance(servInstID)
	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})

	// arbitratorContainer := servJson[consts.HEADER_ARBITRATOR_CONTAINER].(map[string]interface{})
	// arbitrator := arbitratorContainer[consts.HEADER_TD_ARBITRATOR].(map[string]interface{})

	dnodeContainer := servJson[consts.HEADER_DNODE_CONTAINER].(map[string]interface{})
	dnodeArr := dnodeContainer[consts.HEADER_TD_DNODE].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_TD_ARBITRATOR:
		// 服务集群依赖于arbitrator不能单独卸载实例
		// undeployResult = TDengineDeployUtils.UndeployArbitrator(arbitrator, logKey, magicKey, paasResult)
		break
	case consts.HEADER_TD_DNODE:
		dnode := DeployUtils.GetSpecifiedItem(dnodeArr, instID)
		undeployResult = TDengineDeployUtils.UndeployDnode(dnode, true, logKey, magicKey, paasResult)
		break
	case consts.HEADER_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		undeployResult = CollectdDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	if undeployResult {
		info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	} else {
		info := fmt.Sprintf("service inst_id: %s, undeploy fail ......", servInstID)
		global.GLOBAL_RES.PubFailLog(logKey, info)
	}

	return true
}
