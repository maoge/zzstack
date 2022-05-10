package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	RocketMqDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/rocketmq"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type RocketMQDeployer struct {
}

func (h *RocketMQDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
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
	// 伪部署
	if deployFlag == consts.DEPLOY_FLAG_PSEUDO {
		if !RocketMqDeployUtils.DeployFakeService(servJson, servInstID, logKey, magicKey, paasResult) {
			return false
		}

		info := fmt.Sprintf("service inst_id:%s, deploy sucess ......", servInstID)
		global.GLOBAL_RES.PubSuccessLog(logKey, info)
		return true
	}

	// 物理部署
	// 部署namesrv服务
	nameSrvContainer := servJson[consts.HEADER_ROCKETMQ_NAMESRV_CONTAINER].(map[string]interface{})
	nameSrvArr := nameSrvContainer[consts.HEADER_ROCKETMQ_NAMESRV].([]map[string]interface{})
	for _, nameSrv := range nameSrvArr {
		if !RocketMqDeployUtils.DeployNameSrv(nameSrv, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "rocketmq namesrv deploy failed ......")
			return false
		}
	}

	// 部署broker服务
	vbrokerContainer := servJson[consts.HEADER_ROCKETMQ_VBROKER_CONTAINER].(map[string]interface{})
	vbrokerArr := vbrokerContainer[consts.HEADER_ROCKETMQ_VBROKER].([]map[string]interface{})
	namesrvAddrs := RocketMqDeployUtils.GetNameSrvAddrs(nameSrvArr)
	for _, vbroker := range vbrokerArr {
		// String vbrokerInstId = vbroker.getString(FixHeader.HEADER_INST_ID);
		brokerArr := vbroker[consts.HEADER_ROCKETMQ_BROKER].([]map[string]interface{})
		for idx, broker := range brokerArr {
			brokerId := fmt.Sprintf("%d", idx)
			if !RocketMqDeployUtils.DeployBroker(broker, servInstID, namesrvAddrs, brokerId, version, logKey, magicKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "rocketmq broker start failed ......")
				return false
			}
		}
	}

	return true
}

func (h *RocketMQDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RocketMQDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *RocketMQDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
