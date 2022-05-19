package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	ApiSixDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/apisix"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServerlessApisixDeployer struct {
}

func (h *ServerlessApisixDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}
	serv := meta.CMPT_META.GetService(servInstID)

	etcdContainer := servJson[consts.HEADER_ETCD_CONTAINER].(map[string]interface{})
	etcdContainerInstId := etcdContainer[consts.HEADER_INST_ID].(string)
	etcdNodeArr := etcdContainer[consts.HEADER_ETCD].([]map[string]interface{})

	apiSixNodeContainer := servJson[consts.HEADER_APISIX_CONTAINER].(map[string]interface{})
	apiSixNodeArr := apiSixNodeContainer[consts.HEADER_APISIX_SERVER].([]map[string]interface{})
	groupId := apiSixNodeContainer[consts.HEADER_INST_ID].(string)

	if !DeployUtils.CheckBeforeDeploy(serv, etcdNodeArr, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "check before deploy fail ......")
		return false
	}

	etcdLongAddr := DeployUtils.GetEtcdLongAddr(etcdNodeArr)
	etcdShortAddr := DeployUtils.GetEtcdShortAddr(etcdNodeArr)
	etcdFullAddr := DeployUtils.GetEtcdFullAddr(etcdNodeArr)
	for _, etcdNode := range etcdNodeArr {
		if !DeployUtils.DeployEtcdNode(etcdNode, etcdFullAddr, etcdContainerInstId, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "etcd start failed ......")
			return false
		}
	}

	for _, apiSixNode := range apiSixNodeArr {
		if !ApiSixDeployUtils.DeployApiSixNode(apiSixNode, groupId, etcdLongAddr, etcdShortAddr, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "apisix start failed ......")
			return false
		}
	}

	// 部署prometheus
	prometheusRaw := servJson[consts.HEADER_PROMETHEUS]
	if prometheusRaw != nil {
		prometheus := prometheusRaw.(map[string]interface{})
		if len(prometheus) > 0 {
			apisixMetricList := ApiSixDeployUtils.GetApisixMetricList(apiSixNodeArr)
			if !ApiSixDeployUtils.DeployPrometheus(prometheus, servInstID, apisixMetricList, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "prometheus deploy failed ......")
				return false
			}
		}
	}

	// 部署监控组件
	grafanaRaw := servJson[consts.HEADER_GRAFANA]
	if grafanaRaw != nil {
		grafana := grafanaRaw.(map[string]interface{})
		if len(grafana) > 0 {
			if !DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "grafana deploy failed ......")
				return false
			}
		}
	}

	// mod is_deployed flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult)
}

func (h *ServerlessApisixDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})
	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})

	apiSixNodeContainer := servJson[consts.HEADER_APISIX_CONTAINER].(map[string]interface{})
	apiSixNodeArr := apiSixNodeContainer[consts.HEADER_APISIX_SERVER].([]map[string]interface{})

	etcdContainer := servJson[consts.HEADER_ETCD_CONTAINER].(map[string]interface{})
	etcdNodeArr := etcdContainer[consts.HEADER_ETCD].([]map[string]interface{})

	// 1. undeploy grafana and prometheus
	if grafana != nil && len(grafana) > 0 {
		if !DeployUtils.UndeployGrafana(grafana, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "grafana undeploy failed ......")
			return false
		}
	}

	if prometheus != nil && len(prometheus) > 0 {
		if !DeployUtils.UndeployPrometheus(prometheus, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "prometheus undeploy failed ......")
			return false
		}
	}

	// 2. undeploy apisix nodes
	for _, apiSixNode := range apiSixNodeArr {
		if !ApiSixDeployUtils.UndeployApiSixNode(apiSixNode, version, logKey, magicKey, paasResult) {
			info := fmt.Sprintf("service inst_id: %s, undeploy apisix fail ......", servInstID)
			global.GLOBAL_RES.PubFailLog(logKey, info)
		}
	}

	// 3. undeploy etcd nodes
	for _, etcdNode := range etcdNodeArr {
		if !DeployUtils.UndeployEtcdNode(etcdNode, logKey, magicKey, paasResult) {
			info := fmt.Sprintf("service inst_id: %s, undeploy etcd fail ......", servInstID)
			global.GLOBAL_RES.PubFailLog(logKey, info)
		}
	}

	// update deploy flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult)
}

func (h *ServerlessApisixDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	apiSixNodeContainer := servJson[consts.HEADER_APISIX_CONTAINER].(map[string]interface{})
	apiSixNodeArr := apiSixNodeContainer[consts.HEADER_APISIX_SERVER].([]map[string]interface{})

	etcdContainer := servJson[consts.HEADER_ETCD_CONTAINER].(map[string]interface{})
	etcdContainerInstId := etcdContainer[consts.HEADER_INST_ID].(string)
	etcdNodeArr := etcdContainer[consts.HEADER_ETCD].([]map[string]interface{})

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	etcdLongAddr := DeployUtils.GetEtcdLongAddr(etcdNodeArr)
	etcdShortAddr := DeployUtils.GetEtcdShortAddr(etcdNodeArr)
	etcdFullAddr := DeployUtils.GetEtcdFullAddr(etcdNodeArr)

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_APISIX_SERVER:
		apiSixNode := DeployUtils.GetSpecifiedItem(apiSixNodeArr, instID)
		deployResult = ApiSixDeployUtils.DeployApiSixNode(apiSixNode, servInstID, etcdLongAddr, etcdShortAddr, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ETCD:
		etcdNode := DeployUtils.GetSpecifiedItem(etcdNodeArr, instID)
		deployResult = DeployUtils.DeployEtcdNode(etcdNode, etcdFullAddr, etcdContainerInstId, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PROMETHEUS:
		apisixMetricList := ApiSixDeployUtils.GetApisixMetricList(apiSixNodeArr)
		deployResult = ApiSixDeployUtils.DeployPrometheus(prometheus, servInstID, apisixMetricList, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_GRAFANA:
		deployResult = DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return deployResult
}

func (h *ServerlessApisixDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	apiSixNodeContainer := servJson[consts.HEADER_APISIX_CONTAINER].(map[string]interface{})
	apiSixNodeArr := apiSixNodeContainer[consts.HEADER_APISIX_SERVER].([]map[string]interface{})

	etcdContainer := servJson[consts.HEADER_ETCD_CONTAINER].(map[string]interface{})
	etcdNodeArr := etcdContainer[consts.HEADER_ETCD].([]map[string]interface{})

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_APISIX_SERVER:
		apiSixNode := DeployUtils.GetSpecifiedItem(apiSixNodeArr, instID)
		undeployResult = ApiSixDeployUtils.UndeployApiSixNode(apiSixNode, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_ETCD:
		etcdNode := DeployUtils.GetSpecifiedItem(etcdNodeArr, instID)
		undeployResult = DeployUtils.UndeployEtcdNode(etcdNode, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PROMETHEUS:
		undeployResult = DeployUtils.UndeployPrometheus(prometheus, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_GRAFANA:
		undeployResult = DeployUtils.UndeployGrafana(grafana, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	return true
}
