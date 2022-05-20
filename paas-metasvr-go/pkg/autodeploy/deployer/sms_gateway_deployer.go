package deployer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	SmsDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/sms"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type SmsGatewayDeployer struct {
}

func (h *SmsGatewayDeployer) DeployService(servInstID, deployFlag, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, true, paasResult)
	if !ok {
		return false
	}

	serverContainer := servJson[consts.HEADER_SMS_SERVER_CONTAINER].(map[string]interface{})
	serverExtContainer := servJson[consts.HEADER_SMS_SERVER_EXT_CONTAINER].(map[string]interface{})
	processContainer := servJson[consts.HEADER_SMS_PROCESS_CONTAINER].(map[string]interface{})
	clientContainer := servJson[consts.HEADER_SMS_CLIENT_CONTAINER].(map[string]interface{})
	batsaveContainer := servJson[consts.HEADER_SMS_BATSAVE_CONTAINER].(map[string]interface{})
	statsContainer := servJson[consts.HEADER_SMS_STATS_CONTAINER].(map[string]interface{})

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_SERVER, servInstID, serverContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_SERVER_EXT, servInstID, serverExtContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_PROCESS, servInstID, processContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_CLIENT, servInstID, clientContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_BATSAVE, servInstID, batsaveContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.DeploySmsInstanceArr(consts.HEADER_SMS_STATS, servInstID, statsContainer, logKey, magicKey, paasResult) {
		return false
	}

	// mod is_deployed flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult)
}

func (h *SmsGatewayDeployer) UndeployService(servInstID string, force bool, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, logKey, false, paasResult)
	if !ok {
		return false
	}

	serverContainer := servJson[consts.HEADER_SMS_SERVER_CONTAINER].(map[string]interface{})
	serverExtContainer := servJson[consts.HEADER_SMS_SERVER_EXT_CONTAINER].(map[string]interface{})
	processContainer := servJson[consts.HEADER_SMS_PROCESS_CONTAINER].(map[string]interface{})
	clientContainer := servJson[consts.HEADER_SMS_CLIENT_CONTAINER].(map[string]interface{})
	batsaveContainer := servJson[consts.HEADER_SMS_BATSAVE_CONTAINER].(map[string]interface{})
	statsContainer := servJson[consts.HEADER_SMS_STATS_CONTAINER].(map[string]interface{})

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_SERVER, serverContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_SERVER_EXT, serverExtContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_PROCESS, processContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_CLIENT, clientContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_BATSAVE, batsaveContainer, logKey, magicKey, paasResult) {
		return false
	}

	if !SmsDeployUtils.UndeploySmsInstanceArr(consts.HEADER_SMS_STATS, statsContainer, logKey, magicKey, paasResult) {
		return false
	}

	// update deploy flag and local cache
	return DeployUtils.PostProc(servInstID, consts.STR_FALSE, logKey, magicKey, paasResult)
}

func (h *SmsGatewayDeployer) DeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return SmsDeployUtils.DeploySmsGatewayInstance(servInstID, instID, logKey, magicKey, paasResult)
}

func (h *SmsGatewayDeployer) UndeployInstance(servInstID string, instID string, logKey string, magicKey string,
	paasResult *result.ResultBean) bool {

	return SmsDeployUtils.UndeploySmsGatewayInstance(instID, logKey, magicKey, paasResult)
}
