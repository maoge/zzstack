package eventbus

import (
	"context"
	"fmt"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PulsarConsumer struct {
	client   *pulsar.Client
	consumer *pulsar.Consumer
}

func CreatePulsarConsumer() EventBusConsumer {
	topic := fmt.Sprintf("persistent://public/default/%v", consts.SYS_EVENT_TOPIC)

	addr := fmt.Sprintf("pulsar://%v", config.META_SVR_CONFIG.EventbusAddress)
	pulsarClient, err := pulsar.NewClient(pulsar.ClientOptions{
		URL:               addr,
		OperationTimeout:  3 * time.Second,
		ConnectionTimeout: 3 * time.Second,
	})

	if err != nil {
		errInfo := fmt.Sprintf("Could not instantiate Pulsar client: %v, error: %v", addr, err.Error())
		utils.LOGGER.Fatal(errInfo)
		return nil
	}

	pulsarConsumer, err := pulsarClient.Subscribe(pulsar.ConsumerOptions{
		Topic:             topic,
		SubscriptionName:  config.META_SVR_CONFIG.EventbusConsumerSubscription,
		ReceiverQueueSize: config.META_SVR_CONFIG.ThreadPoolCoreSize,
		Type:              pulsar.Exclusive,
		// .ackTimeout(10, TimeUnit.SECONDS)
	})

	if err != nil {
		errInfo := fmt.Sprintf("Subscribe error: %v", err.Error())
		utils.LOGGER.Fatal(errInfo)
	}

	return PulsarConsumer{
		client:   &pulsarClient,
		consumer: &pulsarConsumer,
	}
}

func (m PulsarConsumer) Receive() (interface{}, error) {
	return (*m.consumer).Receive(context.Background())
}

func (m PulsarConsumer) Close() {
	(*m.consumer).Close()
	(*m.client).Close()

	m.consumer = nil
	m.client = nil
}