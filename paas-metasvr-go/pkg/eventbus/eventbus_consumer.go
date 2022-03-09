package eventbus

type EventBusConsumer interface {
	// poll one msg from local cache queue or from broker
	Receive() (interface{}, error)

	// close consumer
	Close()
}
