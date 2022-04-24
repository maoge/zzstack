package common

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

func DeployCollectd(collectd map[string]interface{}, srvInstId, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := collectd[consts.HEADER_SSH_ID].(string)
	collectdPort := collectd[consts.HEADER_COLLECTD_PORT].(string)
	instId := collectd[consts.HEADER_INST_ID].(string)
	metaSvrUrl := collectd[consts.HEADER_META_SVR_URL].(string)
	metaSvrUsr := collectd[consts.HEADER_META_SVR_USR].(string)
	metaSvrPasswd := collectd[consts.HEADER_META_SVR_PASSWD].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		info := fmt.Sprintf("passby deployed collectd, inst_id:%s", instId)
		global.GLOBAL_RES.PubLog(logKey, info)
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	if DeployUtils.CheckPortUpPredeploy(sshClient, collectdPort, logKey, paasResult) {
		return false
	}

	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.COLLECTD_FILE_ID, consts.COLLECTD_ROOT, "", logKey, paasResult) {
		return false
	}

	oldName := consts.PAAS_COLLECTD
	newName := fmt.Sprintf("collectd_%s", instId)
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify paas-collectd.sh env params ......")

	// 替换启停脚本中的如下变量
	// UUID=%UUID%
	// SERV_INST_ID=%SERV_INST_ID%
	// META_SVR_URL=%META_SVR_URL%
	// META_SVR_USR=%META_SVR_USR%
	// META_SVR_PASSWD=%META_SVR_PASSWD%
	// COLLECTD_PORT=%COLLECTD_PORT%
	file := "./bin/paas-collectd.sh"
	if !DeployUtils.SED(sshClient, consts.CONF_UUID, instId, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_SERV_INST_ID, srvInstId, file, logKey, paasResult) {
		return false
	}
	metaSvrUrl = strings.Replace(metaSvrUrl, "/", "\\/", -1)
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_URL, metaSvrUrl, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_USR, metaSvrUsr, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_COLLECTD_PORT, collectdPort, file, logKey, paasResult) {
		return false
	}

	//执行权限
	if !DeployUtils.ChMod(sshClient, file, "+x", logKey, paasResult) {
		return false
	}

	//执行unix脚本命令
	if !DeployUtils.Dos2Unix(sshClient, file, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start collectd ......")
	cmd := fmt.Sprintf("./bin/paas-collectd.sh start")
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "paas-collectd", instId, collectdPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "init collectd success ......")
	return true
}

func UndeployCollectd(collectd map[string]interface{}, logKey, magicKey string, paasResult *result.ResultBean) bool {
	instId := collectd[consts.HEADER_INST_ID].(string)
	sshId := collectd[consts.HEADER_SSH_ID].(string)
	collectdPort := collectd[consts.HEADER_COLLECTD_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	inst := meta.CMPT_META.GetInstance(instId)
	if !DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("passby undeployed collectd, inst_id:%s, serv_ip:%s", instId, ssh.SERVER_IP))
		return true
	}

	global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("start undeploy collectd, inst_id:%s, serv_ip:%s", instId, ssh.SERVER_IP))

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	newName := fmt.Sprintf("collectd_%s", instId)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.COLLECTD_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop collectd ......")

	cmd := fmt.Sprintf("./bin/paas-collectd.sh stop")
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "paas-collectd", instId, collectdPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}
