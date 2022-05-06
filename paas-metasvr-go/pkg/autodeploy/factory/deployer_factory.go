package factory

import (
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployerIntf"
)

type DeployerFactory struct {
	DeployerMap   map[string]deployerIntf.ServiceDeployer
	MaintainerMap map[string]deployerIntf.ServiceMaintainer
}

func NewDeployerFactory() *DeployerFactory {
	res := new(DeployerFactory)
	res.DeployerMap = make(map[string]deployerIntf.ServiceDeployer)
	res.MaintainerMap = make(map[string]deployerIntf.ServiceMaintainer)

	return res
}

func (h *DeployerFactory) GetDeployer(servType string) deployerIntf.ServiceDeployer {
	return h.DeployerMap[servType]
}

func (h *DeployerFactory) GetMaintainer(servType string) deployerIntf.ServiceMaintainer {
	return h.MaintainerMap[servType]
}
