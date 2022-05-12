package deployer

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	MinioDeployerUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/minio"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type StoreMinioDeployer struct {
}

func (h *StoreMinioDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	if DeployUtils.IsServiceDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	version := serv.VERSION

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})

	minioContainer := servJson[consts.HEADER_MINIO_CONTAINER].(map[string]interface{})
	minioArr := minioContainer[consts.HEADER_MINIO].([]map[string]interface{})

	endpoints := MinioDeployerUtils.GetEndpoints(minioArr)
	endpoints += "2>./log/stderr.log 1>./log/stdout.log &"
	endpoints = strings.ReplaceAll(endpoints, "/", "\\/")
	for _, minioNode := range minioArr {
		if !MinioDeployerUtils.DeployMinioNode(minioNode, endpoints, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "minio node deploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *StoreMinioDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	version := serv.VERSION
	// 未部署直接退出不往下执行
	if DeployUtils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})

	minioContainer := servJson[consts.HEADER_MINIO_CONTAINER].(map[string]interface{})
	minioArr := minioContainer[consts.HEADER_MINIO].([]map[string]interface{})

	for _, minioNode := range minioArr {
		if !MinioDeployerUtils.UndeployMinioNode(minioNode, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "minio node undeploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *StoreMinioDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !DeployUtils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	version := serv.VERSION
	// 未部署直接退出不往下执行
	if DeployUtils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	servInst := meta.CMPT_META.GetInstance(servInstID)
	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})

	minioContainer := servJson[consts.HEADER_MINIO_CONTAINER].(map[string]interface{})
	minioArr := minioContainer[consts.HEADER_MINIO].([]map[string]interface{})
	endpoints := MinioDeployerUtils.GetEndpoints(minioArr)

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false
	switch instCmpt.CMPT_NAME {
	case consts.CMPT_MINIO:
		minioNode := DeployUtils.GetSpecifiedItem(minioArr, instID)
		deployResult = MinioDeployerUtils.DeployMinioNode(minioNode, endpoints, version, logKey, magicKey, paasResult)
		break
	default:
		break
	}

	if deployResult {
		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
	} else {
		info := fmt.Sprintf("service inst_id:%s, deploy failed ......", servInstID)
		global.GLOBAL_RES.PubFailLog(logKey, info)
	}

	return true
}

func (h *StoreMinioDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	paasResult.RET_CODE = consts.REVOKE_NOK
	paasResult.RET_INFO = consts.ERR_UNSURPORT_OPERATION
	global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_UNSURPORT_OPERATION)

	return true
}
