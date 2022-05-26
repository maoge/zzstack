package test

import (
	"fmt"
	"reflect"
	"testing"

	"github.com/Valiben/gin_unit_test"
	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/stretchr/testify/assert"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/controller"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/global"

	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func init() {
	utils.InitLogger()
	config.InitMetaSvrConf()
	global.InitGlobalRes()
	meta.InitGlobalCmptMeta()
	eventbus.InitEventBus()

	gin.SetMode(gin.ReleaseMode)

	engine := gin.New()

	info := fmt.Sprintf("start http server: {%v}", config.META_SVR_CONFIG.WebApiAddress)
	utils.LOGGER.Info(info)

	// route.Init(engine)

	defaultGroup := engine.Group("")
	helloController := controller.NewHelloController(defaultGroup)
	RegisterController(helloController)

	accountHandler := controller.NewAccountHandler(defaultGroup)
	RegisterController(accountHandler)

	err := engine.Run(config.META_SVR_CONFIG.WebApiAddress)
	if err == nil {
		info := fmt.Sprintf("http server: {%v} start OK", config.META_SVR_CONFIG.WebApiAddress)
		utils.LOGGER.Info(info)
	} else {
		err := fmt.Sprintf("http server: {%v} start error: %s", config.META_SVR_CONFIG.WebApiAddress, err)
		utils.LOGGER.Error(err)
	}

	// router := gin.Default() // 这需要写到init中，启动gin框架
	// router.POST("/login", LoginHandler)
	// gin_unit_test.SetRouter(defaultGroup) //把启动的engine 对象传入到test框架中
}

func RegisterController(controller interface{}) {
	val := reflect.ValueOf(controller)
	numOfMethod := val.NumMethod()
	for i := 0; i < numOfMethod; i++ {
		val.Method(i).Call(nil)
	}
}

func TestGetOpLogCnt(t *testing.T) {
	params := proto.GetOpLogCntParam{
		USER:     "admin",
		START_TS: 1647496507600,
		END_TS:   1647496587600,
	}

	resp := result.ResultBean{}
	// 调用函数发起http请求
	err := gin_unit_test.TestHandlerUnMarshalResp("POST", "/paas/account/getOpLogCnt", "json", params, &resp)
	assert.Nil(t, err)
	if err != nil {
		t.Errorf("TestLoginHandler: %v\n", err)
		return
	}
	// 得到返回数据结构体， 至此，完美完成一次post请求测试，
	// 如果需要benchmark 输出性能报告也是可以的
	fmt.Println("result:", resp)
}
