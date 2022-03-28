package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/dao/alarmdao"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

type AlarmHandler struct {
	group *gin.RouterGroup
}

func NewAlarmHandler(g *gin.RouterGroup) *AlarmHandler {
	return &AlarmHandler{group: g}
}

func (h *AlarmHandler) GetAlarmCount() {
	h.group.POST("/paas/alarm/getAlarmCount", func(c *gin.Context) {
		var getAlarmCountParam proto.GetAlarmCountParam
		err := c.MustBindWith(&getAlarmCountParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		alarmdao.GetAlarmCount(&getAlarmCountParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *AlarmHandler) GetAlarmList() {
	h.group.POST("/paas/alarm/getAlarmList", func(c *gin.Context) {
		var getAlarmListParam proto.GetAlarmListParam
		err := c.MustBindWith(&getAlarmListParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		alarmdao.GetAlarmList(&getAlarmListParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *AlarmHandler) ClearAlarm() {
	h.group.POST("/paas/alarm/clearAlarm", func(c *gin.Context) {
		var clearAlarmParam proto.ClearAlarmParam
		err := c.MustBindWith(&clearAlarmParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		alarmdao.ClearAlarm(&clearAlarmParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
