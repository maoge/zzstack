package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
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
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
		metadao.GetServiceCount(&getServiceCountParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServiceList() {
	h.group.POST("/paas/metadata/getServiceList", func(c *gin.Context) {
		var param proto.GetServiceListParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetServiceList(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetServTypeVerList() {
	h.group.POST("/paas/metadata/getServTypeVerList", func(c *gin.Context) {
		result := make(map[string]interface{})
		meta.CMPT_META.GetServTypeVerList(&result)
		c.JSON(http.StatusOK, result)
	})
}

func (h *MetaDataHandler) GetServTypeList() {
	h.group.POST("/paas/metadata/getServTypeList", func(c *gin.Context) {
		servSlice := meta.CMPT_META.GetServTypeListFromLocalCache()

		result := make(map[string]interface{})
		result[consts.HEADER_RET_CODE] = consts.REVOKE_OK
		result[consts.HEADER_SERV_TYPE] = servSlice

		c.JSON(http.StatusOK, result)
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
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
		metadao.GetServTypeVerListByPage(&getServTypeVerListByPageParam, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetClickHouseDashboardAddr() {
	h.group.POST("/paas/metadata/getClickHouseDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetClickHouseDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetVoltDBDashboardAddr() {
	h.group.POST("/paas/metadata/getVoltDBDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetVoltDBDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetRocketMQDashboardAddr() {
	h.group.POST("/paas/metadata/getRocketMQDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetRocketMQDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetTiDBDashboardAddr() {
	h.group.POST("/paas/metadata/getTiDBDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetTiDBDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetPulsarDashboardAddr() {
	h.group.POST("/paas/metadata/getPulsarDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
		metadao.GetPulsarDashboardAddr(&param, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetYBDashboardAddr() {
	h.group.POST("/paas/metadata/getYBDashboardAddr", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()
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

		resultBean := result.NewResultBean()
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

		resultBean := result.NewResultBean()
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
		resultBean := result.NewResultBean()

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
		resultBean := result.NewResultBean()

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
		resultBean := result.NewResultBean()

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

		resultBean := result.NewResultBean()

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

		resultBean := result.NewResultBean()
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

		resultBean := result.NewResultBean()
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

		resultBean := result.NewResultBean()
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

		resultBean := result.NewResultBean()
		servClazz := param.SERV_CLAZZ

		data := meta.CMPT_META.GetSurpportSSHList(servClazz)
		resultBean.RET_INFO = utils.Struct2Json(data)

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

		resultBean := result.NewResultBean()
		servType := param.SERV_TYPE

		data := meta.CMPT_META.GetServListFromCache(servType)
		resultBean.RET_INFO = data

		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) LoadServiceTopo() {
	h.group.POST("/paas/metadata/loadServiceTopo", func(c *gin.Context) {
		var param proto.LoadMetaParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		instId := param.INST_ID

		metadao.LoadServiceTopo(instId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) LoadInstanceMeta() {
	h.group.POST("/paas/metadata/loadInstanceMeta", func(c *gin.Context) {
		var param proto.LoadMetaParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		instId := param.INST_ID

		metadao.LoadInstanceMeta(instId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) SaveServiceTopoSkeleton() {
	h.group.POST("/paas/metadata/saveServiceTopoSkeleton", func(c *gin.Context) {
		var param proto.SaveServiceTopoSkeletonParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)
		servType := param.SERV_TYPE
		topoMap := param.TOPO_JSON

		metadao.SaveServTopoSkeleton(servType, topoMap, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) SaveServiceNode() {
	h.group.POST("/paas/metadata/saveServiceNode", func(c *gin.Context) {
		var param proto.SaveServiceNodeParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)
		parentId := param.PARENT_ID
		opType := param.OP_TYPE
		nodeJson := param.NODE_JSON

		metadao.SaveServiceNode(parentId, opType, nodeJson, resultBean, magicKey)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) DelServiceNode() {
	h.group.POST("/paas/metadata/delServiceNode", func(c *gin.Context) {
		var param proto.DelServiceNodeParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)
		parentId := param.PARENT_ID
		instId := param.INST_ID

		metadao.DelServiceNode(parentId, instId, resultBean, magicKey)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetMetaTree() {
	h.group.POST("/paas/metadata/getMetaTree", func(c *gin.Context) {
		var param proto.LoadMetaParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		instId := param.INST_ID

		metadao.GetMetaDataTreeByInstId(instId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetMetaData() {
	h.group.POST("/paas/metadata/getMetaData", func(c *gin.Context) {
		var param proto.LoadMetaParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		instId := param.INST_ID

		metadao.GetMetaDataNodeByInstId(instId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) GetSmsABQueueWeightInfo() {
	h.group.POST("/paas/metadata/getSmsABQueueWeightInfo", func(c *gin.Context) {
		var param proto.ServInstParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		servInstId := param.SERV_INST_ID

		metadao.GetSmsABQueueWeightInfo(servInstId, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) AdjustSmsABQueueWeightInfo() {
	h.group.POST("/paas/metadata/adjustSmsABQueueWeightInfo", func(c *gin.Context) {
		var param proto.AdjustSmsABQueueParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.AdjustSmsABQueueWeightInfo(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) SwitchSmsDBType() {
	h.group.POST("/paas/metadata/switchSmsDBType", func(c *gin.Context) {
		var param proto.SwitchSmsDBTypeParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.SwitchSmsDBType(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) AddCmptVersion() {
	h.group.POST("/paas/metadata/addCmptVersion", func(c *gin.Context) {
		var param proto.CmptVersionParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.AddCmptVersion(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) DelCmptVersion() {
	h.group.POST("/paas/metadata/delCmptVersion", func(c *gin.Context) {
		var param proto.CmptVersionParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)

		metadao.DelCmptVersion(&param, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *MetaDataHandler) ReloadMetaData() {
	h.group.POST("/paas/metadata/reloadMetaData", func(c *gin.Context) {
		var param proto.ReloadMetaDataParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		resultBean := result.NewResultBean()
		magicKey := utils.GetMagicKey(c)
		reloadType := param.RELOAD_TYPE

		metadao.ReloadMetaData(reloadType, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
