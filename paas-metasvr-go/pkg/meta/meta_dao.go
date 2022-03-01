package meta

import (
	"fmt"
	"log"

	"github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/global"
)

var (
	SQL_SEL_ACC_BYNAME = "select ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME from t_account where ACC_NAME = ?"
)

func SetSession(key string, ses string) {
	client := global.GLOBAL_RES.GetRedisClusterClient()

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
	client := global.GLOBAL_RES.GetRedisClusterClient()

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

func DbSelectSingleRow(accName string) map[string]interface{} {
	dbPool := global.GLOBAL_RES.GetDbPool()
	crud := db.CRUD{}
	resultMap, err := crud.QueryRow(dbPool, &SQL_SEL_ACC_BYNAME, accName)
	if err == nil {
		return resultMap
	} else {
		log.Fatal(err.Error())
		return nil
	}

}
