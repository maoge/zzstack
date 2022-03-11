package redisdao

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/err"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

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

func PutSessionToRedis(accSession *proto.AccountSession) {
	key := utils.GetRedisSessionKey(accSession.ACC_NAME)
	str := utils.Struct2Json(accSession)
	RedisSet(key, str)
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

func RedisSet(key, val string) error {
	client := global.GLOBAL_RES.GetRedisClusterClient()
	if client != nil {
		cmd := client.Do("set", key, val)
		_, err := cmd.Result()
		if err != nil {
			return err
		} else {
			return nil
		}
	} else {
		return err.RedisErr{ErrInfo: consts.ERR_REDIS_POOL_NIL}
	}
}
