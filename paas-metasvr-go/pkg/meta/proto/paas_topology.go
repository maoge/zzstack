package proto

type PaasTopology struct {
	INST_ID1  string `db:"INST_ID1"`
	INST_ID2  string `db:"INST_ID2"`
	TOPO_TYPE int    `db:"TOPO_TYPE"`
}
