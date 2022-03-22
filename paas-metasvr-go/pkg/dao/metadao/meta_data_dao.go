package metadao

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	crud "github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
	"github.com/tidwall/gjson"

	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
)

var (
	SQL_COUNT_SERVICE_LIST       = "SELECT count(1) COUNT FROM t_meta_service WHERE 1=1 %s"
	SQL_SEL_SERVICE_LIST         = "SELECT INST_ID, SERV_NAME, SERV_TYPE, SERV_CLAZZ, VERSION, IS_DEPLOYED, IS_PRODUCT FROM t_meta_service WHERE 1=1 %s ORDER BY CREATE_TIME limit ?, ?"
	SQL_COUNT_SERV_TYPE_VER_LIST = "SELECT count(1) COUNT FROM t_meta_cmpt_versions WHERE 1=1 %s"
	SQL_SEL_SERV_TYPE_VER_LIST   = "SELECT SERV_TYPE, VERSION FROM t_meta_cmpt_versions WHERE 1=1 %s ORDER BY SERV_TYPE, VERSION limit ?, ?"
	SQL_ADD_SERVICE              = "INSERT INTO t_meta_service(INST_ID, SERV_NAME, SERV_CLAZZ, SERV_TYPE, VERSION, IS_DEPLOYED, IS_PRODUCT, CREATE_TIME, USER, PASSWORD, PSEUDO_DEPLOY_FLAG) VALUES(?,?,?,?,?,?,?,?,?,?,?)"
	SQL_DEL_TOPOLOGY             = "DELETE FROM t_meta_topology WHERE (INST_ID1 = ? AND INST_ID2 = ?) OR (INST_ID2 = ? and TOPO_TYPE = 2)"
	SQL_DEL_INSTANCE_ATTR        = "DELETE FROM t_meta_instance_attr WHERE INST_ID = ?"
	SQL_DEL_INSTANCE             = "DELETE FROM t_meta_instance WHERE INST_ID = ?"
	SQL_DEL_SERVICE              = "DELETE FROM t_meta_service WHERE inst_id = ?"
	SQL_MOD_SERVICE              = "UPDATE t_meta_service SET SERV_NAME = ?, VERSION = ?, IS_PRODUCT = ? WHERE INST_ID = ?"
	SQL_MOD_SERVICE_VERSION      = "UPDATE t_meta_service SET VERSION = ? WHERE INST_ID = ?"
	SQL_COUNT_SERVER_LIST        = "SELECT count(1) COUNT FROM t_meta_server WHERE 1=1 %s"
	SQL_SEL_SERVER_LIST          = "SELECT SERVER_IP, SERVER_NAME FROM t_meta_server WHERE 1=1 %s order by SERVER_IP limit ?, ?"
	SQL_ADD_SERVER               = "INSERT INTO t_meta_server(SERVER_IP, SERVER_NAME, CREATE_TIME) VALUES(?,?,?)"
	SQL_DEL_SERVER               = "DELETE FROM t_meta_server WHERE SERVER_IP=?"
	SQL_SSH_CNT_BY_IP            = "SELECT count(1) COUNT FROM t_meta_ssh WHERE SERVER_IP=? "
	SQL_SSH_LIST_BY_IP           = "SELECT SSH_NAME, SERV_CLAZZ, SSH_ID, SERVER_IP, SSH_PORT FROM t_meta_ssh WHERE SERVER_IP=? order by SSH_NAME limit ?, ?"
	SQL_ADD_SSH                  = "INSERT INTO t_meta_ssh(SSH_NAME,SSH_PWD,SSH_PORT,SERV_CLAZZ,SERVER_IP,SSH_ID) VALUES(?,?,?,?,?,?)"
	SQL_MOD_SSH                  = "UPDATE t_meta_ssh SET SSH_NAME=?, SSH_PWD=?, SSH_PORT=? WHERE SSH_ID=?"
	SQL_DEL_SSH                  = "DELETE FROM t_meta_ssh WHERE SSH_ID=?"
)

