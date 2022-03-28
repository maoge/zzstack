package accdao

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	"github.com/maoge/paas-metasvr-go/pkg/eventbus"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"

	crud "github.com/maoge/paas-metasvr-go/pkg/db"
)

var (
	UPD_ACC_PASSWD   = "UPDATE t_account SET PASSWD = ? WHERE ACC_NAME = ?"
	SEL_OP_LOG_COUNT = `SELECT count(1) COUNT FROM t_meta_oplogs WHERE 1=1 AND ACC_NAME = ? AND INSERT_TIME >= ? AND INSERT_TIME <= ?`
	SEL_OP_LOG_LIST  = `SELECT ACC_NAME, EVENT_TYPE, OP_DETAIL, INSERT_TIME FROM t_meta_oplogs 
                         WHERE 1=1 AND ACC_NAME = ? AND INSERT_TIME >= ? AND INSERT_TIME <= ? ORDER BY INSERT_TIME ASC LIMIT ?, ?`
)

func Login(loginParam *proto.LoginParam, resultBean *result.ResultBean) bool {
	account := meta.CMPT_META.GetAccount(loginParam.USER)
	if account == nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_ACCOUNT_NOT_EXISTS
		return false
	}

	encrypt := utils.GeneratePasswd(loginParam.USER, loginParam.PASSWORD)
	if encrypt != account.PASSWD {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_PWD_INCORRECT
		return false
	}

	session := meta.CMPT_META.GetAccSession(loginParam.USER)
	if session == nil {
		key := utils.GetRedisSessionKey(loginParam.USER)
		session = redisdao.GetSessionFromRedis(key)

		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = session.MAGIC_KEY

		if session == nil {
			magicKey := utils.GenUUID()
			newSession := proto.NewAccountSession(loginParam.USER, magicKey)
			meta.CMPT_META.AddAccSession(newSession, false)
		} else {
			if !session.IsSessionValid() {
				// remove the old and add new on
				resultBean.RET_INFO = createSession(loginParam.USER, session.MAGIC_KEY)
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
		resultBean.RET_INFO = createSession(loginParam.USER, session.MAGIC_KEY)
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

func ModPasswd(modPasswdParam *proto.ModPasswdParam, resultBean *result.ResultBean) {
	accSession := meta.CMPT_META.GetSessionByMagicKey(modPasswdParam.MAGIC_KEY)
	if accSession == nil || !accSession.IsSessionValid() {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_LOGIN_TIMOUT_OR_NOT_LOGINED
		return
	}
	accName := accSession.ACC_NAME
	passwd := modPasswdParam.PASSWORD
	encrypt := utils.GeneratePasswd(accName, passwd)

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err := crud.Update(dbPool, &UPD_ACC_PASSWD, encrypt, accName)
	if err == nil {
		msgBodyMap := make(map[string]interface{})
		msgBodyMap[consts.HEADER_ACC_NAME] = accName
		msgBodyMap[consts.HEADER_PASSWORD] = encrypt

		msgBody := utils.Struct2Json(msgBodyMap)
		event := proto.NewPaasEvent(consts.EVENT_MOD_ACC_PASSWD.CODE, msgBody, "")

		eventbus.EVENTBUS.PublishEvent(event)
	} else {
		errInfo := fmt.Sprintf("accName:%v ModPasswd fail, %v", accName, err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetOpLogCnt(getOpLogCntParam *proto.GetOpLogCntParam, resultBean *result.ResultBean) {
	user := getOpLogCntParam.USER
	startTs := getOpLogCntParam.START_TS
	endTs := getOpLogCntParam.END_TS

	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &SEL_OP_LOG_COUNT, user, startTs, endTs)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("accName:%v GetOpLogCnt fail, %v", getOpLogCntParam.USER, err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetOpLogList(getOpLogListParam *proto.GetOpLogListParam, resultBean *result.ResultBean) {
	user := getOpLogListParam.USER
	startTs := getOpLogListParam.START_TS
	endTs := getOpLogListParam.END_TS
	start := getOpLogListParam.PAGE_SIZE * (getOpLogListParam.PAGE_NUMBER - 1)
	pageSize := getOpLogListParam.PAGE_SIZE

	dbPool := global.GLOBAL_RES.GetDbPool()
	data, err := crud.SelectAsMapSlice(dbPool, &SEL_OP_LOG_LIST, user, startTs, endTs, start, pageSize)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("accName:%v GetOpLogList fail, %v", getOpLogListParam.USER, err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}
