package controller

import (
	"fmt"

	"github.com/gin-gonic/gin"
)

type HelloController struct {
	group *gin.RouterGroup
}

func NewHelloController(g *gin.RouterGroup) *HelloController {
	return &HelloController{group: g}
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

// func (hello *HelloController) RedisSet() {
// 	hello.group.GET("/setSession", func(c *gin.Context) {
// 		meta.SetSession("abc", "123")
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("setSession %s", "abc")})
// 	})
// }

// func (hello *HelloController) RedisGet() {
// 	hello.group.GET("/getSession", func(c *gin.Context) {
// 		meta.RedisGet("abc")
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc")})
// 	})
// }

// func (hello *HelloController) DbSelectSingleRow() {
// 	hello.group.GET("/dbSelectSingleRow", func(c *gin.Context) {
// 		acc := meta.Accout{}
// 		err := meta.DbSelectSingleRow(&acc, "dev")
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&acc)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbTxSelectSingleRow() {
// 	hello.group.GET("/dbTxSelectSingleRow", func(c *gin.Context) {
// 		acc := meta.Accout{}
// 		err := meta.DbTxSelectSingleRow(&acc, "dev")
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&acc)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbSelectMultiRow() {
// 	hello.group.GET("/dbSelectMultiRow", func(c *gin.Context) {
// 		accSlice := make([]meta.Accout, 0)
// 		err := meta.DbSelectMultiRow(&accSlice)
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&accSlice)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbTxSelectMultiRow() {
// 	hello.group.GET("/dbTxSelectMultiRow", func(c *gin.Context) {
// 		accSlice := make([]meta.Accout, 0)
// 		err := meta.DbTxSelectMultiRow(&accSlice)
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&accSlice)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbSelectSingleRowAsMap() {
// 	hello.group.GET("/dbSelectSingleRowAsMap", func(c *gin.Context) {
// 		resMap, err := meta.DbSelectSingleRowAsMap("dev")
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&resMap)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbSelectSingleRowAsJson() {
// 	hello.group.GET("/dbSelectSingleRowAsJson", func(c *gin.Context) {
// 		bytes, err := meta.DbSelectSingleRowAsJson("dev")
// 		var str string
// 		if err == nil {
// 			str = string(bytes)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbSelectMultiRowAsMapSlice() {
// 	hello.group.GET("/dbSelectMultiRowAsMapSlice", func(c *gin.Context) {
// 		resMapSlice, err := meta.DbSelectMultiRowAsSlice()
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&resMapSlice)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) TxDbSelectMultiRowAsMapSlice() {
// 	hello.group.GET("/dbTxSelectMultiRowAsMapSlice", func(c *gin.Context) {
// 		resMapSlice, err := meta.DbTxSelectMultiRowAsSlice()
// 		var str string
// 		if err == nil {
// 			str = utils.Struct2Json(&resMapSlice)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbSelectMultiRowAsJson() {
// 	hello.group.GET("/dbSelectMultiRowAsJson", func(c *gin.Context) {
// 		bytes, err := meta.DbSelectMultiRowAsJson()
// 		var str string
// 		if err == nil {
// 			str = string(bytes)
// 		} else {
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "acc_info": str})
// 	})
// }

// func (hello *HelloController) DbInsertWithArgsList() {
// 	hello.group.GET("/dbInsertWithArgsList", func(c *gin.Context) {
// 		_, err := meta.InsertWithParamList("scdd1a5d-1d70-e15d-y671-habdbee9dddd", "test", "13800000003", "c@b", "b3fc7e7f88ef098fbc0671303a3a2d4a", 1634113956248)
// 		var str string
// 		if err == nil {
// 			str = "insert OK"
// 		} else {
// 			str = "insert NOK"
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "result_info": str})
// 	})
// }

// func (hello *HelloController) TxDbInsertWithArgsList() {
// 	hello.group.GET("/dbTxInsertWithArgsList", func(c *gin.Context) {
// 		_, err := meta.TxInsertWithParamList("scdd1a5d-1d70-e15d-y671-habdbee9dddd", "test", "13800000003", "c@b", "b3fc7e7f88ef098fbc0671303a3a2d4a", 1634113956248)
// 		var str string
// 		if err == nil {
// 			str = "insert OK"
// 		} else {
// 			str = "insert NOK"
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "result_info": str})
// 	})
// }

// func (hello *HelloController) DbInsertWithNamedMap() {
// 	hello.group.GET("/dbInsertWithNamedMap", func(c *gin.Context) {
// 		paramMap := map[string]interface{}{
// 			"acc_id":      "scdd1a5d-1d70-e15d-y671-habdbee9dddd",
// 			"acc_name":    "test",
// 			"phone_num":   "13800000003",
// 			"mail":        "c@b",
// 			"passwd":      "b3fc7e7f88ef098fbc0671303a3a2d4a",
// 			"create_time": int64(1634113956248),
// 		}

// 		_, err := meta.InsertWithNamedMap(&paramMap)
// 		var str string
// 		if err == nil {
// 			str = "insert OK"
// 		} else {
// 			str = "insert NOK"
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "result_info": str})
// 	})
// }

// func (hello *HelloController) TxDbInsertWithNamedMap() {
// 	hello.group.GET("/dbTxInsertWithNamedMap", func(c *gin.Context) {
// 		paramMap := map[string]interface{}{
// 			"acc_id":      "scdd1a5d-1d70-e15d-y671-habdbee9dddd",
// 			"acc_name":    "test",
// 			"phone_num":   "13800000003",
// 			"mail":        "c@b",
// 			"passwd":      "b3fc7e7f88ef098fbc0671303a3a2d4a",
// 			"create_time": int64(1634113956248),
// 		}

// 		_, err := meta.TxInsertWithNamedMap(&paramMap)
// 		var str string
// 		if err == nil {
// 			str = "insert OK"
// 		} else {
// 			str = "insert NOK"
// 			utils.LOGGER.Error(err.Error())
// 		}
// 		utils.LOGGER.Info(str)
// 		c.JSON(200, gin.H{"msg": fmt.Sprintf("getSession %s", "abc"), "result_info": str})
// 	})
// }
