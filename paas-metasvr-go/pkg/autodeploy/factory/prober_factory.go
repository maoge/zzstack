package factory

import (
	"github.com/maoge/paas-metasvr-go/pkg/autocheck/checkerintf"
)

type ProberFactory struct {
	ProberMap map[string]checkerintf.CmptProber
}

func NewProberFactory() *ProberFactory {
	res := new(ProberFactory)
	res.ProberMap = make(map[string]checkerintf.CmptProber)

	return res
}

func (h *ProberFactory) GetProber(servType string) checkerintf.CmptProber {
	return h.ProberMap[servType]
}
