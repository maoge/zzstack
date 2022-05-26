package meta

import (
	"fmt"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
	"github.com/maoge/paas-metasvr-go/pkg/utils/multimap/setmultimap"
)

var (
	CMPT_META    *CmptMeta = nil
	cmpt_barrier sync.Once
)

type CmptMeta struct {
	mut sync.Mutex

	accountMap    map[string]*proto.Account
	accSessionMap map[string]*proto.AccountSession
	magicKeyMap   map[string]*proto.AccountSession

	metaServRootMap   map[string]string
	metaAttrIdMap     map[int]*proto.PaasMetaAttr
	metaAttrNameMap   map[string]*proto.PaasMetaAttr
	metaCmptIdMap     map[int]*proto.PaasMetaCmpt
	metaCmptNameMap   map[string]*proto.PaasMetaCmpt
	metaCmptAttrMMap  *setmultimap.MultiMap
	metaInstMap       map[string]*proto.PaasInstance
	metaInstAttrMMap  *setmultimap.MultiMap
	metaServiceMap    map[string]*proto.PaasService
	metaTopoMMap      *setmultimap.MultiMap
	metaDeployHostMap map[int]*proto.PaasDeployHost
	metaDeployFileMap map[int]*proto.PaasDeployFile
	metaServerMap     map[string]*proto.PaasServer
	metaSshMMap       *setmultimap.MultiMap
	metaCmptVerMap    map[string]*proto.PaasCmptVer
}

func InitGlobalCmptMeta() {
	cmpt_barrier.Do(func() {
		CMPT_META = new(CmptMeta)
		CMPT_META.Init()
	})
}

func (m *CmptMeta) Init() {
	m.loadAccount()
	m.loadMetaServRoot()
	m.loadMetaAttr()
	m.loadMetaCmpt()
	m.loadMetaCmptAttr()
	m.loadMetaInst()
	m.loadMetaInstAttr()
	m.loadMetaService()
	m.loadMetaTopo()
	m.loadDeployHost()
	m.loadDeployFile()
	m.loadMetaServer()
	m.loadMetaSsh()
	m.loadMetaCmptVersion()

	m.accSessionMap = make(map[string]*proto.AccountSession)
	m.magicKeyMap = make(map[string]*proto.AccountSession)
}

func (m *CmptMeta) ReloadMetaData(dataType string) {
	switch dataType {
	case consts.HEADER_ALL:
		m.reloadAll()
	case consts.HEADER_META_SERVICE:
		m.loadMetaService()
	case consts.HEADER_META_ATTR:
		m.loadMetaAttr()
	case consts.HEADER_META_CMPT:
		m.loadMetaCmpt()
	case consts.HEADER_META_CMPT_ATTR:
		m.loadMetaCmptAttr()
	case consts.HEADER_META_META_INST:
		m.loadMetaInst()
		m.loadMetaInstAttr()
	case consts.HEADER_META_TOPO:
		m.loadMetaTopo()
	case consts.HEADER_META_DEPLOY:
		m.loadDeployHost()
		m.loadDeployFile()
	case consts.HEADER_META_SERVER_SSH:
		m.loadMetaServer()
		m.loadMetaSsh()
	case consts.HEADER_META_CMPT_VERSION:
		m.loadMetaCmptVersion()
	}
}

func (m *CmptMeta) reloadAll() {
	m.loadMetaService()
	m.loadMetaAttr()
	m.loadMetaCmpt()
	m.loadMetaCmptAttr()
	m.loadMetaInst()
	m.loadMetaInstAttr()
	m.loadMetaTopo()
	m.loadDeployHost()
	m.loadDeployFile()
	m.loadMetaServer()
	m.loadMetaSsh()
	m.loadMetaCmptVersion()
}

