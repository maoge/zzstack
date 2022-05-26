package controller

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type DevOpsController struct {
	group *gin.RouterGroup
}

func NewDevOpsController(g *gin.RouterGroup) *DevOpsController {
	return &DevOpsController{group: g}
}

func (h *DevOpsController) DeployService() {
	h.group.POST("/paas/autodeploy/deployService", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		logKey := param.LOG_KEY
		deployFlag := param.DEPLOY_FLAG
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.DeployService(servID, deployFlag, logKey, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) UndeployService() {
	h.group.POST("/paas/autodeploy/undeployService", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.UndeployService(servID, logKey, magicKey, false, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) DeployInstance() {
	h.group.POST("/paas/autodeploy/deployInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.DeployInstance(servID, instID, logKey, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) UndeployInstance() {
	h.group.POST("/paas/autodeploy/undeployInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.UndeployInstance(servID, instID, logKey, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) ForceUndeployServ() {
	h.group.POST("/paas/autodeploy/forceUndeployServ", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.UndeployService(servID, logKey, magicKey, true, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) StartInstance() {
	h.group.POST("/paas/autodeploy/startInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.MaintainInstance(servID, instID, servType, logKey, magicKey, &consts.INSTANCE_OPERATION_START, true, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) StopInstance() {
	h.group.POST("/paas/autodeploy/stopInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.MaintainInstance(servID, instID, servType, logKey, magicKey, &consts.INSTANCE_OPERATION_STOP, true, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) RestartInstance() {
	h.group.POST("/paas/autodeploy/restartInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.MaintainInstance(servID, instID, servType, logKey, magicKey, &consts.INSTANCE_OPERATION_RESTART, true, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) UpdateInstance() {
	h.group.POST("/paas/autodeploy/updateInstance", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.MaintainInstance(servID, instID, servType, logKey, magicKey, &consts.INSTANCE_OPERATION_UPDATE, true, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) BatchUpdateInst() {
	h.group.POST("/paas/autodeploy/batchUpdateInst", func(c *gin.Context) {
		var param proto.DeployServiceParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		logKey := param.LOG_KEY
		magicKey := utils.GetMagicKey(c)
		instIdArr := strings.Split(instID, ",")

		resultBean := result.NewResultBean()
		autodeploy.BatchUpdateInst(servID, servType, logKey, magicKey, instIdArr, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) CheckInstanceStatus() {
	h.group.POST("/paas/autodeploy/checkInstanceStatus", func(c *gin.Context) {
		var param proto.CheckInstanceStatusParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		servType := param.SERV_TYPE
		magicKey := utils.GetMagicKey(c)

		resultBean := result.NewResultBean()
		autodeploy.CheckInstanceStatus(servID, instID, servType, magicKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) GetDeployLog() {
	h.group.POST("/paas/autodeploy/getDeployLog", func(c *gin.Context) {
		var param proto.DeployLogParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		logKey := param.LOG_KEY
		resultBean := result.NewResultBean()

		autodeploy.GetDeployLog(logKey, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DevOpsController) GetAppLog() {
	h.group.POST("/paas/autodeploy/getAppLog", func(c *gin.Context) {
		var param proto.AppLogParam
		err := c.MustBindWith(&param, binding.JSON)
		if err != nil {
			c.String(http.StatusBadRequest, "参数绑定错误"+err.Error())
			return
		}

		servID := param.SERV_ID
		instID := param.INST_ID
		logType := param.LOG_TYPE

		resultBean := result.NewResultBean()
		autodeploy.GetAppLog(servID, instID, logType, resultBean)
		c.JSON(http.StatusOK, resultBean)
	})
}
