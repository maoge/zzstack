package utils

import (
	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
)

func GetMagicKey(c *gin.Context) string {
	var magicKey string
	headers := c.Request.Header[consts.HEADER_MAGIC_KEY]
	if headers == nil {
		// for paas-ui: "Cookie : MAGIC_KEY=32e10f19-75fb-41e0-871a-2e5773c29b06"
		magicKey, _ = c.Cookie("MAGIC_KEY")
	} else {
		// for paas-sdk: "MAGIC_KEY : 32e10f19-75fb-41e0-871a-2e5773c29b06"
		magicKey = headers[0]
	}

	return magicKey
}
