package metadao

import (
	"errors"
	"fmt"
	"net"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	crud "github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
	"github.com/tidwall/gjson"

	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
)

var (
	SQL_COUNT_SERVICE_LIST             = "SELECT count(1) COUNT FROM t_meta_service WHERE 1=1 %s"
	SQL_SEL_SERVICE_LIST               = "SELECT INST_ID, SERV_NAME, SERV_TYPE, SERV_CLAZZ, VERSION, IS_DEPLOYED, IS_PRODUCT FROM t_meta_service WHERE 1=1 %s ORDER BY CREATE_TIME limit ?, ?"
	SQL_COUNT_SERV_TYPE_VER_LIST       = "SELECT count(1) COUNT FROM t_meta_cmpt_versions WHERE 1=1 %s"
	SQL_SEL_SERV_TYPE_VER_LIST         = "SELECT SERV_TYPE, VERSION FROM t_meta_cmpt_versions WHERE 1=1 %s ORDER BY SERV_TYPE, VERSION limit ?, ?"
	SQL_ADD_SERVICE                    = "INSERT INTO t_meta_service(INST_ID, SERV_NAME, SERV_CLAZZ, SERV_TYPE, VERSION, IS_DEPLOYED, IS_PRODUCT, CREATE_TIME, USER, PASSWORD, PSEUDO_DEPLOY_FLAG) VALUES(?,?,?,?,?,?,?,?,?,?,?)"
	SQL_DEL_TOPOLOGY                   = "DELETE FROM t_meta_topology WHERE (INST_ID1 = ? AND INST_ID2 = ?) OR (INST_ID2 = ? and TOPO_TYPE = 2)"
	SQL_DEL_INSTANCE_ATTR              = "DELETE FROM t_meta_instance_attr WHERE INST_ID = ?"
	SQL_DEL_INSTANCE                   = "DELETE FROM t_meta_instance WHERE INST_ID = ?"
	SQL_DEL_SERVICE                    = "DELETE FROM t_meta_service WHERE inst_id = ?"
	SQL_MOD_SERVICE                    = "UPDATE t_meta_service SET SERV_NAME = ?, VERSION = ?, IS_PRODUCT = ? WHERE INST_ID = ?"
	SQL_MOD_SERVICE_VERSION            = "UPDATE t_meta_service SET VERSION = ? WHERE INST_ID = ?"
	SQL_COUNT_SERVER_LIST              = "SELECT count(1) COUNT FROM t_meta_server WHERE 1=1 %s"
	SQL_SEL_SERVER_LIST                = "SELECT SERVER_IP, SERVER_NAME FROM t_meta_server WHERE 1=1 %s order by SERVER_IP limit ?, ?"
	SQL_ADD_SERVER                     = "INSERT INTO t_meta_server(SERVER_IP, SERVER_NAME, CREATE_TIME) VALUES(?,?,?)"
	SQL_DEL_SERVER                     = "DELETE FROM t_meta_server WHERE SERVER_IP=?"
	SQL_SSH_CNT_BY_IP                  = "SELECT count(1) COUNT FROM t_meta_ssh WHERE SERVER_IP=? "
	SQL_SSH_LIST_BY_IP                 = "SELECT SSH_NAME, SERV_CLAZZ, SSH_ID, SERVER_IP, SSH_PORT FROM t_meta_ssh WHERE SERVER_IP=? order by SSH_NAME limit ?, ?"
	SQL_ADD_SSH                        = "INSERT INTO t_meta_ssh(SSH_NAME,SSH_PWD,SSH_PORT,SERV_CLAZZ,SERVER_IP,SSH_ID) VALUES(?,?,?,?,?,?)"
	SQL_MOD_SSH                        = "UPDATE t_meta_ssh SET SSH_NAME=?, SSH_PWD=?, SSH_PORT=? WHERE SSH_ID=?"
	SQL_DEL_SSH                        = "DELETE FROM t_meta_ssh WHERE SSH_ID=?"
	SQL_UPDATE_POS                     = "UPDATE t_meta_instance SET POS_X=?,POS_Y=?, WIDTH=?, HEIGHT=?,ROW_=?,COL_=? WHERE INST_ID = ?"
	SQL_INS_INSTANCE                   = "INSERT INTO t_meta_instance(INST_ID,CMPT_ID,IS_DEPLOYED,POS_X,POS_Y,WIDTH,HEIGHT,ROW_,COL_) VALUES(?,?,?,?,?,?,?,?,?)"
	SQL_INS_INSTANCE_ATTR              = "INSERT INTO t_meta_instance_attr(INST_ID,ATTR_ID,ATTR_NAME,ATTR_VALUE) VALUES(?,?,?,?)"
	SQL_INS_TOPOLOGY                   = "INSERT INTO t_meta_topology(INST_ID1,INST_ID2,TOPO_TYPE) VALUES(?,?,?)"
	SQL_UPD_TOPOLOGY                   = "update t_meta_topology set INST_ID2 = ? where INST_ID1 = ?"
	SQL_DEL_ALL_SUB_TOPOLOGY           = "DELETE FROM t_meta_topology WHERE (INST_ID1 = ? AND TOPO_TYPE = 1) OR (INST_ID1 = ? and TOPO_TYPE = 2)"
	SQL_MOD_ATTR                       = "UPDATE t_meta_instance_attr SET ATTR_VALUE = ? WHERE INST_ID = ? AND ATTR_ID = ?"
	SQL_INS_CMPT_VER                   = "INSERT INTO t_meta_cmpt_versions(SERV_TYPE,VERSION) VALUES(?,?)"
	SQL_DEL_CMPT_VER                   = "DELETE FROM t_meta_cmpt_versions WHERE SERV_TYPE=? AND VERSION=?"
	SQL_UPD_INST_DEPLOY                = "UPDATE t_meta_instance SET IS_DEPLOYED = ? WHERE INST_ID = ?"
	SQL_UPD_SERV_DEPLOY                = "UPDATE t_meta_service SET IS_DEPLOYED = ? WHERE INST_ID = ?"
	SQL_MOD_SERVICE_PSEUDO_DEPLOY_FLAG = "UPDATE t_meta_service SET PSEUDO_DEPLOY_FLAG = ? WHERE INST_ID = ?"
	SQL_UPD_INSTANCE_PRE_EMBADDED      = "UPDATE t_meta_instance_attr SET ATTR_VALUE= ?  WHERE INST_ID = ? AND ATTR_ID = 320" // 320 -> 'PRE_EMBEDDED'

	// SQL_NEXT_SEQ_LOCK                  = "select current_value as CURR_VALUE from t_meta_sequence where seq_name = ? for update"
	SQL_NEXT_SEQ_LOCK   = "select current_value as CURR_VALUE from t_meta_sequence where seq_name = ? "
	SQL_NEXT_SEQ_UPDATE = "update t_meta_sequence set current_value = current_value + %d where seq_name = ?"
	SQL_INS_ALARM       = "INSERT INTO t_meta_alarm(ALARM_ID,SERV_INST_ID,SERV_TYPE,INST_ID,CMPT_NAME,ALARM_TYPE,ALARM_TIME) VALUES(?,?,?,?,?,?,?)"

	SQL_UPD_ALARM_STATE_BY_ALARMID = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where ALARM_ID = ?"
	SQL_UPD_ALARM_STATE_BY_INSTID  = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where SERV_INST_ID = ? and INST_ID = ? and ALARM_TYPE = ?"
)

