package dao

import (
	crud "github.com/maoge/paas-metasvr-go/pkg/db"
	"github.com/maoge/paas-metasvr-go/pkg/db/pool"

	// "github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
)

var (
	SQL_SEL_ACCOUNT string = "select ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME from t_account"
)

func LoadAccount(dbPool *pool.DbPool, accountMap *map[string]proto.Account) bool {
	// dbPool := global.GLOBAL_RES.GetDbPool()

	accSlice := make([]proto.Account, 0)
	err := crud.SelectAsSlice(dbPool, &accSlice, &SQL_SEL_ACCOUNT)
	if err != nil {
		return false
	}

	return true
}
