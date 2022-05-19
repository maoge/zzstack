package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	CommonDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/common"
	RedisDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/redis"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type RedisClusterDeployer struct {
}

func (h *RedisClusterDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RedisDeployUtils.RedisClusterServiceFakeDeploy(servJson, servInstID, logKey, magicKey, consts.STR_DEPLOY, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "redis deploy failed ......")
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

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
			if !CommonDeployUtils.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
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

func (h *RedisClusterDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}
	serv := meta.CMPT_META.GetService(servInstID)

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
			if !CommonDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult) {
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

func (h *RedisClusterDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	node4cluster, nodes4proxy := RedisDeployUtils.GetClusterNodes(&redisNodeArr)

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.CMPT_REDIS_PROXY:
		proxyJson := DeployUtils.GetSpecifiedItem(proxyArr, instID)
		deployResult = RedisDeployUtils.DeployProxyNode(proxyJson, nodes4proxy, logKey, magicKey, paasResult)
		break

	case consts.CMPT_REDIS_NODE:
		redisJson := DeployUtils.GetSpecifiedItem(redisNodeArr, instID)
		deployResult = RedisDeployUtils.DeployRedisNode(redisJson, false, false, true, true, node4cluster, version, logKey, magicKey, paasResult)
		break

	case consts.CMPT_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		deployResult = CommonDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return true
}

func (h *RedisClusterDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})
	proxyArr := proxyContainer[consts.HEADER_REDIS_PROXY].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.CMPT_REDIS_PROXY:
		proxyJson := DeployUtils.GetSpecifiedItem(proxyArr, instID)
		undeployResult = RedisDeployUtils.UndeployProxyNode(proxyJson, false, logKey, magicKey, paasResult)
		// TODO notify clients ......
		break

	case consts.CMPT_REDIS_NODE:
		redisJson := DeployUtils.GetSpecifiedItem(redisNodeArr, instID)
		undeployResult = RedisDeployUtils.UndeployRedisNode(redisJson, true, logKey, magicKey, paasResult)
		break

	case consts.CMPT_COLLECTD:
		collectd := servJson[consts.HEADER_COLLECTD].(map[string]interface{})
		undeployResult = CommonDeployUtils.UndeployCollectd(collectd, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	return true
}