func GetServiceCount(getServiceCountParam *proto.GetServiceCountParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	servName := getServiceCountParam.SERV_NAME
	servClazz := getServiceCountParam.SERV_CLAZZ
	servType := getServiceCountParam.SERV_TYPE

	if servName != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_NAME='%s' ", servName)
	}
	if servClazz != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_CLAZZ='%s' ", servClazz)
	}
	if servType != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_TYPE='%s' ", servType)
	}

	sql := fmt.Sprintf(SQL_COUNT_SERVICE_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &sql)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("GetServiceCount fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetServiceList(getServiceListParam *proto.GetServiceListParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	servInstId := getServiceListParam.SERV_INST_ID
	servName := getServiceListParam.SERV_NAME
	servClazz := getServiceListParam.SERV_CLAZZ
	servType := getServiceListParam.SERV_TYPE
	pageSize := getServiceListParam.PAGE_SIZE
	pageNum := getServiceListParam.PAGE_NUMBER
	start := pageSize * (pageNum - 1)

	if servInstId != "" {
		sqlWhere += fmt.Sprintf(" AND INST_ID = '%s' ", servInstId)
	}
	if servName != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_NAME = '%s' ", servName)
	}
	if servClazz != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_CLAZZ = '%s' ", servClazz)
	}
	if servType != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_TYPE = '%s' ", servType)
	}

	sql := fmt.Sprintf(SQL_SEL_SERVICE_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	data, err := crud.SelectAsMapSlice(dbPool, &sql, start, pageSize)

	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("GetServiceList fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetServTypeVerCount(getServTypeVerCountParam *proto.GetServTypeVerCountParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	servType := getServTypeVerCountParam.SERV_TYPE

	if servType != "" {
		sqlWhere += fmt.Sprintf("  AND SERV_TYPE = '%s' ", servType)
	}

	sql := fmt.Sprintf(SQL_COUNT_SERV_TYPE_VER_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &sql)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("GetServTypeVerCount fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetServTypeVerListByPage(param *proto.GetServTypeVerListByPageParam, resultBean *result.ResultBean) {
	sqlWhere := ""

	servType := param.SERV_TYPE
	pageSize := param.PAGE_SIZE
	pageNum := param.PAGE_NUMBER
	start := pageSize * (pageNum - 1)

	if servType != "" {
		sqlWhere += fmt.Sprintf(" AND SERV_TYPE = '%s' ", servType)
	}

	sql := fmt.Sprintf(SQL_SEL_SERV_TYPE_VER_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	data, err := crud.SelectAsMapSlice(dbPool, &sql, start, pageSize)

	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("GetServTypeVerListByPage fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetClickHouseDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	ret := false
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		instance := meta.CMPT_META.GetInstance(toeId)
		if instance == nil {
			continue
		}

		cmptId := instance.CMPT_ID
		// 102 -> 'GRAFANA'
		if cmptId == 102 {
			httpPort := meta.CMPT_META.GetInstAttr(toeId, 122).ATTR_VALUE // 122 -> 'HTTP_PORT'
			sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE    // 116 -> 'SSH_ID'
			ssh := meta.CMPT_META.GetSshById(sshId)
			serverIp := ssh.SERVER_IP

			url := fmt.Sprintf("http://%s:%s", serverIp, httpPort)

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = url

			ret = true
			break
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func GetVoltDBDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	ret := false
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		voltdbContainer := meta.CMPT_META.GetInstance(toeId)
		if voltdbContainer == nil {
			continue
		}

		cmptId := voltdbContainer.CMPT_ID
		// 291 -> 'VOLTDB_CONTAINER'
		if cmptId == 291 {
			subRelations := make([]*proto.PaasTopology, 0)
			meta.CMPT_META.GetInstRelations(voltdbContainer.INST_ID, &subRelations)
			if len(subRelations) == 0 {
				continue
			}

			for _, item := range subRelations {
				voltdbInstId := item.GetToe(voltdbContainer.INST_ID)
				voltdb := meta.CMPT_META.GetInstance(voltdbInstId)
				if voltdbInstId != "" && voltdb.IsDeployed() {
					httpPort := meta.CMPT_META.GetInstAttr(toeId, 256).ATTR_VALUE // 'VOLT_WEB_PORT'
					sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE    // 116 -> 'SSH_ID'
					ssh := meta.CMPT_META.GetSshById(sshId)
					serverIp := ssh.SERVER_IP

					url := fmt.Sprintf("http://%s:%s", serverIp, httpPort)

					resultBean.RET_CODE = consts.REVOKE_OK
					resultBean.RET_INFO = url

					ret = true
					break
				}
			}
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func GetRocketMQDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	ret := false
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		instance := meta.CMPT_META.GetInstance(toeId)
		if instance == nil {
			continue
		}

		cmptId := instance.CMPT_ID
		// 246 -> 'ROCKETMQ_CONSOLE'
		if cmptId == 246 {
			consolePort := meta.CMPT_META.GetInstAttr(toeId, 249).ATTR_VALUE // 249 -> 'CONSOLE_PORT'
			sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE       // 116 -> 'SSH_ID'
			ssh := meta.CMPT_META.GetSshById(sshId)
			serverIp := ssh.SERVER_IP

			url := fmt.Sprintf("http://%s:%s", serverIp, consolePort)

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = url

			ret = true
			break
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func GetTiDBDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	ret := false
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		instance := meta.CMPT_META.GetInstance(toeId)
		if instance == nil {
			continue
		}

		cmptId := instance.CMPT_ID
		// 217 -> 'DASHBOARD_PROXY'
		if cmptId == 217 {
			dashboardPort := meta.CMPT_META.GetInstAttr(toeId, 230).ATTR_VALUE // 230 -> 'DASHBOARD_PORT'
			sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE         // 116 -> 'SSH_ID'
			ssh := meta.CMPT_META.GetSshById(sshId)
			serverIp := ssh.SERVER_IP

			url := fmt.Sprintf("http://%s:%s/dashboard", serverIp, dashboardPort)

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = url

			ret = true
			break
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func GetPulsarDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	ret := false
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		instance := meta.CMPT_META.GetInstance(toeId)
		if instance == nil {
			continue
		}

		cmptId := instance.CMPT_ID
		// 263 -> 'PULSAR_MANAGER'
		if cmptId == 263 {
			httpPort := meta.CMPT_META.GetInstAttr(toeId, 270).ATTR_VALUE // 270 -> 'PULSAR_MGR_PORT'
			sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE    // 116 -> 'SSH_ID'
			ssh := meta.CMPT_META.GetSshById(sshId)
			serverIp := ssh.SERVER_IP

			url := fmt.Sprintf("http://%s:%s/ui/index.html", serverIp, httpPort)

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = url

			ret = true
			break
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func GetYBDashboardAddr(param *proto.ServInstParam, resultBean *result.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &relations)

	if len(relations) == 0 {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
		return
	}

	var ybMasterContainerID string
	for _, relation := range relations {
		toeId := relation.GetToe(servInstId)
		instance := meta.CMPT_META.GetInstance(toeId)
		if instance == nil {
			continue
		}

		cmptId := instance.CMPT_ID
		// 301 -> 'YB_MASTER_CONTAINER'
		if cmptId == 301 {
			ybMasterContainerID = toeId
			break
		}
	}

	ret := false
	if ybMasterContainerID != "" {
		subRelations := make([]*proto.PaasTopology, 0)
		meta.CMPT_META.GetInstRelations(ybMasterContainerID, &subRelations)

		for _, relation := range subRelations {
			toeId := relation.GetToe(servInstId)
			instance := meta.CMPT_META.GetInstance(toeId)
			if instance == nil {
				continue
			}

			webServPort := meta.CMPT_META.GetInstAttr(toeId, 275).ATTR_VALUE // 270 -> 'WEBSERVER_PORT'
			sshId := meta.CMPT_META.GetInstAttr(toeId, 116).ATTR_VALUE       // 116 -> 'SSH_ID'
			ssh := meta.CMPT_META.GetSshById(sshId)
			serverIp := ssh.SERVER_IP

			url := fmt.Sprintf("http://%s:%s", serverIp, webServPort)

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = url

			ret = true
			break
		}
	}

	if !ret {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = ""
	}
}

func AddService(param *proto.AddServiceParam, instId string, magicKey string, resultBean *result.ResultBean) {
	servName := param.SERV_NAME
	servClazz := param.SERV_CLAZZ
	servType := param.SERV_TYPE
	version := param.VERSION
	isProduct := param.IS_PRODUCT
	user := param.USER
	password := param.PASSWORD
	ts := utils.CurrentTimeMilli()

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.TxInsert(dbPool, &SQL_ADD_SERVICE, instId, servName, servClazz, servType, version, consts.STR_FALSE, isProduct,
		ts, user, password, consts.DEPLOY_FLAG_PHYSICAL)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
		return
	}

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = ""

	// add to local cache
	service := &proto.PaasService{
		INST_ID:            instId,
		SERV_NAME:          servName,
		SERV_CLAZZ:         servClazz,
		SERV_TYPE:          servType,
		VERSION:            version,
		IS_DEPLOYED:        consts.STR_FALSE,
		IS_PRODUCT:         isProduct,
		CREATE_TIME:        ts,
		USER:               user,
		PASSWORD:           password,
		PSEUDO_DEPLOY_FLAG: consts.DEPLOY_FLAG_PHYSICAL,
	}
	meta.CMPT_META.AddService(service)

	msgBody := utils.Struct2Json(service)
	event := proto.NewPaasEvent(consts.EVENT_ADD_SERVICE.CODE, msgBody, magicKey)

	eventbus.EVENTBUS.PublishEvent(event)
}

func DelService(instId string, magicKey string, resultBean *result.ResultBean) {
	serv := meta.CMPT_META.GetService(instId)
	if serv == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = consts.ERR_METADATA_NOT_FOUND
		return
	} else {
		if serv.IsDeployed() {
			errInfo := fmt.Sprintf("service: %v, service_name: %v, is_deployed: %v, is_product: %v",
				instId, serv.SERV_NAME, serv.IsDeployed(), serv.IsProduct())
			utils.LOGGER.Error(errInfo)

			resultBean.RET_CODE = consts.REVOKE_NOK
			resultBean.RET_INFO = consts.ERR_NO_DEL_DEPLOYED_SERV
			return
		}
	}

	// recursive get all subpath item meta data
	childArr := make([]*proto.PaasNode, 0)
	getChildNodeExcludingServRoot(instId, childArr)

	events := make([]*proto.PaasEvent, 0)
	if enumDelService("", instId, childArr, resultBean, &events, magicKey) {
		for _, ev := range events {
			eventbus.EVENTBUS.PublishEvent(ev)
			result := gjson.Parse(ev.MSG_BODY)
			id := result.Get(consts.HEADER_INST_ID).Str
			switch ev.EVENT_CODE {
			case consts.EVENT_DEL_SERVICE.CODE:
				meta.CMPT_META.DelService(instId)
			case consts.EVENT_DEL_INSTANCE.CODE:
				meta.CMPT_META.DelInstance(id)
			case consts.EVENT_DEL_INST_ATTR.CODE:
				meta.CMPT_META.DelInstAttr(id)
			case consts.EVENT_DEL_TOPO.CODE:
				supId := result.Get(consts.HEADER_PARENT_ID).Str
				meta.CMPT_META.DelTopo(supId, id)
			default:
				errInfo := fmt.Sprintf("untracked event: %+v", ev)
				utils.LOGGER.Error(errInfo)
			}
		}

		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
	} else {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
	}
}

func enumDelService(parentId string, instId string, subNodes []*proto.PaasNode, resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) bool {
	if len(subNodes) > 0 {
		for _, node := range subNodes {
			subInstId := node.INST_ID
			subSubNodes := node.NODES
			if subInstId == "" {
				continue
			}

			if meta.CMPT_META.IsInstServRootCmpt(subInstId) {
				continue
			}

			if subSubNodes != nil && len(subSubNodes) > 0 {
				if !enumDelService(instId, subInstId, subSubNodes, resultBean, events, magicKey) {
					return false
				}
			}
		}
	}

	if !DelInstance(parentId, instId, resultBean, events, magicKey) {
		return false
	}

	if parentId == "" {
		dbPool := global.GLOBAL_RES.GetDbPool()
		crud.Delete(dbPool, &SQL_DEL_SERVICE, instId)

		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBody := utils.Struct2Json(msgBodyMap)

		ev := proto.NewPaasEvent(consts.EVENT_DEL_SERVICE.CODE, msgBody, magicKey)
		*events = append(*events, ev)
	}

	return true
}

func DelInstance(parentId string, instId string, resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) bool {
	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, &relations)

	for _, topo := range relations {
		subInstId := topo.GetToe(instId)

		if subInstId == "" {
			continue
		}

		if meta.CMPT_META.IsInstServRootCmpt(subInstId) {
			continue
		}

		if !DelInstance(instId, subInstId, resultBean, events, magicKey) {
			errMsg := fmt.Sprintf("delInstance fail, parent_id:%s, inst_id:%s", instId, subInstId)
			utils.LOGGER.Error(errMsg)
			return false
		}
	}

	dbPool := global.GLOBAL_RES.GetDbPool()

	// 1.1 remove relations
	crud.Delete(dbPool, &SQL_DEL_TOPOLOGY, parentId, instId, instId)

	// 1.2 broadcast event to cluster
	delTopoMap := make(map[string]interface{})
	delTopoMap[consts.HEADER_INST_ID] = instId
	delTopoMap[consts.HEADER_PARENT_ID] = parentId
	delTopoBody := utils.Struct2Json(delTopoMap)

	evDelTopo := proto.NewPaasEvent(consts.EVENT_DEL_TOPO.CODE, delTopoBody, magicKey)
	*events = append(*events, evDelTopo)

	// 2.1 delete instance attribute
	crud.Delete(dbPool, &SQL_DEL_INSTANCE_ATTR, instId)

	// 2.2 broadcast event to cluster
	delInstAttrMap := make(map[string]interface{})
	delInstAttrMap[consts.HEADER_INST_ID] = instId
	delInstAttrBody := utils.Struct2Json(delInstAttrMap)

	evDelInstAttr := proto.NewPaasEvent(consts.EVENT_DEL_INST_ATTR.CODE, delInstAttrBody, magicKey)
	*events = append(*events, evDelInstAttr)

	// 3.1 delete instance
	crud.Delete(dbPool, &SQL_DEL_INSTANCE, instId)

	// 3.2 broadcast event to cluster
	delInstMap := make(map[string]interface{})
	delInstMap[consts.HEADER_INST_ID] = instId
	delInstBody := utils.Struct2Json(delInstMap)

	evDelInst := proto.NewPaasEvent(consts.EVENT_DEL_INSTANCE.CODE, delInstBody, magicKey)
	*events = append(*events, evDelInst)

	return true
}

func getChildNodeExcludingServRoot(instId string, subNodes []*proto.PaasNode) {
	topos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, &topos)
	if len(topos) == 0 {
		return
	}

	for _, topo := range topos {
		toeId := topo.GetToe(instId)
		if toeId == "" {
			continue
		}

		toeInst := meta.CMPT_META.GetInstance(toeId)
		if toeInst == nil {
			continue
		}

		toeCmpt := meta.CMPT_META.GetCmptById(toeInst.CMPT_ID)
		if meta.CMPT_META.IsServRootCmpt(toeCmpt.SERV_TYPE, toeCmpt.CMPT_NAME) {
			continue
		}

		childNodes := make([]*proto.PaasNode, 0)
		getChildNodeExcludingServRoot(toeId, childNodes)

		node := new(proto.PaasNode)
		node.INST_ID = toeId

		instance := meta.CMPT_META.GetInstance(toeId)
		if instance != nil {
			cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
			text := fmt.Sprintf("%v  (%v)", cmpt.CMPT_NAME_CN, instance.INST_ID)
			node.TEXT = text
		} else {
			node.TEXT = toeId
		}

		if len(childNodes) > 0 {
			node.NODES = childNodes
		}

		subNodes = append(subNodes, node)
	}
}

func ModService(param *proto.ModServiceParam, magicKey string, resultBean *result.ResultBean) {
	instId := param.INST_ID
	servName := param.SERV_NAME
	version := param.VERSION
	isProduct := param.IS_PRODUCT

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_MOD_SERVICE, servName, version, isProduct, instId)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
	} else {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""

		meta.CMPT_META.ReloadService(instId)

		msgBodyMap := make(map[string]string, 0)
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBody := utils.Struct2Json(msgBodyMap)

		event := proto.NewPaasEvent(consts.EVENT_MOD_SERVICE.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)
	}
}

func ModServiceVersion(param *proto.ModServiceVersionParam, magicKey string, resultBean *result.ResultBean) {
	instId := param.INST_ID
	version := param.VERSION
	dbPool := global.GLOBAL_RES.GetDbPool()

	_, err := crud.Update(dbPool, &SQL_MOD_SERVICE_VERSION, version, instId)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
	} else {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""

		meta.CMPT_META.ReloadService(instId)

		msgBodyMap := make(map[string]string, 0)
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBody := utils.Struct2Json(msgBodyMap)

		event := proto.NewPaasEvent(consts.EVENT_MOD_SERVICE.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)
	}
}

func GetServerCnt(param *proto.GetServerCountParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	servIp := param.SERVER_IP
	servName := param.SERVER_NAME

	if servIp != "" {
		sqlWhere += fmt.Sprintf(" AND SERVER_IP = '%s' ", servIp)
	}
	if servName != "" {
		sqlWhere += fmt.Sprintf(" AND SERVER_NAME = '%s' ", servName)
	}

	sql := fmt.Sprintf(SQL_COUNT_SERVER_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &sql)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("GetServerCnt fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetServerList(param *proto.GetServerListParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	servIp := param.SERVER_IP
	servName := param.SERVER_NAME
	pageSize := param.PAGE_SIZE
	pageNum := param.PAGE_NUMBER
	start := pageSize * (pageNum - 1)

	if servIp != "" {
		sqlWhere += fmt.Sprintf(" AND SERVER_IP = '%s' ", servIp)
	}
	if servName != "" {
		sqlWhere += fmt.Sprintf(" AND SERVER_NAME = '%s' ", servName)
	}

	sql := fmt.Sprintf(SQL_SEL_SERVER_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	data, err := crud.SelectAsMapSlice(dbPool, &sql, start, pageSize)

	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("GetServerList fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func AddServer(param *proto.AddServerParam, magicKey string, resultBean *result.ResultBean) {
	servIp := param.SERVER_IP
	servName := param.SERVER_NAME
	if meta.CMPT_META.IsServerIpExists(servIp) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SERVER_IP_EXISTS
		return
	}

	ts := utils.CurrentTimeMilli()
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_ADD_SERVER, servIp, servName, ts)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
		utils.LOGGER.Error(err.Error())
	} else {
		server := new(proto.PaasServer)
		server.SERVER_IP = servIp
		server.SERVER_NAME = servName

		meta.CMPT_META.AddServer(server)

		msgBody := utils.Struct2Json(server)
		event := proto.NewPaasEvent(consts.EVENT_ADD_SERVER.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)
	}
}

func DelServer(servIp string, magicKey string, resultBean *result.ResultBean) {
	if meta.CMPT_META.IsServerNull(servIp) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SERVER_NOT_NULL
		return
	}

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Delete(dbPool, &SQL_DEL_SERVER, servIp)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
		utils.LOGGER.Error(err.Error())
	} else {
		msgBodyMap := make(map[string]string)
		msgBodyMap[consts.HEADER_SERVER_IP] = servIp

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_DEL_SERVER.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)
	}
}

