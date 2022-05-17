package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServerlessApisixDeployer struct {
}

func (h *ServerlessApisixDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	// servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	// if !ok {
	// 	return false
	// }
	// serv := meta.CMPT_META.GetService(servInstID)

	// etcdContainer := servJson[consts.HEADER_ETCD_CONTAINER].(map[string]interface{})
	// etcdContainerInstId := etcdContainer[consts.HEADER_INST_ID].(string)
	// etcdNodeArr := etcdContainer[consts.HEADER_ETCD].([]map[string]interface{})

	// apiSixNodeContainer := servJson[consts.HEADER_APISIX_CONTAINER].(map[string]interface{})
	// apiSixNodeArr := apiSixNodeContainer[consts.HEADER_APISIX_SERVER].(map[string]interface{})
	// apiSixInstantId := apiSixNodeContainer[consts.HEADER_INST_ID].(map[string]interface{})

	// if YugaByteDeployer.CheckBeforeDeploy(serv, etcdNodeArr, apiSixNodeArr, logKey) {
	// 	return false
	// }

	return true
}

func CheckBeforeDeploy(serv *proto.PaasService, etcdNodeArr []map[string]interface{},
	apiSixNodeArr []map[string]interface{}, logKey string) bool {

	// 先判断是否是生产环境,生产环境的话etcd必须至少是三个节点的集群,开发、测试环境部署单节点或者集群的etcd
	if serv.IsProduct() {
		if len(etcdNodeArr) < consts.ETCD_PRODUCT_ENV_MIN_NODES {
			global.GLOBAL_RES.PubErrorLog(logKey, consts.ERR_ETCD_NODE_REQUIRED_CLUSTER)
			return false
		}
	} else {
		// etcd的节点不能小于1
		if len(etcdNodeArr) < 1 {
			global.GLOBAL_RES.PubErrorLog(logKey, consts.ERR_ETCD_NODE_LESS_THAN_ONE)
			return false
		}
	}

	// apisix的节点不能小于1
	if apiSixNodeArr == nil || len(apiSixNodeArr) == 0 {
		global.GLOBAL_RES.PubErrorLog(logKey, consts.ERR_APISIX_NODE_LESS_THAN_ONE)
		return false
	}

	return true
}

func (h *ServerlessApisixDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *ServerlessApisixDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}

func (h *ServerlessApisixDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return true
}
