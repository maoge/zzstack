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

// type Accout struct {
// 	ACC_ID      sql.NullString `db:"ACC_ID"`
// 	ACC_NAME    sql.NullString `db:"ACC_NAME"`
// 	PHONE_NUM   sql.NullString `db:"PHONE_NUM"`
// 	MAIL        sql.NullString `db:"MAIL"`
// 	PASSWD      sql.NullString `db:"PASSWD"`
// 	CREATE_TIME sql.NullInt64  `db:"CREATE_TIME"`
// }

type Accout struct {
	ACC_ID      string `db:"ACC_ID"`
	ACC_NAME    string `db:"ACC_NAME"`
	PHONE_NUM   string `db:"PHONE_NUM"`
	MAIL        string `db:"MAIL"`
	PASSWD      string `db:"PASSWD"`
	CREATE_TIME int64  `db:"CREATE_TIME"`
}

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

func DbSelectSingleRow(acc *Accout, accName string) error {
	dbPool := global.GLOBAL_RES.GetDbPool()
	crud := db.CRUD{}

	err := crud.SelectObject(dbPool, acc, &SQL_SEL_ACC_BYNAME, accName)
	if err != nil {
		return err
	} else {
		return nil
	}
}
