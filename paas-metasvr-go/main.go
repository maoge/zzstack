package main

import (
	"fmt"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
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
	config.InitMetaSvrConf()
	global.GLOBAL_RES.Init()
	meta.InitGlobalCmptMeta()
	eventbus.InitEventBus()
}

func startHttp() {
	gin.SetMode(gin.ReleaseMode)

	engine := gin.New()

	info := fmt.Sprintf("start http server: {%v}", config.META_SVR_CONFIG.WebApiAddress)
	utils.LOGGER.Info(info)

	route.Init(engine)
	err := engine.Run(config.META_SVR_CONFIG.WebApiAddress)
	if err == nil {
		info := fmt.Sprintf("http server: {%v} start OK", config.META_SVR_CONFIG.WebApiAddress)
		utils.LOGGER.Info(info)
	} else {
		err := fmt.Sprintf("http server: {%v} start error: %s", config.META_SVR_CONFIG.WebApiAddress, err)
		utils.LOGGER.Error(err)
	}
}
