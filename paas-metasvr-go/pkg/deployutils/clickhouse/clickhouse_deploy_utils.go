package clickhouse

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

func GetRelicaCluster(replicasArr []map[string]interface{}) string {
	// marshell format:
	//    <shard>
	//        <weight>100</weight>
	//        <internal_replication>false</internal_replication>
	//
	//        <replica>
	//            <host>172.20.0.247</host>
	//            <port>9000</port>
	//            <user>default</user>
	//            <password>abcd.1234</password>
	//        </replica>
	//    </shard>
	//
	//    <shard>
	//        <weight>100</weight>
	//        <internal_replication>false</internal_replication>
	//
	//        <replica>
	//            <host>172.20.0.248</host>
	//            <port>9000</port>
	//            <user>default</user>
	//            <password>abcd.1234</password>
	//        </replica>
	//    </shard>

	shards := ""
	for _, replicasNode := range replicasArr {
		replicas := ""
		replicas += "            <shard>"
		replicas += consts.LINE_END

		clickHouseArr := replicasNode[consts.HEADER_CLICKHOUSE_SERVER].([]map[string]interface{})
		replicaType := replicasNode[consts.HEADER_INTERNAL_REPLICATION].(string)
		weight := fmt.Sprintf("                <weight>%d</weight>", consts.CLICKHOUSE_DEFAULT_REPLICA_WEIGHT)
		interReplica := fmt.Sprintf("                <internal_replication>%s</internal_replication>", replicaType)

		replicas += weight + consts.LINE_END
		replicas += interReplica + consts.LINE_END

		for _, clickhouse := range clickHouseArr {
			sshID := clickhouse[consts.HEADER_SSH_ID].(string)
			tcpPort := clickhouse[consts.HEADER_TCP_PORT].(string)

			ssh := meta.CMPT_META.GetSshById(sshID)
			if ssh == nil {
				continue
			}

			host := fmt.Sprintf("                    <host>%s</host>", ssh.SERVER_IP)
			port := fmt.Sprintf("                    <port>%s</port>", tcpPort)
			user := fmt.Sprintf("                    <user>%s</user>", consts.CLICKHOUSE_DEFAULT_USER)
			passwd := fmt.Sprintf("                    <password>%s</password>", consts.CLICKHOUSE_DEFAULT_PASSWD)

			replicas += "                <replica>" + consts.LINE_END
			replicas += host + consts.LINE_END
			replicas += port + consts.LINE_END
			replicas += user + consts.LINE_END
			replicas += passwd + consts.LINE_END
			replicas += "                </replica>" + consts.LINE_END
		}

		replicas += "            </shard>"
		replicas += consts.LINE_END

		shards += replicas
	}

	return shards
}

func GetExporterList(replicasArr []map[string]interface{}) string {
	result := ""
	for _, replicasNode := range replicasArr {
		clickHouseArr := replicasNode[consts.HEADER_CLICKHOUSE_SERVER].([]map[string]interface{})

		for idx, clickhouse := range clickHouseArr {
			sshID := clickhouse[consts.HEADER_SSH_ID].(string)
			port := clickhouse[consts.HEADER_EXPORTER_PORT].(string)

			ssh := meta.CMPT_META.GetSshById(sshID)
			if ssh == nil {
				continue
			}

			exporter := fmt.Sprintf("%s:%s", ssh.SERVER_IP, port)
			if idx > 0 {
				result += ","
			}
			result += exporter
		}
	}

	return result
}

