package meta

import (
	"fmt"
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
	"github.com/maoge/paas-metasvr-go/pkg/utils/multimap/setmultimap"
)

var CMPT_META = &CmptMeta{}

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
	CMPT_META.init()
}

func (m *CmptMeta) init() {
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
}

func (m *CmptMeta) reloadMetaData(dataType string) {
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
	accSlice, err := dao.LoadAccount()
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
	m.metaServRootMap[consts.SERV_TYPE_SMS_GATEWAY] = "SMS_GATEWAY_SERV_CONTAINER"
	m.metaServRootMap[consts.SERV_TYPE_SMS_QUERY] = "SMS_QUERY_SERV_CONTAINER"
}

func (m *CmptMeta) loadMetaAttr() {
	attrSlice, err := dao.LoadMetaAttr()
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
	cmptSlice, err := dao.LoadMetaCmpt()
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
	cmptAttrSlice, err := dao.LoadMetaCmptAttr()
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
	instSlice, err := dao.LoadMetaInst()
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
	instAttrSlice, err := dao.LoadMetaInstAttr()
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
	serviceSlice, err := dao.LoadMetaService()
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
	topoSlice, err := dao.LoadMetaTopo()
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
	deployHostSlice, err := dao.LoadDeployHost()
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
	deployFileSlice, err := dao.LoadDeployFile()
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
	serverSlice, err := dao.LoadMetaServer()
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
	sshSlice, err := dao.LoadMetaSsh()
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
	cmptVerSlice, err := dao.LoadMetaCmptVersion()
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
	return m.accountMap[user]
}

func (m *CmptMeta) GetAccSession(user string) *proto.AccountSession {
	return m.accSessionMap[user]
}