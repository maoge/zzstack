package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"

	"github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	RedisDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/redis"
)

type RedisMasterSlaveDeployer struct {
}

func (h *RedisMasterSlaveDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	if !deployutils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	serv := meta.CMPT_META.GetService(servInstID)
	if deployutils.IsServiceDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})
	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	version := serv.VERSION

	if !RedisDeployUtils.CheckMasterNode(redisNodeArr, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
		return false
	}

	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RedisDeployUtils.RedisMasterSlaveServiceFakeDeploy(servJson, servInstID, logKey, magicKey, consts.STR_DEPLOY, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "redis deploy failed ......")
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	// 1. deploy redis nodes
	for _, redisNode := range redisNodeArr {
		strNodeType := redisNode[consts.ATTR_NODE_TYPE].(string)
		isMaster := strNodeType == consts.TYPE_REDIS_MASTER_NODE

		if !RedisDeployUtils.DeploySingleRedisNode(redisNode, redisNodeArr, isMaster, version, logKey, magicKey, paasResult) {
			return false
		}
	}

	// deploy collectd
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !common.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 3.update t_meta_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *RedisMasterSlaveDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	if !deployutils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	// TODO

	info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func (h *RedisMasterSlaveDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	instCmpt := meta.CMPT_META.GetInstanceCmpt(instID)
	if instCmpt == nil {
		errMsg := fmt.Sprintf("instance %s component not found ......", instID)
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return false
	}

	if !deployutils.GetServiceTopo(servInstID, logKey, paasResult) {
		return false
	}

	// TODO

	return true
}

func (h *RedisMasterSlaveDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisMasterSlaveDeployer) MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum,
	isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisMasterSlaveDeployer) UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile bool,
	rmDeployFile bool, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisMasterSlaveDeployer) CheckInstanceStatus(servInstID, instID, servType, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
