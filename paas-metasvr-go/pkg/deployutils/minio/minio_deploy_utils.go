package minio

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

func GetEndpoints(minioArr []map[string]interface{}) string {
	if minioArr == nil || len(minioArr) == 0 {
		return ""
	}

	result := ""
	maxMinio := len(minioArr) - 1
	for i, minio := range minioArr {
		sshID := minio[consts.HEADER_SSH_ID].(string)
		mount := minio[consts.HEADER_MINIO_MOUNT].(string)
		mountArr := strings.Split(mount, consts.PATH_COMMA)

		ssh := meta.CMPT_META.GetSshById(sshID)
		if ssh == nil {
			continue
		}

		maxArr := len(mountArr) - 1
		for j, mountPoint := range mountArr {
			endPoint := fmt.Sprintf("    http://%s%s ", ssh.SERVER_IP, mountPoint)
			result += endPoint

			if j == maxArr && i == maxMinio {
				result += consts.CONF_END + "\\"
			} else {
				result += "\\\n"
			}

		}
	}
	return result
}

func DeployMinioNode(minioNode map[string]interface{}, endpoints, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := minioNode[consts.HEADER_INST_ID].(string)
	sshId := minioNode[consts.HEADER_SSH_ID].(string)
	minioPort := minioNode[consts.HEADER_PORT].(string)
	minioConsolePort := minioNode[consts.HEADER_CONSOLE_PORT].(string)
	region := minioNode[consts.HEADER_MINIO_REGION].(string)
	browerFlag := minioNode[consts.HEADER_MINIO_BROWSER].(string)

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

	info := fmt.Sprintf("deploy minio: %s:%s, instId:%s", ssh.SERVER_IP, minioPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	// STORE_MINIO_FILE_ID -> 'minio-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.STORE_MINIO_FILE_ID, consts.STORE_MINIO_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.STORE_MINIO_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("minio_%s", minioPort)
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	address := fmt.Sprintf("%s:%s", ssh.SERVER_IP, minioPort)
	consoleAddress := fmt.Sprintf("%s:%s", ssh.SERVER_IP, minioConsolePort)

	// export MINIO_BROWSER=%BROWSER%
	// export MINIO_REGION_NAME=%MINIO_REGION%
	// export MINIO_ACCESS_KEY=%MINIO_USER%
	// export MINIO_SECRET_KEY=%MINIO_PASSWD%
	// nohup ./bin/minio server --address %ADDRESS% --console-address %CONSOLE_ADDRESS% --config-dir ./etc \
	file := consts.START_SHELL
	DeployUtils.SED(sshClient, consts.CONF_MINIO_BROWSER, browerFlag, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MINIO_REGION, region, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MINIO_USER, consts.MINIO_ACCESS_KEY, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_MINIO_PASSWD, consts.MINIO_SECRET_KEY, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_ADDRESS, address, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CONSOLE_ADDRESS, consoleAddress, file, logKey, paasResult)
	DeployUtils.AppendMultiLine(sshClient, consts.CONF_ENDPOINTS, endpoints, file, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_END, "\\\\", file, logKey, paasResult)

	global.GLOBAL_RES.PubLog(logKey, "start minio ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "minio", instId, minioPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployMinioNode(minioNode map[string]interface{}, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := minioNode[consts.HEADER_INST_ID].(string)
	sshId := minioNode[consts.HEADER_SSH_ID].(string)
	minioPort := minioNode[consts.HEADER_PORT].(string)
	mount := minioNode[consts.HEADER_MINIO_MOUNT].(string)

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

	info := fmt.Sprintf("start undeploy minio, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, minioPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	newName := fmt.Sprintf("minio_%s", minioPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.STORE_MINIO_ROOT, newName)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop minio ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "minio", instId, minioPort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	// clear mount points
	mountArr := strings.Split(mount, consts.PATH_COMMA)
	for _, mountPoint := range mountArr {
		path := fmt.Sprintf("%s/.minio.sys", mountPoint)
		DeployUtils.RM(sshClient, path, logKey, paasResult)

		path = fmt.Sprintf("%s/*", mountPoint)
		DeployUtils.RM(sshClient, path, logKey, paasResult)
	}

	return true
}
