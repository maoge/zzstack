package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
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

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}
	serv := meta.CMPT_META.GetService(servInstID)

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

	// mod is_deployed flag and local cache
	if !DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func (h *VoltDBDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

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
	if !DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

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
