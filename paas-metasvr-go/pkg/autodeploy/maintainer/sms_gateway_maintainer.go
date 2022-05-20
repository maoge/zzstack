package maintainer

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type SmsGatewayMaintainer struct {
}

func (h *SmsGatewayMaintainer) MaintainInstance(servInstID, instID, servType string, op consts.OperationEnum, isOperateByHandle bool,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *SmsGatewayMaintainer) UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile, rmDeployFile, isOperateByHandle bool,
	logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}

func (h *SmsGatewayMaintainer) CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool {
	return true
}