func GetSshCntByIp(servIp string, resultBean *result.ResultBean) {
	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &SQL_SSH_CNT_BY_IP, servIp)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("GetSshCntByIp fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetSshListByIp(param *proto.GetSSHListByIPParam, resultBean *result.ResultBean) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	servIp := param.SERVER_IP
	pageSize := param.PAGE_SIZE
	pageNum := param.PAGE_NUMBER
	start := pageSize * (pageNum - 1)

	data, err := crud.SelectAsMapSlice(dbPool, &SQL_SSH_LIST_BY_IP, servIp, start, pageSize)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("GetSshListByIp fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func AddSsh(param *proto.AddSSHParam, magicKey string, resultBean *result.ResultBean) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	sshName := param.SSH_NAME
	sshPwd := param.SSH_PWD
	sshPort := param.SSH_PORT
	servClazz := param.SERV_CLAZZ
	servIp := param.SERVER_IP

	servClazzList := strings.Split(servClazz, ",")
	events := make([]*proto.PaasEvent, 0)

	allOk := true

	for _, servClazzItem := range servClazzList {
		if meta.CMPT_META.IsSshExists(sshName, servIp, servClazzItem) {
			errMsg := fmt.Sprintf("%s ssh exists: %s, %s", servIp, sshName, servClazzItem)
			utils.LOGGER.Error(errMsg)
			continue
		}

		id := utils.GenUUID()
		_, err := crud.Insert(dbPool, &SQL_ADD_SSH, sshName, sshPwd, sshPort, servClazzItem, servIp, id)
		if err == nil {
			ssh := proto.NewPaasSsh(id, sshName, sshPwd, sshPort, servClazzItem, servIp)
			meta.CMPT_META.AddSsh(ssh)

			// broadcast event to cluster
			msgBody := utils.Struct2Json(ssh)
			ev := proto.NewPaasEvent(consts.EVENT_ADD_SSH.CODE, msgBody, magicKey)
			events = append(events, ev)
		} else {
			allOk = false
			resultBean.RET_CODE = consts.REVOKE_NOK
			resultBean.RET_INFO = consts.ERR_DB
			break
		}
	}

	for _, event := range events {
		eventbus.EVENTBUS.PublishEvent(event)
	}

	if allOk {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
	}
}

func ModSSH(param *proto.ModSSHParam, magicKey string, resultBean *result.ResultBean) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	sshName := param.SSH_NAME
	sshPwd := param.SSH_PWD
	sshPort := param.SSH_PORT
	sshId := param.SSH_ID
	servIp := param.SERVER_IP

	_, err := crud.Update(dbPool, &SQL_MOD_SSH, sshName, sshPwd, sshPort, sshId)
	if err == nil {
		meta.CMPT_META.ModSsh(servIp, sshId, sshName, sshPwd, sshPort)

		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_SERVER_IP] = servIp
		msgBodyMap[consts.HEADER_SSH_ID] = sshId
		msgBodyMap[consts.HEADER_SSH_NAME] = sshName
		msgBodyMap[consts.HEADER_SSH_PWD] = sshPwd
		msgBodyMap[consts.HEADER_SSH_PORT] = sshPort
		msgBody := utils.Struct2Json(msgBodyMap)

		ev := proto.NewPaasEvent(consts.EVENT_MOD_SSH.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(ev)
	} else {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
	}
}

