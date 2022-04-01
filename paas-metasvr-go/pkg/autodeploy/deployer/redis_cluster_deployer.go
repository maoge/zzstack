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
		if !RedisDeployUtils.DeployFakeClusterService(servInstID, logKey, magicKey, servJson, paasResult) {
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

	// 1. deploy redis nodes
	init := false
	size := len(redisNodeArr)
	for idx, redisNode := range redisNodeArr {
		if idx == size-1 {
			init = true
		}

		if !RedisDeployUtils.DeployRedisNode(redisNode, init, false, true, node4cluster, version, logKey, magicKey, paasResult) {
			return false
		}
	}

	// 2. deploy proxy
	for _, proxy := range proxyArr {
		if !RedisDeployUtils.DeployProxyNode(proxy, nodes4proxy, logKey, magicKey, paasResult) {
			return false
		}
	}

	// 部署collectd服务
	collectdRaw := servJson[consts.HEADER_COLLECTD]
	if collectdRaw != nil {
		collectd := collectdRaw.(map[string]interface{})
		if len(collectd) > 0 {
			if !common.DeployCollectd(collectd, servInstID, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 3. update t_service.is_deployed and local cache
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

	return true
}

func (h *RedisClusterDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum,
	isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile bool,
	rmDeployFile bool, isOperateByHandle bool, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *RedisClusterDeployer) CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool {
	return true
}
