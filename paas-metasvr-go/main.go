package main

import (
	"fmt"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/route"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func main() {
	diableLog()

	initial()

	startHttp()
}

func diableLog() {
	stderr := os.Stderr
	fd, _ := os.Open(os.DevNull)
	os.Stderr = fd
	os.Stderr = stderr
}

func initial() {
	utils.Init()
	global.GLOBAL_RES.Init()
	meta.InitGlobalCmptMeta()
}

func startHttp() {
	gin.SetMode(gin.ReleaseMode)

	engine := gin.New()

	info := fmt.Sprintf("start http server: {%v}", global.GLOBAL_RES.Config.WebApiAddress)
	utils.LOGGER.Info(info)

	route.Init(engine)
	err := engine.Run(global.GLOBAL_RES.Config.WebApiAddress)
	if err == nil {
		info := fmt.Sprintf("http server: {%v} start OK", global.GLOBAL_RES.Config.WebApiAddress)
		utils.LOGGER.Info(info)
	} else {
		err := fmt.Sprintf("http server: {%v} start error: %s", global.GLOBAL_RES.Config.WebApiAddress, err)
		utils.LOGGER.Error(err)
	}
}