func DelSSH(param *proto.DelSSHParam, magicKey string, resultBean *result.ResultBean) {
	dbPool := global.GLOBAL_RES.GetDbPool()
	sshId := param.SSH_ID
	servIp := param.SERVER_IP

	if meta.CMPT_META.IsSshUsing(sshId) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SSH_IS_USING
		return
	}

	_, err := crud.Delete(dbPool, &SQL_DEL_SSH, sshId)
	if err == nil {
		meta.CMPT_META.DelSsh(servIp, sshId)

		msgBody := utils.Struct2Json(param)
		ev := proto.NewPaasEvent(consts.EVENT_DEL_SSH.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(ev)
	} else {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
	}
}

func LoadServiceTopo(instId string, resultBean *result.ResultBean) bool {
	instance := meta.CMPT_META.GetInstance(instId)
	if instance == nil {
		resultBean.RET_CODE = consts.SERVICE_NOT_INIT
		resultBean.RET_INFO = ""
		return false
	}

	cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
	if cmpt == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_METADATA_NOT_FOUND
		return false
	}

	attrMap := make(map[string]interface{})
	deployFlagArr := make([]map[string]string, 0)

	if !loadInstanceAttribute(instId, &attrMap, &deployFlagArr) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_METADATA_NOT_FOUND
		return false
	}

	topoMap := make(map[string]interface{})
	topoMap[consts.HEADER_SERV_CLAZZ] = cmpt.SERV_CLAZZ
	topoMap[consts.HEADER_SERV_TYPE] = cmpt.SERV_TYPE
	topoMap[cmpt.CMPT_NAME] = attrMap
	topoMap[consts.HEADER_DEPLOY_FLAG] = deployFlagArr

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = topoMap

	return true
}

func LoadInstanceMeta(instId string, resultBean *result.ResultBean) bool {
	instance := meta.CMPT_META.GetInstance(instId)
	if instance == nil {
		resultBean.RET_CODE = consts.SERVICE_NOT_INIT
		resultBean.RET_INFO = ""
		return false
	}

	attrMap := make(map[string]interface{})
	attrs := meta.CMPT_META.GetInstAttrs(instId)
	for _, attr := range attrs {
		attrMap[attr.ATTR_NAME] = attr.ATTR_VALUE
		if attr.ATTR_NAME == consts.HEADER_SSH_ID {
			sshId := attr.ATTR_VALUE
			ssh := meta.CMPT_META.GetSshById(sshId)
			if ssh != nil {
				attrMap[consts.HEADER_IP] = ssh.SERVER_IP
			}
		}
	}

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = attrMap

	return true
}

// 递归方式获取服务下面所有组件的属性及拓扑信息
func loadInstanceAttribute(instId string, attrMap *map[string]interface{}, deployFlagArr *[]map[string]string) bool {
	instance := meta.CMPT_META.GetInstance(instId)
	if instance == nil {
		return false
	}

	cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
	if cmpt == nil {
		return false
	}

	deployMap := make(map[string]string)
	deployMap[instId] = instance.STATUS
	*deployFlagArr = append(*deployFlagArr, deployMap)

	attrs := meta.CMPT_META.GetInstAttrs(instId)
	for _, attr := range attrs {
		(*attrMap)[attr.ATTR_NAME] = attr.ATTR_VALUE

		if attr.ATTR_NAME == consts.HEADER_SSH_ID {
			sshId := attr.ATTR_VALUE
			ssh := meta.CMPT_META.GetSshById(sshId)
			if ssh != nil {
				(*attrMap)[consts.HEADER_IP] = ssh.SERVER_IP
			}
		}
	}

	// instance POS
	pos := make(map[string]int)
	if !instance.IsDefaultPos() {
		pos[consts.HEADER_X] = instance.POS_X
		pos[consts.HEADER_Y] = instance.POS_Y

		if instance.WIDTH != consts.POS_DEFAULT_VALUE && instance.HEIGHT != consts.POS_DEFAULT_VALUE {
			pos["width"] = instance.WIDTH
			pos["height"] = instance.HEIGHT
		}
		if instance.ROW != consts.POS_DEFAULT_VALUE && instance.COL != consts.POS_DEFAULT_VALUE {
			pos["row"] = instance.ROW
			pos["col"] = instance.COL
		}
	}
	(*attrMap)[consts.HEADER_POS] = pos

	// sub components, add sub node skeleton in order to avoid no sub instance.
	subCmpt := cmpt.SUB_CMPT_SET
	if len(subCmpt) == 0 {
		return true
	}

	for subCmptId := range subCmpt {
		subCmpt := meta.CMPT_META.GetCmptById(subCmptId)
		if subCmpt == nil {
			return false
		}

		subCmptName := subCmpt.CMPT_NAME
		subNodeType := subCmpt.NODE_JSON_TYPE
		if subNodeType == consts.SCHEMA_ARRAY {
			(*attrMap)[subCmptName] = make([]map[string]interface{}, 0)
		} else {
			(*attrMap)[subCmptName] = make(map[string]interface{})
		}
	}

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, &relations)

	for _, topo := range relations {
		toeId := topo.GetToe(instId)
		subInst := meta.CMPT_META.GetInstance(toeId)
		if subInst == nil {
			return false
		}

		subCmpt := meta.CMPT_META.GetCmptById(subInst.CMPT_ID)
		if subCmpt == nil {
			return false
		}

		subCmptName := subCmpt.CMPT_NAME
		subNodeType := subCmpt.NODE_JSON_TYPE
		if subNodeType == consts.SCHEMA_ARRAY {
			subCmptSlice := (*attrMap)[subCmptName].([]map[string]interface{})
			tmpMap := make(map[string]interface{})
			if loadInstanceAttribute(toeId, &tmpMap, deployFlagArr) {
				subCmptSlice = append(subCmptSlice, tmpMap)
				(*attrMap)[subCmptName] = subCmptSlice
			} else {
				return false
			}
		} else {
			subCmptMap := (*attrMap)[subCmptName].(map[string]interface{})
			if loadInstanceAttribute(toeId, &subCmptMap, deployFlagArr) {
				(*attrMap)[subCmptName] = subCmptMap
			} else {
				return false
			}
		}
	}

	return true
}

func SaveServTopoSkeleton(servType string, topoMap map[string]interface{}, magicKey string, resultBean *result.ResultBean) {
	servRootName := meta.CMPT_META.GetServRootCmpt(servType)
	subJson := (topoMap)[servRootName]
	if subJson == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = servRootName + " not found"
		return
	}

	subMap := subJson.(map[string]interface{})
	servInstId := subMap[consts.HEADER_INST_ID].(string)
	instance := meta.CMPT_META.GetInstance(servInstId)
	events := make([]*proto.PaasEvent, 0)

	if instance != nil {
		// save pos after move
		enumSavePos(servRootName, subMap, resultBean, &events, magicKey)
	} else {
		// init containers
		enumSaveSkeleton(topoMap, resultBean, &events, magicKey)
	}
}

func ReloadMetaData(reloadType string, magicKey string, resultBean *result.ResultBean) {
	meta.CMPT_META.ReloadMetaData(reloadType)

	// broadcast to cluster
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_RELOAD_TYPE] = reloadType

	msgBody := utils.Struct2Json(msgBodyMap)
	event := proto.NewPaasEvent(consts.EVENT_RELOAD_METADATA.CODE, msgBody, magicKey)
	eventbus.EVENTBUS.PublishEvent(event)
}