func (m *CmptMeta) loadAccount() {
	accSlice, err := LoadAccount()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.accountMap = make(map[string]*proto.Account)

		for _, item := range accSlice {
			m.accountMap[item.ACC_NAME] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadAccount error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaServRoot() {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaServRootMap = make(map[string]string)

	m.metaServRootMap[consts.SERV_TYPE_CACHE_REDIS_CLUSTER] = "REDIS_SERV_CLUSTER_CONTAINER"

	m.metaServRootMap[consts.SERV_TYPE_CACHE_REDIS_MASTER_SLAVE] = "REDIS_SERV_MS_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_CACHE_REDIS_HA_CLUSTER] = "REDIS_HA_CLUSTER_CONTAINER"

	m.metaServRootMap[consts.SERV_TYPE_DB_TDENGINE] = "TDENGINE_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_DB_ORACLE_DG] = "ORACLE_DG_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_DB_TIDB] = "TIDB_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_DB_CLICKHOUSE] = "CLICKHOUSE_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_DB_VOLTDB] = "VOLTDB_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_DB_YUGABYTEDB] = "YUGABYTEDB_SERV_CONTAINER"

	m.metaServRootMap[consts.SERV_TYPE_MQ_ROCKETMQ] = "ROCKETMQ_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_MQ_PULSAR] = "PULSAR_SERV_CONTAINER"

	m.metaServRootMap[consts.SERV_TYPE_SERVERLESS_APISIX] = "APISIX_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_STORE_MINIO] = "MINIO_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_SMS_GATEWAY] = "SMS_GATEWAY_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_SMS_QUERY] = "SMS_QUERY_SERV_CONTAINER"
}

