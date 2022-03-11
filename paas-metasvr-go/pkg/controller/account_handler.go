package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/dao/accdao"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
)

type AccountHandler struct {
	group *gin.RouterGroup
}

func NewAccountHandler(g *gin.RouterGroup) *AccountHandler {
	return &AccountHandler{group: g}
}

func (m *AccountHandler) Login() {
	m.group.POST("/paas/account/login", func(c *gin.Context) {
		// bodyAsByteArray, err := ioutil.ReadAll(c.Request.Body)
		// if err != nil {
		// 	c.JSON(200, gin.H{"ret_code": "-1", "ret_info": "bad request"})
		// } else {
		// 	// utils.LOGGER.Info(fmt.Sprintf("%v", string(jsonData)))
		// 	//jsonBody := string(bodyAsByteArray)
		// 	c.JSON(200, jsonBody)
		// }

		var user proto.AccUser
		err := c.MustBindWith(&user, binding.JSON)
		if err == nil {
			resultBean := proto.NewResultBean()
			accdao.Login(&user, resultBean)
			c.JSON(http.StatusOK, resultBean)
		} else {
			c.String(http.StatusBadRequest, "参数绑定失败"+err.Error())
		}
	})
}
