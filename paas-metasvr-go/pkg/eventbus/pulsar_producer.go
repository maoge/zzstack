package eventbus

import (
	"context"
	"fmt"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PulsarProducer struct {
	client   *pulsar.Client
	producer *pulsar.Producer
}

func CreatePulsarProducer() EventBusProducer {
	topic := fmt.Sprintf("persistent://public/default/%v", consts.SYS_EVENT_TOPIC)
	pulsarClient := global.GLOBAL_RES.PulsarClient
	pulsarProducer, err := (*pulsarClient).CreateProducer(pulsar.ProducerOptions{
		Topic:                   topic,
		BatchingMaxPublishDelay: 1 * time.Second,
		DisableBlockIfQueueFull: false,
		MaxPendingMessages:      1000,
	})

	if err != nil {
		errInfo := fmt.Sprintf("CreateProducer error: %v", err.Error())
		utils.LOGGER.Fatal(errInfo)
	}

	return PulsarProducer{
		client:   pulsarClient,
		producer: &pulsarProducer,
	}
}

func (m PulsarProducer) Send(data []byte) error {
	msg := &pulsar.ProducerMessage{
		Value: data,
	}
	_, err := (*m.producer).Send(context.Background(), msg)
	return err
}

func (m PulsarProducer) SendAsync(data []byte) {
	msg := &pulsar.ProducerMessage{
		Payload: data,
	}

	(*m.producer).SendAsync(context.Background(), msg, asyncSendCallBack)
}

func (m PulsarProducer) Close() {
	(*m.producer).Close()

	m.producer = nil
}

func asyncSendCallBack(_ pulsar.MessageID, _ *pulsar.ProducerMessage, err error) {
	if err != nil {
		errInfo := fmt.Sprintf("SendAsync error: %v", err)
		utils.LOGGER.Fatal(errInfo)
	}
	return
}
