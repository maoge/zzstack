package pulsar

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func GetPulsarBrokerList(pulsarArr []map[string]interface{}, instID string) string {
	result := ""
	for idx, item := range pulsarArr {
		id := item[consts.HEADER_INST_ID].(string)
		sshID := item[consts.HEADER_SSH_ID].(string)
		brokerPort := item[consts.HEADER_BROKER_PORT].(string)

		if instID != "" && id == instID {
			continue
		}

		ssh := meta.CMPT_META.GetSshById(sshID)
		line := fmt.Sprintf("%s:%s", ssh.SERVER_IP, brokerPort)
		if idx == 0 {
			result += "pulsar://"
		} else {
			result += ","
		}

		result += line
	}

	return result
}

func GetPulsarBookieListForPrometheus(bookieArr []map[string]interface{}) string {
	result := ""
	for idx, item := range bookieArr {
		sshID := item[consts.HEADER_SSH_ID].(string)
		httpServPort := item[consts.HEADER_HTTP_SERVER_PORT].(string)

		ssh := meta.CMPT_META.GetSshById(sshID)
		line := fmt.Sprintf("%s:%s", ssh.SERVER_IP, httpServPort)
		if idx > 0 {
			result += consts.METASVR_ADDR_SPLIT
		}

		result += line
	}

	return result
}

func GetPulsarBrokerListForPrometheus(pulsarArr []map[string]interface{}) string {
	result := ""
	for idx, item := range pulsarArr {
		sshID := item[consts.HEADER_SSH_ID].(string)
		webPort := item[consts.HEADER_WEB_PORT].(string)

		ssh := meta.CMPT_META.GetSshById(sshID)
		line := fmt.Sprintf("%s:%s", ssh.SERVER_IP, webPort)
		if idx > 0 {
			result += consts.METASVR_ADDR_SPLIT
		}

		result += line
	}

	return result
}

func GetPulsarZKListForPrometheus(zkArr []map[string]interface{}) string {
	result := ""
	for idx, item := range zkArr {
		sshID := item[consts.HEADER_SSH_ID].(string)
		adminPort := item[consts.HEADER_ADMIN_PORT].(string)

		ssh := meta.CMPT_META.GetSshById(sshID)
		line := fmt.Sprintf("%s:%s", ssh.SERVER_IP, adminPort)
		if idx > 0 {
			result += consts.METASVR_ADDR_SPLIT
		}

		result += line
	}

	return result
}

