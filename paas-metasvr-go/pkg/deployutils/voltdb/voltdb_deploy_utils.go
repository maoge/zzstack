package voltdb

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func GetVoltDBHosts(voltdbServerArr []map[string]interface{}, logKey string,
	paasResult *result.ResultBean) string {

	result := ""
	for idx, voltdb := range voltdbServerArr {
		sshID := voltdb[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshID)
		if ssh == nil {
			continue
		}

		if idx > 0 {
			result += ","
		}
		result += ssh.SERVER_IP
	}

	return result
}

func CreateValidationTable(voltdb map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func DeployVoltDBServer(voltdb map[string]interface{}, version, hosts, userName, userPasswd, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := voltdb[consts.HEADER_INST_ID].(string)
	sshId := voltdb[consts.HEADER_SSH_ID].(string)
	clientPort := voltdb[consts.HEADER_VOLT_CLIENT_PORT].(string)
	adminPort := voltdb[consts.HEADER_VOLT_ADMIN_PORT].(string)
	webPort := voltdb[consts.HEADER_VOLT_WEB_PORT].(string)
	internalPort := voltdb[consts.HEADER_VOLT_INTERNAL_PORT].(string)
	replicaPort := voltdb[consts.HEADER_VOLT_REPLI_PORT].(string)
	zkPort := voltdb[consts.HEADER_VOLT_ZK_PORT].(string)
	sitesPerHost := voltdb[consts.HEADER_SITES_PER_HOST].(string)
	kfactor := voltdb[consts.HEADER_KFACTOR].(string)
	memLimit := voltdb[consts.HEADER_MEM_LIMIT].(string)
	heartBeatTimeout := voltdb[consts.HEADER_HEARTBEAT_TIMEOUT].(string)
	temptablesMaxSize := voltdb[consts.HEADER_TEMPTABLES_MAXSIZE].(string)
	elasticDuration := voltdb[consts.HEADER_ELASTIC_DURATION].(string)
	elasticThroughput := voltdb[consts.HEADER_ELASTIC_THROUGHPUT].(string)
	queryTimeout := voltdb[consts.HEADER_QUERY_TIMEOUT].(string)
	procedureLoginfo := voltdb[consts.HEADER_PROCEDURE_LOGINFO].(string)
	memAlert := voltdb[consts.HEADER_MEM_ALERT].(string)

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

	info := fmt.Sprintf("deploy voltdb: %s:%s, instId:%s", ssh.SERVER_IP, internalPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{clientPort, adminPort, webPort, internalPort, replicaPort, zkPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// DB_VOLTDB_FILE_ID -> 'voltdb-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_VOLTDB_FILE_ID, consts.DB_VOLTDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_VOLTDB_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, internalPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// modify deployment.xml
	// <cluster sitesperhost="%SITES_PER_HOST%"
	//          kfactor="%KFACTOR%"/>
	// <heartbeat timeout="%HEARTBEAT_TIMEOUT%"/>
	// <user name="%ADMIN_NAME%" roles="dev,ops,administrator" password="%ADMIN_PWD%"/>
	// <user name="%USER_NAME%" roles="user" password="%USER_PASSWORD%"/>
	// <temptables maxsize="%TEMPTABLES_MAXSIZE%"/>
	// <elastic duration="%ELASTIC_DURATION%" throughput="%ELASTIC_THROUGHPUT%"/>
	// <query timeout="%QUERY_TIMEOUT%"/>
	// <procedure loginfo="%PROCEDURE_LOGINFO%"/>
	// <memorylimit size="%MEMORYLIMIT_SIZE%" alert="%MEMORYLIMIT_ALERT%"/>
	global.GLOBAL_RES.PubLog(logKey, "modify deployment.xml configures ......")
	deploymentFile := "./deployment.xml"
	DeployUtils.SED(sshClient, consts.CONF_SITES_PER_HOST, sitesPerHost, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_KFACTOR, kfactor, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_HEARTBEAT_TIMEOUT, heartBeatTimeout, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ADMIN_NAME, consts.VOLTDB_ADMIN_NAME, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ADMIN_PWD, consts.VOLTDB_ADMIN_PWD, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_USER_NAME, userName, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_USER_PASSWORD, userPasswd, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_TEMPTABLES_MAXSIZE, temptablesMaxSize, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ELASTIC_DURATION, elasticDuration, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ELASTIC_THROUGHPUT, elasticThroughput, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_QUERY_TIMEOUT, queryTimeout, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PROCEDURE_LOGINFO, procedureLoginfo, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MEMORYLIMIT_SIZE, memLimit, deploymentFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MEMORYLIMIT_ALERT, memAlert, deploymentFile, logKey, paasResult)

	// modify start.sh
	// nohup ./bin/voltdb start --dir=./database --host=%HOSTS% \
	//     --client=%VOLT_CLIENT_PORT% --admin=%VOLT_ADMIN_PORT% \
	//     --http=%VOLT_WEB_PORT% --internal=%VOLT_INTERNAL_PORT% \
	//     --replication=%VOLT_REPLI_PORT% --zookeeper=%VOLT_ZK_PORT% > /dev/null 2>&1 &
	zkAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, zkPort)
	startFile := fmt.Sprintf("./%s", consts.START_SHELL)
	DeployUtils.SED(sshClient, consts.CONF_HOSTS, hosts, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_CLIENT_PORT, clientPort, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_ADMIN_PORT, adminPort, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_WEB_PORT, webPort, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_INTERNAL_PORT, internalPort, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_REPLI_PORT, replicaPort, startFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_VOLT_ZK_PORT, zkAddr, startFile, logKey, paasResult)

	// modify stop.sh
	// --host=%HOST% --user=%USER_NAME% --password=%USER_PASSWORD%
	stopFile := fmt.Sprintf("./%s", consts.STOP_SHELL)
	DeployUtils.SED(sshClient, consts.CONF_HOST, ssh.SERVER_IP, stopFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_USER_NAME, consts.VOLTDB_ADMIN_NAME, stopFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_USER_PASSWORD, consts.VOLTDB_ADMIN_PWD, stopFile, logKey, paasResult)

	// init database
	initDBCmd := "./bin/voltdb init --dir=./database --config=deployment.xml"
	if !DeployUtils.ExecSimpleCmd(sshClient, initDBCmd, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start voltdb-server ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "dashboard-proxy", instId, internalPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployVoltDBServer(voltdb map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := voltdb[consts.HEADER_INST_ID].(string)
	sshId := voltdb[consts.HEADER_SSH_ID].(string)
	internalPort := voltdb[consts.HEADER_VOLT_INTERNAL_PORT].(string)

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

	info := fmt.Sprintf("start undeploy voltdb-server, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, internalPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_VOLTDB_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, internalPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_VOLTDB_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop voltdb-server ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "voltdb-server", instId, internalPort, logKey, paasResult) {
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