func enumSavePos(cmptName string, topoMap map[string]interface{},
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) bool {

	cmpt := meta.CMPT_META.GetCmptByName(cmptName)

	instIdRaw, foundId := topoMap[consts.HEADER_INST_ID]
	posRaw, foundPos := topoMap[consts.HEADER_POS]

	if foundId && foundPos {
		posMap := posRaw.(map[string]interface{})
		instId := instIdRaw.(string)

		if len(posMap) > 0 {
			pos := proto.GetPos(&posMap)
			dbPool := global.GLOBAL_RES.GetDbPool()
			_, err := crud.Update(dbPool, &SQL_UPDATE_POS, pos.X, pos.Y,
				pos.Width, pos.Height, pos.Row, pos.Col, instId)
			if err == nil {
				instance := proto.NewPaasInstance(instId, cmpt.CMPT_ID, consts.STR_FALSE, pos.X, pos.Y,
					pos.Width, pos.Height, pos.Row, pos.Col)

				// update local cache
				meta.CMPT_META.UpdInstPos(instance)

				// broadcast event to cluster
				msgBody := utils.Struct2Json(instance)
				event := proto.NewPaasEvent(consts.EVENT_UPD_INST_POS.CODE, msgBody, magicKey)
				*events = append(*events, event)
			} else {
				errMsg := fmt.Sprintf("enumSavePos error: %v", err.Error())
				utils.LOGGER.Error(errMsg)
			}
		}
	}

	instId := instIdRaw.(string)
	for key, val := range topoMap {
		subCmptName := key
		itemKind := reflect.TypeOf(val).Kind()
		if itemKind == reflect.Map {
			subMap := val.(map[string]interface{})
			subInstIdRaw := subMap[consts.HEADER_INST_ID]
			if subInstIdRaw == nil {
				return true
			}

			subInstId := subInstIdRaw.(string)
			metaInst := meta.CMPT_META.GetInstance(subInstId)
			// 新增支持在已部署服务下扩充新的组件
			if metaInst == nil {
				cmpt = meta.CMPT_META.GetCmptByName(subCmptName)

				// 1. add instance
				addInstance(subInstId, cmpt, &subMap, resultBean, events, magicKey)

				// ignore container instance attributes
				addCmptAttr(subInstId, cmpt, &subMap, resultBean, events, magicKey)

				// 2. add topology relations
				addRelation(instId, subInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
			} else {
				if !enumSavePos(subCmptName, subMap, resultBean, events, magicKey) {
					return false
				}
			}
		} else if itemKind == reflect.Array {
			subMap := val.([]map[string]interface{})
			for _, item := range subMap {
				if !enumSavePos(subCmptName, item, resultBean, events, magicKey) {
					return false
				}
			}
		}
	}

	return true
}

func enumSaveSkeleton(topoMap interface{}, resultBean *result.ResultBean,
	events *[]*proto.PaasEvent, magicKey string) bool {

	itemKind := reflect.TypeOf(topoMap).Kind()
	if itemKind == reflect.Map {
		objMap := topoMap.(map[string]interface{})

		for key, val := range objMap {
			cmpt := meta.CMPT_META.GetCmptByName(key)
			if cmpt == nil {
				continue
			}

			if cmpt.NODE_JSON_TYPE == consts.SCHEMA_OBJECT {
				subNode := val.(map[string]interface{})
				if len(subNode) == 0 {
					continue
				}

				if !addInstanceWithAttrRelation(subNode, cmpt, resultBean, events, magicKey) {
					return false
				}

				if !enumSaveSkeleton(subNode, resultBean, events, magicKey) {
					return false
				}
			} else if cmpt.NODE_JSON_TYPE == consts.SCHEMA_ARRAY {
				subNodeArr := val.([]interface{})
				if len(subNodeArr) == 0 {
					continue
				}

				for _, subNodeRaw := range subNodeArr {
					subNode := subNodeRaw.(map[string]interface{})
					if len(subNode) == 0 {
						continue
					}

					if !addInstanceWithAttrRelation(subNode, cmpt, resultBean, events, magicKey) {
						return false
					}

					if !enumSaveSkeleton(subNode, resultBean, events, magicKey) {
						return false
					}
				}
			}
		}

	} else if itemKind == reflect.Array {
		// not affected currently
	}

	return true
}

func addInstanceWithAttrRelation(jsonMap map[string]interface{}, cmpt *proto.PaasMetaCmpt,
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) bool {

	instIdRaw := jsonMap[consts.HEADER_INST_ID]
	if instIdRaw == nil {
		return true
	}

	instId := instIdRaw.(string)
	// 1. add instance
	addInstance(instId, cmpt, &jsonMap, resultBean, events, magicKey)

	// 2. add component attributes
	addCmptAttr(instId, cmpt, &jsonMap, resultBean, events, magicKey)

	isContainer := cmpt.HaveSubComponent()
	if isContainer {
		// 支持容器包含服务的容器
		subCmptIDSet := cmpt.SUB_CMPT_SET
		for subCmptID := range subCmptIDSet {
			subCmpt := meta.CMPT_META.GetCmptById(subCmptID)
			isSubCmptServRoot := meta.CMPT_META.IsServRootCmpt(subCmpt.SERV_TYPE, subCmpt.CMPT_NAME)
			if isSubCmptServRoot {
				// add container to service container relation
				subServInstIdRaw := jsonMap[consts.HEADER_SERV_INST_ID]
				if subServInstIdRaw != nil {
					subServInstId := subServInstIdRaw.(string)
					if !meta.CMPT_META.IsTopoRelationExists(instId, subServInstId) {
						delRelation(instId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
						addRelation(instId, subServInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
					}
				}
			}
		}
	}

	// 3. add topology relations
	for key, val := range jsonMap {
		subCmpt := meta.CMPT_META.GetCmptByName(key)
		isSubComponent := subCmpt != nil

		if isContainer && isSubComponent {
			subCmptNodeJsonType := subCmpt.NODE_JSON_TYPE
			if subCmptNodeJsonType == consts.SCHEMA_OBJECT {
				node := val.(map[string]interface{})
				if len(node) == 0 {
					continue
				}

				subInstId := node[consts.HEADER_INST_ID].(string)
				addRelation(instId, subInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
			} else if subCmptNodeJsonType == consts.SCHEMA_ARRAY {
				nodeArr := val.([]interface{})
				if len(nodeArr) == 0 {
					continue
				}

				for _, subCmptItemRaw := range nodeArr {
					subCmptItem := subCmptItemRaw.(map[string]interface{})
					if len(subCmptItem) == 0 {
						continue
					}

					subInstId := subCmptItem[consts.HEADER_INST_ID].(string)
					addRelation(instId, subInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
				}
			}
		}
	}

	return true
}

func delRelation(parentId string, topoType int, resultBean *result.ResultBean,
	events *[]*proto.PaasEvent, magicKey string) {

	dbPool := global.GLOBAL_RES.GetDbPool()
	crud.Delete(dbPool, &SQL_DEL_ALL_SUB_TOPOLOGY, parentId, parentId)

	// update local cache
	meta.CMPT_META.DelAllSubTopo(parentId)

	// broadcast event to cluster
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_PARENT_ID] = parentId
	msgBodyMap[consts.HEADER_INST_ID] = ""
	msgBodyMap[consts.HEADER_TOPO_TYPE] = topoType
	msgBody := utils.Struct2Json(msgBodyMap)

	event := proto.NewPaasEvent(consts.EVENT_DEL_TOPO.CODE, msgBody, magicKey)
	*events = append(*events, event)
}

func addRelation(parentId string, instId string, topoType int, resultBean *result.ResultBean,
	events *[]*proto.PaasEvent, magicKey string) {

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_INS_TOPOLOGY, parentId, instId, topoType)
	if err != nil {
		errMsg := fmt.Sprintf("addRelation fail, parentId: %s, instId: %s", parentId, instId)
		utils.LOGGER.Error(errMsg)
	}

	// add to local cache
	topo := proto.NewPaasTopology(parentId, instId, topoType)
	meta.CMPT_META.AddTopo(topo)

	// broadcast event to cluster
	msgBody := utils.Struct2Json(topo)
	event := proto.NewPaasEvent(consts.EVENT_ADD_TOPO.CODE, msgBody, magicKey)
	*events = append(*events, event)
}

func modRelation(parentId string, instId string, topoType int, resultBean *result.ResultBean,
	events *[]*proto.PaasEvent, magicKey string) {

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_UPD_TOPOLOGY, instId, parentId)
	if err != nil {
		errMsg := fmt.Sprintf("modRelation fail, parentId: %s, instId: %s", parentId, instId)
		utils.LOGGER.Error(errMsg)
	}

	// update local cache
	topo := proto.NewPaasTopology(parentId, instId, topoType)
	meta.CMPT_META.ModTopo(topo)

	// broadcast event to cluster
	msgBody := utils.Struct2Json(topo)
	event := proto.NewPaasEvent(consts.EVENT_MOD_TOPO.CODE, msgBody, magicKey)
	*events = append(*events, event)
}

func addCmptAttr(instId string, cmpt *proto.PaasMetaCmpt, subMap *map[string]interface{},
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) {

	attrSlic := meta.CMPT_META.GetCmptAttrs(cmpt.CMPT_ID)
	for _, attr := range attrSlic {
		attrId := attr.ATTR_ID     // .getAttrId();
		attrName := attr.ATTR_NAME // .getAttrName();

		attrRaw := (*subMap)[attrName]
		var attrVal string = ""
		if attrRaw != nil {
			attrVal = attrRaw.(string)
		}

		dbPool := global.GLOBAL_RES.GetDbPool()
		_, err := crud.Insert(dbPool, &SQL_INS_INSTANCE_ATTR, instId, attrId, attrName, attrVal)
		if err != nil {
			errMsg := fmt.Sprintf("save component attribute fail, instId:%s, attrId:%d, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal)
			utils.LOGGER.Error(errMsg)
		}

		// add to local cache
		instAttr := proto.NewPaasInstAttr(instId, attrId, attrName, attrVal)
		meta.CMPT_META.AddInstAttr(instAttr)

		// broadcast event to cluster
		msgBody := utils.Struct2Json(instAttr)
		event := proto.NewPaasEvent(consts.EVENT_ADD_INST_ATTR.CODE, msgBody, magicKey)
		*events = append(*events, event)
	}
}

func modCmptAttr(instId string, cmpt *proto.PaasMetaCmpt, node *map[string]interface{},
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) {

	attrs := meta.CMPT_META.GetCmptAttrs(cmpt.CMPT_ID)
	if len(attrs) == 0 {
		return
	}

	for _, attr := range attrs {
		attrId := attr.ATTR_ID
		attrName := attr.ATTR_NAME
		if attrName == consts.HEADER_INST_ID {
			continue
		}

		attrValRaw := (*node)[attrName]
		var attrVal string = ""
		if attrValRaw != nil {
			attrVal = attrValRaw.(string)
		}

		if attrName == consts.HEADER_SERV_INST_ID {
			// add container to service container relation
			subServInstId := attrVal
			if meta.CMPT_META.IsTopoExists(instId) {
				modRelation(instId, subServInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
			} else {
				addRelation(instId, subServInstId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
			}
		}

		// 元数据模型有可能新增组件属性, 如果都是按update方式则不能动态扩展
		isAttrExists := meta.CMPT_META.IsInstAttrExists(instId, attrId)
		var instAttr *proto.PaasInstAttr = nil
		if isAttrExists {
			// 已经存在做更新动作
			dbPool := global.GLOBAL_RES.GetDbPool()
			_, err := crud.Update(dbPool, &SQL_MOD_ATTR, attrVal, instId, attrId)
			if err != nil {
				errMsg := fmt.Sprintf("save component attribute fail, instId:%s, attrId:%d, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal)
				utils.LOGGER.Error(errMsg)
			}

			// refresh to local cache
			instAttr := proto.NewPaasInstAttr(instId, attrId, attrName, attrVal)
			meta.CMPT_META.UpdInstAttr(instAttr)
		} else {
			// 没有对应的属性,说明组件新增了属性,此时对应做insert操作
			dbPool := global.GLOBAL_RES.GetDbPool()
			_, err := crud.Insert(dbPool, &SQL_INS_INSTANCE_ATTR, instId, attrId, attrName, attrVal)
			if err != nil {
				errMsg := fmt.Sprintf("save component attribute fail, instId:%s, attrId:%d, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal)
				utils.LOGGER.Error(errMsg)
			}

			// refresh to local cache
			instAttr := proto.NewPaasInstAttr(instId, attrId, attrName, attrVal)
			meta.CMPT_META.AddInstAttr(instAttr)
		}

		// broadcast event to cluster
		msgBody := utils.Struct2Json(instAttr)
		ev := proto.NewPaasEvent(consts.EVENT_ADD_INST_ATTR.CODE, msgBody, magicKey)
		*events = append(*events, ev)
	}
}

func addInstance(instId string, cmpt *proto.PaasMetaCmpt, subMap *map[string]interface{},
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) {

	cmptId := cmpt.CMPT_ID

	posRaw := (*subMap)[consts.HEADER_POS]
	pos := proto.NewPaasPos(0, 0)
	if posRaw != nil {
		posMap := posRaw.(map[string]interface{})
		pos = proto.GetPos(&posMap)
	}

	// 1. add instance
	var isDeployed string = consts.STR_TRUE
	if cmpt.IsNeedDeploy() {
		isDeployed = consts.STR_FALSE
	}

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_INS_INSTANCE, instId, cmptId, isDeployed, pos.X, pos.Y,
		pos.Width, pos.Height, pos.Row, pos.Col)
	if err != nil {
		errMsg := fmt.Sprintf("%v", err)
		utils.LOGGER.Error(errMsg)
	}

	instance := proto.NewPaasInstance(instId, cmptId, isDeployed, pos.X, pos.Y,
		pos.Width, pos.Height, pos.Row, pos.Col)

	// add to local cache
	meta.CMPT_META.AddInstance(instance)

	// broadcast event to cluster
	msgBody := utils.Struct2Json(instance)
	event := proto.NewPaasEvent(consts.EVENT_ADD_INSTANCE.CODE, msgBody, magicKey)
	*events = append(*events, event)
}

func SaveServiceNode(parentId string, opType int, nodeJson map[string]interface{}, resultBean *result.ResultBean, magicKey string) {
	events := make([]*proto.PaasEvent, 0)
	if enumSaveServiceNode(parentId, opType, nodeJson, resultBean, &events, magicKey) {
		for _, event := range events {
			eventbus.EVENTBUS.PublishEvent(event)
		}
	}
}

func DelServiceNode(parentId string, instId string, resultBean *result.ResultBean, magicKey string) {
	events := make([]*proto.PaasEvent, 0)
	if DelInstance(parentId, instId, resultBean, &events, magicKey) {
		for _, ev := range events {
			eventbus.EVENTBUS.PublishEvent(ev)

			objMap := make(map[string]interface{})
			utils.Json2Struct([]byte(ev.MSG_BODY), &objMap)
			id := objMap[consts.HEADER_INST_ID].(string)
			switch ev.EVENT_CODE {
			case consts.EVENT_DEL_INSTANCE.CODE:
				meta.CMPT_META.DelInstance(id)
			case consts.EVENT_DEL_INST_ATTR.CODE:
				meta.CMPT_META.DelInstAttr(id)
			case consts.EVENT_DEL_TOPO.CODE:
				supId := objMap[consts.HEADER_PARENT_ID].(string)
				meta.CMPT_META.DelTopo(supId, id)
			default:
				errMsg := fmt.Sprintf("undefined event: %d", ev.EVENT_CODE)
				utils.LOGGER.Error(errMsg)
			}
		}

		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
	} else {
		errMsg := fmt.Sprintf("del_instance nok, parent_id:%s, inst_id:%s", parentId, instId)
		utils.LOGGER.Error(errMsg)
	}
}

func enumSaveServiceNode(parentId string, opType int, nodeJson map[string]interface{},
	resultBean *result.ResultBean, events *[]*proto.PaasEvent, magicKey string) bool {

	for cmptName, subJsonRaw := range nodeJson {
		cmpt := meta.CMPT_META.GetCmptByName(cmptName)
		if cmpt == nil {
			continue
		}

		itemKind := reflect.TypeOf(subJsonRaw).Kind()
		if itemKind != reflect.Slice {
			resultBean.RET_CODE = consts.REVOKE_NOK
			resultBean.RET_INFO = consts.ERR_JSONNODE_ILLEGAL
			return false
		}

		subs := subJsonRaw.([]interface{})
		for _, nodeRaw := range subs {
			node := nodeRaw.(map[string]interface{})
			instIdRaw := node[consts.HEADER_INST_ID]
			if instIdRaw == nil {
				continue
			}

			instId := instIdRaw.(string)

			// 防止前端未保存实例的情况下，第一次保存opType传了2
			if meta.CMPT_META.GetInstance(instId) == nil {
				opType = consts.OP_TYPE_ADD
			}

			if opType == consts.OP_TYPE_ADD {
				// 1. add instance
				addInstance(instId, cmpt, &node, resultBean, events, magicKey)

				// 2. add component attribute
				addCmptAttr(instId, cmpt, &node, resultBean, events, magicKey)

				// 3. add relation
				addRelation(parentId, instId, consts.TOPO_TYPE_CONTAIN, resultBean, events, magicKey)
			} else if opType == consts.OP_TYPE_MOD {
				// mod component attribute
				modCmptAttr(instId, cmpt, &node, resultBean, events, magicKey)
			} else {
				resultBean.RET_CODE = consts.REVOKE_NOK
				resultBean.RET_INFO = consts.ERR_OP_TYPE
				return false
			}
		}
	}

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = ""
	return true
}

func GetMetaDataTreeByInstId(instId string, resultBean *result.ResultBean) {
	arr := make([]map[string]interface{}, 0)
	getChildNode(instId, &arr)

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = arr
}

func GetMetaDataNodeByInstId(instId string, resultBean *result.ResultBean) {
	instance := meta.CMPT_META.GetInstance(instId)
	if instance == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		return
	}

	cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
	if cmpt == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_CMPT_NOT_FOUND
		return
	}

	metaJson := make(map[string]interface{})
	attrArr := make([]map[string]interface{}, 0)

	attrs := meta.CMPT_META.GetInstAttrs(instId)
	for _, attr := range attrs {
		node := make(map[string]interface{})
		node[consts.HEADER_ATTR_NAME] = attr.ATTR_NAME
		node[consts.HEADER_ATTR_VALUE] = attr.ATTR_VALUE

		if attr.ATTR_NAME == consts.HEADER_SSH_ID {
			sshID := attr.ATTR_VALUE
			ssh := meta.CMPT_META.GetSshById(sshID)

			tmp := make(map[string]interface{})
			tmp[consts.HEADER_ATTR_NAME] = consts.HEADER_IP
			tmp[consts.HEADER_ATTR_NAME_CN] = consts.HEADER_IP
			tmp[consts.HEADER_ATTR_VALUE] = ssh.SERVER_IP
			attrArr = append(attrArr, tmp)
		}

		metaAttr := meta.CMPT_META.GetAttr(attr.ATTR_ID)
		if metaAttr != nil {
			node[consts.HEADER_ATTR_NAME_CN] = metaAttr.ATTR_NAME_CN
		}

		attrArr = append(attrArr, node)
	}
	metaJson[cmpt.CMPT_NAME_CN] = attrArr

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = metaJson
}

func GetSmsABQueueWeightInfo(servInstId string, resultBean *result.ResultBean) {
	instance := meta.CMPT_META.GetInstance(servInstId)
	if instance == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		return
	}

	cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
	if cmpt == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_CMPT_NOT_FOUND
		return
	}

	var attr *proto.PaasInstAttr = nil
	topos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, &topos)
	find := false
	// 从 sms-server | sms-process | sms-client | sms-batsave | sms-statistics 中任意一个去取REDIS_CLUSTER_QUEUE属性
	for _, topo := range topos {
		containerId := topo.GetToe(servInstId)
		if containerId == "" {
			continue
		}

		subTopos := make([]*proto.PaasTopology, 0)
		meta.CMPT_META.GetInstRelations(containerId, &subTopos)
		for _, subTopo := range subTopos {
			instId := subTopo.GetToe(containerId)
			attr := meta.CMPT_META.GetInstAttr(instId, 203) // 203 -> 'REDIS_CLUSTER_QUEUE'
			if attr != nil {
				find = true
				break
			}
		}

		if find {
			break
		}
	}

	if attr == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_REDIS_QUEUE_NOT_INITIALIZED
		return
	}

	// 取到REDIS_CLUSTER_QUEUE后,对应取下面每个集群的权重信息
	redisQueueServId := attr.ATTR_VALUE
	queueServInst := meta.CMPT_META.GetInstance(redisQueueServId)
	if queueServInst == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_REDIS_QUEUE_NOT_INITIALIZED
	} else {
		abQueueClusterTopos := make([]*proto.PaasTopology, 0)
		meta.CMPT_META.GetInstRelations(redisQueueServId, &abQueueClusterTopos)
		if len(abQueueClusterTopos) == 0 {
			resultBean.RET_CODE = consts.REVOKE_NOK
			resultBean.RET_INFO = consts.ERR_REDIS_QUEUE_NOT_INITIALIZED
		} else {
			retInfo := make(map[string]interface{})
			for _, abTopo := range abQueueClusterTopos {
				queueServId := abTopo.GetToe(redisQueueServId)

				containerNameAttr := meta.CMPT_META.GetInstAttr(queueServId, 142) // 142, 'SERV_CONTAINER_NAME'
				queueContainerAttrs := meta.CMPT_META.GetInstAttrs(queueServId)
				item := make(map[string]interface{})
				for _, containerAttr := range queueContainerAttrs {
					item[containerAttr.ATTR_NAME] = containerAttr.ATTR_VALUE
				}
				retInfo[containerNameAttr.ATTR_VALUE] = item
			}

			resultBean.RET_CODE = consts.REVOKE_OK
			resultBean.RET_INFO = retInfo
		}
	}
}

