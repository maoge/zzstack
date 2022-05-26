package sms

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func DeploySmsInstanceArr(header, servInstID string, container map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	smsInstanceArr := container[header].([]map[string]interface{})
	if smsInstanceArr == nil || len(smsInstanceArr) == 0 {
		return true
	}

	containerInstId := container[consts.HEADER_INST_ID].(string)

	for _, item := range smsInstanceArr {
		ret := true

		instId := item[consts.HEADER_INST_ID].(string)
		version := DeployUtils.GetVersion(servInstID, containerInstId, instId)

		switch header {
		case consts.HEADER_SMS_SERVER:
		case consts.HEADER_SMS_SERVER_EXT:
			ret = DeploySmsServerNode(item, header, version, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_PROCESS:
			ret = DeploySmsProcessNode(item, header, version, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_CLIENT:
			ret = DeploySmsClientNode(item, header, version, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_BATSAVE:
			ret = DeploySmsBatSaveNode(item, header, version, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_STATS:
			ret = DeploySmsStatsNode(item, header, version, logKey, magicKey, paasResult)
			break
		default:
			break
		}

		if !ret {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func UndeploySmsInstanceArr(header string, container map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	smsInstanceArr := container[header].([]map[string]interface{})
	if smsInstanceArr == nil || len(smsInstanceArr) == 0 {
		return true
	}

	for _, item := range smsInstanceArr {
		ret := true

		switch header {
		case consts.HEADER_SMS_SERVER:
		case consts.HEADER_SMS_SERVER_EXT:
			ret = UndeploySmsServerNode(item, header, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_PROCESS:
			ret = UndeploySmsProcessNode(item, header, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_CLIENT:
			ret = UndeploySmsClientNode(item, header, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_BATSAVE:
			ret = UndeploySmsBatSaveNode(item, header, logKey, magicKey, paasResult)
			break
		case consts.HEADER_SMS_STATS:
			ret = UndeploySmsStatsNode(item, header, logKey, magicKey, paasResult)
			break
		default:
			break
		}

		if !ret {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
			return false
		}
	}

	return true
}

func DeploySmsGatewayInstance(servInstID, instID, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	metadao.LoadInstanceMeta(instID, paasResult)
	instItem := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	version := DeployUtils.GetServiceVersion(servInstID, instID)

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	cmptName := instCmpt.CMPT_NAME
	result := true

	switch cmptName {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		result = DeploySmsServerNode(instItem, cmptName, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_PROCESS:
		result = DeploySmsProcessNode(instItem, cmptName, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_CLIENT:
		result = DeploySmsClientNode(instItem, cmptName, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_BATSAVE:
		result = DeploySmsBatSaveNode(instItem, cmptName, version, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_STATS:
		result = DeploySmsStatsNode(instItem, cmptName, version, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	return result
}

func UndeploySmsGatewayInstance(instID, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	metadao.LoadInstanceMeta(instID, paasResult)
	instItem := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	cmptName := instCmpt.CMPT_NAME
	result := true

	switch cmptName {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		result = UndeploySmsServerNode(instItem, cmptName, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_PROCESS:
		result = UndeploySmsProcessNode(instItem, cmptName, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_CLIENT:
		result = UndeploySmsClientNode(instItem, cmptName, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_BATSAVE:
		result = UndeploySmsBatSaveNode(instItem, cmptName, logKey, magicKey, paasResult)
		break
	case consts.HEADER_SMS_STATS:
		result = UndeploySmsStatsNode(instItem, cmptName, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	return result
}

func DeploySmsServerNode(instItem map[string]interface{}, cmptName, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	metaSvrUrl := instItem[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := instItem[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := instItem[consts.HEADER_META_SVR_PASSWD].(string)
	jvmOps := instItem[consts.HEADER_JVM_OPS].(string)
	webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	cmpp20Port := instItem[consts.HEADER_CMPP20_PORT].(string)
	cmpp30Port := instItem[consts.HEADER_CMPP30_PORT].(string)
	sgip12Port := instItem[consts.HEADER_SGIP12_PORT].(string)
	smpp34Port := instItem[consts.HEADER_SMPP34_PORT].(string)
	smgp30Port := instItem[consts.HEADER_SMGP30_PORT].(string)
	httpPort := instItem[consts.HEADER_HTTP_PORT].(string)
	http2Port := instItem[consts.HEADER_HTTP_PORT2].(string)
	httpsPort := instItem[consts.HEADER_HTTPS_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{webConsolePort, cmpp20Port, cmpp30Port, sgip12Port, smpp34Port, smgp30Port, httpPort, http2Port, httpsPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_SERVER_FILE_ID -> 'smsserver-xxx.zip'
	oldName := "smsserver"
	newName := "smsserver_" + instId
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_SERVER_FILE_ID, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
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

	global.GLOBAL_RES.PubLog(logKey, "modify smsserver.sh env params ......")

	file := "./bin/smsserver.sh"

	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// JAVA_OPTS="%JVM_OPS%"

	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")
	DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_JVM_OPS, jvmOpsNew, file, logKey, paasResult)

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
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, cmptName, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsserver.sh start")
		if !DeployUtils.Startup(sshClient, instId, cmptName, startCmd, webConsolePort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func DeploySmsProcessNode(instItem map[string]interface{}, cmptName, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	metaSvrUrl := instItem[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := instItem[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := instItem[consts.HEADER_META_SVR_PASSWD].(string)
	rocketMQServ := instItem[consts.HEADER_ROCKETMQ_SERV].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)
	jvmOps := instItem[consts.HEADER_JVM_OPS].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	realPort := DeployUtils.GetRealPort(instItem)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{realPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_PROCESS_FILE_ID -> 'smsprocess-xxx.zip'
	oldName := "smsprocess"
	newName := "smsprocess_" + processor
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_PROCESS_FILE_ID, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
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

	global.GLOBAL_RES.PubLog(logKey, "modify smsprocess.sh env params ......")

	file := "./bin/smsprocess.sh"
	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// ROCKETMQ_SERV=%ROCKETMQ_SERV%
	// PROCESSOR=%PROCESSOR%
	// JAVA_OPTS="%JVM_OPS%"
	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")

	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PROCESSOR, processor, file, logKey, paasResult) {
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
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, cmptName, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsprocess.sh start")
		if !DeployUtils.Startup(sshClient, instId, cmptName, startCmd, realPort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func DeploySmsClientNode(instItem map[string]interface{}, cmptName, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	metaSvrUrl := instItem[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := instItem[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := instItem[consts.HEADER_META_SVR_PASSWD].(string)
	rocketMQServ := instItem[consts.HEADER_ROCKETMQ_SERV].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)
	jvmOps := instItem[consts.HEADER_JVM_OPS].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	realPort := DeployUtils.GetRealPort(instItem)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{realPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_CLIENT_FILE_ID -> 'smsclient-xxx.zip'
	oldName := "smsclient-standard"
	newName := "smsclient-standard_" + processor
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_CLIENT_FILE_ID, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
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

	global.GLOBAL_RES.PubLog(logKey, "modify smsclient.sh env params ......")
	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// ROCKETMQ_SERV=%ROCKETMQ_SERV%
	// PROCESSOR=%PROCESSOR%
	// JAVA_OPTS="%JVM_OPS%"
	file := "./bin/smsclient.sh"
	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PROCESSOR, processor, file, logKey, paasResult) {
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
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, cmptName, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsclient.sh start")
		if !DeployUtils.Startup(sshClient, instId, cmptName, startCmd, realPort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func DeploySmsBatSaveNode(instItem map[string]interface{}, cmptName, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	metaSvrUrl := instItem[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := instItem[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := instItem[consts.HEADER_META_SVR_PASSWD].(string)
	rocketMQServ := instItem[consts.HEADER_ROCKETMQ_SERV].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)
	jvmOps := instItem[consts.HEADER_JVM_OPS].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)
	dbInstId := instItem[consts.HEADER_DB_INST_ID].(string)
	esServer := instItem[consts.HEADER_ES_SERVER].(string)
	esMtServer := instItem[consts.HEADER_ES_MT_SERVER].(string)

	realPort := DeployUtils.GetRealPort(instItem)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{realPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_BATSAVE_FILE_ID -> 'smsbatsave-xxx.zip'
	oldName := "smsbatsave"
	newName := "smsbatsave_" + processor + "_" + dbInstId
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_BATSAVE_FILE_ID, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
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

	global.GLOBAL_RES.PubLog(logKey, "modify smsbatsave.sh env params ......")

	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// ROCKETMQ_SERV=%ROCKETMQ_SERV%
	// PROCESSOR=%PROCESSOR%
	// JAVA_OPTS="%JVM_OPS%"
	file := "./bin/smsbatsave.sh"
	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PROCESSOR, processor, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_JVM_OPS, jvmOpsNew, file, logKey, paasResult) {
		return false
	}

	esConfigFile := "./conf/elasticsearch.properties"
	if !DeployUtils.SED(sshClient, consts.CONF_ES_SERVER, esServer, esConfigFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_ES_MT_SERVER, esMtServer, esConfigFile, logKey, paasResult) {
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
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, cmptName, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsbatsave.sh start")
		if !DeployUtils.Startup(sshClient, instId, cmptName, startCmd, realPort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func DeploySmsStatsNode(instItem map[string]interface{}, cmptName, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	metaSvrUrl := instItem[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := instItem[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := instItem[consts.HEADER_META_SVR_PASSWD].(string)
	jvmOps := instItem[consts.HEADER_JVM_OPS].(string)
	webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby deployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	info := fmt.Sprintf("start deploy %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{webConsolePort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// SMS_STATS_FILE_ID -> 'smsstatistics-xxx.zip'
	oldName := "smsstatistics"
	newName := "smsstatistics_" + instId
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, consts.SMS_STATS_FILE_ID, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
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

	global.GLOBAL_RES.PubLog(logKey, "modify smsstatistics.sh env params ......")

	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// ROCKETMQ_SERV=%ROCKETMQ_SERV%
	// PROCESSOR=%PROCESSOR%
	// JAVA_OPTS="%JVM_OPS%"
	file := "./bin/smsstatistics.sh"
	metaSvrUrlNew := strings.ReplaceAll(metaSvrUrl, "/", "\\/")
	jvmOpsNew := strings.ReplaceAll(jvmOps, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrlNew, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult) {
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
		if !DeployUtils.StartupPreEmbadded(sshClient, instId, cmptName, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		startCmd := fmt.Sprintf("./bin/smsstatistics.sh start")
		if !DeployUtils.Startup(sshClient, instId, cmptName, startCmd, webConsolePort, consts.STR_DEPLOYED, logKey, magicKey, paasResult) {
			return false
		}
	}

	return true
}

func UndeploySmsServerNode(instItem map[string]interface{}, cmptName, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := fmt.Sprintf("smsserver_%s", instId)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", cmptName))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	stopCmd := "./bin/smsserver.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, cmptName, stopCmd, newName, webConsolePort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func UndeploySmsProcessNode(instItem map[string]interface{}, cmptName, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := fmt.Sprintf("smsprocess_%s", processor)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", cmptName))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	realPort := DeployUtils.GetRealPort(instItem)

	stopCmd := "./bin/smsprocess.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, cmptName, stopCmd, newName, realPort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func UndeploySmsClientNode(instItem map[string]interface{}, cmptName, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := fmt.Sprintf("smsclient-standard_%s", processor)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", cmptName))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	realPort := DeployUtils.GetRealPort(instItem)

	stopCmd := "./bin/smsclient.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, cmptName, stopCmd, newName, realPort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func UndeploySmsBatSaveNode(instItem map[string]interface{}, cmptName, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	// webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)
	processor := instItem[consts.HEADER_PROCESSOR].(string)
	dbInstId := instItem[consts.HEADER_DB_INST_ID].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := fmt.Sprintf("smsbatsave_%s_%s", processor, dbInstId)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", cmptName))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	realPort := DeployUtils.GetRealPort(instItem)

	stopCmd := "./bin/smsbatsave.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, cmptName, stopCmd, newName, realPort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func UndeploySmsStatsNode(instItem map[string]interface{}, cmptName, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := instItem[consts.HEADER_INST_ID].(string)
	sshId := instItem[consts.HEADER_SSH_ID].(string)
	webConsolePort := instItem[consts.HEADER_WEB_CONSOLE_PORT].(string)

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	if DeployUtils.CheckInstanceNotDeployed(instId, logKey, paasResult) {
		info := fmt.Sprintf("passby undeployed %s, inst_id:%s, serv_ip:%s", cmptName, instId, ssh.SERVER_IP)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	newName := fmt.Sprintf("smsstatistics_%s", instId)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)

	// stop
	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("stop %s ......", cmptName))

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	stopCmd := "./bin/smsstatistics.sh stop"
	return DeployUtils.Shutdown(sshClient, instId, cmptName, stopCmd, newName, webConsolePort, consts.STR_SAVED, logKey, magicKey, paasResult)
}

func MaintainInstance(servInstID, instID, servType string, op *consts.OperationExt,
	isHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	inst := meta.CMPT_META.GetInstance(instID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	cmptName := cmpt.CMPT_NAME

	metadao.LoadInstanceMeta(instID, paasResult)
	instItem := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	if op.Code == consts.INSTANCE_OPERATION_UPDATE.Code {
		version := DeployUtils.GetServiceVersion(servInstID, instID)
		return updateSmsNode(instItem, instID, version, cmptName, op, logKey, magicKey, paasResult)
	} else {
		return maintainSmsNode(instItem, instID, cmptName, op, isHandle, logKey, magicKey, paasResult)
	}
}

func updateSmsNode(item map[string]interface{}, instID, version, cmptName string,
	op *consts.OperationExt, logKey, magicKey string, paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("%s %s, inst_id:%s, serv_ip:%s", op.Action, cmptName, instId, ssh.SERVER_IP))

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	newName := ""
	stopCmd := ""
	startCmd := ""
	processor := ""
	dbInstId := ""
	oldName := ""

	fileId := 0

	baseDir := fmt.Sprintf("%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT)

	switch cmptName {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		oldName = "smsserver"
		newName = "smsserver_" + instId
		fileId = consts.SMS_SERVER_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsserver.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsserver.sh start", newName)
		break
	case consts.HEADER_SMS_PROCESS:
		oldName = "smsprocess"
		processor = item[consts.HEADER_PROCESSOR].(string)
		newName = "smsprocess_" + processor
		fileId = consts.SMS_PROCESS_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsprocess.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsprocess.sh start", newName)
		break
	case consts.HEADER_SMS_CLIENT:
		oldName = "smsclient-standard"
		processor = item[consts.HEADER_PROCESSOR].(string)
		newName = "smsclient-standard_" + processor
		fileId = consts.SMS_CLIENT_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsclient.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsclient.sh start", newName)
		break
	case consts.HEADER_SMS_BATSAVE:
		oldName = "smsbatsave"
		processor = item[consts.HEADER_PROCESSOR].(string)
		dbInstId = item[consts.HEADER_DB_INST_ID].(string)
		newName = "smsbatsave_" + processor + "_" + dbInstId
		fileId = consts.SMS_BATSAVE_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsbatsave.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsbatsave.sh start", newName)
		break
	case consts.HEADER_SMS_STATS:
		oldName = "smsstatistics"
		newName = "smsstatistics_" + instId
		fileId = consts.SMS_STATS_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsstatistics.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsstatistics.sh start", newName)
		break

	default:
		break
	}

	// 1. scp deploy file and unzip
	if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, fileId, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CD(sshClient, baseDir, logKey, paasResult) {
		return false
	}

	// 2. stop instance
	global.GLOBAL_RES.PubLog(logKey, stopCmd)
	if !DeployUtils.ExecSimpleCmd(sshClient, stopCmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.IsPreEmbadded(instId) {
		time.Sleep(time.Duration(consts.STOP_WAIT_MILLI_SECONDS) * time.Millisecond)
		if DeployUtils.IsProcExist(sshClient, instId, logKey, paasResult) {
			global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s 更新前执行进程停止失败", cmptName, instId))
			return false
		}
	}

	// 3. 更新文件
	rmInstanceJar := fmt.Sprintf("rm ./%s/*.jar ", newName)
	rmLibJar := fmt.Sprintf("rm ./%s/lib/*.jar ", newName)
	cmdInstanceJar := fmt.Sprintf("cp ./%s/*.jar ./%s ", oldName, newName)
	cmdLibJar := fmt.Sprintf("cp ./%s/lib/*.jar ./%s/lib ", oldName, newName)
	if !DeployUtils.ExecSimpleCmd(sshClient, rmInstanceJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, rmLibJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, cmdInstanceJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, cmdLibJar, logKey, paasResult) {
		return false
	}

	// 4. 重新拉起
	global.GLOBAL_RES.PubLog(logKey, startCmd)
	if DeployUtils.IsPreEmbadded(instId) {
		global.GLOBAL_RES.PubLog(logKey, "PRE_EMBADDED instance, do not need to start ......")
		if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_PRE_EMBADDED, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		if !DeployUtils.ExecSimpleCmd(sshClient, startCmd, logKey, paasResult) {
			return false
		}

		time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
		if !DeployUtils.IsProcExist(sshClient, instId, logKey, paasResult) {
			global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s 更新后执行进程拉起失败", cmptName, instId))

			return false
		}
	}
	if !DeployUtils.RM(sshClient, oldName, logKey, paasResult) {
		return false
	}

	// 5. 修改实例的version属性
	// 227 -> 'VERSION'
	if !metadao.ModInstanceAttr(instId, 227, "VERSION", version, logKey, magicKey, paasResult) {
		return false
	}

	res := true
	if !DeployUtils.IsPreEmbadded(instId) {
		time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
		res = DeployUtils.IsProcExist(sshClient, instId, logKey, paasResult)
		if res {
			res = metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult)
		}
	}

	resInfo := "fail"
	if res {
		resInfo = "success"
	}

	global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s %s", op.Action, cmptName, resInfo))
	paasResult.RET_INFO = version
	return res
}

func maintainSmsNode(item map[string]interface{}, instID, cmptName string,
	op *consts.OperationExt, isHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	instId := item[consts.HEADER_INST_ID].(string)
	sshId := item[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("%s %s, inst_id:%s, serv_ip:%s", op.Action, cmptName, instId, ssh.SERVER_IP))

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	newName := ""
	dir := ""
	cmd := ""
	processor := ""
	dbInstId := ""

	switch cmptName {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		newName = "smsserver_" + instId
		dir = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)
		cmd = fmt.Sprintf("./bin/smsserver.sh %s", op.Action)
		break
	case consts.HEADER_SMS_PROCESS:
		processor = item[consts.HEADER_PROCESSOR].(string)
		newName = "smsprocess_" + processor
		dir = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)
		cmd = fmt.Sprintf("./bin/smsprocess.sh %s", op.Action)
		break
	case consts.HEADER_SMS_CLIENT:
		processor = item[consts.HEADER_PROCESSOR].(string)
		newName = "smsclient-standard_" + processor
		dir = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)
		cmd = fmt.Sprintf("./bin/smsclient.sh %s", op.Action)
		break
	case consts.HEADER_SMS_BATSAVE:
		processor = item[consts.HEADER_PROCESSOR].(string)
		dbInstId = item[consts.HEADER_DB_INST_ID].(string)
		newName = "smsbatsave_" + processor + "_" + dbInstId
		dir = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)
		cmd = fmt.Sprintf("./bin/smsbatsave.sh %s", op.Action)
		break
	case consts.HEADER_SMS_STATS:
		newName = "smsstatistics_" + instId
		dir = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, newName)
		cmd = fmt.Sprintf("./bin/smsstatistics.sh %s", op.Action)
		break
	default:
		break
	}

	if !DeployUtils.CD(sshClient, dir, logKey, paasResult) {
		return false
	}

	// 非预埋或者执行故障切换时才拉起
	if !DeployUtils.IsPreEmbadded(instId) || !isHandle {
		if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
			return false
		}

		if op.Code == consts.INSTANCE_OPERATION_START.Code || op.Code == consts.INSTANCE_OPERATION_RESTART.Code {
			time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
			if !DeployUtils.IsProcExist(sshClient, instId, logKey, paasResult) {
				return false
			}
		}
	}

	res := true
	if !DeployUtils.IsPreEmbadded(instId) || !isHandle {
		switch op.Code {
		case consts.INSTANCE_OPERATION_STOP.Code:
			res = metadao.UpdateInstanceDeployFlag(instId, consts.STR_WARN, logKey, magicKey, paasResult)
			break
		case consts.INSTANCE_OPERATION_START.Code:
		case consts.INSTANCE_OPERATION_RESTART.Code:
			res = metadao.UpdateInstanceDeployFlag(instId, consts.STR_DEPLOYED, logKey, magicKey, paasResult)
			break
		default:
			break
		}
	}

	if res && (op.Code == consts.INSTANCE_OPERATION_START.Code || op.Code == consts.INSTANCE_OPERATION_RESTART.Code) {
		// 预埋节点如果手工启动
		if DeployUtils.IsPreEmbadded(instId) && !isHandle {
			// 预埋实例拉起成功，修改PRE_EMBEDDED属性为S_FALSE，视为与正常实例一样
			res = metadao.UpdateInstancePreEmbadded(instId, consts.S_FALSE, logKey, magicKey, paasResult)
		}
	}

	if !DeployUtils.IsPreEmbadded(instId) || !isHandle {
		resInfo := "fail"
		if res {
			resInfo = "success"
		}

		global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s %s", op.Action, cmptName, resInfo))
		if op.Code == consts.INSTANCE_OPERATION_START.Code || op.Code == consts.INSTANCE_OPERATION_RESTART.Code {
			time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
			res = DeployUtils.IsProcExist(sshClient, instId, logKey, paasResult)
		}
	} else {
		global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("pre-embadded instance passby %s", op.Action))
	}

	return res
}

func UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile, rmDeployFile, isHandle bool,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	inst := meta.CMPT_META.GetInstance(instID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	cmptName := cmpt.CMPT_NAME
	version := DeployUtils.GetServiceVersion(servInstID, instID)
	sshId := meta.CMPT_META.GetInstAttr(instID, 116).ATTR_VALUE // 116 -> 'SSH_ID'

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("update %s, inst_id:%s, serv_ip:%s", cmptName, instID, ssh.SERVER_IP))

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	newName := ""
	stopCmd := ""
	startCmd := ""
	processor := ""
	dbInstId := ""
	oldName := ""
	baseDir := fmt.Sprintf("%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT)

	fileId := 0

	switch cmptName {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		oldName = "smsserver"
		newName = "smsserver_" + instID
		fileId = consts.SMS_SERVER_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsserver.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsserver.sh start", newName)
		break
	case consts.HEADER_SMS_PROCESS:
		oldName = "smsprocess"
		processor = meta.CMPT_META.GetInstAttr(instID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE // 205 -> 'PROCESSOR'
		newName = "smsprocess_" + processor
		fileId = consts.SMS_PROCESS_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsprocess.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsprocess.sh start", newName)
		break
	case consts.HEADER_SMS_CLIENT:
		oldName = "smsclient-standard"
		processor = meta.CMPT_META.GetInstAttr(instID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE // 205 -> 'PROCESSOR'
		newName = "smsclient-standard_" + processor
		fileId = consts.SMS_CLIENT_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsclient.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsclient.sh start", newName)
		break
	case consts.HEADER_SMS_BATSAVE:
		oldName = "smsbatsave"
		processor = meta.CMPT_META.GetInstAttr(instID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE // 205 -> 'PROCESSOR'
		dbInstId = meta.CMPT_META.GetInstAttr(instID, consts.DB_INST_ATTR_ID).ATTR_VALUE    // 213 -> 'DB_INST_ID'
		newName = "smsbatsave_" + processor + "_" + dbInstId
		fileId = consts.SMS_BATSAVE_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsbatsave.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsbatsave.sh start", newName)
		break
	case consts.HEADER_SMS_STATS:
		oldName = "smsstatistics"
		newName = "smsstatistics_" + instID
		fileId = consts.SMS_STATS_FILE_ID
		stopCmd = fmt.Sprintf("./%s/bin/smsstatistics.sh stop", newName)
		startCmd = fmt.Sprintf("./%s/bin/smsstatistics.sh start", newName)
		break
	default:
		break
	}

	// 1. scp deploy file and unzip
	if loadDeployFile {
		if !DeployUtils.FetchAndExtractZipDeployFile(sshClient, fileId, consts.SMS_GATEWAY_ROOT, oldName, version, logKey, paasResult) {
			return false
		}
	}

	if !DeployUtils.CD(sshClient, baseDir, logKey, paasResult) {
		return false
	}

	// 2. stop instance
	global.GLOBAL_RES.PubLog(logKey, stopCmd)
	if !DeployUtils.ExecSimpleCmd(sshClient, stopCmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.IsPreEmbadded(instID) {
		time.Sleep(time.Duration(consts.STOP_WAIT_MILLI_SECONDS) * time.Millisecond)
		if DeployUtils.IsProcExist(sshClient, instID, logKey, paasResult) {
			global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s 更新前执行进程停止失败", cmptName, instID))

			return false
		}
	}

	// 3. 更新文件
	rmInstanceJar := fmt.Sprintf("rm ./%s/*.jar ", newName)
	rmLibJar := fmt.Sprintf("rm ./%s/lib/*.jar ", newName)
	cmdInstanceJar := fmt.Sprintf("cp ./%s/*.jar ./%s ", oldName, newName)
	cmdLibJar := fmt.Sprintf("cp ./%s/lib/*.jar ./%s/lib ", oldName, newName)
	if !DeployUtils.ExecSimpleCmd(sshClient, rmInstanceJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, rmLibJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, cmdInstanceJar, logKey, paasResult) {
		return false
	}
	if !DeployUtils.ExecSimpleCmd(sshClient, cmdLibJar, logKey, paasResult) {
		return false
	}

	// 4. 重新拉起
	global.GLOBAL_RES.PubLog(logKey, startCmd)

	if DeployUtils.IsPreEmbadded(instID) {
		global.GLOBAL_RES.PubLog(logKey, "PRE_EMBADDED instance, do not need to start ......")
		if !metadao.UpdateInstanceDeployFlag(instID, consts.STR_PRE_EMBADDED, logKey, magicKey, paasResult) {
			return false
		}
	} else {
		if !DeployUtils.ExecSimpleCmd(sshClient, startCmd, logKey, paasResult) {
			return false
		}

		time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
		if !DeployUtils.IsProcExist(sshClient, instID, logKey, paasResult) {
			global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("%s %s 更新后执行进程拉起失败", cmptName, instID))

			return false
		}
	}
	if rmDeployFile {
		if !DeployUtils.RM(sshClient, oldName, logKey, paasResult) {
			return false
		}
	}

	// 5. 修改实例的version属性
	// 227 -> 'VERSION'
	if !metadao.ModInstanceAttr(instID, 227, "VERSION", version, logKey, magicKey, paasResult) {
		return false
	}

	res := true
	// 非预埋节点且成功启动后才更新IS_DEPLOYED = 1
	if !DeployUtils.IsPreEmbadded(instID) {
		time.Sleep(time.Duration(consts.START_WAIT_MILLI_SECONDS) * time.Millisecond)
		res = DeployUtils.IsProcExist(sshClient, instID, logKey, paasResult)
		if res {
			res = metadao.UpdateInstanceDeployFlag(instID, consts.STR_DEPLOYED, logKey, magicKey, paasResult)
		}
	}

	resInfo := "fail"
	if res {
		resInfo = "success"
	}
	global.GLOBAL_RES.PubSuccessLog(logKey, fmt.Sprintf("update %s to version:%s %s", cmptName, version, resInfo))

	paasResult.RET_INFO = version
	return res
}

func CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool {
	inst := meta.CMPT_META.GetInstance(instID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	sshId := meta.CMPT_META.GetInstAttr(instID, 116).ATTR_VALUE // 116 -> 'SSH_ID'

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, "", paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	webConsolePort := meta.CMPT_META.GetInstAttr(instID, 146).ATTR_VALUE // 146 -> 'WEB_CONSOLE_PORT'
	processorAttr := meta.CMPT_META.GetInstAttr(instID, 205)             // 205 -> 'PROCESSOR'
	realPort := ""
	if processorAttr != nil {
		processor := processorAttr.ATTR_VALUE
		iWebConsolePort, _ := strconv.Atoi(webConsolePort)
		iProcessor, _ := strconv.Atoi(processor)
		realPort = strconv.Itoa(iWebConsolePort + iProcessor)
	} else {
		realPort = webConsolePort
	}

	ret := true
	switch cmpt.CMPT_NAME {
	case consts.HEADER_SMS_SERVER:
	case consts.HEADER_SMS_SERVER_EXT:
		ret = DeployUtils.CheckPortUp(sshClient, "smsserver", instID, realPort, "", paasResult)
		break
	case consts.HEADER_SMS_PROCESS:
		ret = DeployUtils.CheckPortUp(sshClient, "smsprocess", instID, realPort, "", paasResult)
		break
	case consts.HEADER_SMS_CLIENT:
		ret = DeployUtils.CheckPortUp(sshClient, "smsclient", instID, realPort, "", paasResult)
		break
	case consts.HEADER_SMS_BATSAVE:
		ret = DeployUtils.CheckPortUp(sshClient, "smsbatsave", instID, realPort, "", paasResult)
		break
	case consts.HEADER_SMS_STATS:
		ret = DeployUtils.CheckPortUp(sshClient, "smsstatistics", instID, realPort, "", paasResult)
		break
	default:
		break
	}

	if !ret {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = "service port not up"
	}

	return ret
}
