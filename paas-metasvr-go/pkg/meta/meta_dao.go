package meta

import (
	"fmt"
	"log"

	crud "github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/global"
)

var (
	SQL_SEL_ACC_BYNAME         = "select ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME from t_account where ACC_NAME = ?"
	SQL_SEL_ALL_ACC            = "select ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME from t_account order by ACC_NAME"
	SQL_INS_ACC_WITH_ARGS      = "insert into t_account(ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME) values (?,?,?,?,?,?)"
	SQL_INS_ACC_WITH_NAMED_MAP = "insert into t_account(ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME) values (:acc_id, :acc_name, :phone_num, :mail, :passwd, :create_time)"
	// SQL_INS_ACC_WITH_NAMED_MAP = "insert into t_account(ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD) values (:acc_id, :acc_name, :phone_num, :mail, :passwd, now())"
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

	err := crud.SelectAsObject(dbPool, acc, &SQL_SEL_ACC_BYNAME, accName)
	if err != nil {
		return err
	} else {
		return nil
	}
}

func DbTxSelectSingleRow(acc *Accout, accName string) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	err := crud.TxSelectAsObject(dbPool, acc, &SQL_SEL_ACC_BYNAME, accName)
	if err != nil {
		return err
	} else {
		return nil
	}
}

func DbSelectMultiRow(accSlice *[]Accout) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	err := crud.SelectAsSlice(dbPool, accSlice, &SQL_SEL_ALL_ACC)
	if err != nil {
		return err
	} else {
		return nil
	}
}

func DbTxSelectMultiRow(accSlice *[]Accout) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	err := crud.TxSelectAsSlice(dbPool, accSlice, &SQL_SEL_ALL_ACC)
	if err != nil {
		return err
	} else {
		return nil
	}
}

func DbSelectSingleRowAsMap(accName string) (map[string]interface{}, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	return crud.SelectAsMap(dbPool, &SQL_SEL_ACC_BYNAME, accName)
}

func DbSelectSingleRowAsJson(accName string) ([]byte, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	return crud.SelectAsJson(dbPool, &SQL_SEL_ACC_BYNAME, accName)
}

func DbSelectMultiRowAsSlice() ([]interface{}, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	return crud.SelectAsMapSlice(dbPool, &SQL_SEL_ALL_ACC)
}

func DbTxSelectMultiRowAsSlice() ([]interface{}, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	return crud.TxSelectAsMapSlice(dbPool, &SQL_SEL_ALL_ACC)
}

func DbSelectMultiRowAsJson() ([]byte, error) {
	dbPool := global.GLOBAL_RES.GetDbPool()

	return crud.SelectAsJsonArray(dbPool, &SQL_SEL_ALL_ACC)
}

func InsertWithParamList(args ...interface{}) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	res, err := crud.Insert(dbPool, &SQL_INS_ACC_WITH_ARGS, args...)
	if err != nil {
		log.Fatalf("%v", err)
		return err
	} else {
		id, _ := res.LastInsertId()
		affectedRows, _ := res.RowsAffected()
		log.Printf("LastInsertId: %v, RowsAffected:%v", id, affectedRows)
		return nil
	}
}

func TxInsertWithParamList(args ...interface{}) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	res, err := crud.TxInsert(dbPool, &SQL_INS_ACC_WITH_ARGS, args...)
	if err != nil {
		log.Fatalf("%v", err)
		return err
	} else {
		id, _ := res.LastInsertId()
		affectedRows, _ := res.RowsAffected()
		log.Printf("LastInsertId: %v, RowsAffected:%v", id, affectedRows)
		return nil
	}
}

func InsertWithNamedMap(argMap *map[string]interface{}) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	res, err := crud.NamedInsert(dbPool, &SQL_INS_ACC_WITH_NAMED_MAP, argMap)
	if err != nil {
		log.Fatalf("%v", err)
		return err
	} else {
		id, _ := res.LastInsertId()
		affectedRows, _ := res.RowsAffected()
		log.Printf("LastInsertId: %v, RowsAffected:%v", id, affectedRows)
		return nil
	}
}

func TxInsertWithNamedMap(argMap *map[string]interface{}) error {
	dbPool := global.GLOBAL_RES.GetDbPool()

	res, err := crud.TxNamedInsert(dbPool, &SQL_INS_ACC_WITH_NAMED_MAP, argMap)
	if err != nil {
		log.Fatalf("%v", err)
		return err
	} else {
		id, _ := res.LastInsertId()
		affectedRows, _ := res.RowsAffected()
		log.Printf("LastInsertId: %v, RowsAffected:%v", id, affectedRows)
		return nil
	}
}
