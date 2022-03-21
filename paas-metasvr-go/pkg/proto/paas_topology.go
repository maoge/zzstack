package proto

type PaasTopology struct {
	INST_ID1  string `db:"INST_ID1"`
	INST_ID2  string `db:"INST_ID2"`
	TOPO_TYPE int    `db:"TOPO_TYPE"`
}

func (m *PaasTopology) GetToe(instId string) string {
	if instId == m.INST_ID1 {
		return m.INST_ID2
	} else {
		return m.INST_ID1
	}
}
