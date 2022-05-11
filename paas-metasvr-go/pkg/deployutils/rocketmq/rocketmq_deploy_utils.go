package rocketmq

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

func GetNameSrvAddrs(nameSrvArr []map[string]interface{}) string {
	result := ""
	for idx, nameSrv := range nameSrvArr {
		sshId := nameSrv[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshId)
		if ssh == nil {
			continue
		}

		servIp := ssh.SERVER_IP
		port := nameSrv[consts.HEADER_LISTEN_PORT].(string)
		if idx > 0 {
			result += ";"
		}

		line := fmt.Sprintf("%s:%s", servIp, port)
		result += line
	}
	return result
}

func GetSingleNameSrvAddrs(nameSrvArr []map[string]interface{}) string {
	for _, nameSrv := range nameSrvArr {
		sshId := nameSrv[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshId)
		if ssh == nil {
			continue
		}

		servIp := ssh.SERVER_IP
		port := nameSrv[consts.HEADER_LISTEN_PORT].(string)

		return fmt.Sprintf("%s:%s", servIp, port)
	}
	return ""
}

func ModFakeServiceDeployFlag(servJson map[string]interface{}, servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})
	for _, nameSrv := range nameSrvArr {
		rocketID := nameSrv[consts.HEADER_INST_ID].(string)
		if !metadao.UpdateInstanceDeployFlag(rocketID, deployFlag, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "namesrv fake deploy failed ......")
			return false
		}
		global.GLOBAL_RES.PubLog(logKey, "namesrv fake deploy success ......")
	}

	// 部署broker服务
	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbroker := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})
	for _, vbroker := range vbroker {
		brokerArr := vbroker[consts.HEADER_ROCKETMQ_BROKER].([]map[string]interface{})
		for _, broker := range brokerArr {
			brokerID := broker[consts.HEADER_INST_ID].(string)
			if !metadao.UpdateInstanceDeployFlag(brokerID, deployFlag, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq broker fake deploy fail ......")
				return false
			}
			global.GLOBAL_RES.PubLog(logKey, "init rocketmq broker success ......")
		}
	}

	if !metadao.UpdateInstanceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}

	if !metadao.UpdateServiceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}

	pseudoFlag := consts.DEPLOY_FLAG_PHYSICAL
	if deployFlag == consts.STR_TRUE {
		pseudoFlag = consts.DEPLOY_FLAG_PSEUDO
	}

	if !metadao.ModServicePseudoFlag(servInstID, pseudoFlag, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployNameSrv(nameSrv map[string]interface{}, version, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := nameSrv[consts.HEADER_SSH_ID].(string)
	port := nameSrv[consts.HEADER_LISTEN_PORT].(string)
	instId := nameSrv[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("deploy namesrv: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	// 判断有没有JDK及JDK版本(严格用1.8)
	home, _ := DeployUtils.PWD(sshClient, logKey, paasResult)
	strFilePath := fmt.Sprintf("%s/%s/%s/%s", home, consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, "jdk")
	res, err := DeployUtils.IsDirExistInCurrPath(sshClient, strFilePath, logKey, paasResult)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		return false
	} else {
		if !res {
			// COMMON_TOOLS_JDK_FILE_ID -> 'jdk1.8.0_202.tar.gz'
			if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.COMMON_TOOLS_JDK_FILE_ID, consts.COMMON_TOOLS_ROOT, "", logKey, paasResult) {
				return false
			}

			oldName := DeployUtils.GetVersionedFileName(consts.COMMON_TOOLS_JDK_FILE_ID, version, logKey, paasResult)
			newJdkName := "jdk"
			if !DeployUtils.MV(sshClient, newJdkName, oldName, logKey, paasResult) {
				return false
			}
		}
	}

	DeployUtils.CD(sshClient, "~/", logKey, paasResult)

	// MQ_ROCKET_MQ_FILE_ID -> 'rocketmq-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.MQ_ROCKET_MQ_FILE_ID, consts.MQ_ROCKETMQ_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.MQ_ROCKET_MQ_FILE_ID, version, logKey, paasResult)
	newName := "rocketmq_namesrv_" + port

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify rocketmq_namesrv configure files ......")
	newConf := "conf/namesrv.properties"
	mqRoot := fmt.Sprintf("%s/%s/%s/%s", home, consts.PAAS_ROOT, consts.MQ_ROCKETMQ_ROOT, newName)
	newMqRoot := strings.ReplaceAll(mqRoot, "/", "\\/")
	kvConfPath := newMqRoot + "\\/conf\\/kvConfig.json"

	DeployUtils.SED(sshClient, consts.CONF_ROCKET_HOME, newMqRoot, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_KV_CONFIG_PATH, kvConfPath, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_PORT, port, newConf, logKey, paasResult)

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")

	startShell := fmt.Sprintf("export JAVA_HOME=%s \rexport ROCKETMQ_HOME=%s \rnohup sh ./bin/mqnamesrv -c %s/conf/namesrv.properties > %s/logs/mqnamesrv.log >/dev/null 2>&1 & \r",
		strFilePath, mqRoot, mqRoot, mqRoot)
	if !DeployUtils.CreateShell(sshClient, consts.START_SHELL, startShell, logKey, paasResult) {
		return false
	}

	stopShell := fmt.Sprintf("export JAVA_HOME=%s \rnohup sh bin/mqshutdown namesrv >/dev/null 2>&1 & ", strFilePath)
	if !DeployUtils.CreateShell(sshClient, consts.STOP_SHELL, stopShell, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start namesrv ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "namesrv", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployNameSrv(nameSrv map[string]interface{}, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := nameSrv[consts.HEADER_SSH_ID].(string)
	port := nameSrv[consts.HEADER_LISTEN_PORT].(string)
	instId := nameSrv[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("start undeploy namesrv, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := "rocketmq_namesrv_" + port
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_ROCKETMQ_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop namesrv ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "namesrv", instId, port, logKey, paasResult) {
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

func DeployBroker(broker map[string]interface{}, servInstID, namesrvAddrs, brokerId, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := broker[consts.HEADER_SSH_ID].(string)
	port := broker[consts.HEADER_LISTEN_PORT].(string)
	instId := broker[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("deploy broker: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	home, _ := DeployUtils.PWD(sshClient, logKey, paasResult)
	strFilePath := fmt.Sprintf("%s/%s/%s/%s", home, consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, "jdk")
	res, err := DeployUtils.IsDirExistInCurrPath(sshClient, strFilePath, logKey, paasResult)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		return false
	} else {
		if !res {
			// COMMON_TOOLS_JDK_FILE_ID -> 'jdk1.8.0_202.tar.gz'
			if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.COMMON_TOOLS_JDK_FILE_ID, consts.COMMON_TOOLS_ROOT, "", logKey, paasResult) {
				return false
			}

			oldName := DeployUtils.GetVersionedFileName(consts.COMMON_TOOLS_JDK_FILE_ID, version, logKey, paasResult)
			newJdkName := "jdk"
			if !DeployUtils.MV(sshClient, newJdkName, oldName, logKey, paasResult) {
				return false
			}
		}
	}

	DeployUtils.CD(sshClient, "~/", logKey, paasResult)

	// MQ_ROCKET_MQ_FILE_ID -> 'rocketmq-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.MQ_ROCKET_MQ_FILE_ID, consts.MQ_ROCKETMQ_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.MQ_ROCKET_MQ_FILE_ID, version, logKey, paasResult)
	newName := "rocketmq_broker_" + port
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify rocketmq_broker configure files ......")
	newConf := "conf/broker.conf"
	mqRoot := fmt.Sprintf("%s/%s/%s/%s", home, consts.PAAS_ROOT, consts.MQ_ROCKETMQ_ROOT, newName)
	newMqRoot := strings.ReplaceAll(mqRoot, "/", "\\/")
	DeployUtils.SED(sshClient, consts.CONF_BROKER_CLUSTER_NAME, servInstID, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BROKER_NAME, instId, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BROKER_ID, brokerId, newConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_NAMESRV_ADDR, namesrvAddrs, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_BROKER_IP, ssh.SERVER_IP, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_LISTEN_PORT, port, newConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_STORE_ROOT, newMqRoot+"\\/store", newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_COMMIT_LOG_PATH, newMqRoot+"\\/store\\/commitlog", newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CONSUME_QUEUE_PATH, newMqRoot+"\\/store\\/consumequeue", newConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_INDEX_PATH, newMqRoot+"\\/store\\/index", newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CHECKPOINT_PATH, newMqRoot+"\\/store\\/checkpoint", newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ABORT_FILE_PATH, newMqRoot+"\\/store\\/abort", newConf, logKey, paasResult)

	DeployUtils.SED(sshClient, consts.CONF_BROKER_ROLE, broker["BROKER_ROLE"].(string), newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_FLUSH_DISK_TYPE, broker["FLUSH_DISK_TYPE"].(string), newConf, logKey, paasResult)

	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")
	jdkHome := strFilePath
	startShell := fmt.Sprintf("export JAVA_HOME=%s \rexport ROCKETMQ_HOME=%s \rnohup sh bin/mqbroker -c conf/broker.conf >/dev/null 2>&1 &\r", jdkHome, mqRoot)
	if !DeployUtils.CreateShell(sshClient, consts.START_SHELL, startShell, logKey, paasResult) {
		return false
	}

	stopShell := "\r"
	stopShell += fmt.Sprintf(" PIDS=\\`ps -ef | grep java | grep -v grep | grep %s |awk '{print \\$2}'\\`", newName)
	stopShell += " if [ -z \"\\$PIDS\" ]; then"
	stopShell += "    echo \"ERROR: broker does not started!\""
	stopShell += "    exit 127"
	stopShell += " fi"
	stopShell += " echo -e \"Stopping broker ...\""
	stopShell += " for PID in \\$PIDS ; do"
	stopShell += "    kill \\$PID > /dev/null 2>&1"
	stopShell += " done "
	stopShell += " sleep 2 "
	stopShell += " for PID in \\$PIDS ; do"
	stopShell += "    PID_EXIST=\\`ps -f -p \\$PID | grep java\\`"
	stopShell += "    if [ -n \"\\$PID_EXIST\" ]; then"
	stopShell += "        kill -9 \\$PID > /dev/null 2>&1"
	stopShell += " fi"
	stopShell += " done "
	stopShell += " echo \"OK!\" "
	stopShell += " echo \"PID: \\$PIDS\" "
	if !DeployUtils.CreateShell(sshClient, consts.STOP_SHELL, stopShell, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start rocketmq broker ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "rocketmq broker", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployBroker(broker map[string]interface{}, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := broker[consts.HEADER_SSH_ID].(string)
	port := broker[consts.HEADER_LISTEN_PORT].(string)
	instId := broker[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("start undeploy rocketmq broker, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("rocketmq_broker_%s", port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_ROCKETMQ_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop rocketmq broker ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "rocketmq broker", instId, port, logKey, paasResult) {
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

func DeployConsole(console map[string]interface{}, servInstID, singleNameSrv, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := console[consts.HEADER_SSH_ID].(string)
	consolePort := console[consts.HEADER_CONSOLE_PORT].(string)
	instId := console[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("deploy rocketmq-console: %s:%s, instId:%s", ssh.SERVER_IP, consolePort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, consolePort, logKey, paasResult) {
		return false
	}

	// ROCKETMQ_CONSOLE_FILE_ID -> 'rocketmq-console-1.1.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.ROCKETMQ_CONSOLE_FILE_ID, consts.MQ_ROCKETMQ_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.ROCKETMQ_CONSOLE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("rocketmq-console_%s", consolePort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify rocketmq-console configure ......")
	// sed 替换 start.sh 中的变量
	// -Drocketmq.namesrv.addr=%NAMESRV_ADDR%
	// -Dserver.port=%CONSOLE_PORT%
	DeployUtils.SED(sshClient, consts.CONF_NAMESRV_ADDR, singleNameSrv, consts.START_SHELL, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CONSOLE_PORT, consolePort, consts.START_SHELL, logKey, paasResult)

	// start
	global.GLOBAL_RES.PubLog(logKey, "start rocketmq-console ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "rocketmq-console", instId, consolePort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployConsole(console map[string]interface{}, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := console[consts.HEADER_SSH_ID].(string)
	consolePort := console[consts.HEADER_CONSOLE_PORT].(string)
	instId := console[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("start undeploy rocketmq console, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, consolePort)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("rocketmq-console_%s", consolePort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.MQ_ROCKETMQ_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop rocketmq-console ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "rocketmq-console", instId, consolePort, logKey, paasResult) {
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
