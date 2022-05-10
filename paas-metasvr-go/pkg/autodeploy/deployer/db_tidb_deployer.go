package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	TiDBDeployerUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/tidb"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type TiDBDeployer struct {
}

func (h *TiDBDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
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
	pdContainer := servJson[consts.HEADER_PD_SERVER_CONTAINER].(map[string]interface{})
	pdArr := pdContainer[consts.HEADER_PD_SERVER].([]map[string]interface{})
	// 部署pd-server服务
	pdLongAddr := TiDBDeployerUtils.GetPDLongAddress(pdArr)
	pdShortAddr := TiDBDeployerUtils.GetPDShortAddress(pdArr)
	for _, pd := range pdArr {
		if !TiDBDeployerUtils.DeployPdServer(pd, version, pdLongAddr, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pd-server deploy failed ......")
			return false
		}
	}

	// 部署tikv-server服务
	tikvServerContainer := servJson[consts.HEADER_TIKV_SERVER_CONTAINER].(map[string]interface{})
	tikvArr := tikvServerContainer[consts.HEADER_TIKV_SERVER].([]map[string]interface{})
	for _, tikv := range tikvArr {
		if !TiDBDeployerUtils.DeployTikvServer(tikv, version, pdShortAddr, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "tikv-server deploy failed ......")
			return false
		}
	}

	// 部署tidb-server服务
	tidbServerContainer := servJson[consts.HEADER_TIDB_SERVER_CONTAINER].(map[string]interface{})
	tidbArr := tidbServerContainer[consts.HEADER_TIDB_SERVER].([]map[string]interface{})
	for _, tidb := range tidbArr {
		if !TiDBDeployerUtils.DeployTidbServer(tidb, version, pdShortAddr, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "tidb-server deploy failed ......")
			return false
		}
	}

	// tidb服务部署完成修改root密码,默认密码为空串
	DeployUtils.ResetDBPwd(tidbArr[0], logKey, paasResult)

	// 部署dashboard-proxy
	dashboard := servJson[consts.HEADER_DASHBOARD_PROXY].(map[string]interface{})
	if dashboard != nil && len(dashboard) > 0 {
		pdAddress := TiDBDeployerUtils.GetFirstPDAddress(pdArr)
		if !TiDBDeployerUtils.DeployDashboard(dashboard, version, pdAddress, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "dashboard proxy deploy failed ......")
			return false
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

func (h *TiDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
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

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})

	// 卸载tidb-server服务
	tidbServerContainer := servJson[consts.HEADER_TIDB_SERVER_CONTAINER].(map[string]interface{})
	tidbServer := tidbServerContainer[consts.HEADER_TIDB_SERVER].([]map[string]interface{})
	for _, tidb := range tidbServer {
		if !TiDBDeployerUtils.UndeployTidbServer(tidb, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "tidb-server undeploy failed ......")
			return false
		}
	}

	pdServerContainer := servJson[consts.HEADER_PD_SERVER_CONTAINER].(map[string]interface{})
	pdServerArr := pdServerContainer[consts.HEADER_PD_SERVER].([]map[string]interface{})

	tikvServerContainer := servJson[consts.HEADER_TIKV_SERVER_CONTAINER].(map[string]interface{})
	tikvServerArr := tikvServerContainer[consts.HEADER_TIKV_SERVER].([]map[string]interface{})

	pdCtl := pdServerArr[0]

	// 卸载dashboard-proxy
	dashboard := servJson[consts.HEADER_DASHBOARD_PROXY].(map[string]interface{})
	if dashboard != nil && len(dashboard) > 0 {
		if !TiDBDeployerUtils.UndeployDashboard(dashboard, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "dashboard-proxy undeploy failed ......")
			return false
		}
	}

	// 卸载tikv-server服务
	for _, tikv := range tikvServerArr {
		if !TiDBDeployerUtils.UndeployTikvServer(tikv, pdCtl, version, logKey, magicKey, true, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "tikv-server undeploy failed ......")
			return false
		}
	}

	// 卸载pd-server服务
	for _, pd := range pdServerArr {
		if !TiDBDeployerUtils.UndeployPdServer(pd, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pd-server undeploy failed ......")
			return false
		}
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

func (h *TiDBDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
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

	pdContainer := servJson[consts.HEADER_PD_SERVER_CONTAINER].(map[string]interface{})
	tikvContainer := servJson[consts.HEADER_TIKV_SERVER_CONTAINER].(map[string]interface{})
	tidbContainer := servJson[consts.HEADER_TIDB_SERVER_CONTAINER].(map[string]interface{})
	dashboard := servJson[consts.HEADER_DASHBOARD_PROXY].(map[string]interface{})

	pdArr := pdContainer[consts.HEADER_PD_SERVER].([]map[string]interface{})
	tikvArr := tikvContainer[consts.HEADER_TIKV_SERVER].([]map[string]interface{})
	tidbArr := tidbContainer[consts.HEADER_TIDB_SERVER].([]map[string]interface{})

	pdShortAddr := TiDBDeployerUtils.GetPDShortAddress(pdArr)
	pdLongAddr := TiDBDeployerUtils.GetPDLongAddress(pdArr)

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_TIDB_SERVER:
		tidbItem := DeployUtils.GetSpecifiedItem(tidbArr, instID)
		deployResult = TiDBDeployerUtils.DeployTidbServer(tidbItem, version, pdShortAddr, logKey, magicKey, paasResult)
		break
	case consts.HEADER_TIKV_SERVER:
		tikvItem := DeployUtils.GetSpecifiedItem(tikvArr, instID)
		deployResult = TiDBDeployerUtils.DeployTikvServer(tikvItem, version, pdShortAddr, logKey, magicKey, paasResult)
		break
	case consts.HEADER_PD_SERVER:
		pdItem := DeployUtils.GetSpecifiedItem(pdArr, instID)
		deployResult = TiDBDeployerUtils.DeployPdServer(pdItem, version, pdLongAddr, logKey, magicKey, paasResult)
		break
	case consts.HEADER_DASHBOARD_PROXY:
		pdAddress := TiDBDeployerUtils.GetFirstPDAddress(pdArr)
		deployResult = TiDBDeployerUtils.DeployDashboard(dashboard, version, pdAddress, logKey, magicKey, paasResult)
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

	return deployResult
}

func (h *TiDBDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
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

	pdContainer := servJson[consts.HEADER_PD_SERVER_CONTAINER].(map[string]interface{})
	tikvContainer := servJson[consts.HEADER_TIKV_SERVER_CONTAINER].(map[string]interface{})
	tidbContainer := servJson[consts.HEADER_TIDB_SERVER_CONTAINER].(map[string]interface{})
	dashboard := servJson[consts.HEADER_DASHBOARD_PROXY].(map[string]interface{})

	pdArr := pdContainer[consts.HEADER_PD_SERVER].([]map[string]interface{})
	tikvArr := tikvContainer[consts.HEADER_TIKV_SERVER].([]map[string]interface{})
	tidbArr := tidbContainer[consts.HEADER_TIDB_SERVER].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_TIDB_SERVER:
		tidbItem := DeployUtils.GetSpecifiedItem(tidbArr, instID)
		undeployResult = TiDBDeployerUtils.UndeployTidbServer(tidbItem, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_TIKV_SERVER:
		tikvItem := DeployUtils.GetSpecifiedItem(tikvArr, instID)
		pdCtl := pdArr[0]
		undeployResult = TiDBDeployerUtils.UndeployTikvServer(tikvItem, pdCtl, version, logKey, magicKey, false, paasResult)
		break
	case consts.HEADER_PD_SERVER:
		pdItem := DeployUtils.GetSpecifiedItem(pdArr, instID)
		undeployResult = TiDBDeployerUtils.UndeployPdServer(pdItem, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_DASHBOARD_PROXY:
		undeployResult = TiDBDeployerUtils.UndeployDashboard(dashboard, version, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	return undeployResult
}
