package eventbus

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

// pulsar go sdk under windows need: set CGO_ENABLED=0

var EVENTBUS EventBus

type EventBus interface {
	init()

	send([]byte) error

	sendAsync([]byte)

	receive() (interface{}, error)

	close()
}

func InitEventBus() {
	EVENTBUS = newEventBus(consts.EVENTBUS_PULSAR)
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
