package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
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

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	minioContainer := servJson[consts.HEADER_MINIO_CONTAINER].(map[string]interface{})
	minioArr := minioContainer[consts.HEADER_MINIO].([]map[string]interface{})

	endpoints := MinioDeployerUtils.GetEndpoints(minioArr)
	for _, minioNode := range minioArr {
		if !MinioDeployerUtils.DeployMinioNode(minioNode, endpoints, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "minio node deploy failed ......")
			return false
		}
	}

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *StoreMinioDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	minioContainer := servJson[consts.HEADER_MINIO_CONTAINER].(map[string]interface{})
	minioArr := minioContainer[consts.HEADER_MINIO].([]map[string]interface{})

	for _, minioNode := range minioArr {
		if !MinioDeployerUtils.UndeployMinioNode(minioNode, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "minio node undeploy failed ......")
			return false
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *StoreMinioDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

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

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return true
}

func (h *StoreMinioDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	paasResult.RET_CODE = consts.REVOKE_NOK
	paasResult.RET_INFO = consts.ERR_UNSURPORT_OPERATION
	global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_UNSURPORT_OPERATION)

	return true
}
