package proto

import (
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type PaasEvent struct {
	EVENT_CODE   int32  `json:"EVENT_CODE,omitempty"`
	META_SERV_ID string `json:"META_SERV_ID,omitempty"`
	EVENT_TS     int64  `json:"EVENT_TS,omitempty"`
	MSG_BODY     string `json:"MSG_BODY,omitempty"`
	MAGIC_KEY    string `json:"MAGIC_KEY,omitempty"`
}

func NewPaasEvent(eventCode int32, msgBody string, magicKey string) *PaasEvent {
	return &PaasEvent{
		EVENT_CODE:   eventCode,
		META_SERV_ID: config.META_SVR_CONFIG.MetaServId,
		EVENT_TS:     utils.CurrentTimeMilli(),
		MSG_BODY:     msgBody,
		MAGIC_KEY:    magicKey,
	}
}
