package metadao

import (
	"fmt"

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
			errInfo := fmt.Sprintf("service: %v, service_name: %v, is_deployed: %v, is_product: %v", instId, serv.SERV_NAME, serv.IsDeployed(), serv.IsProduct())
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
	// TODO
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
			// node.put("text", cmpt.getCmptNameCn() + " (" + instance.getInstId() + ")");
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