func AdjustSmsABQueueWeightInfo(param *proto.AdjustSmsABQueueParam, magicKey string, resultBean *result.ResultBean) {
	servInstID := param.SERV_INST_ID
	queueServInstID := param.QUEUE_SERV_INST_ID
	topoJson := param.TOPO_JSON

	servInst := meta.CMPT_META.GetInstance(queueServInstID)
	if servInst == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND
		return
	}

	topos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(queueServInstID, &topos)
	if len(topos) == 0 {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_REDIS_QUEUE_NOT_INITIALIZED
		return
	}

	// "RedisClusterA" | "RedisClusterB"
	clusterARaw := topoJson[consts.REDIS_CLUSTER_A]
	clusterBRaw := topoJson[consts.REDIS_CLUSTER_B]
	if clusterARaw == nil || clusterBRaw == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_REDIS_A_OR_B_MISSING
		return
	}

	clusterA := clusterARaw.(map[string]interface{})
	clusterB := clusterBRaw.(map[string]interface{})

	instAId := clusterA[consts.HEADER_INST_ID].(string)
	instBId := clusterB[consts.HEADER_INST_ID].(string)

	weightA := clusterA[consts.HEADER_WEIGHT].(string)
	weightB := clusterB[consts.HEADER_WEIGHT].(string)

	// update database
	dbPool := global.GLOBAL_RES.GetDbPool()

	_, err := crud.Update(dbPool, &SQL_MOD_ATTR, weightA, instAId, 141) // 141 -> 'WEIGHT'
	if err != nil {
		errMsg := fmt.Sprintf("SQL_MOD_ATTR fail")
		utils.LOGGER.Error(errMsg)
	}

	_, err = crud.Update(dbPool, &SQL_MOD_ATTR, weightB, instBId, 141) // 141 -> 'WEIGHT
	if err != nil {
		errMsg := fmt.Sprintf("SQL_MOD_ATTR fail")
		utils.LOGGER.Error(errMsg)
	}

	// update local cache
	meta.CMPT_META.AdjustSmsABQueueWeightInfo(instAId, weightA, instBId, weightB)

	// broadcast event bus to synchronize local cache
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_INST_ID_A] = instAId
	msgBodyMap[consts.HEADER_WEIGHT_A] = weightA
	msgBodyMap[consts.HEADER_INST_ID_B] = instBId
	msgBodyMap[consts.HEADER_WEIGHT_B] = weightB

	msgBody := utils.Struct2Json(msgBodyMap)
	event := proto.NewPaasEvent(consts.EVENT_AJUST_QUEUE_WEIGHT.CODE, msgBody, magicKey)
	eventbus.EVENTBUS.PublishEvent(event)

	// 依次遍历每个sms服务下的每个模块的web console接口, 指定调整权重操作
	servTopoRetVal := result.NewResultBean()
	if LoadServiceTopo(servInstID, servTopoRetVal) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_LOAD_SERV_TOPO_FAIL
		return
	}

	servTopo := servTopoRetVal.RET_INFO.(map[string]interface{})

	cmd := consts.CMD_ADJUST_REDIS_WEIGHT
	msg := fmt.Sprintf("%s:%s,%s:%s", consts.REDIS_CLUSTER_A, weightA, consts.REDIS_CLUSTER_B, weightB)
	data := [2]string{cmd, msg}
	buff := ""
	for _, s := range data {
		buff += "\t"
		buff += s
		buff += "\r\n"
	}
	byteBuff := []byte(buff)

	if enumSmsTopoToSendData(&servTopo, byteBuff) {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
	} else {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_AJUST_REDIS_WEIGHT_PARTAIL_OK
	}
}

