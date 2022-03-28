package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/dao/accdao"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type AccountHandler struct {
	group *gin.RouterGroup
}

func NewAccountHandler(g *gin.RouterGroup) *AccountHandler {
	return &AccountHandler{group: g}
}

func (h *AccountHandler) Login() {
	h.group.POST("/paas/account/login", func(c *gin.Context) {
		var loginParam proto.LoginParam
		err := c.MustBindWith(&loginParam, binding.JSON)
		if err == nil {
			resultBean := result.NewResultBean()
			accdao.Login(&loginParam, resultBean)
			c.JSON(http.StatusOK, resultBean)
		} else {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
		}
	})
}

func (h *AccountHandler) ModPassWord() {
	h.group.POST("/paas/account/modPassWord", func(c *gin.Context) {
		/*bodyBytes, err := ioutil.ReadAll(c.Request.Body)
		if err != nil {
			c.JSON(200, gin.H{"ret_code": "-1", "ret_info": "bad request"})
			return
		}

		jsonBody := string(bodyBytes)
		utils.LOGGER.Info(fmt.Sprintf("%v", string(jsonBody)))
		c.JSON(200, jsonBody)*/

		var modPasswdParam proto.ModPasswdParam
		err := c.MustBindWith(&modPasswdParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		accdao.ModPasswd(&modPasswdParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *AccountHandler) GetOpLogCnt() {
	h.group.POST("/paas/account/getOpLogCnt", func(c *gin.Context) {
		var getOpLogCntParam proto.GetOpLogCntParam
		err := c.MustBindWith(&getOpLogCntParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		accdao.GetOpLogCnt(&getOpLogCntParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *AccountHandler) GetOpLogList() {
	h.group.POST("/paas/account/getOpLogList", func(c *gin.Context) {
		var getOpLogListParam proto.GetOpLogListParam
		err := c.MustBindWith(&getOpLogListParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		accdao.GetOpLogList(&getOpLogListParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
