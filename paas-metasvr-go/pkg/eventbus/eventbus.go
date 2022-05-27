package eventbus

import (
	"sync"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

// pulsar go sdk under windows need: set CGO_ENABLED=0

var (
	EVENTBUS         EventBus = nil
	eventbus_barrier sync.Once
)

type EventBus interface {
	Init()

	// async send msg
	Publish([]byte)

	PublishEvent(*proto.PaasEvent)

	// blocking receive msg when received
	Receive() (interface{}, error)

	Close()
}

func InitEventBus() {
	eventbus_barrier.Do(func() {
		if config.META_SVR_CONFIG.EventbusEnabled {
			EVENTBUS = newEventBus(consts.EVENTBUS_PULSAR)
		}
	})
}

func newEventBus(busType string) EventBus {
	switch busType {
	case consts.EVENTBUS_PULSAR:
		return NewPulsarBusImpl()
	default:
		utils.LOGGER.Fatal("undefine eventbus type: " + busType)
		return nil
	}
}
