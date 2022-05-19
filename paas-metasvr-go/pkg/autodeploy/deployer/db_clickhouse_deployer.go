package deployer

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	ClickHouseDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/clickhouse"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ClickHouseDeployer struct {
}

func (h *ClickHouseDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

	// deploy zookeeper instances
	zkAddrList := DeployUtils.GetZKAddress(zkArr)
	for idx, zk := range zkArr {
		if !DeployUtils.DeployZookeeper(zk, (idx + 1), version, zkAddrList, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "zookeeper deploy failed ......")
			return false
		}
	}

	// deploy clickhouse instances
	replicasContainer := servJson[consts.HEADER_CLICKHOUSE_REPLICAS_CONTAINER].(map[string]interface{})
	replicasArr := replicasContainer[consts.HEADER_CLICKHOUSE_REPLICAS].([]map[string]interface{})
	replicaCluster := ClickHouseDeployUtils.GetRelicaCluster(replicasArr)
	zkCluster := DeployUtils.GetZkCluster(zkArr)

	replicaCluster = strings.ReplaceAll(replicaCluster, "/", "\\/")
	replicaCluster = strings.ReplaceAll(replicaCluster, "\n", "\\\n")

	zkCluster = strings.ReplaceAll(zkCluster, "/", "\\/")
	zkCluster = strings.ReplaceAll(zkCluster, "\n", "\\\n")
	for _, replicas := range replicasArr {
		replicasID := replicas[consts.HEADER_INST_ID].(string)
		clickHouseArr := replicas[consts.HEADER_CLICKHOUSE_SERVER].([]map[string]interface{})
		for _, clickhouse := range clickHouseArr {
			if !ClickHouseDeployUtils.DeployClickHouseServer(clickhouse, version, replicasID, replicaCluster, zkCluster, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubErrorLog(logKey, "clickhouse-server deploy failed ......")
				return false
			}
		}
	}

	// 部署监控组件
	// CLICKHOUSE_EXPORTER -> PROMETHEUS -> GRAFANA
	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})
	if prometheus != nil && len(prometheus) != 0 {
		exporters := ClickHouseDeployUtils.GetExporterList(replicasArr)
		if !ClickHouseDeployUtils.DeployPrometheus(prometheus, servInstID, exporters, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "prometheus deploy failed ......")
			return false
		}
	}

	if grafana != nil && len(grafana) != 0 {
		if !DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "grafana deploy failed ......")
			return false
		}
	}

	// mod is_deployed flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult)
}

func (h *ClickHouseDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

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
			global.GLOBAL_RES.PubFailLog(logKey, "prometheus undeploy failed ......")
			return false
		}
	}

	// 卸载clickhouse服务
	replicasContainer := servJson[consts.HEADER_CLICKHOUSE_REPLICAS_CONTAINER].(map[string]interface{})
	replicasArr := replicasContainer[consts.HEADER_CLICKHOUSE_REPLICAS].([]map[string]interface{})
	for _, replicas := range replicasArr {
		clickHouseArr := replicas[consts.HEADER_CLICKHOUSE_SERVER].([]map[string]interface{})
		for _, clickhouse := range clickHouseArr {
			if !ClickHouseDeployUtils.UndeployClickHouseServer(clickhouse, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "clickhouse-server undeploy failed ......")
				return false
			}
		}
	}

	// 卸载zookeeper服务
	for _, zk := range zkArr {
		if !DeployUtils.UndeployZookeeper(zk, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "zookeeper undeploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult)
}

func (h *ClickHouseDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	zkContainer := servJson[consts.HEADER_ZOOKEEPER_CONTAINER].(map[string]interface{})
	zkArr := zkContainer[consts.HEADER_ZOOKEEPER].([]map[string]interface{})

	replicasContainer := servJson[consts.HEADER_CLICKHOUSE_REPLICAS_CONTAINER].(map[string]interface{})
	replicasArr := replicasContainer[consts.HEADER_CLICKHOUSE_REPLICAS].([]map[string]interface{})

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	replicaCluster := ClickHouseDeployUtils.GetRelicaCluster(replicasArr)
	zkCluster := DeployUtils.GetZkCluster(zkArr)

	replicaCluster = strings.ReplaceAll(replicaCluster, "/", "\\/")
	replicaCluster = strings.ReplaceAll(replicaCluster, "\n", "\\\n")

	zkCluster = strings.ReplaceAll(zkCluster, "/", "\\/")
	zkCluster = strings.ReplaceAll(zkCluster, "\n", "\\\n")

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ZOOKEEPER:
		zk := DeployUtils.GetSpecifiedItem(zkArr, instID)
		zkAddrList := DeployUtils.GetZKAddress(zkArr)
		deployResult = DeployUtils.DeployZookeeper(zk, len(zkArr), version, zkAddrList, logKey, magicKey, paasResult)
		break

	case consts.HEADER_PROMETHEUS:
		exporters := ClickHouseDeployUtils.GetExporterList(replicasArr)
		deployResult = ClickHouseDeployUtils.DeployPrometheus(prometheus, servInstID, exporters, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_GRAFANA:
		deployResult = DeployUtils.DeployGrafana(grafana, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_CLICKHOUSE_SERVER:
		clickhouse, replicasID := DeployUtils.GetSpecifiedClickHouseItem(replicasArr, instID)
		deployResult = ClickHouseDeployUtils.DeployClickHouseServer(clickhouse, version, replicasID, replicaCluster, zkCluster, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return deployResult
}

func (h *ClickHouseDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	prometheus := servJson[consts.HEADER_PROMETHEUS].(map[string]interface{})
	grafana := servJson[consts.HEADER_GRAFANA].(map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_ZOOKEEPER:
	case consts.HEADER_CLICKHOUSE_SERVER:
		// 缩容复杂，非特殊情况不做缩容
		info := fmt.Sprintf("service inst_id:%s, undeploy not support ......", servInstID)
		global.GLOBAL_RES.PubFailLog(logKey, info)
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
	return undeployResult
}
