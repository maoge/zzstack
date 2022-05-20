package tdengine

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

func GetArbitratorAddr(tdArbitrator map[string]interface{}) string {
	sshID := tdArbitrator[consts.HEADER_SSH_ID].(string)
	arbitPort := tdArbitrator[consts.HEADER_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshID)
	if ssh == nil {
		return ""
	}

	return fmt.Sprintf("%s:%s", ssh.SERVER_IP, arbitPort)
}

func DeployArbitrator(tdArbitrator map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := tdArbitrator[consts.HEADER_SSH_ID].(string)
	port := tdArbitrator[consts.HEADER_PORT].(string)
	instId := tdArbitrator[consts.HEADER_INST_ID].(string)

	if DeployUtils.CheckInstanceDeployed(instId, logKey, paasResult) {
		return true
	}

	sshClient, ssh, ok := DeployUtils.GetSshClient(sshId, logKey, paasResult)
	if !ok {
		return false
	}
	defer sshClient.Close()

	info := fmt.Sprintf("deploy tdengine-arbitrator: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	// DB_TDENGINE_FILE_ID -> 'tdengine-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_TDENGINE_FILE_ID, consts.DB_TDENGINE_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TDENGINE_FILE_ID, version, logKey, paasResult)
	newName := "arbitrator_" + port

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")

	DeployUtils.SED(sshClient, consts.CONF_PORT, port, consts.ARBITRATOR_START_SHELL, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PORT, port, consts.ARBITRATOR_STOP_SHELL, logKey, paasResult)
	DeployUtils.RM(sshClient, "taosd_*.sh", logKey, paasResult)

	// start
	global.GLOBAL_RES.PubLog(logKey, "start arbitrtoe ......")
	cmd := fmt.Sprintf("./%s", consts.ARBITRATOR_START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "arbitrtor", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployArbitrator(arbitrator map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := arbitrator[consts.HEADER_INST_ID].(string)
	sshId := arbitrator[consts.HEADER_SSH_ID].(string)
	port := arbitrator[consts.HEADER_PORT].(string)

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

	info := fmt.Sprintf("start undeploy arbitrator, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := "arbitrator_" + port
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TDENGINE_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop arbitrator ......")
	cmd := fmt.Sprintf("./%s", consts.ARBITRATOR_STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "arbitrator", instId, port, logKey, paasResult) {
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

func DeployDnode(dnode map[string]interface{}, bIsFirst bool, arbitratorAddr, firstNodeIp, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := dnode[consts.HEADER_SSH_ID].(string)
	port := dnode[consts.HEADER_PORT].(string)
	instId := dnode[consts.HEADER_INST_ID].(string)

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

	info := fmt.Sprintf("deploy tdengine dnode: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	// BOOKIE_FILE_ID -> 'bookkeeper-4.14.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_TDENGINE_FILE_ID, consts.DB_TDENGINE_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TDENGINE_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("tdengine_%s", port)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify taos configure files ......")
	newConf := "etc/taos.cfg"

	DeployUtils.SED(sshClient, consts.CONF_FIRSTEP, firstNodeIp, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_FQDN, ssh.SERVER_IP, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_PORT, port, newConf, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ARBITRATOR_ADDR, arbitratorAddr, newConf, logKey, paasResult)

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")
	if !DeployUtils.SED(sshClient, consts.CONF_INST_ID, instId, consts.TAOSD_START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_INST_ID, instId, consts.TAOSD_STOP_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, "arbitrator_*.sh", logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start taosd ......")
	cmd := fmt.Sprintf("./%s", consts.TAOSD_START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "taosd", instId, port, logKey, paasResult) {
		return false
	}

	// 后续dnode要加入第一个dnode组成集群
	if !bIsFirst {
		exportCmd := "export LANG=zh_CN.UTF-8 && export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib"
		if !DeployUtils.ExecSimpleCmd(sshClient, exportCmd, logKey, paasResult) {
			return false
		}

		strNodeIp := strings.Split(firstNodeIp, ":")
		loginFirstNode := fmt.Sprintf("./bin/taos -h %s -P %s -c ./etc", strNodeIp[0], strNodeIp[1])
		if !sshClient.LoginTaosShell(loginFirstNode, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "login taosd failed ......")
			return false
		}
		createNode := fmt.Sprintf("CREATE DNODE \"%s:%s\" ;", ssh.SERVER_IP, port)
		if !sshClient.CreateOrDropNode(createNode, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "taosd create node is failed ......")
			return false
		}

		// 解析show dnodes;
		showNodes := "show dnodes;"
		if !sshClient.CheckTaosNodeOnline(showNodes, firstNodeIp, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "taosd status is offline ......")
			return false
		}
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployDnode(dnode map[string]interface{}, remove bool, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := dnode[consts.HEADER_INST_ID].(string)
	sshId := dnode[consts.HEADER_SSH_ID].(string)
	port := dnode[consts.HEADER_PORT].(string)

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

	info := fmt.Sprintf("start undeploy tdengine dnode, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("tdengine_%s", port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TDENGINE_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// 删除服务不需要考虑节点
	if remove {
		// 删除单节点时操作，集群中删除该节点
		exportCmd := fmt.Sprintf("export LANG=zh_CN.UTF-8 && export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib")
		if !DeployUtils.ExecSimpleCmd(sshClient, exportCmd, logKey, paasResult) {
			return false
		}
		loginFirstNode := fmt.Sprintf("./bin/taos -h %s -P %s -c ./etc", ssh.SERVER_IP, port)
		if !sshClient.LoginTaosShell(loginFirstNode, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "login taosd failed ......")
			return false
		}

		dropNode := fmt.Sprintf("DROP DNODE \"%s:%s\" ;", ssh.SERVER_IP, port)
		if !sshClient.CreateOrDropNode(dropNode, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "taosd drop node is failed ......")
			return false
		}

		showdNodes := "show dnodes;"
		if !sshClient.CheckTaosNodeOffLine(showdNodes, ssh.SERVER_IP+":"+port, logKey, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, "taosd status is offline ......")
			return false
		}

		if !DeployUtils.ExecSimpleCmd(sshClient, "quit;", logKey, paasResult) {
			return false
		}
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop tdengine dnode ......")
	cmd := fmt.Sprintf("./%s", consts.TAOSD_STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "tdengine dnode", instId, port, logKey, paasResult) {
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
