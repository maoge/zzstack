package autodeploy

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"

	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/global_factory"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"

	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
)

func DeployService(instID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsServiceDeployed(logKey, service, paasResult) {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
		return
	}

	if serviceDeployer.DeployService(instID, deployFlag, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("service deploy success ......")
	} else {
		utils.LOGGER.Info("service deploy fail ......")
	}
}

func UndeployService(instID, logKey, magicKey string, force bool, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsServiceNotDeployed(logKey, service, paasResult) {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
		return
	}

	if serviceDeployer.UndeployService(instID, force, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("service undeploy success ......")
	} else {
		utils.LOGGER.Info("service undeploy fail ......")
	}
}

func DeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(servInstID, logKey, paasResult)
	if !found {
		return
	}

	servInst, found := DeployUtils.GetInstance(servInstID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsServiceNotDeployed(logKey, service, paasResult) {
		return
	}

	inst, found := DeployUtils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := DeployUtils.GetCmptById(servInstID, instID, servInst.CMPT_ID, paasResult)
	if cmpt == nil {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
		return
	}

	if serviceDeployer.DeployInstance(servInstID, instID, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("deploy instance success ......")
	} else {
		utils.LOGGER.Info("deploy instance fail ......")
	}
}

func UndeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(servInstID, logKey, paasResult)
	if !found {
		return
	}

	servInst, found := DeployUtils.GetInstance(servInstID, logKey, paasResult)
	if !found {
		return
	}

	inst, found := DeployUtils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := DeployUtils.GetCmptById(servInstID, instID, servInst.CMPT_ID, paasResult)
	if cmpt == nil {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
		return
	}

	if serviceDeployer.UndeployInstance(servInstID, instID, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("undeploy instance success ......")
	} else {
		utils.LOGGER.Info("undeploy instance fail ......")
	}
}

func MaintainInstance(servInstID, instID, servType, logKey, magicKey string, op *consts.OperationExt,
	isOperateByHandle bool, paasResult *result.ResultBean) {

	service, found := DeployUtils.GetService(servInstID, logKey, paasResult)
	if !found {
		return
	}

	servInst, found := DeployUtils.GetInstance(servInstID, logKey, paasResult)
	if !found {
		return
	}

	inst, found := DeployUtils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := DeployUtils.GetCmptById(servInstID, instID, servInst.CMPT_ID, paasResult)
	if cmpt == nil {
		return
	}

	serviceMaintainer := global_factory.GetServiceMaintainer(instID, service.SERV_TYPE, paasResult)
	if serviceMaintainer == nil {
		return
	}

	if serviceMaintainer.MaintainInstance(servInstID, instID, servType, op, isOperateByHandle, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("maintain instance success ......")
	} else {
		utils.LOGGER.Info("maintain instance fail ......")
	}
}

func BatchUpdateInst(servInstID, servType, logKey, magicKey string, instIdArr []string, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(servInstID, logKey, paasResult)
	if !found {
		return
	}

	successInstId := ""
	for _, instId := range instIdArr {
		// 批量更新前先将实例IS_DEPLOY置为2，防止更新的过程中又被监控扫描程序拉起
		metadao.UpdateInstanceDeployFlag(instId, consts.STR_WARN, logKey, magicKey, paasResult)
	}

	len := len(instIdArr)

	for i, instID := range instIdArr {
		loadDeployFile := false
		rmDeployFile := false

		if i == 0 {
			// 第一个要拉取部署文件
			loadDeployFile = true

			if len > 1 {
				nextInstID := instIdArr[i+1]
				if !DeployUtils.IsSameSSH(instID, nextInstID) {
					// 如果后面的与当前是部署在同一台，本次用完后不删除部署文件
					rmDeployFile = true
				}
			} else {
				rmDeployFile = true
			}
		} else if i < (len - 1) {
			privInstID := instIdArr[i-1]
			nextInstID := instIdArr[i+1]
			if !DeployUtils.IsSameSSH(instID, nextInstID) {
				// 如果后面的与当前是部署在同一台，本次用完后不删除部署文件
				rmDeployFile = true
			}
			if !DeployUtils.IsSameSSH(instID, privInstID) {
				// 如果当前的与前面的部署在不同的机器，则要拉取
				loadDeployFile = true
			}
		} else {
			// 最后一个要删除部署文件
			rmDeployFile = true

			privInstID := instIdArr[i-1]
			if !DeployUtils.IsSameSSH(instID, privInstID) {
				// 如果当前的与前面的部署在不同的机器，则要拉取
				loadDeployFile = true
			}
		}

		inst := meta.CMPT_META.GetInstance(instID)
		if inst == nil {
			info := fmt.Sprintf("instance id:%s not found", instID)
			paasResult.RET_CODE = consts.REVOKE_NOK
			global.GLOBAL_RES.PubLog(logKey, info)
			break
		}

		if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
			paasResult.RET_CODE = consts.REVOKE_NOK
			global.GLOBAL_RES.PubLog(logKey, consts.ERR_INSTANCE_NOT_DEPLOYED)
			break
		}

		servInst := meta.CMPT_META.GetInstance(servInstID)
		if servInst == nil {
			paasResult.RET_CODE = consts.REVOKE_NOK
			global.GLOBAL_RES.PubLog(logKey, consts.ERR_SERVICE_NOT_FOUND)
			break
		}

		cmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)
		maintainResult := false
		serviceMaintainer := global_factory.GetServiceMaintainer(instID, service.SERV_TYPE, paasResult)
		if serviceMaintainer == nil {
			return
		}

		// InstanceOperationEnum op = InstanceOperationEnum.INSTANCE_OPERATION_UPDATE;
		op_action := consts.OP_UPDATE
		if serviceMaintainer != nil {
			maintainResult = serviceMaintainer.UpdateInstanceForBatch(servInstID, instID, servType, loadDeployFile,
				rmDeployFile, true, logKey, magicKey, paasResult)
		} else {
			errInfo := fmt.Sprintf("service deployer not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, cmpt.SERV_TYPE)
			global.GLOBAL_RES.PubLog(logKey, errInfo)

			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = errInfo

			break
		}

		if maintainResult {
			global.GLOBAL_RES.PubLog(logKey, fmt.Sprintf("instance %s success ......", op_action))

			if strings.Count(successInstId, "") > 1 {
				successInstId += ","
			}

			successInstId += instID
		} else {
			paasResult.RET_CODE = consts.REVOKE_NOK
			global.GLOBAL_RES.PubFailLog(logKey, fmt.Sprintf("instance %s fail ......", op_action))
			break
		}
	}

	paasResult.RET_INFO = successInstId
}

func CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) {
	service, found := DeployUtils.GetService(instID, "", paasResult)
	if !found {
		return
	}

	servInst, found := DeployUtils.GetInstance(servInstID, "", paasResult)
	if !found {
		return
	}

	inst, found := DeployUtils.GetInstance(instID, "", paasResult)
	if !found {
		return
	}

	if DeployUtils.IsInstanceNotDeployed("", inst, paasResult) {
		return
	}

	cmpt := DeployUtils.GetCmptById(servInstID, instID, servInst.CMPT_ID, paasResult)
	if cmpt == nil {
		return
	}

	serviceMaintainer := global_factory.GetServiceMaintainer(instID, service.SERV_TYPE, paasResult)
	if serviceMaintainer == nil {
		return
	}

	if serviceMaintainer.CheckInstanceStatus(servInstID, instID, servType, magicKey, paasResult) {
		utils.LOGGER.Info("CheckInstanceStatus instance success ......")
	} else {
		utils.LOGGER.Info("CheckInstanceStatus instance fail ......")
	}
}

func GetDeployLog(logKey string, paasResult *result.ResultBean) {
	global.GLOBAL_RES.GetDeployLog(logKey, paasResult)
}

func GetAppLog(servID, instID, logType string, paasResult *result.ResultBean) {
	instance := meta.CMPT_META.GetInstance(instID)
	if instance == nil || !instance.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED
		return
	}

	cmptName := meta.CMPT_META.GetInstCmptName(instID)
	logFile := getSmsLogFile(instance, cmptName, logType)

	sshId := meta.CMPT_META.GetInstAttr(instID, 116).ATTR_VALUE // 116, 'SSH_ID'
	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, "", paasResult) {
		return
	} else {
		defer sshClient.Close()
	}

	context, err := sshClient.Tail(logFile, 100)
	if err != nil {
		paasResult.RET_CODE = consts.REVOKE_OK
		paasResult.RET_INFO = err.Error()
	} else {
		replacedContext := strings.ReplaceAll(context, consts.LINE_END, consts.HTML_LINE_END)

		paasResult.RET_CODE = consts.REVOKE_OK
		paasResult.RET_INFO = replacedContext
	}
}

func getSmsLogFile(instance *proto.PaasInstance, cmptName, logType string) string {
	result := ""
	fullFilePath := ""
	logFileName := getLogFileName(instance, cmptName, logType)

	switch cmptName {
	case consts.APP_SMS_SERVER:
		fullFilePath = fmt.Sprintf("smsserver_%s/logs/%s", instance.INST_ID, logFileName)
		break
	case consts.APP_SMS_PROCESS:
		processor := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		fullFilePath = fmt.Sprintf("smsprocess_%s/logs/%s", processor, logFileName)
		break
	case consts.APP_SMS_CLIENT:
		chanGrp := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		fullFilePath = fmt.Sprintf("smsclient-standard_%s/logs/%s", chanGrp, logFileName)
		break
	case consts.APP_SMS_BATSAVE:
		batSaveGrp := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		dbInstId := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.DB_INST_ATTR_ID).ATTR_VALUE
		fullFilePath = fmt.Sprintf("smsbatsave_%s_%s/logs/%s", batSaveGrp, dbInstId, logFileName)
		break
	case consts.APP_SMS_STATS:
		fullFilePath = fmt.Sprintf("smsstatistics_%s/logs/%s", instance.INST_ID, logFileName)
		break
	default:
		break
	}

	if fullFilePath != "" {
		result = fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SMS_GATEWAY_ROOT, fullFilePath)
	}

	return result
}

func getLogFileName(instance *proto.PaasInstance, cmptName, logType string) string {
	logFileName := ""

	if logType == consts.LOG_TYPE_STDOUT {
		return "stdout.log"
	}

	switch cmptName {
	case consts.APP_SMS_SERVER:
		logFileName = fmt.Sprintf("smsserver-%s.log", logType)
		break
	case consts.APP_SMS_PROCESS:
		processor := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		logFileName = fmt.Sprintf("smsprocess-%s-%s.log", logType, processor)
		break
	case consts.APP_SMS_CLIENT:
		chanGrp := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		logFileName = fmt.Sprintf("smsclient-%s-%s.log", logType, chanGrp)
		break
	case consts.APP_SMS_BATSAVE:
		batSaveGrp := meta.CMPT_META.GetInstAttr(instance.INST_ID, consts.PROCESSOR_ATTR_ID).ATTR_VALUE
		logFileName = fmt.Sprintf("smsbatsave-%s-%s.log", logType, batSaveGrp)
		break
	case consts.APP_SMS_STATS:
		logFileName = fmt.Sprintf("smsstatistics-%s.log", logType)
		break
	default:
		break
	}

	return logFileName
}
