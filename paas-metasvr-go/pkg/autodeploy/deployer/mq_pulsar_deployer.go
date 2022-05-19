package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	PulsarDeployerUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type PulsarDeployer struct {
}

func (h *PulsarDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})
	// 部署zookeeper服务
	zkAddrList := DeployUtils.GetZKAddress(zkArr)
	for idx, zk := range zkArr {
		if !DeployUtils.DeployZookeeper(zk, (idx + 1), version, zkAddrList, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "zookeeper deploy failed ......")
			return false
		}
	}

	// 部署bookkeeper服务
	bookieContainer := servJson[consts.HEADER_PULSAR_BOOKKEEPER_CONTAINER].(map[string]interface{})
	bookieArr := bookieContainer[consts.HEADER_PULSAR_BOOKKEEPER].([]map[string]interface{})
	zkShortAddress := DeployUtils.GetZKShortAddress(zkArr)
	for idx, bookie := range bookieArr {
		initMeta := idx == 0
		if !PulsarDeployerUtils.DeployBookie(bookie, version, zkShortAddress, logKey, magicKey, initMeta, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "bookkeeper deploy failed ......")
			return false
		}
	}

	// 部署pulsar broker服务
	pulsarContainer := servJson[consts.HEADER_PULSAR_BROKER_CONTAINER].(map[string]interface{})
	pulsarArr := pulsarContainer[consts.HEADER_PULSAR_BROKER].([]map[string]interface{})
	pulsarClusterName := pulsarContainer[consts.HEADER_INST_ID].(string)
	brokerAddrList := PulsarDeployerUtils.GetPulsarBrokerList(pulsarArr, "")
	for idx, pulsar := range pulsarArr {
		initMeta := idx == 0
		if !PulsarDeployerUtils.DeployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, zkShortAddress, logKey, magicKey, initMeta, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pulsar deploy failed ......")
			return false
		}
	}

	// 部署pulsar-manager
	pulsarManager := servJson[consts.HEADER_PULSAR_MANAGER].(map[string]interface{})
	if pulsarManager != nil && len(pulsarManager) > 0 {
		bookies := PulsarDeployerUtils.GetPulsarBookieListForPrometheus(bookieArr)
		if !PulsarDeployerUtils.DeployPulsarManager(pulsarManager, bookies, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pulsar-manager deploy failed ......")
			return false
		}
	}

	// 部署prometheus
	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	if prometheus != nil && len(prometheus) > 0 {
		brokers := PulsarDeployerUtils.GetPulsarBrokerListForPrometheus(pulsarArr)
		bookies := PulsarDeployerUtils.GetPulsarBookieListForPrometheus(bookieArr)
		zks := PulsarDeployerUtils.GetPulsarZKListForPrometheus(zkArr)
		if !PulsarDeployerUtils.DeployPulsarPrometheus(prometheus, pulsarClusterName, brokers, bookies, zks, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "prometheus deploy failed ......")
			return false
		}
	}

	// 部署grafana
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})
	if grafana != nil && len(grafana) > 0 {
		if !DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "grafana deploy failed ......")
			return false
		}
	}

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *PulsarDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

	bookieContainer := servJson[consts.HEADER_PULSAR_BOOKKEEPER_CONTAINER].(map[string]interface{})
	bookieArr := bookieContainer[consts.HEADER_PULSAR_BOOKKEEPER].([]map[string]interface{})

	pulsarContainer := servJson[consts.HEADER_PULSAR_BROKER_CONTAINER].(map[string]interface{})
	pulsarArr := pulsarContainer[consts.HEADER_PULSAR_BROKER].([]map[string]interface{})

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	// 卸载grafana
	if grafana != nil && len(grafana) > 0 {
		if !DeployUtils.UndeployGrafana(grafana, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "grafana undeploy failed ......")
			return false
		}
	}

	// 卸载prometheus
	if prometheus != nil && len(prometheus) > 0 {
		if !DeployUtils.UndeployPrometheus(prometheus, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pulsar undeploy failed ......")
			return false
		}
	}

	// 卸载pulsar-manager
	pulsarManager := servJson[consts.HEADER_PULSAR_MANAGER].(map[string]interface{})
	if pulsarManager != nil && len(pulsarManager) > 0 {
		if !PulsarDeployerUtils.UndeployPulsarManager(pulsarManager, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "prometheus deploy failed ......")
			return false
		}
	}

	// 卸载pulsar broker
	for _, pulsar := range pulsarArr {
		if !PulsarDeployerUtils.UndeployPulsar(pulsar, "", "", version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pulsar undeploy failed ......")
			return false
		}
	}

	// 卸载bookkeeper
	for _, bookie := range bookieArr {
		if !PulsarDeployerUtils.UndeployBookie(bookie, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "bookkeeper undeploy failed ......")
			return false
		}
	}

	// 卸载zookkeeper
	for _, zk := range zkArr {
		if !DeployUtils.UndeployZookeeper(zk, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "zookeeper undeploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *PulsarDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

	bookieContainer := servJson[consts.HEADER_PULSAR_BOOKKEEPER_CONTAINER].(map[string]interface{})
	bookieArr := bookieContainer[consts.HEADER_PULSAR_BOOKKEEPER].([]map[string]interface{})

	pulsarContainer := servJson[consts.HEADER_PULSAR_BROKER_CONTAINER].(map[string]interface{})
	pulsarArr := pulsarContainer[consts.HEADER_PULSAR_BROKER].([]map[string]interface{})
	pulsarClusterName := pulsarContainer[consts.HEADER_INST_ID].(string)

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	pulsarManager := servJson[consts.HEADER_PULSAR_MANAGER].(map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	zkShortAddress := DeployUtils.GetZKShortAddress(zkArr)
	bookies := PulsarDeployerUtils.GetPulsarBookieListForPrometheus(bookieArr)

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ZOOKEEPER:
		zk := DeployUtils.GetSpecifiedItem(zkArr, instID)
		zkAddrList := DeployUtils.GetZKAddress(zkArr)
		deployResult = DeployUtils.DeployZookeeper(zk, len(zkArr), version, zkAddrList, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PULSAR_BOOKKEEPER:
		bookie := DeployUtils.GetSpecifiedItem(bookieArr, instID)
		deployResult = PulsarDeployerUtils.DeployBookie(bookie, version, zkShortAddress, logKey, magicKey, false, paasResult)
		break

	case consts.HEADER_PULSAR_BROKER:
		pulsar := DeployUtils.GetSpecifiedItem(pulsarArr, instID)
		brokerAddrList := PulsarDeployerUtils.GetPulsarBrokerList(pulsarArr, "")
		deployResult = PulsarDeployerUtils.DeployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, zkShortAddress, logKey, magicKey, false, paasResult)
		break

	case consts.HEADER_PULSAR_MANAGER:
		deployResult = PulsarDeployerUtils.DeployPulsarManager(pulsarManager, bookies, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PROMETHEUS:
		brokers := PulsarDeployerUtils.GetPulsarBrokerListForPrometheus(pulsarArr)
		zks := PulsarDeployerUtils.GetPulsarZKListForPrometheus(bookieArr)
		deployResult = PulsarDeployerUtils.DeployPulsarPrometheus(prometheus, pulsarClusterName, brokers, bookies, zks, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_GRAFANA:
		deployResult = DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return true
}

func (h *PulsarDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

	bookieContainer := servJson[consts.HEADER_PULSAR_BOOKKEEPER_CONTAINER].(map[string]interface{})
	bookieArr := bookieContainer[consts.HEADER_PULSAR_BOOKKEEPER].([]map[string]interface{})

	pulsarContainer := servJson[consts.HEADER_PULSAR_BROKER_CONTAINER].(map[string]interface{})
	pulsarArr := pulsarContainer[consts.HEADER_PULSAR_BROKER].([]map[string]interface{})

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	pulsarManager := servJson[consts.HEADER_PULSAR_MANAGER].(map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ZOOKEEPER:
		zk := DeployUtils.GetSpecifiedItem(zkArr, instID)
		undeployResult = DeployUtils.UndeployZookeeper(zk, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PULSAR_BOOKKEEPER:
		bookie := DeployUtils.GetSpecifiedItem(bookieArr, instID)
		undeployResult = PulsarDeployerUtils.UndeployBookie(bookie, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PULSAR_BROKER:
		pulsar := DeployUtils.GetSpecifiedItem(pulsarArr, instID)
		pulsarClusterName := pulsarContainer[consts.HEADER_INST_ID].(string)
		brokerAddrList := PulsarDeployerUtils.GetPulsarBrokerList(pulsarArr, instID)
		undeployResult = PulsarDeployerUtils.UndeployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PULSAR_MANAGER:
		undeployResult = PulsarDeployerUtils.UndeployPulsarManager(pulsarManager, version, logKey, magicKey, paasResult)
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
