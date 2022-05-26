package proto

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PaasService struct {
	INST_ID            string `db:"INST_ID"`
	SERV_NAME          string `db:"SERV_NAME"`
	SERV_CLAZZ         string `db:"SERV_CLAZZ"`
	SERV_TYPE          string `db:"SERV_TYPE"`
	VERSION            string `db:"VERSION"`
	IS_DEPLOYED        string `db:"IS_DEPLOYED"`
	IS_PRODUCT         string `db:"IS_PRODUCT"`
	CREATE_TIME        int64  `db:"CREATE_TIME"`
	USER               string `db:"USER"`
	PASSWORD           string `db:"PASSWORD"`
	PSEUDO_DEPLOY_FLAG string `db:"PSEUDO_DEPLOY_FLAG"`
}

func (m *PaasService) IsDeployed() bool {
	return m.IS_DEPLOYED == consts.STR_TRUE
}

func (m *PaasService) IsProduct() bool {
	return m.IS_PRODUCT == consts.STR_TRUE
}

func ParsePaasService(s string) *PaasService {
	paasService := new(PaasService)
	utils.Json2Struct([]byte(s), paasService)

	return paasService
}
