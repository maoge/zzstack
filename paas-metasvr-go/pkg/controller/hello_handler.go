package controller

import (
	"fmt"
	"log"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type HelloController struct {
	group *gin.RouterGroup
}

func NewHelloController(g *gin.RouterGroup) HelloController {
	return HelloController{group: g}
}

func (hello *HelloController) Hello() {
	hello.group.GET("/hello", func(c *gin.Context) {
		c.JSON(200, gin.H{"msg": "Hello World"})
	})
}

func (hello *HelloController) SayHello() {
	hello.group.GET("/sayHello", func(c *gin.Context) {
		name := c.DefaultQuery("name", "")
		c.JSON(200, gin.H{"msg": fmt.Sprintf("hello %s", name)})
	})
}

func (hello *HelloController) RedisSet() {
	hello.group.GET("/setSession", func(c *gin.Context) {
		meta.SetSession("abc", "123")
		c.JSON(200, gin.H{"msg": fmt.Sprintf("setSession %s", "abc")})
	})
}

func (hello *HelloController) RedisGet() {
	hello.group.GET("/getSession", func(c *gin.Context) {
		meta.GetSession("abc")
		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc")})
	})
}

func (hello *HelloController) DbSelectSingleRow() {
	hello.group.GET("/dbSelectSingleRow", func(c *gin.Context) {
		acc := meta.Accout{}
		err := meta.DbSelectSingleRow(&acc, "dev")
		var str string
		if err == nil {
			str = utils.Struct2Json(&acc)
		} else {
			log.Println(err.Error())
		}
		log.Println(str)
		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
	})
}
