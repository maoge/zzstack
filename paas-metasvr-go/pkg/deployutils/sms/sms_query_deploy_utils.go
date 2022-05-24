package sms

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func GetSmsQueryServList(smsQueryArr []map[string]interface{}) string {
	result := ""
	for idx, item := range smsQueryArr {
		sshId := item[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshId)
		if ssh == nil {
			continue
		}

		if idx > 0 {
			result += consts.LINE_SEP
		}

		servIp := ssh.SERVER_IP
		vertxPort := item[consts.HEADER_VERTX_PORT].(string)
		line := fmt.Sprintf("         server %s:%s;", servIp, vertxPort)
		result += line
	}

	return result
}

func DeploySmsQueryArr(servInstID, smsQueryContainerId string, smsQueryArr []map[string]interface{},
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	for _, item := range smsQueryArr {
		instId := item[consts.HEADER_INST_ID].(string)
		version := DeployUtils.GetVersion(servInstID, smsQueryContainerId, instId)

		if !DeploySmsQueryNode(item, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func DeployNgxArr(servInstID, ngxContainerId string, ngxArr []map[string]interface{}, servList,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	for _, item := range ngxArr {
		instId := item[consts.HEADER_INST_ID].(string)
		version := DeployUtils.GetVersion(servInstID, ngxContainerId, instId)

		if !DeployNgxNode(item, servList, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func UndeployNgxArr(servInstID, ngxContainerId string, ngxArr []map[string]interface{},
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	for _, item := range ngxArr {
		instId := item[consts.HEADER_INST_ID].(string)
		version := DeployUtils.GetVersion(servInstID, ngxContainerId, instId)

		if !UndeployNgxNode(item, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func UndeploySmsQueryArr(servInstID, smsQueryContainerId string, smsQueryArr []map[string]interface{},
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	for _, item := range smsQueryArr {
		instId := item[consts.HEADER_INST_ID].(string)
		version := DeployUtils.GetVersion(servInstID, smsQueryContainerId, instId)

		if !UndeploySmsQueryNode(item, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func DeploySmsQueryNode(item map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)
	vertxPort := item[consts.HEADER_VERTX_PORT].(string)
	metaSvrUrl := item[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := item[consts.HEADER_META_SVR_USR].(string)
	metaSvrPwd := item[consts.HEADER_META_SVR_PASSWD].(string)
	jvmOps := item[consts.HEADER_JVM_OPS].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", "SMS_QUERY", instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", "SMS_QUERY", instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{vertxPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_QUERY_SERVER_FILE_ID -> 'smsqueryserver-%VERSION%.zip'
	oldName := "smsqueryserver"
	newName := "smsqueryserver_" + instId
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_QUERY_SERVER_FILE_ID, consts.SMS_QUERY_ROOT, oldName, version, logKey, paasResult) {
		return false
	}

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify smsqueryserver.sh env params ......")

	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// JAVA_OPTS="%JVM_OPS%"
	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")

	file := "./bin/smsqueryserver.sh"
	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPwd, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_JVM_OPS, jvmOpsNew, file, logKey, paasResult) {
		return false
	}

	// 执行权限
	if !DeployUtils.ChMod(sshClient, file, "+x", logKey, paasResult) {
		return false
	}

	//执行unix脚本命令
	if !DeployUtils.Dos2Unix(sshClient, file, logKey, paasResult) {
		global.GLOBAL_RES.PubErrorLog(logKey, "dos2unix failed......")
		return false
	}

	// start
	if DeployUtils.IsPreEmbadded(instId) {
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, "SMS_QUERY_SERVER", logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsqueryserver.sh start")
		if !DeployUtils.Startup(sshClient, instId, "SMS_QUERY_SERVER", startCmd, vertxPort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func UndeploySmsQueryNode(item map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)
	vertxPort := item[consts.HEADER_VERTX_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", "SMS_QUERY_SERVICE", instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := "smsqueryserver_" + instId
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_QUERY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", "SMS_QUERY_SERVICE"))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	stopCmd := "./bin/smsqueryserver.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, "SMS_QUERY_SERVICE", stopCmd, newName, vertxPort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func DeployNgxNode(item map[string]interface{}, servList, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)
	wrokerProcess := item[consts.HEADER_WORKER_PROCESSES].(string)
	lisnPort := item[consts.HEADER_LISTEN_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", "nginx", instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", "nginx", instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{lisnPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// NGX_FILE_ID -> 'nginx-1.19.6.tar.gz'
	oldName := DeployUtils.GetVersionedFileName(consts.NGX_FILE_ID, version, logKey, paasResult)
	newName := oldName + "_" + lisnPort
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.NGX_FILE_ID, consts.COMMON_TOOLS_ROOT, oldName, version, logKey, paasResult) {
		return false
	}

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "fetch and replace nginx.conf configure ......")
	// scp nginx_sms_query.conf
	if !DeployUtils.FetchFile(sshClient, consts.NGX_SMS_QUERY_CONF_FILE_ID, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "scp nginx_sms_query.conf fail ......")
		return false
	}

	// worker_processes  %WORKER_PROCESSES%;
	// %SERVER_LIST%
	// listen       %LISTEN_PORT%;
	if !DeployUtils.SED(sshClient, consts.CONF_WORKER_PROCESSES, wrokerProcess, consts.NGX_SMS_QUERY_CONF, logKey, paasResult) {
		return false
	}
	if !DeployUtils.AppendMultiLine(sshClient, consts.CONF_SERVER_LIST, servList, consts.NGX_SMS_QUERY_CONF, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_PORT, lisnPort, consts.NGX_SMS_QUERY_CONF, logKey, paasResult) {
		return false
	}

	if !DeployUtils.MV(sshClient, "./conf/"+consts.NGX_CONF, consts.NGX_SMS_QUERY_CONF, logKey, paasResult) {
		return false
	}

	startCmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.Startup(sshClient, instId, "nginx", startCmd, lisnPort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployNgxNode(item map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)
	lisnPort := item[consts.HEADER_LISTEN_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", "nginx", instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	fileName := DeployUtils.GetVersionedFileName(consts.NGX_FILE_ID, version, logKey, paasResult)
	newName := fileName + "_" + lisnPort
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop nginx ......")

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	stopCmd := "./stop.sh"
	return DeployUtils.Shutdown(sshClient, instId, "nginx", stopCmd, newName, lisnPort, consts.STR_SAVED, logKey, magicKey, paasResult)
}
