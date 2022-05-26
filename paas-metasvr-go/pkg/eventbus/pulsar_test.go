package eventbus

import (
	"context"
	"fmt"
	"log"
	"testing"
	"time"

	"github.com/apache/pulsar-client-go/pulsar"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

func TestPulsarProducer(t *testing.T) {
	addr := "pulsar://172.20.0.171:61000,172.20.0.172:61000"
	pulsarClient, err := pulsar.NewClient(pulsar.ClientOptions{
		URL:               addr,
		OperationTimeout:  3 * time.Second,
		ConnectionTimeout: 3 * time.Second,
	})

	defer pulsarClient.Close()

	if err != nil {
		log.Fatalf("%v", err)
	}

	topic := fmt.Sprintf("persistent://public/default/%v", consts.SYS_EVENT_TOPIC)
	pulsarProducer, err := pulsarClient.CreateProducer(pulsar.ProducerOptions{
		Topic:                   topic,
		BatchingMaxPublishDelay: 1 * time.Second,
		DisableBlockIfQueueFull: false,
		MaxPendingMessages:      1000,
	})

	if err != nil {
		log.Fatalf("CreateProducer error: %v", err)
	}

	msg := &pulsar.ProducerMessage{
		Value: "data",
	}

	msgId, err := pulsarProducer.Send(context.Background(), msg)
	log.Printf("msgId: %v", msgId)
}
