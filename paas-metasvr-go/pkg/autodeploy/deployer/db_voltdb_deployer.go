package deployer

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	VoltDBDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/voltdb"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type VoltDBDeployer struct {
}

func (h *VoltDBDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
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
	voltdbContainer := servJson[consts.HEADER_VOLTDB_CONTAINER].(map[string]interface{})
	voltdbServerArr := voltdbContainer[consts.HEADER_VOLTDB_SERVER].([]map[string]interface{})

	hosts := VoltDBDeployUtils.GetVoltDBHosts(voltdbServerArr, logKey, paasResult)
	userName := serv.USER
	userPasswd := serv.PASSWORD

	// 1. deploy voltdb server
	for _, voltdb := range voltdbServerArr {
		if !VoltDBDeployUtils.DeployVoltDBServer(voltdb, version, hosts, userName, userPasswd, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "voltdb-server deploy failed ......")
			return false
		}
	}

	// 2. 创建dual表用于连接池validationQuery
	// create table dual (id varchar(48) not null primary key);
	// insert into dual values('abc');
	if !VoltDBDeployUtils.CreateValidationTable(voltdbServerArr[0], version, logKey, magicKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "voltdb create validation table failed ......")
		return false
	}

	// update t_meta_service.is_deployed and local cache
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

func (h *VoltDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
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

	voltdbContainer := servJson[consts.HEADER_VOLTDB_CONTAINER].(map[string]interface{})
	voltdbServerArr := voltdbContainer[consts.HEADER_VOLTDB_SERVER].([]map[string]interface{})

	// 1. stop and remove
	for _, voltdb := range voltdbServerArr {
		if !VoltDBDeployUtils.UndeployVoltDBServer(voltdb, version, logKey, magicKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pulsar undeploy failed ......")
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

func (h *VoltDBDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	global.GLOBAL_RES.PubLog(logKey, "voltdb 社区版本不支持动态扩缩容")
	return true
}

func (h *VoltDBDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	global.GLOBAL_RES.PubLog(logKey, "voltdb 社区版本不支持动态扩缩容")
	return true
}
