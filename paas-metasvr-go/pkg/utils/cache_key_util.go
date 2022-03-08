package utils

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

func GetRedisSessionKey(accName string) string {
	return fmt.Sprintf("%v%v", consts.SESSION_KEY_PREFIX, accName)
}
