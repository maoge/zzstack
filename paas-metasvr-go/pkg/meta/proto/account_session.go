package proto

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type AccountSession struct {
	ACC_NAME        string `json:"ACC_NAME,omitempty"`
	MAGIC_KEY       string `json:"MAGIC_KEY,omitempty"`
	SESSION_TIMEOUT int64  `json:"SESSION_TIMEOUT,omitempty"`
}

func NewAccountSession(accName, magicKey string) *AccountSession {
	return &AccountSession{
		ACC_NAME:        accName,
		MAGIC_KEY:       magicKey,
		SESSION_TIMEOUT: utils.CurrentTimeMilli() + consts.SESSION_TTL,
	}
}

func (m *AccountSession) IsSessionValid() bool {
	return m.SESSION_TIMEOUT > utils.CurrentTimeMilli()
}
