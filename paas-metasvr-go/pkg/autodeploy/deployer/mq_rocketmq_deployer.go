package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	CollectdDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	RocketMqDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/rocketmq"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type RocketMQDeployer struct {
}

func (h *RocketMQDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	// 伪部署
	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RocketMqDeployUtils.ModFakeServiceDeployFlag(servJson, servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	// 物理部署
	// 部署namesrv服务
	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})
	for _, nameSrv := range nameSrvArr {
		if !RocketMqDeployUtils.DeployNameSrv(nameSrv, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "rocketmq namesrv deploy failed ......")
			return false
		}
	}

	// 部署broker服务
	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbrokerArr := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})
	namesrvAddrs := RocketMqDeployUtils.GetNameSrvAddrs(nameSrvArr)
	for _, vbroker := range vbrokerArr {
		// String vbrokerInstId = vbroker.getString(FixHeader.HEADER_INST_ID);
		brokerArr := vbroker[consts.HEADER_ROCKETMQ_BROKER].([]map[string]interface{})
		for idx, broker := range brokerArr {
			brokerId := fmt.Sprintf("%d", idx)
			if !RocketMqDeployUtils.DeployBroker(broker, servInstID, namesrvAddrs, brokerId, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq broker start failed ......")
				return false
			}
		}
	}

	// 部署collectd服务
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !CollectdDeployUtils.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "collectd deploy failed ......")
				return false
			}
		}
	}

	// 部署rocketmq-console
	singleNameSrv := RocketMqDeployUtils.GetSingleNameSrvAddrs(nameSrvArr)
	consoleRaw := servJson[consts.HEADER_ROCKETMQ_CONSOLE]
	if consoleRaw != nil {
		console := consoleRaw.(map[string]interface{})
		if len(console) > 0 {
			if !RocketMqDeployUtils.DeployConsole(console, servInstID, singleNameSrv, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq-console deploy failed ......")
				return false
			}
		}
	}

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *RocketMQDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	deployFlag := serv.PSEUDO_DEPLOY_FLAG

	// 伪部署反向操作
	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RocketMqDeployUtils.ModFakeServiceDeployFlag(servJson, servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, undeploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	//卸载broker服务
	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbrokerArr := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})
	for _, vbroker := range vbrokerArr {
		brokerArr := vbroker[consts.HEADER_ROCKETMQ_BROKER].([]map[string]interface{})
		for _, broker := range brokerArr {
			if !RocketMqDeployUtils.UndeployBroker(broker, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq broker undeploy failed ......")
				return false
			}
		}
	}

	// 卸载namesrv服务
	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})
	for _, nameSrv := range nameSrvArr {
		if !RocketMqDeployUtils.UndeployNameSrv(nameSrv, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "rocketmq namesrv undeploy failed ......")
			return false
		}
	}

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

	// 卸载rockmetmq-console服务
	consoleRaw := servJson[consts.HEADER_ROCKETMQ_CONSOLE]
	if consoleRaw != nil {
		console := consoleRaw.(map[string]interface{})
		if len(console) > 0 {
			if !RocketMqDeployUtils.UndeployConsole(console, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq-console undeploy failed ......")
				return false
			}
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *RocketMQDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})

	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbrokerArr := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})

	namesrvAddrs := RocketMqDeployUtils.GetNameSrvAddrs(nameSrvArr)
	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ROCKETMQ_NAMESRV:
		nameSrv := DeployUtils.GetSpecifiedItem(nameSrvArr, instID)
		deployResult = RocketMqDeployUtils.DeployNameSrv(nameSrv, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ROCKETMQ_BROKER:
		broker := DeployUtils.GetSpecifiedRocketMQBroker(vbrokerArr, instID)
		vbrokerInstId := DeployUtils.GetSpecifiedVBrokerId(vbrokerArr, instID)
		brokerArr := DeployUtils.GetSpecifiedBrokerArr(vbrokerArr, instID)
		brokerId := fmt.Sprintf("%d", len(brokerArr)-1)
		deployResult = RocketMqDeployUtils.DeployBroker(broker, vbrokerInstId, namesrvAddrs, brokerId, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		deployResult = CollectdDeployUtils.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ROCKETMQ_CONSOLE:
		console := servJson[consts.HEADER_ROCKETMQ_CONSOLE].(map[string]interface{})
		deployResult = RocketMqDeployUtils.DeployConsole(console, servInstID, namesrvAddrs, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return true
}

func (h *RocketMQDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})

	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbrokerArr := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ROCKETMQ_NAMESRV:
		nameSrv := DeployUtils.GetSpecifiedItem(nameSrvArr, instID)
		undeployResult = RocketMqDeployUtils.UndeployNameSrv(nameSrv, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ROCKETMQ_BROKER:
		broker := DeployUtils.GetSpecifiedRocketMQBroker(vbrokerArr, instID)
		undeployResult = RocketMqDeployUtils.UndeployBroker(broker, logKey, magicKey, paasResult)
		break

	case consts.HEADER_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		undeployResult = CollectdDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ROCKETMQ_CONSOLE:
		console := servJson[consts.HEADER_ROCKETMQ_CONSOLE].(map[string]interface{})
		undeployResult = RocketMqDeployUtils.UndeployConsole(console, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	return true
}
