package proto

import "github.com/maoge/paas-metasvr-go/pkg/utils"

type PaasTopology struct {
	INST_ID1  string `db:"INST_ID1"`
	INST_ID2  string `db:"INST_ID2"`
	TOPO_TYPE int    `db:"TOPO_TYPE"`
}

func NewPaasTopology(parentId string, instId string, topoType int) *PaasTopology {
	topo := new(PaasTopology)
	topo.INST_ID1 = parentId
	topo.INST_ID2 = instId
	topo.TOPO_TYPE = topoType

	return topo
}

func (m *PaasTopology) GetToe(instId string) string {
	if instId == m.INST_ID1 {
		return m.INST_ID2
	} else {
		return m.INST_ID1
	}
}

func ParsePaasTopology(msg string) *PaasTopology {
	topo := new(PaasTopology)
	utils.Json2Struct([]byte(msg), topo)
	return topo
}
