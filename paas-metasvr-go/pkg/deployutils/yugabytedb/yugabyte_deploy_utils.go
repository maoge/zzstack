package yugabytedb

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func GetYbMasterList(ybMasterArr []map[string]interface{}) string {
	result := ""
	for idx, ybMaster := range ybMasterArr {
		sshId := ybMaster[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshId)
		if ssh == nil {
			continue
		}

		if idx > 0 {
			result += ","
		}

		rpcBindPort := ybMaster[consts.HEADER_RPC_BIND_PORT].(string)
		result += fmt.Sprintf("%s:%s", ssh.SERVER_IP, rpcBindPort)
	}

	return result
}

func DeployMaster(ybMaster map[string]interface{}, version, masterList, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := ybMaster[consts.HEADER_INST_ID].(string)
	sshId := ybMaster[consts.HEADER_SSH_ID].(string)

	rpcBindPort := ybMaster[consts.HEADER_RPC_BIND_PORT].(string)
	webServPort := ybMaster[consts.HEADER_WEBSERVER_PORT].(string)
	durableWalWrite := ybMaster[consts.HEADER_DURABLE_WAL_WRITE].(string)
	enableLoadBalancing := ybMaster[consts.HEADER_ENABLE_LOAD_BALANCING].(string)
	maxClockSkewUsec := ybMaster[consts.HEADER_MAX_CLOCK_SKEW_USEC].(string)
	replicFactor := ybMaster[consts.HEADER_REPLICATION_FACTOR].(string)
	ybNumShardsPerTServer := ybMaster[consts.HEADER_YB_NUM_SHARDS_PER_TSERVER].(string)
	ysqlNumShardsPerTServer := ybMaster[consts.HEADER_YSQL_NUM_SHARDS_PER_TSERVER].(string)
	placementCloud := ybMaster[consts.HEADER_PLACEMENT_CLOUD].(string)
	placementZone := ybMaster[consts.HEADER_PLACEMENT_ZONE].(string)
	placementRegion := ybMaster[consts.HEADER_PLACEMENT_REGION].(string)
	cdcWalRetentionTimeSecs := ybMaster[consts.HEADER_CDC_WAL_RETENTION_TIME_SECS].(string)

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

	info := fmt.Sprintf("deploy yb-master: %s:%s, instId:%s", ssh.SERVER_IP, rpcBindPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{rpcBindPort, webServPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// DB_YUGABYTEDB_FILE_ID -> 'yugabyte-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_YUGABYTEDB_FILE_ID, consts.DB_YUGABYTEDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_YUGABYTEDB_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("yb-master_%s", rpcBindPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	currPath, _ := DeployUtils.PWD(sshClient, logKey, paasResult)

	//  etc/yb-master.conf
	//  %YB_MASTER_ADDR% 替换为master1:bind_port1,master2:bind_port2,master3:bind_port3
	//  %FS_DATA_DIRS% 替换为masterList
	//  %RPC_BIND_ADDR% 替换为masterIp:rpc_bind_port
	//  %SERV_BROADCAST_ADDR% 替换为masterIp
	//  %WEBSERV_INTERFACE% 替换为masterIp
	//  %WEBSERV_PORT% 替换为$WEBSERV_PORT
	//  %DURABLE_WAL_WRITE% 替换为$DURABLE_WAL_WRITE
	//  %ENABLE_LOAD_BALANCING% 替换为$ENABLE_LOAD_BALANCING
	//  %MAX_CLOCK_SKEW_USEC% 替换为$MAX_CLOCK_SKEW_USEC
	//  %YB_NUM_SHARDS_PER_TSERVER% 替换为$YB_NUM_SHARDS_PER_TSERVER
	//  %YSQL_NUM_SHARDS_PER_TSERVER% 替换为$YSQL_NUM_SHARDS_PER_TSERVER
	//  %PLACEMENT_CLOUD% 替换为$PLACEMENT_CLOUD
	//  %PLACEMENT_ZONE% 替换为$PLACEMENT_ZONE
	//  %PLACEMENT_REGION% 替换为$PLACEMENT_REGION
	//  %CDC_WAL_RETENTION_TIME_SECS% 替换为$CDC_WAL_RETENTION_TIME_SECS
	global.GLOBAL_RES.PubLog(logKey, "modify etc/yb-master.conf params ......")
	ybMasterConf := "./etc/yb-master.conf"
	fsDataDirs := fmt.Sprintf("%s/data", currPath) // .replaceAll("/", "\\\\/");
	rpcBindAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, rpcBindPort)

	DeployUtils.SED(sshClient, consts.CONF_YB_MASTER_ADDR, masterList, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_FS_DATA_DIRS, fsDataDirs, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_RPC_BIND_ADDR, rpcBindAddr, ybMasterConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_SERV_BROADCAST_ADDR, ssh.SERVER_IP, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_WEBSERV_INTERFACE, ssh.SERVER_IP, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_WEBSERVER_PORT, webServPort, ybMasterConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_DURABLE_WAL_WRITE, durableWalWrite, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ENABLE_LOAD_BALANCING, enableLoadBalancing, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MAX_CLOCK_SKEW_USEC, maxClockSkewUsec, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_REPLICATION_FACTOR, replicFactor, ybMasterConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_YB_NUM_SHARDS_PER_TSERVER, ybNumShardsPerTServer, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_YSQL_NUM_SHARDS_PER_TSERVER, ysqlNumShardsPerTServer, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_CLOUD, placementCloud, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_ZONE, placementZone, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_REGION, placementRegion, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CDC_WAL_RETENTION_TIME_SECS, cdcWalRetentionTimeSecs, ybMasterConf, logKey, paasResult)

	// 修改启停脚本
	DeployUtils.MV(sshClient, consts.START_SHELL, consts.YB_MASTER_START_SHELL, logKey, paasResult)
	DeployUtils.MV(sshClient, consts.STOP_SHELL, consts.YB_MASTER_STOP_SHELL, logKey, paasResult)

	DeployUtils.RM(sshClient, consts.YB_TSERVER_START_SHELL, logKey, paasResult)
	DeployUtils.RM(sshClient, consts.YB_TSERVER_STOP_SHELL, logKey, paasResult)
	DeployUtils.RM(sshClient, consts.YB_TSERVER_CONF, logKey, paasResult)

	// 先执行符号路径替换
	global.GLOBAL_RES.PubLog(logKey, "replacing symbol link ......")
	preInstallCmd := "bin/post_install.sh"
	if !DeployUtils.ExecSimpleCmd(sshClient, preInstallCmd, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start yb-master ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "yb-master", instId, rpcBindPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployMaster(ybMaster map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := ybMaster[consts.HEADER_INST_ID].(string)
	sshId := ybMaster[consts.HEADER_SSH_ID].(string)
	rpcBindPort := ybMaster[consts.HEADER_RPC_BIND_PORT].(string)

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

	info := fmt.Sprintf("start undeploy yb-master, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, rpcBindPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("yb-master_%s", rpcBindPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_YUGABYTEDB_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop yb-master ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "yb-master", instId, rpcBindPort, logKey, paasResult) {
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

func DeployTServer(ybTServer map[string]interface{}, version, masterList, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := ybTServer[consts.HEADER_INST_ID].(string)
	sshId := ybTServer[consts.HEADER_SSH_ID].(string)

	maxClockSkewUsec := ybTServer[consts.HEADER_MAX_CLOCK_SKEW_USEC].(string)
	rpcBindPort := ybTServer[consts.HEADER_RPC_BIND_PORT].(string)
	webServPort := ybTServer[consts.HEADER_WEBSERVER_PORT].(string)
	durableWalWrite := ybTServer[consts.HEADER_DURABLE_WAL_WRITE].(string)
	ybNumShardsPerTServer := ybTServer[consts.HEADER_YB_NUM_SHARDS_PER_TSERVER].(string)
	ysqlNumShardsPerTServer := ybTServer[consts.HEADER_YSQL_NUM_SHARDS_PER_TSERVER].(string)
	placementCloud := ybTServer[consts.HEADER_PLACEMENT_CLOUD].(string)
	placementZone := ybTServer[consts.HEADER_PLACEMENT_ZONE].(string)
	placementRegion := ybTServer[consts.HEADER_PLACEMENT_REGION].(string)
	pgProxyBindPort := ybTServer[consts.HEADER_PGSQL_PROXY_BIND_PORT].(string)
	pgProxyWebServPort := ybTServer[consts.HEADER_PGSQL_PROXY_WEBSERVER_PORT].(string)
	cqlProxyBindPort := ybTServer[consts.HEADER_CQL_PROXY_BIND_PORT].(string)
	cqlProxyWebservPort := ybTServer[consts.HEADER_CQL_PROXY_WEBSERVER_PORT].(string)
	ysqlMaxConnections := ybTServer[consts.HEADER_YSQL_MAX_CONNECTIONS].(string)
	rocksdbCompactFlushRateLimitBytesPerSec := ybTServer[consts.HEADER_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC].(string)
	rocksdbUniversalCompactionMinMergeWidth := ybTServer[consts.HEADER_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH].(string)
	rocksdbUniversalCompactionSizeRatio := ybTServer[consts.HEADER_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO].(string)
	timestampHistoryRetentionIntervalSec := ybTServer[consts.HEADER_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC].(string)
	remoteBootstrapRateLimitBytesPerSec := ybTServer[consts.HEADER_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC].(string)

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

	info := fmt.Sprintf("deploy yb-tserver: %s:%s, instId:%s", ssh.SERVER_IP, rpcBindPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{rpcBindPort, webServPort, pgProxyBindPort, pgProxyWebServPort, cqlProxyBindPort, cqlProxyWebservPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// DB_YUGABYTEDB_FILE_ID -> 'yugabyte-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_YUGABYTEDB_FILE_ID, consts.DB_YUGABYTEDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_YUGABYTEDB_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("yb-tserver_%s", rpcBindPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	currPath, _ := DeployUtils.PWD(sshClient, logKey, paasResult)

	// etc/yb-tserver.conf
	// %MASTER_ADDRS% 替换为master1:bind_port1,master2:bind_port2,master3:bind_port3
	// %FS_DATA_DIRS% 替换为masterList
	// %MAX_CLOCK_SKEW_USEC% 替换为$MAX_CLOCK_SKEW_USEC
	// %RPC_BIND_ADDR% 替换为servIp:rpcBindPort
	// %SERV_BROADCAST_ADDR% 替换为servIp
	// %WEBSERV_INTERFACE% 替换为servIp
	// %WEBSERV_PORT% 替换为$WEBSERV_PORT
	// %DURABLE_WAL_WRITE% 替换为$DURABLE_WAL_WRITE
	// %YB_NUM_SHARDS_PER_TSERVER% 替换为$YB_NUM_SHARDS_PER_TSERVER
	// %YSQL_NUM_SHARDS_PER_TSERVER% 替换为$YSQL_NUM_SHARDS_PER_TSERVER
	// %PLACEMENT_CLOUD% 替换为$PLACEMENT_CLOUD
	// %PLACEMENT_ZONE% 替换为$PLACEMENT_ZONE
	// %PLACEMENT_REGION% 替换为$PLACEMENT_REGION
	// %PGSQL_PROXY_BIND_ADDR% 替换为$servIp:$PGSQL_PROXY_BIND_PORT
	// %PGSQL_PROXY_WEBSERVER_PORT% 替换为$PGSQL_PROXY_WEBSERVER_PORT
	// %YSQL_MAX_CONNECTIONS% 替换为$YSQL_MAX_CONNECTIONS
	// %CQL_PROXY_BIND_ADDR% 替换为servIp:$CQL_PROXY_BIND_PORT
	// %CQL_PROXY_WEBSERVER_PORT% 替换为$CQL_PROXY_WEBSERVER_PORT
	// %ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC% 替换为$ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC
	// %ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH% 替换为$ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH
	// %ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO% 替换为$ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO
	// %TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC% 替换为$TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC
	// %REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC% 替换为$REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC
	global.GLOBAL_RES.PubLog(logKey, "modify etc/yb-tserver.conf params ......")

	ybMasterConf := "./etc/yb-tserver.conf"
	fsDataDirs := fmt.Sprintf("%s/data", currPath) //.replaceAll("/", "\\\\/");
	rpcBindAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, rpcBindPort)
	pgProxyBindAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, pgProxyBindPort)
	cqlProxyBindAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, cqlProxyBindPort)

	DeployUtils.SED(sshClient, consts.CONF_MASTER_ADDRS, masterList, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_FS_DATA_DIRS, fsDataDirs, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MAX_CLOCK_SKEW_USEC, maxClockSkewUsec, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_RPC_BIND_ADDR, rpcBindAddr, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_SERV_BROADCAST_ADDR, ssh.SERVER_IP, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_WEBSERV_INTERFACE, ssh.SERVER_IP, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_WEBSERVER_PORT, webServPort, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_DURABLE_WAL_WRITE, durableWalWrite, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_YB_NUM_SHARDS_PER_TSERVER, ybNumShardsPerTServer, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_YSQL_NUM_SHARDS_PER_TSERVER, ysqlNumShardsPerTServer, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_CLOUD, placementCloud, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_ZONE, placementZone, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PLACEMENT_REGION, placementRegion, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PGSQL_PROXY_BIND_ADDR, pgProxyBindAddr, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PGSQL_PROXY_WEBSERVER_PORT, pgProxyWebServPort, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PGSQL_PROXY_WEBSERVER_PORT, pgProxyWebServPort, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_YSQL_MAX_CONNECTIONS, ysqlMaxConnections, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CQL_PROXY_BIND_ADDR, cqlProxyBindAddr, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CQL_PROXY_WEBSERVER_PORT, cqlProxyWebservPort, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC, rocksdbCompactFlushRateLimitBytesPerSec, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH, rocksdbUniversalCompactionMinMergeWidth, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO, rocksdbUniversalCompactionSizeRatio, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC, timestampHistoryRetentionIntervalSec, ybMasterConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC, remoteBootstrapRateLimitBytesPerSec, ybMasterConf, logKey, paasResult)

	// 修改启停脚本
	if !DeployUtils.MV(sshClient, consts.START_SHELL, consts.YB_TSERVER_START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, consts.STOP_SHELL, consts.YB_TSERVER_STOP_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, consts.YB_MASTER_START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, consts.YB_MASTER_STOP_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, consts.YB_MASTER_CONF, logKey, paasResult) {
		return false
	}

	// 先执行符号路径替换
	global.GLOBAL_RES.PubLog(logKey, "replacing symbol link ......")
	preInstallCmd := "bin/post_install.sh"
	if !DeployUtils.ExecSimpleCmd(sshClient, preInstallCmd, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start yb-tserver ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "yb-tserver", instId, rpcBindPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployTServer(ybTServer map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := ybTServer[consts.HEADER_INST_ID].(string)
	sshId := ybTServer[consts.HEADER_SSH_ID].(string)
	rpcBindPort := ybTServer[consts.HEADER_RPC_BIND_PORT].(string)

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

	info := fmt.Sprintf("start undeploy yb-tserver, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, rpcBindPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("yb-tserver_%s", rpcBindPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_YUGABYTEDB_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop yb-tserver ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "yb-tserver", instId, rpcBindPort, logKey, paasResult) {
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
