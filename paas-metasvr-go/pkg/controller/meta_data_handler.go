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

func (h *MetaDataHandler) ModService() {
	h.group.POST("/paas/metadata/modService", func(c *gin.Context) {
		var param proto.ModServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		magicKey := utils.GetMagicKey(c)
		resultBean := proto.NewResultBean()
		metadao.ModService(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) ModServiceVersion() {
	h.group.POST("/paas/metadata/modServiceVersion", func(c *gin.Context) {
		var param proto.ModServiceVersionParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		magicKey := utils.GetMagicKey(c)
		resultBean := proto.NewResultBean()
		metadao.ModServiceVersion(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServerCount() {
	h.group.POST("/paas/metadata/getServerCount", func(c *gin.Context) {
		var param proto.GetServerCountParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		metadao.GetServerCnt(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServerList() {
	h.group.POST("/paas/metadata/getServerList", func(c *gin.Context) {
		var param proto.GetServerListParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		metadao.GetServerList(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) AddServer() {
	h.group.POST("/paas/metadata/addServer", func(c *gin.Context) {
		var param proto.AddServerParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		magicKey := utils.GetMagicKey(c)
		resultBean := proto.NewResultBean()

		metadao.AddServer(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) DelServer() {
	h.group.POST("/paas/metadata/delServer", func(c *gin.Context) {
		var param proto.DelServerParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servIp := param.SERVER_IP
		magicKey := utils.GetMagicKey(c)
		resultBean := proto.NewResultBean()

		metadao.DelServer(servIp, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetSSHCountByIP() {
	h.group.POST("/paas/metadata/getSSHCountByIP", func(c *gin.Context) {
		var param proto.GetSSHCountByIPParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servIp := param.SERVER_IP
		resultBean := proto.NewResultBean()

		metadao.GetSshCntByIp(servIp, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetSSHListByIP() {
	h.group.POST("/paas/metadata/getSSHListByIP", func(c *gin.Context) {
		var param proto.GetSSHListByIPParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()

		metadao.GetSshListByIp(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) AddSSH() {
	h.group.POST("/paas/metadata/addSSH", func(c *gin.Context) {
		var param proto.AddSSHParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.AddSsh(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) ModSSH() {
	h.group.POST("/paas/metadata/modSSH", func(c *gin.Context) {
		var param proto.ModSSHParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.ModSSH(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) DelSSH() {
	h.group.POST("/paas/metadata/delSSH", func(c *gin.Context) {
		var param proto.DelSSHParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.DelSSH(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetUserByServiceType() {
	h.group.POST("/paas/metadata/getUserByServiceType", func(c *gin.Context) {
		var param proto.GetUserByServiceTypeParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		servClazz := param.SERV_CLAZZ

		data := meta.CMPT_META.GetSurpportSSHList(servClazz)
		resultBean.RET_INFO = data

		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServList() {
	h.group.POST("/paas/metadata/getServList", func(c *gin.Context) {
		var param proto.GetServListParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		servType := param.SERV_TYPE

		data := meta.CMPT_META.GetServListFromCache(servType)
		resultBean.RET_INFO = data

		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) LoadServiceTopo() {
	h.group.POST("/paas/metadata/loadServiceTopo", func(c *gin.Context) {
		var param proto.LoadServiceTopoParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := proto.NewResultBean()
		instId := param.INST_ID

		metadao.LoadServiceTopo(instId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
