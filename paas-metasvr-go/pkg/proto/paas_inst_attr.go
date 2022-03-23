package proto

type PaasInstAttr struct {
	INST_ID    string `db:"INST_ID"`
	ATTR_ID    int    `db:"ATTR_ID"`
	ATTR_NAME  string `db:"ATTR_NAME"`
	ATTR_VALUE string `db:"ATTR_VALUE"`
}

func NewPaasInstAttr(instId string, attrId int, attrName string, attrVal string) *PaasInstAttr {
	attr := new(PaasInstAttr)
	attr.INST_ID = instId
	attr.ATTR_ID = attrId
	attr.ATTR_NAME = attrName
	attr.ATTR_VALUE = attrVal

	return attr
}
