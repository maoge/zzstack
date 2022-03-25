package controller

import (
	"fmt"

	"github.com/gin-gonic/gin"
)

type BenchController struct {
	group *gin.RouterGroup
}

func NewBenchController(g *gin.RouterGroup) *BenchController {
	return &BenchController{group: g}
}

func (h *BenchController) Ping() {
	h.group.GET("/paas/bench/ping", func(c *gin.Context) {
		c.JSON(200, "Pong")
	})
}

func (h *BenchController) Test() {
	h.group.GET("/paas/bench/test", func(c *gin.Context) {
		req := c.Request
		remoteAddr := req.RemoteAddr
		localAddr := req.Host
		info := fmt.Sprintf("remoteAddr:%s, localAddr:%s", remoteAddr, localAddr)

		c.JSON(200, info)
	})
}
