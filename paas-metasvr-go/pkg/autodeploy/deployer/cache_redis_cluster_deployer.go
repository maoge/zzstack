package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	RedisDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/redis"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type RedisClusterDeployer struct {
}

func (h *RedisClusterDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
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

	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RedisDeployUtils.RedisClusterServiceFakeDeploy(servJson, servInstID, logKey, magicKey, consts.STR_DEPLOY, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "redis deploy failed ......")
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	version := serv.VERSION
	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	node4cluster, nodes4proxy := RedisDeployUtils.GetClusterNodes(&redisNodeArr)

	utils.LOGGER.Info(node4cluster)
	utils.LOGGER.Info(nodes4proxy)

	// 1.deploy redis nodes
	init := false
	size := len(redisNodeArr)
	for idx, redisNode := range redisNodeArr {
		if idx == size-1 {
			init = true
		}

		if !RedisDeployUtils.DeployRedisNode(redisNode, init, false, true, false, node4cluster, version, logKey, magicKey, paasResult) {
			return false
		}
	}

	// 2.deploy proxy
	for _, proxy := range proxyArr {
		if !RedisDeployUtils.DeployProxyNode(proxy, nodes4proxy, logKey, magicKey, paasResult) {
			return false
		}
	}

	// 3.deploy collectd
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !common.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 4.update t_meta_service.is_deployed and local cache
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

func (h *RedisClusterDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
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
		if !RedisDeployUtils.RedisClusterServiceFakeDeploy(servJson, servInstID, logKey, magicKey, consts.STR_UNDEPLOY, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "redis undeploy failed ......")
			return false
		}

		info := fmt.Sprintf("service inst_id: %s, undeploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	// 1.undeploy proxy
	for _, proxyJson := range proxyArr {
		if !RedisDeployUtils.UndeployProxyNode(proxyJson, force, logKey, magicKey, paasResult) {
			if !force {
				return false
			}
		}
	}

	// 2.undeploy redis nodes
	for _, redisJson := range redisNodeArr {
		if !RedisDeployUtils.UndeployRedisNode(redisJson, false, logKey, magicKey, paasResult) {
			if !force {
				return false
			}
		}
	}
	// 3.卸载collectd服务
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !common.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 4.update t_meta_service is_deployed flag
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

func (h *RedisClusterDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
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

	servInst := meta.CMPT_META.GetInstance(servInstID)
	serv := meta.CMPT_META.GetService(servInstID)
	version := serv.VERSION

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)
	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	node4cluster, nodes4proxy := RedisDeployUtils.GetClusterNodes(&redisNodeArr)

	if instCmpt.CMPT_NAME == consts.CMPT_REDIS_PROXY {
		for _, proxyJson := range proxyArr {
			currId := proxyJson[consts.HEADER_INST_ID]
			if currId != instID {
				continue
			}

			if !RedisDeployUtils.DeployProxyNode(proxyJson, nodes4proxy, logKey, magicKey, paasResult) {
				return false
			}
			// TODO notify clients ......
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_REDIS_NODE {
		for _, redisJson := range redisNodeArr {
			currId := redisJson[consts.HEADER_INST_ID]
			if currId != instID {
				continue
			}

			if !RedisDeployUtils.DeployRedisNode(redisJson, false, false, true, true, node4cluster, version, logKey, magicKey, paasResult) {
				return false
			}
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_COLLECTD {
		//卸载collectd服务
		collectdRaw, found := servJson[consts.HEADER_COLLECTD]
		if found {
			collectd := collectdRaw.(map[string]interface{})
			if !common.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	return true
}

func (h *RedisClusterDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
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

	servInst := meta.CMPT_META.GetInstance(servInstID)

	topoJson := paasResult.RET_INFO.(map[string]interface{})
	paasResult.RET_INFO = ""

	servCmpt := meta.CMPT_META.GetCmptById(servInst.CMPT_ID)
	servJson := topoJson[servCmpt.CMPT_NAME].(map[string]interface{})

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	if instCmpt.CMPT_NAME == consts.CMPT_REDIS_PROXY {
		for _, proxyJson := range proxyArr {
			currId := proxyJson[consts.HEADER_INST_ID]
			if currId != instID {
				continue
			}

			if !RedisDeployUtils.UndeployProxyNode(proxyJson, false, logKey, magicKey, paasResult) {
				return false
			}
			// TODO notify clients ......
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_REDIS_NODE {
		for _, redisJson := range redisNodeArr {
			currId := redisJson[consts.HEADER_INST_ID]
			if currId != instID {
				continue
			}
			if !RedisDeployUtils.UndeployRedisNode(redisJson, true, logKey, magicKey, paasResult) {
				return false
			}
		}
	} else if instCmpt.CMPT_NAME == consts.CMPT_COLLECTD {
		//卸载collectd服务
		collectdRaw, found := servJson[consts.HEADER_COLLECTD]
		if found {
			collectd := collectdRaw.(map[string]interface{})
			if !common.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	return true
}
