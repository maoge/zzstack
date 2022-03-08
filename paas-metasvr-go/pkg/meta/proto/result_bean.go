package proto

import "github.com/maoge/paas-metasvr-go/pkg/consts"

type ResultBean struct {
	RET_CODE int
	RET_INFO string
}

func NewResultBean() *ResultBean {
	return &ResultBean{
		RET_CODE: consts.REVOKE_OK,
		RET_INFO: "",
	}
}
