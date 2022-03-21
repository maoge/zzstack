package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type MetaDataHandler struct {
	group *gin.RouterGroup
}

func NewMetaDataHandler(g *gin.RouterGroup) *MetaDataHandler {
	return &MetaDataHandler{group: g}
}

func (h *MetaDataHandler) GetMetaSvrClusterState() {
	h.group.GET("/paas/metadata/getMetaSvrClusterState", func(c *gin.Context) {
		// TODO get raft group state info
		c.JSON(http.StatusOK, "ok")
	})
}

func (h *MetaDataHandler) GetCmptMetaData() {
	h.group.GET("/paas/metadata/getCmptMetaData", func(c *gin.Context) {
		resultBean := proto.NewResultBean()
		resultBean.RET_INFO = meta.CMPT_META.GetMetaData2Json()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServiceCount() {
	h.group.POST("/paas/metadata/getServiceCount", func(c *gin.Context) {
		var getServiceCountParam proto.GetServiceCountParam
		err := c.MustBindWith(&getServiceCountParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetServiceCount(&getServiceCountParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServiceList() {
	h.group.POST("/paas/metadata/getServiceList", func(c *gin.Context) {
		var getServiceListParam proto.GetServiceListParam
		err := c.MustBindWith(&getServiceListParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetServiceList(&getServiceListParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServTypeVerCount() {
	h.group.POST("/paas/metadata/getServTypeVerCount", func(c *gin.Context) {
		var getServTypeVerCountParam proto.GetServTypeVerCountParam
		err := c.MustBindWith(&getServTypeVerCountParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetServTypeVerCount(&getServTypeVerCountParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServTypeVerListByPage() {
	h.group.POST("/paas/metadata/getServTypeVerListByPage", func(c *gin.Context) {
		var getServTypeVerListByPageParam proto.GetServTypeVerListByPageParam
		err := c.MustBindWith(&getServTypeVerListByPageParam, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetServTypeVerListByPage(&getServTypeVerListByPageParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetClickHouseDashboardAddr() {
	h.group.POST("/paas/metadata/getClickHouseDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetClickHouseDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetVoltDBDashboardAddr() {
	h.group.POST("/paas/metadata/getVoltDBDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetVoltDBDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetRocketMQDashboardAddr() {
	h.group.POST("/paas/metadata/getRocketMQDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetRocketMQDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetTiDBDashboardAddr() {
	h.group.POST("/paas/metadata/getTiDBDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetTiDBDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetPulsarDashboardAddr() {
	h.group.POST("/paas/metadata/getPulsarDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetPulsarDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetYBDashboardAddr() {
	h.group.POST("/paas/metadata/getYBDashboardAddr", func(c *gin.Context) {
		var param proto.GetDashboardAddrParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := proto.NewResultBean()
		metadao.GetYBDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) AddService() {
	h.group.POST("/paas/metadata/addService", func(c *gin.Context) {
		var param proto.AddServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		magicKey := utils.GetMagicKey(c)
		instId := utils.GenUUID()
		resultBean := proto.NewResultBean()
		metadao.AddService(&param, instId, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) DelService() {
	h.group.POST("/paas/metadata/delService", func(c *gin.Context) {
		var param proto.DelServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		magicKey := utils.GetMagicKey(c)
		instId := param.INST_ID
		resultBean := proto.NewResultBean()
		metadao.DelService(instId, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
