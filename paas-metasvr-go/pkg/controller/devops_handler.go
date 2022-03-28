package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/autodeploy"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type DeployController struct {
	group *gin.RouterGroup
}

func NewDeployController(g *gin.RouterGroup) *DeployController {
	return &DeployController{group: g}
}

func (h *DeployController) DeployService() {
	h.group.POST("/paas/metadata/deployService", func(c *gin.Context) {
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

func (h *DeployController) UndeployService() {
	h.group.POST("/paas/metadata/undeployService", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) DeployInstance() {
	h.group.POST("/paas/metadata/deployInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) UndeployInstance() {
	h.group.POST("/paas/metadata/undeployInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) ForceUndeployServ() {
	h.group.POST("/paas/metadata/forceUndeployServ", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) StartInstance() {
	h.group.POST("/paas/metadata/startInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) StopInstance() {
	h.group.POST("/paas/metadata/stopInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) RestartInstance() {
	h.group.POST("/paas/metadata/restartInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) UpdateInstance() {
	h.group.POST("/paas/metadata/updateInstance", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) BatchUpdateInst() {
	h.group.POST("/paas/metadata/batchUpdateInst", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) CheckInstanceStatus() {
	h.group.POST("/paas/metadata/checkInstanceStatus", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) GetDeployLog() {
	h.group.POST("/paas/metadata/getDeployLog", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}

func (h *DeployController) GetAppLog() {
	h.group.POST("/paas/metadata/getAppLog", func(c *gin.Context) {
		resultBean := result.NewResultBean()
		c.JSON(http.StatusOK, resultBean)
	})
}