func GetServiceCount(getServiceCountParam *proto.GetServiceCountParam, resultBean *proto.ResultBean) {
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

func GetServiceList(getServiceListParam *proto.GetServiceListParam, resultBean *proto.ResultBean) {
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

func GetServTypeVerCount(getServTypeVerCountParam *proto.GetServTypeVerCountParam, resultBean *proto.ResultBean) {
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

func GetServTypeVerListByPage(param *proto.GetServTypeVerListByPageParam, resultBean *proto.ResultBean) {
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

func GetClickHouseDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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

func GetVoltDBDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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
			meta.CMPT_META.GetInstRelations(voltdbContainer.INST_ID, subRelations)
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

func GetRocketMQDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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

func GetTiDBDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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

func GetPulsarDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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

func GetYBDashboardAddr(param *proto.GetDashboardAddrParam, resultBean *proto.ResultBean) {
	servInstId := param.SERV_INST_ID

	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(servInstId, relations)

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
		meta.CMPT_META.GetInstRelations(ybMasterContainerID, subRelations)

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

func AddService(param *proto.AddServiceParam, instId string, magicKey string, resultBean *proto.ResultBean) {
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
		resultBean.RET_INFO = consts.ERR_DB
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

func DelService(instId string, magicKey string, resultBean *proto.ResultBean) {
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
	if enumDelService("", instId, childArr, resultBean, events, magicKey) {
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

func enumDelService(parentId string, instId string, subNodes []*proto.PaasNode, resultBean *proto.ResultBean, events []*proto.PaasEvent, magicKey string) bool {
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
		events = append(events, ev)
	}

	return true
}

func DelInstance(parentId string, instId string, resultBean *proto.ResultBean, events []*proto.PaasEvent, magicKey string) bool {
	relations := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, relations)

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
	events = append(events, evDelTopo)

	// 2.1 delete instance attribute
	crud.Delete(dbPool, &SQL_DEL_INSTANCE_ATTR, instId)

	// 2.2 broadcast event to cluster
	delInstAttrMap := make(map[string]interface{})
	delInstAttrMap[consts.HEADER_INST_ID] = instId
	delInstAttrBody := utils.Struct2Json(delInstAttrMap)

	evDelInstAttr := proto.NewPaasEvent(consts.EVENT_DEL_INST_ATTR.CODE, delInstAttrBody, magicKey)
	events = append(events, evDelInstAttr)

	// 3.1 delete instance
	crud.Delete(dbPool, &SQL_DEL_INSTANCE, instId)

	// 3.2 broadcast event to cluster
	delInstMap := make(map[string]interface{})
	delInstMap[consts.HEADER_INST_ID] = instId
	delInstBody := utils.Struct2Json(delInstMap)

	evDelInst := proto.NewPaasEvent(consts.EVENT_DEL_INSTANCE.CODE, delInstBody, magicKey)
	events = append(events, evDelInst)

	return true
}

func getChildNodeExcludingServRoot(instId string, subNodes []*proto.PaasNode) {
	topos := make([]*proto.PaasTopology, 0)
	meta.CMPT_META.GetInstRelations(instId, topos)
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

func ModService(param *proto.ModServiceParam, magicKey string, resultBean *proto.ResultBean) {
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

func ModServiceVersion(param *proto.ModServiceVersionParam, magicKey string, resultBean *proto.ResultBean) {
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

func GetServerCnt(param *proto.GetServerCountParam, resultBean *proto.ResultBean) {
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

func GetServerList(param *proto.GetServerListParam, resultBean *proto.ResultBean) {
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

func AddServer(param *proto.AddServerParam, magicKey string, resultBean *proto.ResultBean) {
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

func DelServer(servIp string, magicKey string, resultBean *proto.ResultBean) {
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

func GetSshCntByIp(servIp string, resultBean *proto.ResultBean) {
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

func GetSshListByIp(param *proto.GetSSHListByIPParam, resultBean *proto.ResultBean) {
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

func AddSsh(param *proto.AddSSHParam, magicKey string, resultBean *proto.ResultBean) {
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

func ModSSH(param *proto.ModSSHParam, magicKey string, resultBean *proto.ResultBean) {
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

func DelSSH(param *proto.DelSSHParam, magicKey string, resultBean *proto.ResultBean) {
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

func LoadServiceTopo(instId string, resultBean *proto.ResultBean) bool {
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
		if instance.ROW_ != consts.POS_DEFAULT_VALUE && instance.COL_ != consts.POS_DEFAULT_VALUE {
			pos["row"] = instance.ROW_
			pos["col"] = instance.COL_
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
			continue
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
	meta.CMPT_META.GetInstRelations(instId, relations)

	for _, topo := range relations {
		toeId := topo.GetToe(instId)
		subInst := meta.CMPT_META.GetInstance(toeId)
		if subInst == nil {
			continue
		}

		subCmpt := meta.CMPT_META.GetCmptById(subInst.CMPT_ID)
		if subCmpt == nil {
			continue
		}

		subCmptName := subCmpt.CMPT_NAME
		subNodeType := subCmpt.NODE_JSON_TYPE
		if subNodeType == consts.SCHEMA_ARRAY {
			subCmptSlice := (*attrMap)[subCmptName].([]map[string]interface{})
			tmpMap := make(map[string]interface{})
			if loadInstanceAttribute(toeId, &tmpMap, deployFlagArr) {
				subCmptSlice = append(subCmptSlice, tmpMap)
			} else {
				return false
			}
		} else {
			subCmptMap := (*attrMap)[subCmptName].(map[string]interface{})
			if !loadInstanceAttribute(toeId, &subCmptMap, deployFlagArr) {
				return false
			}
		}
	}

	return true
}
