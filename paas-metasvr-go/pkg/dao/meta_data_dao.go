package dao

import (
	crud "github.com/maoge/paas-metasvr-go/pkg/db"

	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
)

var (
	SQL_SEL_ACCOUNT        = "select ACC_ID,ACC_NAME,PHONE_NUM,MAIL,PASSWD,CREATE_TIME from t_account"
	SQL_SEL_META_ATTR      = "SELECT ATTR_ID,ATTR_NAME,ATTR_NAME_CN,AUTO_GEN FROM t_meta_attr ORDER BY ATTR_ID"
	SQL_SEL_META_CMPT      = "SELECT CMPT_ID,CMPT_NAME,CMPT_NAME_CN,IS_NEED_DEPLOY,SERV_TYPE,SERV_CLAZZ,NODE_JSON_TYPE,SUB_CMPT_ID FROM t_meta_cmpt ORDER BY CMPT_ID"
	SQL_SEL_META_INST      = "SELECT INST_ID,CMPT_ID,IS_DEPLOYED,POS_X,POS_Y,WIDTH,HEIGHT,ROW_,COL_ FROM t_meta_instance"
	SQL_SEL_META_CMPT_ATTR = "SELECT CMPT_ID,ATTR_ID FROM t_meta_cmpt_attr ORDER BY CMPT_ID,ATTR_ID"
	SQL_SEL_META_INST_ATTR = "SELECT INST_ID,ATTR_ID,ATTR_NAME,ATTR_VALUE FROM t_meta_instance_attr"
	SQL_SEL_META_SERVICE   = "SELECT INST_ID,SERV_NAME,SERV_CLAZZ,SERV_TYPE,VERSION,IS_DEPLOYED,IS_PRODUCT,CREATE_TIME,USER,PASSWORD,PSEUDO_DEPLOY_FLAG FROM t_meta_service"
)

func LoadAccount() ([]*proto.Account, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	accSlice := make([]*proto.Account, 0)
	err := crud.SelectAsSlice(dbPool, &accSlice, &SQL_SEL_ACCOUNT)
	if err != nil {
		return nil, err
	}

	return accSlice, nil
}

func LoadMetaAttr() ([]*proto.PaasMetaAttr, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	attrSlice := make([]*proto.PaasMetaAttr, 0)
	err := crud.SelectAsSlice(dbPool, &attrSlice, &SQL_SEL_META_ATTR)
	if err != nil {
		return nil, err
	}

	return attrSlice, nil
}

func LoadMetaCmpt() ([]*proto.PaasMetaCmpt, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	cmptSlice := make([]*proto.PaasMetaCmpt, 0)
	err := crud.SelectAsSlice(dbPool, &cmptSlice, &SQL_SEL_META_CMPT)
	if err != nil {
		return nil, err
	}

	for _, item := range cmptSlice {
		item.InitSubCmptSet()
	}

	return cmptSlice, nil
}

func LoadMetaInst() ([]*proto.PaasInstance, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	instSlice := make([]*proto.PaasInstance, 0)
	err := crud.SelectAsSlice(dbPool, &instSlice, &SQL_SEL_META_INST)
	if err != nil {
		return nil, err
	}

	return instSlice, nil
}

func LoadMetaCmptAttr() ([]*proto.PaasCmptAttr, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	cmptAttrSlice := make([]*proto.PaasCmptAttr, 0)
	err := crud.SelectAsSlice(dbPool, &cmptAttrSlice, &SQL_SEL_META_CMPT_ATTR)
	if err != nil {
		return nil, err
	}

	return cmptAttrSlice, nil
}

func LoadMetaInstAttr() ([]*proto.PaasInstAttr, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	instAttrSlice := make([]*proto.PaasInstAttr, 0)
	err := crud.SelectAsSlice(dbPool, &instAttrSlice, &SQL_SEL_META_INST_ATTR)
	if err != nil {
		return nil, err
	}

	return instAttrSlice, nil
}

func LoadMetaService() ([]*proto.PaasService, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	serviceSlice := make([]*proto.PaasService, 0)
	err := crud.SelectAsSlice(dbPool, &serviceSlice, &SQL_SEL_META_SERVICE)
	if err != nil {
		return nil, err
	}

	return serviceSlice, nil
}
