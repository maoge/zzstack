package meta

import (
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/dao"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils/multimap/setmultimap"
)

type CmptMeta struct {
	mut sync.Mutex

	accountMap    map[string]proto.Account
	accSessionMap map[string]proto.AccountSession
	magicKeyMap   map[string]proto.AccountSession

	metaServRootMap  map[string]string
	metaAttrIdMap    map[uint32]proto.PaasMetaAttr
	metaAttrNameMap  map[string]proto.PaasMetaAttr
	metaCmptIdMap    map[uint32]proto.PaasMetaCmpt
	metaCmptNameMap  map[string]proto.PaasMetaCmpt
	metaCmptAttrMMap *setmultimap.MultiMap // private Multimap<Integer, Integer>     metaCmptAttrMMap;  // setmultimap.New()
	metaInstMap      map[string]proto.PaasInstance

	// private Multimap<String, PaasInstAttr> metaInstAttrMMap;
	// private Map<String,  PaasService>      metaServiceMap;
	// private Multimap<String, PaasTopology> metaTopoMMap;
	// private Map<Integer, PaasDeployHost>   metaDeployHostMap;
	// private Map<Integer, PaasDeployFile>   metaDeployFileMap;
	// private Map<String,  PaasServer>       metaServerMap;
	// private Multimap<String, PaasSsh>      metaSshMMap;
	// private Map<String,  PaasCmptVer>      metaCmptVerMap;

}

func NewCmptMeta() *CmptMeta {
	cmptMeta := &CmptMeta{}
	cmptMeta.Init()
	return cmptMeta
}

func (m *CmptMeta) Init() {
	m.release()

	m.accountMap = make(map[string]proto.Account, 0)
	m.accSessionMap = make(map[string]proto.AccountSession, 0)
	m.magicKeyMap = make(map[string]proto.AccountSession, 0)

	m.metaServRootMap = make(map[string]string, 0)
	m.metaAttrIdMap = make(map[uint32]proto.PaasMetaAttr, 0)
	m.metaAttrNameMap = make(map[string]proto.PaasMetaAttr, 0)
	m.metaCmptIdMap = make(map[uint32]proto.PaasMetaCmpt, 0)
	m.metaCmptNameMap = make(map[string]proto.PaasMetaCmpt, 0)
	m.metaCmptAttrMMap = setmultimap.New()
	m.metaInstMap = make(map[string]proto.PaasInstance, 0)

	m.loadAccount()
}

func (m *CmptMeta) release() {
	m.mut.Lock()
	defer m.mut.Unlock()
}

func (m *CmptMeta) loadAccount() {
	tempAccountMap := make(map[string]proto.Account, 0)
	dao.LoadAccount(&tempAccountMap)
}
