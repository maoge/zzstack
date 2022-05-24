package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	SmsDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/sms"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type SmsQueryDeployer struct {
}

func (h *SmsQueryDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	ngxContainer := servJson[consts.HEADER_NGX_CONTAINER].(map[string]interface{})
	smsQueryContainer := servJson[consts.HEADER_SMS_QUERY_CONTAINER].(map[string]interface{})

	ngxContainerId := ngxContainer[consts.HEADER_INST_ID].(string)
	smsQueryContainerId := smsQueryContainer[consts.HEADER_INST_ID].(string)

	ngxArr := ngxContainer[consts.HEADER_NGX].([]map[string]interface{})
	smsQueryArr := smsQueryContainer[consts.HEADER_SMS_QUERY].([]map[string]interface{})

	servList := SmsDeployUtils.GetSmsQueryServList(smsQueryArr)

	if !SmsDeployUtils.DeploySmsQueryArr(servInstID, smsQueryContainerId, smsQueryArr, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeployNgxArr(servInstID, ngxContainerId, ngxArr, servList, logKey, magicKey, paasResult) {
		return false
	}

	// mod is_deployed flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult)
}

func (h *SmsQueryDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	ngxContainer := servJson[consts.HEADER_NGX_CONTAINER].(map[string]interface{})
	smsQueryContainer := servJson[consts.HEADER_SMS_QUERY_CONTAINER].(map[string]interface{})

	ngxContainerId := ngxContainer[consts.HEADER_INST_ID].(string)
	smsQueryContainerId := smsQueryContainer[consts.HEADER_INST_ID].(string)

	ngxArr := ngxContainer[consts.HEADER_NGX].([]map[string]interface{})
	smsQueryArr := smsQueryContainer[consts.HEADER_SMS_QUERY].([]map[string]interface{})

	if !SmsDeployUtils.UndeployNgxArr(servInstID, ngxContainerId, ngxArr, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsQueryArr(servInstID, smsQueryContainerId, smsQueryArr, logKey, magicKey, paasResult) {
		return false
	}

	// update deploy flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult)
}

func (h *SmsQueryDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	ngxContainer := servJson[consts.HEADER_NGX_CONTAINER].(map[string]interface{})
	smsQueryContainer := servJson[consts.HEADER_SMS_QUERY_CONTAINER].(map[string]interface{})

	ngxArr := ngxContainer[consts.HEADER_NGX].([]map[string]interface{})
	smsQueryArr := smsQueryContainer[consts.HEADER_SMS_QUERY].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	deployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_NGX:
		servList := SmsDeployUtils.GetSmsQueryServList(smsQueryArr)
		ngx := DeployUtils.GetSpecifiedItem(ngxArr, instID)
		deployResult = SmsDeployUtils.DeployNgxNode(ngx, servList, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_SMS_QUERY:
		smsQry := DeployUtils.GetSpecifiedItem(smsQueryArr, instID)
		deployResult = SmsDeployUtils.DeploySmsQueryNode(smsQry, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(deployResult, servInstID, logKey, "deploy")
	return deployResult
}

func (h *SmsQueryDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, version, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	ngxContainer := servJson[consts.HEADER_NGX_CONTAINER].(map[string]interface{})
	smsQueryContainer := servJson[consts.HEADER_SMS_QUERY_CONTAINER].(map[string]interface{})

	ngxArr := ngxContainer[consts.HEADER_NGX].([]map[string]interface{})
	smsQueryArr := smsQueryContainer[consts.HEADER_SMS_QUERY].([]map[string]interface{})

	inst := meta.CMPT_META.GetInstance(instID)
	instCmpt := meta.CMPT_META.GetCmptById(inst.CMPT_ID)
	undeployResult := false

	switch instCmpt.CMPT_NAME {
	case consts.HEADER_NGX:
		ngx := DeployUtils.GetSpecifiedItem(ngxArr, instID)
		undeployResult = SmsDeployUtils.UndeployNgxNode(ngx, version, logKey, magicKey, paasResult)
		break

	case consts.HEADER_SMS_QUERY:
		smsQry := DeployUtils.GetSpecifiedItem(smsQueryArr, instID)
		undeployResult = SmsDeployUtils.UndeploySmsQueryNode(smsQry, version, logKey, magicKey, paasResult)
		break

	default:
		break
	}

	DeployUtils.PostDeployLog(undeployResult, servInstID, logKey, "undeploy")
	return undeployResult
}
