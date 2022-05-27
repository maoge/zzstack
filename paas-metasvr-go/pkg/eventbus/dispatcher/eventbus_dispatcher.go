package dispatcher

import (
	"fmt"
	"sync"

	"github.com/Jeffail/tunny"
	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

var (
	EVENT_DISPATCHER   *EventDispatcher = nil
	dispatcher_barrier sync.Once
)

type EventDispatcher struct {
	Running bool
}

func InitEventDispatcher() {
	dispatcher_barrier.Do(func() {
		if config.META_SVR_CONFIG.EventbusEnabled {
			EVENT_DISPATCHER = new(EventDispatcher)
			EVENT_DISPATCHER.Start()
		}
	})
}

func (h *EventDispatcher) Start() {
	h.Running = true
	go h.runLoop()
}

func (h *EventDispatcher) runLoop() {
	pool := tunny.NewFunc(config.META_SVR_CONFIG.ThreadPoolCoreSize, func(payload interface{}) interface{} {
		buff := payload.(pulsar.Message).Payload()
		return DoWork(buff)
	})
	defer pool.Close()

	for {
		if !h.Running {
			break
		}

		res, err := eventbus.EVENTBUS.Receive()
		if err != nil {
			pool.Process(res)
		}
	}
}

func (h *EventDispatcher) Stop() {
	h.Running = false
}

func DoWork(buff []byte) bool {
	eventBody := make(map[string]interface{})
	utils.Json2Struct(buff, &eventBody)

	eventCode := eventBody[consts.HEADER_EVENT_CODE].(int32)
	metaServId := eventBody[consts.HEADER_META_SERV_ID].(string)
	eventTs := eventBody[consts.HEADER_EVENT_TS].(int64)
	msgBody := eventBody[consts.HEADER_MSG_BODY].(string)

	if (utils.CurrentTimeMilli() - eventTs) > config.META_SVR_CONFIG.EventbusExpireTtl {
		info := fmt.Sprintf("event msg expired:%s", string(buff))
		utils.LOGGER.Info(info)
		return true
	}

	if config.META_SVR_CONFIG.MetaServId == metaServId {
		return true
	}

	switch eventCode {
	case consts.EVENT_SYNC_SESSION.CODE:
		break

	case consts.EVENT_ADD_SERVICE.CODE:
		ProcAddService(msgBody)
		break

	case consts.EVENT_DEL_SERVICE.CODE:
		ProcDelService(msgBody)
		break

	case consts.EVENT_MOD_SERVICE.CODE:
		ProcModService(msgBody)
		break

	case consts.EVENT_UPD_SERVICE_DEPLOY.CODE:
		ProcUpdServiceDeploy(msgBody)
		break

	case consts.EVENT_ADD_INSTANCE.CODE:
		ProcAddInstance(msgBody)
		break

	case consts.EVENT_DEL_INSTANCE.CODE:
		ProcDelInstance(msgBody)
		break

	case consts.EVENT_UPD_INST_POS.CODE:
		ProcUpdInstPos(msgBody)
		break

	case consts.EVENT_UPD_INST_DEPLOY.CODE:
		ProcUpdInstDeploy(msgBody)
		break

	case consts.EVENT_ADD_INST_ATTR.CODE:
		ProcAddInstAttr(msgBody)
		break

	case consts.EVENT_DEL_INST_ATTR.CODE:
		ProcDelInstAttr(msgBody)
		break

	case consts.EVENT_ADD_TOPO.CODE:
		AddTopo(msgBody)
		break

	case consts.EVENT_DEL_TOPO.CODE:
		DelTopo(msgBody)
		break

	case consts.EVENT_MOD_TOPO.CODE:
		ModTopo(msgBody)
		break

	case consts.EVENT_ADD_SERVER.CODE:
		ProcAddServer(msgBody)
		break

	case consts.EVENT_DEL_SERVER.CODE:
		ProcDelServer(msgBody)
		break

	case consts.EVENT_ADD_SSH.CODE:
		AddSSH(msgBody)
		break

	case consts.EVENT_MOD_SSH.CODE:
		ModSSH(msgBody)
		break

	case consts.EVENT_DEL_SSH.CODE:
		DelSSH(msgBody)
		break

	case consts.EVENT_ADD_SESSON.CODE:
		AddSession(msgBody)
		break

	case consts.EVENT_REMOVE_SESSON.CODE:
		RemoveSession(msgBody)
		break

	case consts.EVENT_AJUST_QUEUE_WEIGHT.CODE:
		AdjustQueueWeight(msgBody)
		break

	case consts.EVENT_SWITCH_DB_TYPE.CODE:
		SwitchDBType(msgBody)
		break

	case consts.EVENT_ADD_CMPT_VER.CODE:
		AddCmptVerion(msgBody)
		break

	case consts.EVENT_DEL_CMPT_VER.CODE:
		DelCmptVerion(msgBody)
		break

	case consts.EVENT_MOD_ACC_PASSWD.CODE:
		ModPasswd(msgBody)
		break

	case consts.EVENT_UPD_INST_PRE_EMBEDDED.CODE:
		ProcUpdInstPreEmbadded(msgBody)
		break

	case consts.EVENT_RELOAD_METADATA.CODE:
		ProcReloadMetaData(msgBody)
		break

	default:
		break
	}

	return true
}