func DeployClickHouseServer(clickhouse map[string]interface{}, version, parentID, replicaCluster, zkCluster, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := clickhouse[consts.HEADER_INST_ID].(string)
	sshId := clickhouse[consts.HEADER_SSH_ID].(string)
	httpPort := clickhouse[consts.HEADER_HTTP_PORT].(string)
	tcpPort := clickhouse[consts.HEADER_TCP_PORT].(string)
	mysqlPort := clickhouse[consts.HEADER_MYSQL_PORT].(string)
	exporterPort := clickhouse[consts.HEADER_EXPORTER_PORT].(string)
	interServerHttpPort := clickhouse[consts.HEADER_INTERSERVER_HTTP_PORT].(string)
	maxConnections := clickhouse[consts.HEADER_MAX_CONNECTIONS].(string)
	maxConcurrentQueries := clickhouse[consts.HEADER_MAX_CONCURRENT_QUERIES].(string)
	maxServerMemoryUsage := clickhouse[consts.HEADER_MAX_SERVER_MEMORY_USAGE].(string)
	maxMemoryUsage := clickhouse[consts.HEADER_MAX_MEMORY_USAGE].(string)

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

	info := fmt.Sprintf("deploy clickhouse-server: %s:%s, instId:%s", ssh.SERVER_IP, tcpPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{httpPort, tcpPort, mysqlPort, exporterPort, interServerHttpPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// DB_CLICKHOUSE_FILE_ID -> 'clickhouse-xxx.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_CLICKHOUSE_FILE_ID, consts.DB_CLICKHOUSE_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_CLICKHOUSE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, tcpPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// etc/config.xml
	// <http_port>%HTTP_PORT%</http_port>
	// <tcp_port>%TCP_PORT%</tcp_port>
	// <mysql_port>%MYSQL_PORT%</mysql_port>
	// <interserver_http_port>%INTERSERVER_HTTP_PORT%</interserver_http_port>
	// <listen_host>%LISTEN_HOST%</listen_host>
	// <max_connections>%MAX_CONNECTIONS%</max_connections>
	// <max_concurrent_queries>%MAX_CONCURRENT_QUERIES%</max_concurrent_queries>
	// <max_server_memory_usage>%MAX_SERVER_MEMORY_USAGE%</max_server_memory_usage>
	// %CLICKHOUSE_SHARDS%
	// %ZK_NODES%
	// <shard>%SHARD_ID%</shard>
	// <replica>%REPLICA_ID%</replica>
	confFile := "etc/config.xml"
	if !DeployUtils.SED(sshClient, consts.CONF_HTTP_PORT, httpPort, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_TCP_PORT, tcpPort, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MYSQL_PORT, mysqlPort, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_INTERSERVER_HTTP_PORT, interServerHttpPort, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_HOST, ssh.SERVER_IP, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_CONNECTIONS, maxConnections, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_CONCURRENT_QUERIES, maxConcurrentQueries, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_SERVER_MEMORY_USAGE, maxServerMemoryUsage, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.AppendMultiLine(sshClient, consts.CONF_CLICKHOUSE_SHARDS, replicaCluster, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.AppendMultiLine(sshClient, consts.CONF_ZK_NODES, zkCluster, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_SHARD_ID, parentID, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_REPLICA_ID, instId, confFile, logKey, paasResult) {
		return false
	}

	// etc/user.xml
	// <max_memory_usage>%MAX_MEMORY_USAGE%</max_memory_usage>
	// <password>%PASSWORD%</password>
	userFile := "etc/users.xml"
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_MEMORY_USAGE, maxMemoryUsage, userFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PASSWORD, consts.CLICKHOUSE_DEFAULT_PASSWD, userFile, logKey, paasResult) {
		return false
	}

	// start.sh
	// export SCRAPE_URI=%SCRAPE_URI%
	// export TELEMETRY_ADDRESS=%TELEMETRY_ADDRESS%
	// export CLICKHOUSE_USER=%CLICKHOUSE_USER%
	// export CLICKHOUSE_PASSWORD=%CLICKHOUSE_PASSWORD%
	scrapeUri := fmt.Sprintf("http://%s:%s/", ssh.SERVER_IP, httpPort)
	scrapeUri = strings.ReplaceAll(scrapeUri, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_SCRAPE_URI, scrapeUri, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	telemetryAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, exporterPort)
	if !DeployUtils.SED(sshClient, consts.CONF_TELEMETRY_ADDRESS, telemetryAddr, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	if !DeployUtils.SED(sshClient, consts.CONF_CLICKHOUSE_USER, consts.CLICKHOUSE_DEFAULT_USER, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	if !DeployUtils.SED(sshClient, consts.CONF_CLICKHOUSE_PASSWORD, consts.CLICKHOUSE_DEFAULT_PASSWD, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start clickhouse ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "clickhouse", instId, tcpPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployClickHouseServer(clickhouse map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := clickhouse[consts.HEADER_INST_ID].(string)
	sshId := clickhouse[consts.HEADER_SSH_ID].(string)
	tcpPort := clickhouse[consts.HEADER_TCP_PORT].(string)

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

	info := fmt.Sprintf("start undeploy clickhouse, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, tcpPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_CLICKHOUSE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, tcpPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_CLICKHOUSE_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop clickhouse ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "clickhouse", instId, tcpPort, logKey, paasResult) {
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

func DeployPrometheus(prometheus map[string]interface{}, clusterName, exporters, version, logKey, magicKey string,
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
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// scp prometheus_clickhouse.yml
	if !DeployUtils.FetchFile(sshClient, consts.PROMETHEUS_CLICKHOUSE_YML_FILE_ID, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "scp prometheus_clickhouse.yml fail ......")
		return false
	}

	// cluster: %CLUSTER_NAME%
	// targets: [%CLICKHOUSE_EXPORTER_LIST%]
	if !DeployUtils.SED(sshClient, consts.CONF_CLUSTER_NAME, clusterName, consts.PROMETHEUS_CLICKHOUSE_YML, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_CLICKHOUSE_EXPORTER_LIST, exporters, consts.PROMETHEUS_CLICKHOUSE_YML, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, consts.PROMETHEUS_YML, consts.PROMETHEUS_CLICKHOUSE_YML, logKey, paasResult) {
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
