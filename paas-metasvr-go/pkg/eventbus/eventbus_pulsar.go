package eventbus

import (
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PulsarBusImpl struct {
	producer EventBusProducer
	consumer EventBusConsumer
}

func NewPulsarBusImpl() EventBus {
	pulsarBus := new(PulsarBusImpl)
	pulsarBus.Init()
	return pulsarBus
}

func (p *PulsarBusImpl) Init() {
	p.producer = CreatePulsarProducer()
	p.consumer = CreatePulsarConsumer()
}

func (p *PulsarBusImpl) Publish(data []byte) {
	p.producer.SendAsync(data)
}

func (p *PulsarBusImpl) PublishEvent(event *proto.PaasEvent) {
	eventMsg := utils.Struct2Json(event)
	p.producer.SendAsync([]byte(eventMsg))
}

func (p *PulsarBusImpl) Receive() (interface{}, error) {
	return p.consumer.Receive()
}

func (p *PulsarBusImpl) Close() {
	p.producer.Close()
	p.consumer.Close()
}
