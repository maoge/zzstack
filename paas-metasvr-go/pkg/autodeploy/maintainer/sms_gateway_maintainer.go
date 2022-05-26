package maintainer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	SmsDeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils/sms"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type SmsGatewayMaintainer struct {
}

func (h *SmsGatewayMaintainer) MaintainInstance(servInstID, instID, servType string, op *consts.OperationExt, isHandle bool,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	return SmsDeployUtils.MaintainInstance(servInstID, instID, servType, op, isHandle, logKey, magicKey, paasResult)
}

func (h *SmsGatewayMaintainer) UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile, rmDeployFile, isHandle bool,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	return SmsDeployUtils.UpdateInstanceForBatch(servInstID, instID, servType, loadDeployFile, rmDeployFile,
		isHandle, logKey, magicKey, paasResult)
}

func (h *SmsGatewayMaintainer) CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool {
	return SmsDeployUtils.CheckInstanceStatus(servInstID, instID, servType, magicKey, paasResult)
}
