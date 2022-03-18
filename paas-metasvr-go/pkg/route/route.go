package route

import (
	"reflect"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/controller"
)

func RegisterController(controller interface{}) {
	val := reflect.ValueOf(controller)
	numOfMethod := val.NumMethod()
	for i := 0; i < numOfMethod; i++ {
		val.Method(i).Call(nil)
	}
}

func Init(e *gin.Engine) {
	defaultGroup := e.Group("")
	helloController := controller.NewHelloController(defaultGroup)
	RegisterController(helloController)

	accountHandler := controller.NewAccountHandler(defaultGroup)
	RegisterController(accountHandler)

	alarmHandler := controller.NewAlarmHandler(defaultGroup)
	RegisterController(alarmHandler)
}