func SwitchSmsDBType(param *proto.SwitchSmsDBTypeParam, magicKey string, resultBean *result.ResultBean) {
	servInstID := param.DB_SERV_INST_ID
	dbServInstID := param.DB_SERV_INST_ID
	dbType := param.ACTIVE_DB_TYPE
	dbName := param.DB_NAME

	smsServInst := meta.CMPT_META.GetInstance(servInstID) // PaasInstance
	if smsServInst == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SMS_INSTANCE_NOT_FOUND
		return
	}

	// Collection<PaasTopology> smsTopos = meta.getInstRelations();
	smsTopos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstID, &smsTopos)
	if len(smsTopos) == 0 {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SMS_SERV_NOT_INITIALIZED
		return
	}

	dbServInst := meta.CMPT_META.GetInstance(dbServInstID) // PaasInstance
	if dbServInst == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB_INSTANCE_NOT_FOUND
		return
	}

	dbServTopos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(dbServInstID, &dbServTopos)
	if len(dbServTopos) == 0 {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB_SERV_NOT_INITIALIZED
		return
	}

	// find the specified dg container
	dgContainerID := ""
	for _, topo := range dbServTopos {
		id := topo.GetToe(dbServInstID)
		dgNameAttr := meta.CMPT_META.GetInstAttr(id, 136) // 136 -> 'DG_NAME'
		if dgNameAttr != nil && dgNameAttr.ATTR_VALUE == dbName {
			dgContainerID = id
			break
		}
	}

	if dgContainerID == "" {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_SPECIFIED_DG_NOT_FOUND
		return
	}

	// update database
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_MOD_ATTR, dbType, dgContainerID, 225)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
		return
	}

	// update local cache
	meta.CMPT_META.SwitchSmsDBType(dgContainerID, dbType)

	// broadcast event bus to synchronize local cache
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_INST_ID] = dgContainerID
	msgBodyMap[consts.HEADER_ACTIVE_DB_TYPE] = dbType

	msgBody := utils.Struct2Json(msgBodyMap)
	event := proto.NewPaasEvent(consts.EVENT_SWITCH_DB_TYPE.CODE, msgBody, magicKey)
	eventbus.EVENTBUS.PublishEvent(event)

	cmd := consts.CMD_SWITCH_DB_TYPE
	data := [3]string{cmd, dbType, dbName}
	buff := ""
	for _, s := range data {
		buff += "\t"
		buff += s
		buff += "\r\n"
	}
	byteBuff := []byte(buff)

	// 依次遍历每个sms服务下的每个模块的web console接口, 指定调整权重操作
	servTopoRetVal := result.NewResultBean()
	if !LoadServiceTopo(servInstID, servTopoRetVal) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_LOAD_SERV_TOPO_FAIL
		return
	}

	servTopo := servTopoRetVal.RET_INFO.(map[string]interface{})
	if enumSmsTopoToSendData(&servTopo, byteBuff) {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = ""
	} else {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_AJUST_REDIS_WEIGHT_PARTAIL_OK
	}
}

func AddCmptVersion(param *proto.CmptVersionParam, magicKey string, resultBean *result.ResultBean) {
	servType := param.SERV_TYPE
	version := param.VERSION

	if meta.CMPT_META.IsCmptVersionExist(servType, version) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DUMPLICATE_VERSION_EXCEPTION
		return
	}

	// 1. update database
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_INS_CMPT_VER, servType, version)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DUMPLICATE_KEY_OR_DB_EXCEPTION
		return
	}

	// 2. update local cache
	meta.CMPT_META.AddCmptVersion(servType, version)

	// 3. broadcast by event bus
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_SERV_TYPE] = servType
	msgBodyMap[consts.HEADER_VERSION] = version

	msgBody := utils.Struct2Json(msgBodyMap)
	event := proto.NewPaasEvent(consts.EVENT_ADD_CMPT_VER.CODE, msgBody, magicKey)
	eventbus.EVENTBUS.PublishEvent(event)

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = ""
}

func DelCmptVersion(param *proto.CmptVersionParam, magicKey string, resultBean *result.ResultBean) {
	servType := param.SERV_TYPE
	version := param.VERSION

	if !meta.CMPT_META.IsCmptVersionExist(servType, version) {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DEL_SERV_VER_NOT_EXISTS
		return
	}

	// 1. update database
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_DEL_CMPT_VER, servType, version)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_DB
		return
	}

	// 2. update local cache
	meta.CMPT_META.DelCmptVersion(servType, version)

	// 3. broadcast by event bus
	msgBodyMap := make(map[string]interface{})
	msgBodyMap[consts.HEADER_SERV_TYPE] = servType
	msgBodyMap[consts.HEADER_VERSION] = version

	msgBody := utils.Struct2Json(msgBodyMap)
	event := proto.NewPaasEvent(consts.EVENT_DEL_CMPT_VER.CODE, msgBody, magicKey)
	eventbus.EVENTBUS.PublishEvent(event)

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = ""
}

