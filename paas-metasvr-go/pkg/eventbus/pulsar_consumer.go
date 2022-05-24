package eventbus

import (
	"context"
	"fmt"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PulsarConsumer struct {
	client   *pulsar.Client
	consumer *pulsar.Consumer
}

func CreatePulsarConsumer() EventBusConsumer {
	topic := fmt.Sprintf("persistent://public/default/%v", consts.SYS_EVENT_TOPIC)
	pulsarClient := global.GLOBAL_RES.PulsarClient
	pulsarConsumer, err := (*pulsarClient).Subscribe(pulsar.ConsumerOptions{
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
		client:   pulsarClient,
		consumer: &pulsarConsumer,
	}
}

func (m PulsarConsumer) Receive() (interface{}, error) {
	return (*m.consumer).Receive(context.Background())
}

func (m PulsarConsumer) Close() {
	(*m.consumer).Close()

	m.consumer = nil
}
