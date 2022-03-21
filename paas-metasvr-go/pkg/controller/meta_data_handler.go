package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
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
		c.JSON(http.StatusOK, "ok")
	})
}