func enumSmsTopoToSendData(servTopo *map[string]interface{}, data []byte) bool {
	smsServContainer := (*servTopo)[consts.HEADER_SMS_GATEWAY_SERV_CONTAINER].(map[string]interface{})

	serverContainer := smsServContainer[consts.HEADER_SMS_SERVER_CONTAINER].(map[string]interface{})
	processContainer := smsServContainer[consts.HEADER_SMS_PROCESS_CONTAINER].(map[string]interface{})
	clientContainer := smsServContainer[consts.HEADER_SMS_CLIENT_CONTAINER].(map[string]interface{})
	batSaveContainer := smsServContainer[consts.HEADER_SMS_BATSAVE_CONTAINER].(map[string]interface{})
	statsContainer := smsServContainer[consts.HEADER_SMS_STATS_CONTAINER].(map[string]interface{})

	smsServerArr := serverContainer[consts.HEADER_SMS_SERVER].([]map[string]interface{})
	smsProcessArr := processContainer[consts.HEADER_SMS_PROCESS].([]map[string]interface{})
	smsClientArr := clientContainer[consts.HEADER_SMS_CLIENT].([]map[string]interface{})
	batSaveArr := batSaveContainer[consts.HEADER_SMS_BATSAVE].([]map[string]interface{})
	statsArr := statsContainer[consts.HEADER_SMS_STATS].([]map[string]interface{})

	dispachSmsServerOk := dispatchSmsEvent(consts.HEADER_SMS_SERVER, smsServerArr, data)
	dispachSmsProcessOk := dispatchSmsEvent(consts.HEADER_SMS_PROCESS, smsProcessArr, data)
	dispachSmsClientOk := dispatchSmsEvent(consts.HEADER_SMS_CLIENT, smsClientArr, data)
	dispachSmsBatSaveOk := dispatchSmsEvent(consts.HEADER_SMS_BATSAVE, batSaveArr, data)
	dispachSmsStatsOk := dispatchSmsEvent(consts.HEADER_SMS_STATS, statsArr, data)

	return dispachSmsServerOk && dispachSmsProcessOk && dispachSmsClientOk && dispachSmsBatSaveOk && dispachSmsStatsOk
}

func dispatchSmsEvent(smsCmpt string, instArr []map[string]interface{}, data []byte) bool {
	if len(instArr) == 0 {
		return true
	}

	allOk := true
	for _, item := range instArr {
		instID := item[consts.HEADER_INST_ID].(string)
		ip := item[consts.HEADER_IP].(string)
		webConsolePort := item[consts.HEADER_WEB_CONSOLE_PORT].(string)
		processor := item[consts.HEADER_PROCESSOR].(string)
		passwd := consts.SMS_CONSOLE_PASSWD

		var portOffset int = 0
		if processor != "" {
			portOffset, _ = strconv.Atoi(processor)
		}
		basePort, _ := strconv.Atoi(webConsolePort)
		realPort := basePort + portOffset

		inst := meta.CMPT_META.GetInstance(instID)
		if inst == nil {
			continue
		}

		notfyOk := false
		for retry := 0; retry < consts.NOTIFY_RETRY_CNT; retry++ {
			if notfifySmsInstanceEvent(ip, realPort, passwd, data) {
				notfyOk = true
				break
			} else {
				errMsg := fmt.Sprintf("send msg: %s to addr: %s:%d fail, restry count: %d", string(data), ip, realPort, retry)
				utils.LOGGER.Error(errMsg)
				time.Sleep(time.Duration(100) * time.Millisecond)
			}
		}

		if !notfyOk {
			allOk = false
		}
	}

	return allOk
}

func notfifySmsInstanceEvent(ip string, realPort int, passwd string, data []byte) bool {
	addr := fmt.Sprintf("%s:%d", ip, realPort)
	timeout := time.Duration(3) * time.Second
	conn, err := net.DialTimeout("tcp", addr, timeout)
	if err != nil {
		return false
	} else {
		errMsg := fmt.Sprintf("dial address: %s fail", addr)
		utils.LOGGER.Error(errMsg)
	}

	defer conn.Close()

	_, err = conn.Write(data)
	if err != nil {
		return false
	} else {
		errMsg := fmt.Sprintf("send to %s msg: %s fail", addr, string(data))
		utils.LOGGER.Error(errMsg)
	}

	return true
}

func getChildNode(instId string, arr *[]map[string]interface{}) {
	topos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, &topos)
	if len(topos) == 0 {
		return
	}

	for _, topo := range topos {
		node := make(map[string]interface{})
		childNodes := make([]map[string]interface{}, 0)

		getChildNode(topo.INST_ID2, &childNodes)

		node["inst_id"] = topo.INST_ID2

		instance := meta.CMPT_META.GetInstance(topo.INST_ID2)
		if instance != nil {
			cmpt := meta.CMPT_META.GetCmptById(instance.CMPT_ID)
			text := fmt.Sprintf("%s (%s)", cmpt.CMPT_NAME_CN, instance.INST_ID)
			node["text"] = text
		} else {
			node["text"] = topo.INST_ID2
		}

		if len(childNodes) > 0 {
			node["nodes"] = childNodes
		}

		*arr = append(*arr, node)
	}
}

func UpdateInstancePreEmbadded(instId, preEmbadded, logKey, magicKey string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_UPD_INSTANCE_PRE_EMBADDED, preEmbadded, instId)
	if err == nil {
		// update local cache
		meta.CMPT_META.UpdInstPreEmbadded(instId, preEmbadded)

		// broadcast event to cluster
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBodyMap[consts.HEADER_PRE_EMBADDED] = preEmbadded

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_UPD_INST_PRE_EMBEDDED.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)

		return true
	} else {
		errMsg := fmt.Sprintf("UpdateInstancePreEmbadded fail, instId: %s, preEmbadded: %s", instId, preEmbadded)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		if logKey != "" {
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		}

		return false
	}
}

func ModInstanceAttr(instId string, attrId int, attrName, attrVal, logKey, magicKey string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_MOD_ATTR, attrVal, instId, attrId)
	if err == nil {
		// add to local cache
		instAttr := proto.NewPaasInstAttr(instId, attrId, attrName, attrVal)
		meta.CMPT_META.UpdInstAttr(instAttr)

		// broadcast event to cluster
		msgBody := utils.Struct2Json(instAttr)
		event := proto.NewPaasEvent(consts.EVENT_ADD_INST_ATTR.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)

		return true
	} else {
		errMsg := fmt.Sprintf("save component attribute fail, instId:%s, attrId:%d, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		if logKey != "" {
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		}

		return false
	}
}

func UpdateInstanceDeployFlag(instId, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_UPD_INST_DEPLOY, deployFlag, instId)
	if err == nil {
		// add to local cache
		meta.CMPT_META.UpdInstDeploy(instId, deployFlag)

		// broadcast event to cluster
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBodyMap[consts.HEADER_IS_DEPLOYED] = deployFlag

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_UPD_INST_DEPLOY.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)

		return true
	} else {
		errMsg := fmt.Sprintf("UpdateInstanceDeployFlag fail, instId: %s, deployFlag: %s", instId, deployFlag)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		if logKey != "" {
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		}

		return false
	}
}

func UpdateServiceDeployFlag(instId, deployFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_UPD_SERV_DEPLOY, deployFlag, instId)

	if err == nil {
		// add to local
		meta.CMPT_META.UpdServDeploy(instId, deployFlag)

		// broadcast event to cluster
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_INST_ID] = instId
		msgBodyMap[consts.HEADER_IS_DEPLOYED] = deployFlag

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_UPD_SERVICE_DEPLOY.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)

		return true
	} else {
		errMsg := fmt.Sprintf("UpdateServiceDeployFlag fail, instId: %s, deployFlag: %s", instId, deployFlag)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg

		if logKey != "" {
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		}

		return false
	}
}

func ModServicePseudoFlag(servInstID, pseudoFlag, logKey, magicKey string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_MOD_SERVICE_PSEUDO_DEPLOY_FLAG, pseudoFlag, servInstID)

	if err == nil {
		meta.CMPT_META.ReloadService(servInstID)

		// broadcast event to cluster
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_INST_ID] = servInstID

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_MOD_SERVICE.CODE, msgBody, magicKey)
		eventbus.EVENTBUS.PublishEvent(event)

		return true
	} else {
		errMsg := fmt.Sprintf("ModServicePseudoFlag fail, servInstID: %s, pseudoFlag: %s", servInstID, pseudoFlag)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_DB

		if logKey != "" {
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)
		}

		return false
	}
}

func GetNextSeqMargin(seqName string, step uint) (*proto.LongMargin, error) {
	updateSeqSql := fmt.Sprintf(SQL_NEXT_SEQ_UPDATE, step)

	dbPool := global.GLOBAL_RES.GetDbPool()
	result, err := crud.NextSeqMargin(dbPool, &SQL_NEXT_SEQ_LOCK, &updateSeqSql, seqName)
	if err != nil {
		return nil, err
	}

	if result == nil || len(result) == 0 {
		errMsg := fmt.Sprintf("sequence %s not exits", seqName)
		return nil, errors.New(errMsg)
	}

	startId := result[consts.HEADER_CURR_VALUE].(int64)
	endId := startId + (int64)(step) - 1

	seq := proto.NewLongMargin(startId, endId)
	return seq, nil
}

func InsertAlarm(alarmId int64, servInstId, servType, instId, cmptName string, alarmType int, alarmTime int64) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Insert(dbPool, &SQL_INS_ALARM, alarmId, servInstId, servType, instId, cmptName, alarmType, alarmTime)
	if err != nil {
		errMsg := fmt.Sprintf("InsertAlarm fail, servInstId: %s, instId: %s, cmptName: %s, alarmType: %d", servInstId, instId, cmptName, alarmType)
		utils.LOGGER.Error(errMsg)
	}

	return err == nil
}

func UpdateAlarmStateByAlarmId(alarmId int64, dealTime int64, dealUser, dealFlag string, paasResult *result.ResultBean) bool {
	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &SQL_UPD_ALARM_STATE_BY_ALARMID, dealTime, dealUser, dealFlag, alarmId)
	if err != nil {
		errMsg := fmt.Sprintf("UpdateAlarmStateByAlarmId fail, alarmId: %d, dealFlag: %s", alarmId, dealFlag)
		utils.LOGGER.Error(errMsg)
	}

	return err == nil
}
