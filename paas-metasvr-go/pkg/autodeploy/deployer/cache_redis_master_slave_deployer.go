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

	serv := meta.CMPT_META.GetService(servInstID)
	// 未部署直接退出不往下执行
	if deployutils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	inst := meta.CMPT_META.GetInstance(servInstID)
	cmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servJson := topoJson[cmpt.CMPT_NAME].(map[string]interface{})

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

	serv := meta.CMPT_META.GetService(servInstID)
	version := serv.VERSION

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servInst := meta.CMPT_META.GetInstance(servInstID)
	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)
	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})
	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	// 服务未部署直接退出不往下执行
	if deployutils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	if instCmpt.CMPT_NAME == consts.CMPT_REDIS_NODE {
		selfNode := RedisDeployUtils.GetSelfRedisNode(redisNodeArr, instID)
		strNodeType := selfNode[consts.ATTR_NODE_TYPE].(string)
		isMaster := strNodeType == consts.TYPE_REDIS_MASTER_NODE

		if RedisDeployUtils.CheckMasterNode(redisNodeArr, paasResult) {
			global.GLOBAL_RES.PubErrorLog(logKey, paasResult.RET_INFO.(string))
			return false
		}

		// 主节点直接部署，启动start脚本
		// 从节点，先启动，再执行slaveof挂载到主节点上
		if !RedisDeployUtils.DeploySingleRedisNode(selfNode, redisNodeArr, isMaster, version, logKey, magicKey, paasResult) {
			return false
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_COLLECTD {
		collectdRaw := servJson[consts.HEADER_COLLECTD]
		if collectdRaw != nil {
			collectd := collectdRaw.(map[string]interface{})
			if len(collectd) > 0 {
				if !common.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
					return false
				}
			}
		}
	}

	successLog := fmt.Sprintf("instance inst_id: %s, deploy sucess ......", instID)
	global.GLOBAL_RES.PubSuccessLog(logKey, successLog)

	return true
}

func (h *RedisMasterSlaveDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
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

	serv := meta.CMPT_META.GetService(servInstID)
	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servInst := meta.CMPT_META.GetInstance(servInstID)
	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)
	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})
	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	// 服务未部署直接退出不往下执行
	if deployutils.IsServiceNotDeployed(logKey, serv, paasResult) {
		return false
	}

	if instCmpt.CMPT_NAME == consts.CMPT_REDIS_NODE {
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

		if !RedisDeployUtils.UndeploySingleRedisNode(selfNode, true, logKey, magicKey, paasResult) {
			return false
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_COLLECTD {
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
	}

	return true
}
