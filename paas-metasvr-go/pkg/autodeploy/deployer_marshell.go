package autodeploy

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/global_factory"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func DeployService(instID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) {
	service, found := deployutils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsServiceDeployed(logKey, service, paasResult) {
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
	service, found := deployutils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsServiceNotDeployed(logKey, service, paasResult) {
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
	service, found := deployutils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsServiceNotDeployed(logKey, service, paasResult) {
		return
	}

	inst, found := deployutils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsInstanceDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := deployutils.GetCmptByName(servInstID, instID, service.SERV_TYPE, paasResult)
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
	service, found := deployutils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	inst, found := deployutils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := deployutils.GetCmptByName(servInstID, instID, service.SERV_TYPE, paasResult)
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

func MaintainInstance(servInstID, instID, servType, logKey, magicKey string, op consts.OperationEnum,
	isOperateByHandle bool, paasResult *result.ResultBean) {

	service, found := deployutils.GetService(instID, logKey, paasResult)
	if !found {
		return
	}

	inst, found := deployutils.GetInstance(instID, logKey, paasResult)
	if !found {
		return
	}

	if deployutils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return
	}

	cmpt := deployutils.GetCmptByName(servInstID, instID, service.SERV_TYPE, paasResult)
	if cmpt == nil {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
		return
	}

	if serviceDeployer.MaintainInstance(servInstID, instID, servType, op, isOperateByHandle, logKey, magicKey, paasResult) {
		utils.LOGGER.Info("maintain instance success ......")
	} else {
		utils.LOGGER.Info("maintain instance fail ......")
	}
}

func BatchUpdateInst(servInstID, servType, logKey, magicKey string, instIdArr []string, paasResult *result.ResultBean) {
	// TODO
}

func CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) {
	service, found := deployutils.GetService(instID, "", paasResult)
	if !found {
		return
	}

	inst, found := deployutils.GetInstance(instID, "", paasResult)
	if !found {
		return
	}

	if deployutils.IsInstanceNotDeployed("", inst, paasResult) {
		return
	}

	cmpt := deployutils.GetCmptByName(servInstID, instID, service.SERV_TYPE, paasResult)
	if cmpt == nil {
		return
	}

	serviceDeployer := global_factory.GetServiceDeployer(instID, service.SERV_TYPE, paasResult)
	if serviceDeployer == nil {
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

func GetAppLog(servID, instID, logType string, paasResult *result.ResultBean) {
	// TODO
}
