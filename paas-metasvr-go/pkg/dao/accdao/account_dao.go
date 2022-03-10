package accdao

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/err"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func Login(accUser *proto.AccUser, resultBean *proto.ResultBean) bool {
	account := meta.CMPT_META.GetAccount(accUser.USER)
	if account == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_ACCOUNT_NOT_EXISTS
		return false
	}

	encrypt := utils.GeneratePasswd(accUser.USER, accUser.PASSWORD)
	if encrypt != account.PASSWD {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_PWD_INCORRECT
		return false
	}

	session := meta.CMPT_META.GetAccSession(accUser.USER)
	if session == nil {
		key := utils.GetRedisSessionKey(accUser.USER)
		session = GetSessionFromRedis(key)

		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = session.MAGIC_KEY

		if session == nil {
			magicKey := utils.GenUUID()
			newSession := proto.NewAccountSession(accUser.USER, magicKey)
			meta.CMPT_META.AddAccSession(newSession, false)
		} else {
			if !session.IsSessionValid() {
				// remove the old and add new on
				resultBean.RET_INFO = createSession(accUser.USER, session.MAGIC_KEY)
			} else {
				// local cache not exists, fill from redis to local cache
				meta.CMPT_META.AddAccSession(session, true)
			}
		}

		return true
	}

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = session.MAGIC_KEY

	// if session ttl, create new
	if !session.IsSessionValid() {
		resultBean.RET_INFO = createSession(accUser.USER, session.MAGIC_KEY)
	}

	return true
}

func createSession(accName, oldMagicKey string) string {
	meta.CMPT_META.RemoveTtlSession(accName, oldMagicKey, false)

	magicKey := utils.GenUUID()
	newSession := proto.NewAccountSession(accName, magicKey)
	meta.CMPT_META.AddAccSession(newSession, false)

	return magicKey
}

func GetSessionFromRedis(userSessionKey string) *proto.AccountSession {
	val, err := RedisGet(userSessionKey)
	if err != nil {
		return nil
	}

	str := val.(string)
	var session proto.AccountSession
	utils.Json2Struct([]byte(str), &session)
	return &session
}

func RedisGet(key string) (interface{}, error) {
	client := global.GLOBAL_RES.GetRedisClusterClient()

	if client != nil {
		cmd := client.Do("get", key)
		val, err := cmd.Result()
		if err != nil {
			return nil, err
		} else {
			return val, nil
		}
	} else {
		return nil, err.RedisErr{ErrInfo: consts.ERR_REDIS_POOL_NIL}
	}
}
