package main

import (
	"fmt"
	"runtime"

	"net/http"
	_ "net/http/pprof"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus/dispatcher"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/global_factory"

	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/route"
	"github.com/maoge/paas-metasvr-go/pkg/sequence"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

// pprof: http://127.0.0.1:9090/debug/pprof/

func main() {
	startHttp()
}

func init() {
	consts.InitEventMap()
	utils.InitLogger()
	config.InitMetaSvrConf()
	global.InitGlobalRes()
	meta.InitGlobalCmptMeta()
	global_factory.InitDeployerFactory()
	eventbus.InitEventBus()
	dispatcher.InitEventDispatcher()
	sequence.InitSeqence()
}

func startHttp() {
	gin.SetMode(gin.ReleaseMode)

	if config.META_SVR_CONFIG.PProfEnabled {
		runtime.SetBlockProfileRate(1)     // 开启对阻塞操作的跟踪，block
		runtime.SetMutexProfileFraction(1) // 开启对锁调用的跟踪，mutex
		go http.ListenAndServe("0.0.0.0:6060", nil)
		utils.LOGGER.Info("pprof enabled ......")
	}

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
