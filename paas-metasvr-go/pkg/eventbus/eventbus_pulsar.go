package eventbus

type PulsarBusImpl struct {
	producer EventBusProducer
	consumer EventBusConsumer
}

func NewPulsarBusImpl() EventBus {
	pulsarBus := PulsarBusImpl{}
	pulsarBus.init()
	return pulsarBus
}

func (p PulsarBusImpl) init() {
	p.producer = CreatePulsarProducer()
	p.consumer = CreatePulsarConsumer()
}

func (p PulsarBusImpl) send(data []byte) error {
	return p.producer.Send(data)
}

func (p PulsarBusImpl) sendAsync(data []byte) {
	p.producer.SendAsync(data)
}

func (p PulsarBusImpl) receive() (interface{}, error) {
	return p.consumer.Receive()
}

func (p PulsarBusImpl) close() {
	p.producer.Close()
	p.consumer.Close()
}
