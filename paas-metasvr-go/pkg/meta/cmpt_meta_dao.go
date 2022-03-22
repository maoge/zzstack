package meta

import (
	crud "github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
)

var (
	SQL_SEL_ACCOUNT        = "select ACC_ID,ACC_NAME,PHONE_NUM,MAIL,PASSWD,CREATE_TIME from t_account"
	SQL_SEL_META_ATTR      = "SELECT ATTR_ID,ATTR_NAME,ATTR_NAME_CN,AUTO_GEN FROM t_meta_attr ORDER BY ATTR_ID"
	SQL_SEL_META_CMPT      = "SELECT CMPT_ID,CMPT_NAME,CMPT_NAME_CN,IS_NEED_DEPLOY,SERV_TYPE,SERV_CLAZZ,NODE_JSON_TYPE,SUB_CMPT_ID FROM t_meta_cmpt ORDER BY CMPT_ID"
	SQL_SEL_META_INST      = "SELECT INST_ID,CMPT_ID,IS_DEPLOYED,POS_X,POS_Y,WIDTH,HEIGHT,ROW_,COL_ FROM t_meta_instance"
	SQL_SEL_META_CMPT_ATTR = "SELECT CMPT_ID,ATTR_ID FROM t_meta_cmpt_attr ORDER BY CMPT_ID,ATTR_ID"
	SQL_SEL_META_INST_ATTR = "SELECT INST_ID,ATTR_ID,ATTR_NAME,ATTR_VALUE FROM t_meta_instance_attr"
	SQL_SEL_META_SERVICE   = "SELECT INST_ID,SERV_NAME,SERV_CLAZZ,SERV_TYPE,VERSION,IS_DEPLOYED,IS_PRODUCT,CREATE_TIME,USER,PASSWORD,PSEUDO_DEPLOY_FLAG FROM t_meta_service"
	SQL_SEL_META_TOPO      = "SELECT INST_ID1,INST_ID2,TOPO_TYPE FROM t_meta_topology"
	SQL_SEL_META_DEP_HOST  = "SELECT HOST_ID,IP_ADDRESS,USER_NAME,USER_PWD,SSH_PORT,CREATE_TIME FROM t_meta_deploy_host"
	SQL_SEL_META_DEP_FILE  = "SELECT FILE_ID,HOST_ID,SERV_TYPE,VERSION,FILE_NAME,FILE_DIR,CREATE_TIME FROM t_meta_deploy_file"
	SQL_SEL_META_SERVER    = "SELECT SERVER_IP,SERVER_NAME FROM t_meta_server"
	SQL_SEL_META_SSH       = "SELECT SSH_ID,SSH_NAME,SSH_PWD,SSH_PORT,SERV_CLAZZ,SERVER_IP FROM t_meta_ssh"
	SQL_SEL_META_CMPT_VER  = "SELECT SERV_TYPE, VERSION from t_meta_cmpt_versions order by SERV_TYPE, VERSION"
	SQL_SEL_SERVICE_BY_ID  = "SELECT INST_ID,SERV_NAME,SERV_CLAZZ,SERV_TYPE,IS_DEPLOYED,IS_PRODUCT,CREATE_TIME,USER,PASSWORD,PSEUDO_DEPLOY_FLAG,VERSION FROM t_meta_service WHERE INST_ID = ?"
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

func LoadMetaTopo() ([]*proto.PaasTopology, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	topoSlice := make([]*proto.PaasTopology, 0)
	err := crud.SelectAsSlice(dbPool, &topoSlice, &SQL_SEL_META_TOPO)
	if err != nil {
		return nil, err
	}

	return topoSlice, nil
}

func LoadDeployHost() ([]*proto.PaasDeployHost, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	deployHostSlice := make([]*proto.PaasDeployHost, 0)
	err := crud.SelectAsSlice(dbPool, &deployHostSlice, &SQL_SEL_META_DEP_HOST)
	if err != nil {
		return nil, err
	}

	return deployHostSlice, nil
}

func LoadDeployFile() ([]*proto.PaasDeployFile, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	deployFileSlice := make([]*proto.PaasDeployFile, 0)
	err := crud.SelectAsSlice(dbPool, &deployFileSlice, &SQL_SEL_META_DEP_FILE)
	if err != nil {
		return nil, err
	}

	return deployFileSlice, nil
}

func LoadMetaServer() ([]*proto.PaasServer, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	serverSlice := make([]*proto.PaasServer, 0)
	err := crud.SelectAsSlice(dbPool, &serverSlice, &SQL_SEL_META_SERVER)
	if err != nil {
		return nil, err
	}

	return serverSlice, nil
}

func LoadMetaSsh() ([]*proto.PaasSsh, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	sshSlice := make([]*proto.PaasSsh, 0)
	err := crud.SelectAsSlice(dbPool, &sshSlice, &SQL_SEL_META_SSH)
	if err != nil {
		return nil, err
	}

	return sshSlice, nil
}

func LoadMetaCmptVersion() ([]*proto.PaasCmptVer, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	cmptVerSlice := make([]*proto.PaasCmptVer, 0)
	err := crud.SelectAsSlice(dbPool, &cmptVerSlice, &SQL_SEL_META_CMPT_VER)
	if err != nil {
		return nil, err
	}

	for _, item := range cmptVerSlice {
		item.InitVerList()
	}

	return cmptVerSlice, nil
}

func GetServiceById(instID string) (*proto.PaasService, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()
	service := new(proto.PaasService)
	err := crud.SelectAsObject(dbPool, service, &SQL_SEL_SERVICE_BY_ID, instID)
	return service, err
}
