package eventbus

type EventBusProducer interface {
	// sync send msg
	Send([]byte) error

	// async send msg
	SendAsync([]byte) error

	// close producer
	Close()
}