func (m *CmptMeta) loadMetaAttr() {
	attrSlice, err := LoadMetaAttr()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaAttrIdMap = make(map[int]*proto.PaasMetaAttr)
		m.metaAttrNameMap = make(map[string]*proto.PaasMetaAttr)

		for _, item := range attrSlice {
			m.metaAttrIdMap[item.ATTR_ID] = item
			m.metaAttrNameMap[item.ATTR_NAME] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaAttr error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaCmpt() {
	cmptSlice, err := LoadMetaCmpt()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaCmptIdMap = make(map[int]*proto.PaasMetaCmpt)
		m.metaCmptNameMap = make(map[string]*proto.PaasMetaCmpt)

		for _, item := range cmptSlice {
			m.metaCmptIdMap[item.CMPT_ID] = item
			m.metaCmptNameMap[item.CMPT_NAME] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaCmpt error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaCmptAttr() {
	cmptAttrSlice, err := LoadMetaCmptAttr()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaCmptAttrMMap = setmultimap.New()

		for _, item := range cmptAttrSlice {
			m.metaCmptAttrMMap.Put(item.CMPT_ID, item.ATTR_ID)
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaCmptAttr error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaInst() {
	instSlice, err := LoadMetaInst()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaInstMap = make(map[string]*proto.PaasInstance)

		for _, item := range instSlice {
			m.metaInstMap[item.INST_ID] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaInst error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaInstAttr() {
	instAttrSlice, err := LoadMetaInstAttr()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaInstAttrMMap = setmultimap.New()

		for _, item := range instAttrSlice {
			m.metaInstAttrMMap.Put(item.INST_ID, item)
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaInstAttr error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaService() {
	serviceSlice, err := LoadMetaService()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaServiceMap = make(map[string]*proto.PaasService)

		for _, item := range serviceSlice {
			m.metaServiceMap[item.INST_ID] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaService error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaTopo() {
	topoSlice, err := LoadMetaTopo()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaTopoMMap = setmultimap.New()

		for _, item := range topoSlice {
			m.metaTopoMMap.Put(item.INST_ID1, item)
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaTopo error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadDeployHost() {
	deployHostSlice, err := LoadDeployHost()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaDeployHostMap = make(map[int]*proto.PaasDeployHost)

		for _, item := range deployHostSlice {
			m.metaDeployHostMap[item.HOST_ID] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadDeployHost error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadDeployFile() {
	deployFileSlice, err := LoadDeployFile()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaDeployFileMap = make(map[int]*proto.PaasDeployFile)

		for _, item := range deployFileSlice {
			m.metaDeployFileMap[item.FILE_ID] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadDeployFile error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaServer() {
	serverSlice, err := LoadMetaServer()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaServerMap = make(map[string]*proto.PaasServer)

		for _, item := range serverSlice {
			m.metaServerMap[item.SERVER_IP] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaServer error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaSsh() {
	sshSlice, err := LoadMetaSsh()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaSshMMap = setmultimap.New()

		for _, item := range sshSlice {
			m.metaSshMMap.Put(item.SERVER_IP, item)
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaSsh error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaCmptVersion() {
	cmptVerSlice, err := LoadMetaCmptVersion()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		m.metaCmptVerMap = make(map[string]*proto.PaasCmptVer)

		for _, item := range cmptVerSlice {
			v, ok := m.metaCmptVerMap[item.SERV_TYPE]
			if ok {
				v.AddVersion(item.VERSION)
			} else {
				item.VERSION = ""
				m.metaCmptVerMap[item.SERV_TYPE] = item
			}
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaCmptVersion error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) GetAccount(user string) *proto.Account {
	m.mut.Lock()
	defer m.mut.Unlock()

	return m.accountMap[user]
}

func (m *CmptMeta) ModPasswd(accName, passwd string) {
	account, found := m.accountMap[accName]
	if found {
		account.PASSWD = passwd
	}
}

func (m *CmptMeta) GetAccSession(user string) *proto.AccountSession {
	m.mut.Lock()
	defer m.mut.Unlock()

	return m.accSessionMap[user]
}

func (m *CmptMeta) AddAccSession(accSession *proto.AccountSession, isLocalOnly bool) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.accSessionMap[accSession.ACC_NAME] = accSession
	m.magicKeyMap[accSession.MAGIC_KEY] = accSession

	if !isLocalOnly {
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_ACC_NAME] = accSession.ACC_NAME
		msgBodyMap[consts.HEADER_MAGIC_KEY] = accSession.MAGIC_KEY
		msgBodyMap[consts.HEADER_SESSION_TIMEOUT] = accSession.SESSION_TIMEOUT

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_ADD_SESSON.CODE, msgBody, "") // EVENT_ADD_SESSON - 10020

		eventbus.EVENTBUS.PublishEvent(event)
		redisdao.PutSessionToRedis(accSession)
	}
}

func (m *CmptMeta) RemoveTtlSession(accName, magicKey string, isLocalOnly bool) {
	m.mut.Lock()
	defer m.mut.Unlock()

	delete(m.accSessionMap, accName)
	delete(m.magicKeyMap, magicKey)

	if !isLocalOnly {
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_ACC_NAME] = accName
		msgBodyMap[consts.HEADER_MAGIC_KEY] = magicKey

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_REMOVE_SESSON.CODE, msgBody, "") // EVENT_REMOVE_SESSON - 10021

		eventbus.EVENTBUS.PublishEvent(event)
	}
}

func (m *CmptMeta) GetSessionByMagicKey(magicKey string) *proto.AccountSession {
	m.mut.Lock()
	defer m.mut.Unlock()

	return m.magicKeyMap[magicKey]
}

func (m *CmptMeta) GetAccNameByMagicKey(magicKey string) string {
	m.mut.Lock()
	defer m.mut.Unlock()

	accSession := m.magicKeyMap[magicKey]
	if accSession != nil {
		return accSession.ACC_NAME
	} else {
		return ""
	}
}

func (m *CmptMeta) GetMetaData2Json() interface{} {
	m.mut.Lock()
	defer m.mut.Unlock()

	var metaMap map[string]interface{}
	metaMap = make(map[string]interface{})

	metaMap["metaServRootMap"] = utils.Struct2Json(m.metaServRootMap)
	metaMap["metaAttrIdMap"] = utils.Struct2Json(m.metaAttrIdMap)
	metaMap["metaCmptIdMap"] = utils.Struct2Json(m.metaCmptIdMap)
	metaMap["metaCmptAttrMMap"] = utils.Struct2Json(m.metaCmptAttrMMap)
	metaMap["metaInstMap"] = utils.Struct2Json(m.metaInstMap)
	metaMap["metaInstAttrMMap"] = utils.Struct2Json(m.metaInstAttrMMap)
	metaMap["metaServiceMap"] = utils.Struct2Json(m.metaServiceMap)
	metaMap["metaTopoMMap"] = utils.Struct2Json(m.metaTopoMMap)
	metaMap["metaDeployHostMap"] = utils.Struct2Json(m.metaDeployHostMap)
	metaMap["metaDeployFileMap"] = utils.Struct2Json(m.metaDeployFileMap)
	metaMap["metaServerMap"] = utils.Struct2Json(m.metaServerMap)
	metaMap["metaSshMMap"] = utils.Struct2Json(m.metaSshMMap)
	metaMap["metaCmptVerMap"] = utils.Struct2Json(m.metaCmptVerMap)

	return &metaMap
}

func (m *CmptMeta) GetInstRelations(servInstId string, relations *[]*proto.PaasTopology) {
	m.mut.Lock()
	defer m.mut.Unlock()

	dataArr, found := m.metaTopoMMap.Get(servInstId)
	if !found {
		return
	}

	size := len(dataArr) - 1
	idx := 0
	for {
		if idx > size {
			break
		}

		item := dataArr[idx].(*proto.PaasTopology)
		idx++

		*relations = append(*relations, item)
	}
}

func (m *CmptMeta) GetInstCmptName(instId string) string {
	m.mut.Lock()
	defer m.mut.Unlock()

	instRef := m.metaInstMap[instId]
	if instRef == nil {
		return ""
	}

	cmptRef := m.metaCmptIdMap[instRef.CMPT_ID]
	if cmptRef == nil {
		return ""
	}

	return cmptRef.CMPT_NAME
}

func (m *CmptMeta) GetInstance(instId string) *proto.PaasInstance {
	m.mut.Lock()
	defer m.mut.Unlock()

	return m.metaInstMap[instId]
}

func (m *CmptMeta) DelInstance(instId string) {
	delete(m.metaInstMap, instId)
}

func (m *CmptMeta) GetCmptById(cmptId int) *proto.PaasMetaCmpt {
	return m.metaCmptIdMap[cmptId]
}

func (m *CmptMeta) GetCmptByName(cmptName string) *proto.PaasMetaCmpt {
	return m.metaCmptNameMap[cmptName]
}

func (m *CmptMeta) GetInstanceCmpt(instID string) *proto.PaasMetaCmpt {
	instance, found := m.metaInstMap[instID]
	if !found {
		return nil
	}

	return m.metaCmptIdMap[instance.CMPT_ID]
}

func (m *CmptMeta) GetCmptAttrs(cmptId int) []*proto.PaasMetaAttr {
	attrSlice := make([]*proto.PaasMetaAttr, 0)

	m.mut.Lock()
	defer m.mut.Unlock()

	attrIdList, found := m.metaCmptAttrMMap.Get(cmptId)
	if !found {
		return attrSlice
	}

	for _, attrIdRaw := range attrIdList {
		attrId := attrIdRaw.(int)
		data, found := m.metaAttrIdMap[attrId]
		if found {
			attrSlice = append(attrSlice, data)
		}
	}

	return attrSlice
}

func (m *CmptMeta) GetInstAttr(instId string, attrId int) *proto.PaasInstAttr {
	m.mut.Lock()
	defer m.mut.Unlock()

	dataArr, found := m.metaInstAttrMMap.Get(instId)
	if !found {
		return nil
	}

	var result *proto.PaasInstAttr
	for _, rawItem := range dataArr {
		item := rawItem.(*proto.PaasInstAttr)
		if item.ATTR_ID == attrId {
			result = item
			break
		}
	}

	return result
}

func (m *CmptMeta) GetInstAttrs(instId string) []*proto.PaasInstAttr {
	m.mut.Lock()
	defer m.mut.Unlock()

	attrs := make([]*proto.PaasInstAttr, 0)
	values, found := m.metaInstAttrMMap.Get(instId)
	if !found {
		return attrs
	}

	for _, rawItem := range values {
		attr := rawItem.(*proto.PaasInstAttr)
		attrs = append(attrs, attr)
	}

	return attrs
}

func (m *CmptMeta) DelInstAttr(instId string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaInstAttrMMap.RemoveAll(instId)
}

func (m *CmptMeta) GetSshById(sshId string) *proto.PaasSsh {
	var result *proto.PaasSsh

	entries := m.metaSshMMap.Entries()
	for _, entry := range entries {
		ssh := entry.Value.(*proto.PaasSsh)
		if ssh.SSH_ID == sshId {
			result = ssh
		}
	}

	return result
}

func (m *CmptMeta) AddService(service *proto.PaasService) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaServiceMap[service.INST_ID] = service
}

func (m *CmptMeta) GetService(instId string) *proto.PaasService {
	return m.metaServiceMap[instId]
}

func (m *CmptMeta) DelService(instId string) {
	delete(m.metaServiceMap, instId)
}

func (m *CmptMeta) DelParentTopo(parentId string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaTopoMMap.RemoveAll(parentId)
}

func (m *CmptMeta) DelTopo(parentId, instId string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaTopoMMap.RemoveAll(instId)

	parentSubs, found := m.metaTopoMMap.Get(parentId)
	if !found {
		return
	}

	for _, rawItem := range parentSubs {
		topo := rawItem.(*proto.PaasTopology)
		toeId := topo.GetToe(parentId)
		if toeId == instId {
			m.metaTopoMMap.Remove(parentId, topo)
			break
		}
	}
}

func (m *CmptMeta) IsServRootCmpt(servType, cmptName string) bool {
	servRootCmptName := m.metaServRootMap[servType]
	if servRootCmptName == "" {
		return false
	}

	return servRootCmptName == cmptName
}

func (m *CmptMeta) IsInstServRootCmpt(instID string) bool {
	inst := m.metaInstMap[instID]
	if inst == nil {
		return false
	}

	cmptID := inst.CMPT_ID
	cmpt := m.metaCmptIdMap[cmptID]
	if cmpt == nil {
		return false
	}

	return m.IsServRootCmpt(cmpt.SERV_TYPE, cmpt.CMPT_NAME)
}

func (m *CmptMeta) ReloadService(instID string) {
	service, err := GetServiceById(instID)
	if err != nil {
		m.metaServiceMap[instID] = service
	} else {
		errMsg := fmt.Sprintf("ReloadService: %s, error: %v", instID, err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) IsServerIpExists(servIp string) bool {
	m.mut.Lock()
	defer m.mut.Unlock()

	_, found := m.metaServerMap[servIp]
	return found
}

func (m *CmptMeta) IsServerNull(servIp string) bool {
	m.mut.Lock()
	defer m.mut.Unlock()

	value, found := m.metaSshMMap.Get(servIp)
	if !found {
		return true
	}

	if len(value) > 0 {
		return false
	} else {
		return true
	}
}

func (m *CmptMeta) AddServer(server *proto.PaasServer) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaServerMap[server.SERVER_IP] = server
}

func (m *CmptMeta) DelServer(serverIp string) {
	delete(m.metaServerMap, serverIp)
}

func (m *CmptMeta) IsSshExists(sshName, servIp, servClazz string) bool {
	m.mut.Lock()
	defer m.mut.Unlock()

	values, found := m.metaSshMMap.Get(servIp)
	if !found {
		return false
	}

	for _, rawItem := range values {
		item := rawItem.(*proto.PaasSsh)
		if item.SSH_NAME == sshName && item.SERV_CLAZZ == servClazz {
			return true
		}
	}

	return false
}

func (m *CmptMeta) AddSsh(ssh *proto.PaasSsh) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaSshMMap.Put(ssh.SERVER_IP, ssh)
}

func (m *CmptMeta) ModSsh(serverIp string, sshId string, sshName string, sshPwd string, sshPort int) {
	m.mut.Lock()
	defer m.mut.Unlock()

	values, found := m.metaSshMMap.Get(serverIp)
	if !found {
		return
	}

	for _, rawItem := range values {
		ssh := rawItem.(*proto.PaasSsh)
		if ssh.SSH_ID == sshId {
			ssh.SSH_NAME = sshName
			ssh.SSH_PWD = sshPwd
			ssh.SSH_PORT = sshPort
			break
		}
	}
}

func (m *CmptMeta) DelSsh(servIp, sshId string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	values, found := m.metaSshMMap.Get(servIp)
	if !found {
		return
	}

	for _, rawItem := range values {
		ssh := rawItem.(*proto.PaasSsh)
		if ssh.SSH_ID == sshId {
			m.metaSshMMap.Remove(servIp, ssh)
			break
		}
	}
}

func (m *CmptMeta) IsSshUsing(sshId string) bool {
	m.mut.Lock()
	defer m.mut.Unlock()

	values := m.metaInstAttrMMap.Values()
	for _, value := range values {
		attr := value.(*proto.PaasInstAttr)

		// 116 -> 'SSH_ID'
		if attr.ATTR_ID == 116 && attr.ATTR_VALUE == sshId {
			return true
		}
	}

	return false
}

func (m *CmptMeta) GetSurpportSSHList(servClazz string) []map[string]interface{} {
	m.mut.Lock()
	defer m.mut.Unlock()

	res := make([]map[string]interface{}, 0)
	for serverIP := range m.metaServerMap {
		sshList, found := m.metaSshMMap.Get(serverIP)
		if !found {
			continue
		}

		var subSSH []map[string]string
		for _, rawSsh := range sshList {
			ssh := rawSsh.(*proto.PaasSsh)
			if ssh.SERV_CLAZZ != servClazz {
				continue
			}

			if subSSH == nil {
				subSSH = make([]map[string]string, 0)
			}

			item := make(map[string]string)
			item[consts.HEADER_SSH_NAME] = ssh.SSH_NAME
			item[consts.HEADER_SSH_ID] = ssh.SSH_ID

			subSSH = append(subSSH, item)
		}

		if subSSH != nil {
			node := make(map[string]interface{})
			node[consts.HEADER_SERVER_IP] = serverIP
			node[consts.HEADER_SSH_LIST] = subSSH
			res = append(res, node)
		}
	}

	return res
}

func (m *CmptMeta) GetServListFromCache(servType string) []map[string]interface{} {
	m.mut.Lock()
	defer m.mut.Unlock()

	res := make([]map[string]interface{}, 0)
	for servInstId, service := range m.metaServiceMap {
		// 未部署服务不加入可用服务列表
		if !service.IsDeployed() {
			continue
		}

		instance := m.metaInstMap[servInstId]
		_, found := m.metaTopoMMap.Get(servInstId)

		// 只录了服务还未做初始化
		if instance == nil || !found {
			continue
		}

		if service.SERV_TYPE == servType {
			node := make(map[string]interface{})
			node[consts.HEADER_SERV_NAME] = service.SERV_NAME
			node[consts.HEADER_INST_ID] = service.INST_ID

			res = append(res, node)
		}
	}

	return res
}

func (m *CmptMeta) GetServRootCmpt(servType string) string {
	return m.metaServRootMap[servType]
}

func (m *CmptMeta) GetServTypeVerList(result *map[string]interface{}) {
	cmptVerMap := make(map[string]interface{})
	for key, value := range m.metaCmptVerMap {
		cmptVerMap[key] = value.ToJsonMap()
	}
	(*result)[consts.HEADER_RET_CODE] = consts.REVOKE_OK
	(*result)["metaCmptVerMap"] = cmptVerMap
}

func (m *CmptMeta) GetServTypeListFromLocalCache() []string {
	servMap := make(map[string]bool)
	for key := range m.metaCmptVerMap {
		servMap[key] = true
	}

	servSlice := make([]string, 0)
	for key := range servMap {
		servSlice = append(servSlice, key)
	}

	return servSlice
}

func (m *CmptMeta) UpdInstPos(inst *proto.PaasInstance) {
	instPtr := m.metaInstMap[inst.INST_ID]
	if instPtr != nil {
		instPtr.POS_X = inst.POS_X
		instPtr.POS_Y = inst.POS_Y
		instPtr.WIDTH = inst.WIDTH
		instPtr.HEIGHT = inst.HEIGHT
		instPtr.ROW = inst.ROW
		instPtr.COL = inst.COL
	}
}

func (m *CmptMeta) AddInstance(instance *proto.PaasInstance) {
	m.metaInstMap[instance.INST_ID] = instance
}

func (m *CmptMeta) AddInstAttr(instAttr *proto.PaasInstAttr) {
	m.mut.Lock()
	defer m.mut.Unlock()

	attrCollection, found := m.metaInstAttrMMap.Get(instAttr.INST_ID)
	attrId := instAttr.ATTR_ID

	var attrOld *proto.PaasInstAttr = nil
	if found {
		for _, attrRaw := range attrCollection {
			attr := attrRaw.(*proto.PaasInstAttr)
			if attr.ATTR_ID == attrId {
				attrOld = attr
				break
			}
		}
	}

	if attrOld == nil {
		m.metaInstAttrMMap.Put(instAttr.INST_ID, instAttr)
	} else {
		attrOld.ATTR_VALUE = instAttr.ATTR_VALUE
	}

}

func (m *CmptMeta) AddTopo(topo *proto.PaasTopology) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaTopoMMap.Put(topo.INST_ID1, topo)
}

func (m *CmptMeta) ModTopo(topo *proto.PaasTopology) {
	m.mut.Lock()
	defer m.mut.Unlock()

	parentID := topo.INST_ID1
	inst := m.metaInstMap[parentID]
	if inst != nil && inst.CMPT_ID == 801 { // 'HA_CONTAINER'
		m.metaTopoMMap.RemoveAll(parentID)
	}

	m.metaTopoMMap.Put(parentID, topo)
}

func (m *CmptMeta) DelAllSubTopo(parentId string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.metaTopoMMap.RemoveAll(parentId)
}

func (m *CmptMeta) IsTopoRelationExists(parentId, subId string) bool {
	res := false
	m.mut.Lock()
	defer m.mut.Unlock()

	topos, found := m.metaTopoMMap.Get(parentId)
	if !found {
		return false
	}

	for _, topoRaw := range topos {
		topo := topoRaw.(*proto.PaasTopology)
		if topo.INST_ID2 == subId {
			res = true
			break
		}
	}

	return res
}

func (m *CmptMeta) IsTopoExists(parentId string) bool {
	m.mut.Lock()
	defer m.mut.Unlock()

	topos, found := m.metaTopoMMap.Get(parentId)
	if !found {
		return false
	}

	return len(topos) > 0
}

func (m *CmptMeta) IsInstAttrExists(instId string, attrId int) bool {
	var res bool = false
	m.mut.Lock()
	defer m.mut.Unlock()

	attrs, found := m.metaInstAttrMMap.Get(instId)
	if !found {
		return false
	}

	for _, attrRaw := range attrs {
		attr := attrRaw.(*proto.PaasInstAttr)
		if attr.ATTR_ID == attrId {
			res = true
			break
		}
	}

	return res
}

func (m *CmptMeta) UpdInstAttr(instAttr *proto.PaasInstAttr) {
	m.mut.Lock()
	defer m.mut.Unlock()

	attrId := instAttr.ATTR_ID
	attrArr, found := m.metaInstAttrMMap.Get(instAttr.INST_ID)
	if !found {
		return
	}

	for _, attrRaw := range attrArr {
		attr := attrRaw.(*proto.PaasInstAttr)
		if attr.ATTR_ID == attrId {
			attr.ATTR_VALUE = instAttr.ATTR_VALUE
			break
		}
	}
}

func (m *CmptMeta) UpdInstPreEmbadded(instId, preEmbadded string) {
	m.mut.Lock()
	defer m.mut.Unlock()

	instAttrRef := m.GetInstAttr(instId, 320) // 320 -> 'PRE_EMBEDDED'
	if instAttrRef == nil {
		return
	}

	instAttrRef.ATTR_VALUE = preEmbadded
}

func (m *CmptMeta) GetAttr(attrId int) *proto.PaasMetaAttr {
	return m.metaAttrIdMap[attrId]
}

func (m *CmptMeta) GetSameLevelInstList(servInstId, instId string) []*proto.PaasTopology {
	m.mut.Lock()
	defer m.mut.Unlock()

	dataArr, found := m.metaTopoMMap.Get(servInstId)
	if !found {
		return nil
	}

	list := make([]*proto.PaasTopology, 0)
	for _, rawItem := range dataArr {
		item := rawItem.(*proto.PaasTopology)
		list = append(list, item)
	}

	return list
}

func (m *CmptMeta) AdjustSmsABQueueWeightInfo(instAId, weightA, instBId, weightB string) {
	// 141 -> 'WEIGHT'
	attrA := m.GetInstAttr(instAId, 141)
	attrB := m.GetInstAttr(instBId, 141)

	if attrA == nil || attrB == nil {
		return
	}

	attrA.ATTR_VALUE = weightA
	attrB.ATTR_VALUE = weightB
}

func (m *CmptMeta) SwitchSmsDBType(dgContainerID, dbType string) {
	// 225 -> 'ACTIVE_DB_TYPE'
	attr := m.GetInstAttr(dgContainerID, 225)
	attr.ATTR_VALUE = dbType
}

func (m *CmptMeta) AddCmptVersion(servType, version string) {
	cmptVer := m.metaCmptVerMap[servType]
	if cmptVer == nil {
		cmptVer := proto.NewPaasCmptVer(servType, version)
		m.metaCmptVerMap[servType] = cmptVer
	} else {
		cmptVer.AddVersion(version)
	}
}

func (m *CmptMeta) DelCmptVersion(servType, version string) {
	cmptVer := m.metaCmptVerMap[servType]
	if cmptVer != nil {
		cmptVer.DelVersion(version)
	}
}

func (m *CmptMeta) IsCmptVersionExist(servType, version string) bool {
	cmptVer := m.metaCmptVerMap[servType]
	if cmptVer == nil {
		return false
	}

	return cmptVer.IsVersionExist(version)
}

func (m *CmptMeta) UpdInstDeploy(instId, deployFlag string) {
	ref := m.metaInstMap[instId]
	if ref == nil {
		return
	}

	ref.STATUS = deployFlag
}

func (m *CmptMeta) UpdServDeploy(servInstId, deployFlag string) {
	ref := m.metaServiceMap[servInstId]
	if ref == nil {
		return
	}

	ref.IS_DEPLOYED = deployFlag
}

func (m *CmptMeta) GetDeployFile(fileId int) *proto.PaasDeployFile {
	return m.metaDeployFileMap[fileId]
}

func (m *CmptMeta) GetDeployHost(hostId int) *proto.PaasDeployHost {
	return m.metaDeployHostMap[hostId]
}
