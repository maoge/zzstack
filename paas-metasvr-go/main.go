package main

import (
	"log"
	"os"

	"github.com/gin-gonic/gin"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/route"
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
	global.GLOBAL_RES.Init()
}

func startHttp() {
	gin.SetMode(gin.ReleaseMode)

	engine := gin.New()

	route.Init(engine)
	err := engine.Run(global.GLOBAL_RES.Config.WebApiAddress)
	if err == nil {
		log.Printf("http server: {%v} start OK", global.GLOBAL_RES.Config.WebApiAddress)
	} else {
		log.Fatalf("http server: {%v} start error: %s", global.GLOBAL_RES.Config.WebApiAddress, err)
	}
}
