package meta

import (
	"fmt"
	"log"

	"github.com/maoge/paas-metasvr-go/pkg/global"
)

func SetSession(key string, ses string) {
	client := global.GLOBAL_RES.RedisPool.GetClusterClient()

	if client != nil {
		cmd := client.Do("set", key, ses)
		res, err := cmd.Result()
		if err != nil {
			log.Fatalf("getSession error: %v", err)
		} else {
			fmt.Println(res.(string))
		}
	} else {
		log.Fatal("redis pool get connection result nil")
	}
}

func GetSession(key string) {
	client := global.GLOBAL_RES.RedisPool.GetClusterClient()

	if client != nil {
		cmd := client.Do("get", key)
		val, err := cmd.Result()
		if err != nil {
			log.Fatalf("getSession error: %v", err)
		} else {
			fmt.Println(val.(string))
		}
	} else {
		log.Fatal("redis pool get connection result nil")
	}
}
