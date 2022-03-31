package factory

import (
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/deployerIntf"
)

type DeployerFactory struct {
	DeployerMap map[string]deployerIntf.ServiceDeployer
}

func NewDeployerFactory() *DeployerFactory {
	res := new(DeployerFactory)
	res.DeployerMap = make(map[string]deployerIntf.ServiceDeployer)

	return res
}

func (h *DeployerFactory) Get(servType string) deployerIntf.ServiceDeployer {
	return h.DeployerMap[servType]
}
