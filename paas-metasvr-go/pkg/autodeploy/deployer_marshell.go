package autodeploy

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/factory"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func DeployService(instID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) {
	service := meta.CMPT_META.GetService(instID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)

		return
	}

	if service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_ALLREADY_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_ALLREADY_DEPLOYED)

		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.DeployService(instID, deployFlag, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("service deploy success ......")
	} else {
		utils.LOGGER.Info("service deploy fail ......")
	}
}

func UndeployService(instID, logKey, magicKey string, force bool, paasResult *result.ResultBean) {
	service := meta.CMPT_META.GetService(instID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)

		return
	}

	if !service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_DEPLOYED)

		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.UndeployService(instID, force, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("service undeploy success ......")
	} else {
		utils.LOGGER.Info("service undeploy fail ......")
	}
}

func DeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) {
	service := meta.CMPT_META.GetService(servInstID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)

		return
	}

	if !service.IsDeployed() {
		errMsg := fmt.Sprintf("service inst_id:%s, %s", servInstID, consts.ERR_SERVICE_NOT_DEPLOYED)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	if inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is allready deployed, inst_id:%s", instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_ALLREADY_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	cmpt := meta.CMPT_META.GetCmptByName(service.SERV_TYPE)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.DeployInstance(servInstID, instID, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("deploy instance success ......")
	} else {
		utils.LOGGER.Info("deploy instance fail ......")
	}
}

func UndeployInstance(servInstID, instID, logKey, magicKey string, paasResult *result.ResultBean) {
	service := meta.CMPT_META.GetService(servInstID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)

		return
	}

	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	if !inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is not deployed, inst_id:%s", instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	cmpt := meta.CMPT_META.GetCmptByName(service.SERV_TYPE)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.UndeployInstance(servInstID, instID, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("undeploy instance success ......")
	} else {
		utils.LOGGER.Info("undeploy instance fail ......")
	}
}

func MaintainInstance(servInstID, instID, servType, logKey, magicKey string, op consts.OperationEnum,
	isOperateByHandle bool, paasResult *result.ResultBean) {

	service := meta.CMPT_META.GetService(servInstID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)

		return
	}

	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	if !inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is not deployed, inst_id:%s", instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		return
	}

	cmpt := meta.CMPT_META.GetCmptByName(service.SERV_TYPE)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.MaintainInstance(servInstID, instID, servType, op, isOperateByHandle, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("maintain instance success ......")
	} else {
		utils.LOGGER.Info("maintain instance fail ......")
	}
}

func CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) {
	service := meta.CMPT_META.GetService(servInstID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND

		return
	}

	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND

		return
	}

	if !inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is not deployed, inst_id:%s", instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED

		return
	}

	cmpt := meta.CMPT_META.GetCmptByName(service.SERV_TYPE)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, service_type:%s", servInstID, instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	serviceDeployer := factory.DEPLOYER_FACTORY.Get(service.SERV_TYPE)
	if serviceDeployer == nil {
		errMsg := fmt.Sprintf("service deployer not found, service_id:%s, service_type:%s", instID, service.SERV_TYPE)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return
	}

	if serviceDeployer.CheckInstanceStatus(servInstID, instID, servType, magicKey, paasResult) {
		utils.LOGGER.Info("CheckInstanceStatus instance success ......")
	} else {
		utils.LOGGER.Info("CheckInstanceStatus instance fail ......")
	}
}

func GetDeployLog(logKey string, paasResult *result.ResultBean) {
	global.GLOBAL_RES.GetDeployLog(logKey, paasResult)
}
