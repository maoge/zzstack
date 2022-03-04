package proto

import "github.com/maoge/paas-metasvr-go/pkg/consts"

type PaasMetaAttr struct {
	ATTR_ID      int    `db:"ATTR_ID"`
	ATTR_NAME    string `db:"ATTR_NAME"`
	ATTR_NAME_CN string `db:"ATTR_NAME_CN"`
	AUTO_GEN     string `db:"AUTO_GEN"`
}

func (attr *PaasMetaAttr) IsAutoGen() bool {
	return attr.AUTO_GEN == consts.STR_TRUE
}