func DeployBookie(bookie map[string]interface{}, version, zkAddrList, logKey, magicKey string, initMeta bool,
	paasResult *result.ResultBean) bool {

	instId := bookie[consts.HEADER_INST_ID].(string)
	sshId := bookie[consts.HEADER_SSH_ID].(string)
	bookiePort := bookie[consts.HEADER_BOOKIE_PORT].(string)
	httpServerPort := bookie[consts.HEADER_HTTP_SERVER_PORT].(string)
	grpcPort := bookie[consts.HEADER_GRPC_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy bookkeeper: %s:%s, instId:%s", ssh.SERVER_IP, bookiePort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{bookiePort, httpServerPort, grpcPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// BOOKIE_FILE_ID -> 'bookkeeper-4.14.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.BOOKIE_FILE_ID, consts.MQ_PULSAR_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.BOOKIE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, bookiePort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// bookieId=%BOOKIE_ID%
	// bookiePort=%BOOKIE_PORT%
	// advertisedAddress=%ADVERTISED_ADDRESS%
	// httpServerPort=%HTTP_SERVER_PORT%
	// journalDirectories=%JOURNAL_DIRS%
	// ledgerDirectories=%LEDGER_DIRS%
	// metadataServiceUri=%META_DATA_SERVICE_URI%
	// zkServers=%ZK_SERVERS%
	// storageserver.grpc.port=%GRPC_PORT%

	journalDir := "data/bk-txn"
	journalDir = strings.ReplaceAll(journalDir, "/", "\\/")

	ledgerDir := "data/bk-data"
	ledgerDir = strings.ReplaceAll(ledgerDir, "/", "\\/")

	global.GLOBAL_RES.PubLog(logKey, "modify bookie configures ......")
	configFile := "./conf/bk_server.conf"
	DeployUtils.SED(sshClient, consts.CONF_BOOKIE_ID, instId, configFile, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_BOOKIE_PORT, bookiePort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ADVERTISED_ADDRESS, ssh.SERVER_IP, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_HTTP_SERVER_PORT, httpServerPort, configFile, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_JOURNAL_DIRS, journalDir, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LEDGER_DIRS, ledgerDir, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ZK_SERVERS, zkAddrList, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_GRPC_PORT, grpcPort, configFile, logKey, paasResult)

	// stop.sh %BOOKIE_PORT%
	DeployUtils.SED(sshClient, consts.CONF_BOOKIE_PORT, bookiePort, consts.STOP_SHELL, logKey, paasResult)

	// init bookie metadata
	if initMeta {
		initMetaCmd := fmt.Sprintf("bin/bookkeeper shell metaformat")
		global.GLOBAL_RES.PubLog(logKey, initMetaCmd)
		if !DeployUtils.ExecSimpleCmd(sshClient, initMetaCmd, logKey, paasResult) {
			return false
		}
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start bookkeeper ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "bookkeeper", instId, bookiePort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployBookie(bookie map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := bookie[consts.HEADER_INST_ID].(string)
	sshId := bookie[consts.HEADER_SSH_ID].(string)
	bookiePort := bookie[consts.HEADER_BOOKIE_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy bookeeper, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, bookiePort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.BOOKIE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, bookiePort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_PULSAR_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop bookeeper ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "clickhouse", instId, bookiePort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployPulsar(pulsar map[string]interface{}, pulsarClusterName, brokerAddrList, version, zkAddrList, logKey, magicKey string,
	initMeta bool, paasResult *result.ResultBean) bool {

	instId := pulsar[consts.HEADER_INST_ID].(string)
	sshId := pulsar[consts.HEADER_SSH_ID].(string)
	brokerPort := pulsar[consts.HEADER_BROKER_PORT].(string)
	webPort := pulsar[consts.HEADER_WEB_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy pulsar: %s:%s, instId:%s", ssh.SERVER_IP, brokerPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{brokerPort, webPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// PULSAR_FILE_ID -> 'pulsar-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.PULSAR_FILE_ID, consts.MQ_PULSAR_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.PULSAR_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, brokerPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// zookeeperServers=%ZK_SERVERS%
	// configurationStoreServers=%CONF_STORE_SERVERS%
	// brokerServicePort=%BROKER_PORT%
	// webServicePort=%WEB_PORT%
	// bindAddress=%BROKER_ADDRESS%
	// advertisedAddress=%ADVERTISED_ADDRESS%
	// clusterName=%CLUSTER_NAME%
	// bookkeeperMetadataServiceUri=%BOOKIE_META_URI%
	global.GLOBAL_RES.PubLog(logKey, "modify pulsar configures ......")
	configFile := "./conf/broker.conf"

	metaDataServiceUri := fmt.Sprintf("zk+hierarchical://%s/ledgers", zkAddrList)
	metaDataServiceUri = strings.ReplaceAll(metaDataServiceUri, "/", "\\/")
	metaDataServiceUri = strings.ReplaceAll(metaDataServiceUri, ",", ";")

	DeployUtils.SED(sshClient, consts.CONF_ZK_SERVERS, zkAddrList, configFile, logKey, paasResult)
	// 多集群部署时管理多个pulsar集群元数据的zookeeper集群地址，单集群部署时可以和zookeeperServers设置一样
	DeployUtils.SED(sshClient, consts.CONF_STORE_SERVERS, zkAddrList, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BROKER_PORT, brokerPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_WEB_PORT, webPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BROKER_ADDRESS, ssh.SERVER_IP, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ADVERTISED_ADDRESS, ssh.SERVER_IP, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CLUSTER_NAME, pulsarClusterName, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_META_DATA_SERVICE_URI, metaDataServiceUri, configFile, logKey, paasResult)

	// start
	global.GLOBAL_RES.PubLog(logKey, "start pulsar-broker ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "pulsar-broker", instId, brokerPort, logKey, paasResult) {
		return false
	}

	// initialize cluster metadata
	if initMeta {
		global.GLOBAL_RES.PubLog(logKey, "initialize cluster metadata ......")
		initMetaCmd := fmt.Sprintf("./bin/pulsar initialize-cluster-metadata --cluster %s --zookeeper %s --configuration-store %s --web-service-url http://%s:%s --broker-service-url pulsar://%s:%s",
			pulsarClusterName, zkAddrList, zkAddrList, ssh.SERVER_IP, webPort, ssh.SERVER_IP, brokerPort)
		global.GLOBAL_RES.PubLog(logKey, initMetaCmd)
		if !DeployUtils.ExecSimpleCmd(sshClient, initMetaCmd, logKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "initialize cluster fail")
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	} else {
		global.GLOBAL_RES.PubLog(logKey, "update broker-url ......")
		updateUrlCmd := fmt.Sprintf("./bin/pulsar-admin --admin-url http://%s:%s clusters update %s --url http://%s:%s --broker-url %s",
			ssh.SERVER_IP, webPort, pulsarClusterName, ssh.SERVER_IP, webPort, brokerAddrList)
		global.GLOBAL_RES.PubLog(logKey, updateUrlCmd)
		if !DeployUtils.ExecSimpleCmd(sshClient, updateUrlCmd, logKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "update broker-url fail")
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployPulsar(pulsar map[string]interface{}, pulsarClusterName, brokerAddrList, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := pulsar[consts.HEADER_INST_ID].(string)
	sshId := pulsar[consts.HEADER_SSH_ID].(string)
	brokerPort := pulsar[consts.HEADER_BROKER_PORT].(string)
	webPort := pulsar[consts.HEADER_WEB_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy pulsar-broker, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, brokerPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.PULSAR_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, brokerPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_PULSAR_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// 卸载pulsar broker时要从broker列表把即将卸载的broker节点剔除
	if pulsarClusterName != "" && brokerAddrList != "" {
		global.GLOBAL_RES.PubLog(logKey, "update broker-url ......")
		updateUrlCmd := fmt.Sprintf("./bin/pulsar-admin --admin-url http://%s:%s clusters update %s --url http://%s:%s --broker-url %s",
			ssh.SERVER_IP, webPort, pulsarClusterName, ssh.SERVER_IP, webPort, brokerAddrList)
		if !DeployUtils.ExecSimpleCmd(sshClient, updateUrlCmd, logKey, paasResult) {
			return false
		}
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop pulsar-broker ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "pulsar-broker", instId, brokerPort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployPulsarManager(pulsarManager map[string]interface{}, bookies, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := pulsarManager[consts.HEADER_INST_ID].(string)
	sshId := pulsarManager[consts.HEADER_SSH_ID].(string)
	pulsarMgrPort := pulsarManager[consts.HEADER_PULSAR_MGR_PORT].(string)
	herdDbPort := pulsarManager[consts.HEADER_HERDDB_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy dashboard: %s:%s, instId:%s", ssh.SERVER_IP, pulsarMgrPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{pulsarMgrPort, herdDbPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// PULSAR_MANAGER_FILE_ID -> 'pulsar-manager-0.2.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.PULSAR_MANAGER_FILE_ID, consts.MQ_PULSAR_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.PULSAR_MANAGER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, pulsarMgrPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// %PULSAR_MGR_PORT%
	// %HERDDB_PORT%
	// %BOOKIE_LIST%
	global.GLOBAL_RES.PubLog(logKey, "modify pulsar-manager configures ......")
	configFile := "./application.properties"
	DeployUtils.SED(sshClient, consts.CONF_PULSAR_MGR_PORT, pulsarMgrPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_HERDDB_PORT, herdDbPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BOOKIE_LIST, bookies, configFile, logKey, paasResult)

	// start
	global.GLOBAL_RES.PubLog(logKey, "start pulsar-manager ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortsUp(sshClient, "pulsar-manager", instId, checkPorts, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployPulsarManager(pulsarManager map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := pulsarManager[consts.HEADER_INST_ID].(string)
	sshId := pulsarManager[consts.HEADER_SSH_ID].(string)
	pulsarMgrPort := pulsarManager[consts.HEADER_PULSAR_MGR_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy pulsar-manager, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, pulsarMgrPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.PULSAR_MANAGER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, pulsarMgrPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_PULSAR_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop pulsar-manager ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "pulsar-manager", instId, pulsarMgrPort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployPulsarPrometheus(prometheus map[string]interface{}, pulsarClusterName, brokers, bookies, zks, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := prometheus[consts.HEADER_INST_ID].(string)
	sshId := prometheus[consts.HEADER_SSH_ID].(string)
	prometheusPort := prometheus[consts.HEADER_PROMETHEUS_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy prometheus: %s:%s, instId:%s", ssh.SERVER_IP, prometheusPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, prometheusPort, logKey, paasResult) {
		return false
	}

	// PROMETHEUS_FILE_ID -> 'prometheus-2.27.1.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.PROMETHEUS_FILE_ID, consts.COMMON_TOOLS_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.PROMETHEUS_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, prometheusPort)
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// start.sh stop.sh %LISTEN_ADDRESS%
	prometheusAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, prometheusPort)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.STOP_SHELL, logKey, paasResult)

	// scp prometheus_pulsar.yml
	if !DeployUtils.FetchFile(sshClient, consts.PROMETHEUS_PULSAR_YML_FILE_ID, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "scp prometheus_pulsar.yml fail ......")
		return false
	}

	// cluster: %CLUSTER_NAME%
	// %PULSAR_BROKERS%
	// %PULSAR_BOOKIES%
	// %PULSAR_ZOOKEEPERS%
	DeployUtils.SED(sshClient, consts.CONF_CLUSTER_NAME, pulsarClusterName, consts.PROMETHEUS_PULSAR_YML, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult)
	if !DeployUtils.MV(sshClient, consts.PROMETHEUS_YML, consts.PROMETHEUS_PULSAR_YML, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start prometheus ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "prometheus", instId, prometheusPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}
