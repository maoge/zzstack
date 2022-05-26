package deployerIntf

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type ServiceMaintainer interface {
	MaintainInstance(servInstID, instID, servType string, op *consts.OperationExt, isHandle bool,
		logKey, magicKey string, paasResult *result.ResultBean) bool

	UpdateInstanceForBatch(servInstID, instID, servType string, loadDeployFile, rmDeployFile, isHandle bool,
		logKey, magicKey string, paasResult *result.ResultBean) bool

	CheckInstanceStatus(servInstID, instID, servType, magicKey string, paasResult *result.ResultBean) bool
}
