package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"

	"github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	RedisDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/redis"
)

type RedisMasterSlaveDeployer struct {
}

func (h *RedisMasterSlaveDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

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

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *RedisMasterSlaveDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}
	serv := meta.CMPT_META.GetService(servInstID)

	// 卸载伪部署
	if serv.PSEUDO_DEPLOY_FLAG == consts.DEPLOY_FLAG_PSEUDO {
		if !RedisDeployUtils.RedisMasterSlaveServiceFakeDeploy(servJson, servInstID, logKey, magicKey, consts.STR_DEPLOY, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "redis undeploy failed ......")
			return false
		}

		info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	for _, redisNode := range redisNodeArr {
		// redisJson, false, logKey, magicKey, result
		if !RedisDeployUtils.UndeploySingleRedisNode(redisNode, false, logKey, magicKey, paasResult) {
			if !force {
				return false
			}
		}
	}

	// undeploy collectd
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !common.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "collectd undeploy failed ......")
				return false
			}
		}
	}

	// update t_meta_service.is_deployed and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *RedisMasterSlaveDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.CMPT_REDIS_NODE:
		selfNode := RedisDeployUtils.GetSelfRedisNode(redisNodeArr, instID)
		strNodeType := selfNode[consts.ATTR_NODE_TYPE].(string)
		isMaster := strNodeType == consts.TYPE_REDIS_MASTER_NODE

		if RedisDeployUtils.CheckMasterNode(redisNodeArr, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, paasResult.RET_INFO.(string))
			return false
		}

		// 主节点直接部署，启动start脚本
		// 从节点，先启动，再执行slaveof挂载到主节点上
		deployResult = RedisDeployUtils.DeploySingleRedisNode(selfNode, redisNodeArr, isMaster, version, logKey, magicKey, paasResult)
		break

	case consts.CMPT_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		deployResult = common.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return true
}

func (h *RedisMasterSlaveDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.CMPT_REDIS_NODE:
		selfNode := RedisDeployUtils.GetSelfRedisNode(redisNodeArr, instID)
		strNodeType := selfNode[consts.ATTR_NODE_TYPE].(string)
		isMaster := strNodeType == consts.TYPE_REDIS_MASTER_NODE

		// 不允许卸载主节点
		if isMaster {
			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = consts.ERR_FORBIDEN_REMOVE_REDIS_MNODE
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_FORBIDEN_REMOVE_REDIS_MNODE)
			return false
		}

		undeployResult = RedisDeployUtils.UndeploySingleRedisNode(selfNode, true, logKey, magicKey, paasResult)
		break

	case consts.CMPT_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		undeployResult = common.UndeployCollectd(collectd, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	return true
}
