package deployutils

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func GetServiceTopo(servInstID, logKey string, paasResult *result.ResultBean) bool {
	if !metadao.LoadServiceTopo(servInstID, paasResult) {
		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
		}

		return false
	}

	return true
}

func GetService(instID string, logKey string, paasResult *result.ResultBean) (*proto.PaasService, bool) {
	service := meta.CMPT_META.GetService(instID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)
		}

		return nil, false
	}

	return service, true
}

func GetInstance(instID string, logKey string, paasResult *result.ResultBean) (*proto.PaasInstance, bool) {
	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return nil, false
	}

	return inst, true
}

func IsServiceDeployed(logKey string, service *proto.PaasService, paasResult *result.ResultBean) bool {
	if service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_ALLREADY_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_ALLREADY_DEPLOYED)
		}

		return true
	}
	return false
}

func IsServiceNotDeployed(logKey string, service *proto.PaasService, paasResult *result.ResultBean) bool {
	if !service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_DEPLOYED)
		}

		return true
	}
	return false
}

func IsInstanceDeployed(logKey string, inst *proto.PaasInstance, paasResult *result.ResultBean) bool {
	if inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is allready deployed, inst_id:%s", inst.INST_ID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_ALLREADY_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return true
	}

	return false
}

func IsInstanceNotDeployed(logKey string, inst *proto.PaasInstance, paasResult *result.ResultBean) bool {
	if inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is not deployed, inst_id:%s", inst.INST_ID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return true
	}

	return false
}

func GetCmptByName(servInstID, instID, servType string, paasResult *result.ResultBean) *proto.PaasMetaCmpt {
	cmpt := meta.CMPT_META.GetCmptByName(servType)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, servType)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return cmpt
}

func GetDeployFile(fileId int, logKey string, paasResult *result.ResultBean) *proto.PaasDeployFile {
	deployFile := meta.CMPT_META.GetDeployFile(fileId)
	if deployFile == nil {
		errMsg := fmt.Sprintf("deploy file id: %d not found ......", fileId)
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return deployFile
}

func CheckPortUpPredeploy(sshClient *SSHClient, port, logKey string, paasResult *result.ResultBean) bool {
	using, err := sshClient.IsPortUsed(port)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if using {
		errMsg := fmt.Sprintf("redis-server: %s, port: %s is in using", sshClient.Ip, port)
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return using
}

func FetchAndExtractTgzDeployFile(sshClient *SSHClient, fileId int, subPath, version, logKey string, paasResult *result.ResultBean) bool {
	deployFile := GetDeployFile(fileId, logKey, paasResult)
	if deployFile == nil {
		return false
	}

	hostId := deployFile.HOST_ID
	srcFileName := deployFile.FILE_NAME
	srcFileDir := deployFile.FILE_DIR

	if version == "" {
		version = deployFile.VERSION
	}

	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	deployHost := meta.CMPT_META.GetDeployHost(hostId)

	srcIp := deployHost.IP_ADDRESS
	srcPort := deployHost.SSH_PORT
	srcUser := deployHost.USER_NAME
	srcPwd := deployHost.USER_PWD

	rootDir := fmt.Sprintf("%s/%s", consts.PAAS_ROOT, subPath)

	global.GLOBAL_RES.PubLog(logKey, "create install dir ......")
	if !MkDir(sshClient, rootDir, logKey, paasResult) {
		return false
	}
	if !CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	srcFile := srcFileDir + srcFileName
	desFile := "./" + srcFileName
	global.GLOBAL_RES.PubLog(logKey, "scp deploy file ......")
	if !sshClient.SCP(srcUser, srcPwd, srcIp, srcPort, srcFile, desFile, logKey, paasResult) {
		return false
	}

	// 防止文件没有下载下来
	if !IsFileExist(sshClient, desFile, false, logKey, paasResult) {
		return false
	}

	idx := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:idx]

	global.GLOBAL_RES.PubLog(logKey, "unpack install tar file ......")
	if !TAR(sshClient, consts.TAR_ZXVF, srcFileName, oldName, logKey, paasResult) {
		return false
	}
	if !RM(sshClient, srcFileName, logKey, paasResult) {
		return false
	}

	return true
}

func InitRedisCluster(sshClient *SSHClient, initCmd, logKey string, paasResult *result.ResultBean) bool {
	bytes, ok, err := sshClient.InitRedisCluster(initCmd)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if ok {
		global.GLOBAL_RES.PubSuccessLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_OK
		paasResult.RET_INFO = ""
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INIT_REDIS_CLUSTER_FAIL
	}

	return ok
}
