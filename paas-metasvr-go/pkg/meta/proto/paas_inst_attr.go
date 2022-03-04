package proto

type PaasInstAttr struct {
	INST_ID    string `db:"INST_ID"`
	ATTR_ID    int    `db:"ATTR_ID"`
	ATTR_NAME  string `db:"ATTR_NAME"`
	ATTR_VALUE string `db:"ATTR_VALUE"`
}
