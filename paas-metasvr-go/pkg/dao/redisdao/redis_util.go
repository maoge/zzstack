package redisdao

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/err"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func GetSessionFromRedis(userSessionKey string) *proto.AccountSession {
	val, err := Get(userSessionKey)
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
	Set(key, str)
}

func Get(key string) (interface{}, error) {
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

func Set(key, val string) error {
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

func Pexpire(key string, expire int) (bool, error) {
	client := global.GLOBAL_RES.GetRedisClusterClient()
	if client != nil {
		cmd := client.Do("pexpire", key)
		val, err := cmd.Result()
		if err != nil {
			return false, err
		} else {
			cnt := val.(int)
			return cnt > 0, nil
		}
	} else {
		return false, err.RedisErr{ErrInfo: consts.ERR_REDIS_POOL_NIL}
	}
}

func Del(key string) error {
	client := global.GLOBAL_RES.GetRedisClusterClient()
	if client != nil {
		cmd := client.Do("del", key)
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

func Exists(key string) (bool, error) {
	client := global.GLOBAL_RES.GetRedisClusterClient()
	if client != nil {
		cmd := client.Do("exists", key)
		val, err := cmd.Result()
		if err != nil {
			return false, err
		} else {
			cnt := val.(int)
			return cnt > 0, nil
		}
	} else {
		return false, err.RedisErr{ErrInfo: consts.ERR_REDIS_POOL_NIL}
	}
}
