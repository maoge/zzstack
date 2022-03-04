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

	metaServRootMap  map[string]string
	metaAttrIdMap    map[int]*proto.PaasMetaAttr
	metaAttrNameMap  map[string]*proto.PaasMetaAttr
	metaCmptIdMap    map[int]*proto.PaasMetaCmpt
	metaCmptNameMap  map[string]*proto.PaasMetaCmpt
	metaCmptAttrMMap *setmultimap.MultiMap
	metaInstMap      map[string]*proto.PaasInstance
	metaInstAttrMMap *setmultimap.MultiMap
	metaServiceMap   map[string]*proto.PaasService

	// private Map<String,  PaasService>      metaServiceMap;
	// private Multimap<String, PaasTopology> metaTopoMMap;
	// private Map<Integer, PaasDeployHost>   metaDeployHostMap;
	// private Map<Integer, PaasDeployFile>   metaDeployFileMap;
	// private Map<String,  PaasServer>       metaServerMap;
	// private Multimap<String, PaasSsh>      metaSshMMap;
	// private Map<String,  PaasCmptVer>      metaCmptVerMap;

}

func InitGlobalCmptMeta() {
	CMPT_META.Init()
}

func (m *CmptMeta) Init() {
	m.release()

	m.loadAccount()
	m.loadMetaServRoot()
	m.loadMetaAttr()
	m.loadMetaCmpt()
	m.loadMetaCmptAttr()
	m.loadMetaInst()
	m.loadMetaInstAttr()
	m.loadMetaService()
}

func (m *CmptMeta) release() {
	m.mut.Lock()
	defer m.mut.Unlock()

	m.accountMap = make(map[string]*proto.Account)
	m.accSessionMap = make(map[string]*proto.AccountSession)
	m.magicKeyMap = make(map[string]*proto.AccountSession)

	m.metaServRootMap = make(map[string]string)
	m.metaAttrIdMap = make(map[int]*proto.PaasMetaAttr)
	m.metaAttrNameMap = make(map[string]*proto.PaasMetaAttr)
	m.metaCmptIdMap = make(map[int]*proto.PaasMetaCmpt)
	m.metaCmptNameMap = make(map[string]*proto.PaasMetaCmpt)
	m.metaCmptAttrMMap = setmultimap.New()
	m.metaInstMap = make(map[string]*proto.PaasInstance)
	m.metaInstAttrMMap = setmultimap.New()
	m.metaServiceMap = make(map[string]*proto.PaasService)
}

func (m *CmptMeta) loadAccount() {
	accSlice, err := dao.LoadAccount()
	if err == nil {
		m.mut.Lock()
		defer m.mut.Unlock()

		for _, item := range accSlice {
			m.accountMap[item.ACC_NAME] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadAccount error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}

func (m *CmptMeta) loadMetaServRoot() {
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

		for _, item := range serviceSlice {
			m.metaServiceMap[item.INST_ID] = item
		}
	} else {
		errMsg := fmt.Sprintf("loadMetaService error: %v", err.Error())
		utils.LOGGER.Error(errMsg)
	}
}
