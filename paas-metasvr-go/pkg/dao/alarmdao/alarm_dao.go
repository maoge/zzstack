package alarmdao

import (
	"fmt"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"

	crud "github.com/maoge/paas-metasvr-go/pkg/db"
)

var (
	SQL_SEL_ALARM_CNT  = "SELECT count(*) COUNT from t_meta_alarm where 1=1 %s"
	SQL_SEL_ALARM_LIST = `SELECT a.ALARM_ID,a.SERV_INST_ID,a.SERV_TYPE,a.INST_ID,a.CMPT_NAME,
                                 a.ALARM_TYPE,IFNULL(a.ALARM_TIME,0) ALARM_TIME,IFNULL(a.DEAL_TIME,0) DEAL_TIME,a.DEAL_ACC_NAME,a.IS_DEALED,
                                 s.SERV_NAME,s.SERV_CLAZZ,s.IS_PRODUCT,s.VERSION 
                            FROM t_meta_alarm a 
                            LEFT JOIN t_meta_service s 
                              ON a.SERV_INST_ID = s.INST_ID 
                           WHERE 1=1 %s ORDER BY ALARM_TIME DESC LIMIT ?, ?`

	SQL_UPD_ALARM_STATE_BY_ALARMID = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where ALARM_ID = ?"
)

func GetAlarmCount(getAlarmCountParam *proto.GetAlarmCountParam, resultBean *result.ResultBean) {
	sqlWhere := ""
	dealFlag := getAlarmCountParam.DEAL_FLAG
	if dealFlag != "" {
		sqlWhere = fmt.Sprintf(" AND IS_DEALED = %s", dealFlag)
	}
	sql := fmt.Sprintf(SQL_SEL_ALARM_CNT, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	out := proto.Count{}
	err := crud.SelectAsObject(dbPool, &out, &sql)
	if err == nil {
		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = out.COUNT
	} else {
		errInfo := fmt.Sprintf("GetAlarmCount fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func GetAlarmList(getAlarmListParam *proto.GetAlarmListParam, resultBean *result.ResultBean) {
	servInstId := getAlarmListParam.SERV_INST_ID
	instId := getAlarmListParam.INST_ID
	dealFlag := getAlarmListParam.DEAL_FLAG
	start := getAlarmListParam.PAGE_SIZE * (getAlarmListParam.PAGE_NUMBER - 1)
	pageSize := getAlarmListParam.PAGE_SIZE

	sqlWhere := ""
	if servInstId != "" {
		sqlWhere += fmt.Sprintf(" AND a.SERV_INST_ID = '%s'", servInstId)
	}
	if instId != "" {
		sqlWhere += fmt.Sprintf(" AND a.INST_ID = '%s'", instId)
	}
	if dealFlag != "" {
		sqlWhere += fmt.Sprintf(" AND IS_DEALED = '%s'", dealFlag)
	}

	sql := fmt.Sprintf(SQL_SEL_ALARM_LIST, sqlWhere)

	dbPool := global.GLOBAL_RES.GetDbPool()
	data, err := crud.SelectAsMapSlice(dbPool, &sql, start, pageSize)
	if err == nil {
		// process: timestamp -> string format date, ALARM_TYPE -> ALARM_INFO
		for _, item := range data {
			node := item.(map[string]interface{})
			alarmType := node[consts.HEADER_ALARM_TYPE].(int32)
			alarmInfo := consts.EVENT_MAP[alarmType]
			if alarmInfo != nil {
				node[consts.HEADER_ALARM_INFO] = alarmInfo
			} else {
				node[consts.HEADER_ALARM_INFO] = ""
			}

			alarmTime := node[consts.HEADER_ALARM_TIME].(int64)
			dealTime := node[consts.HEADER_DEAL_TIME].(int64)
			if alarmTime != 0 {
				node[consts.HEADER_ALARM_TIME] = utils.TimeFmt(&alarmTime)
			}
			if dealTime != 0 {
				node[consts.HEADER_DEAL_TIME] = utils.TimeFmt(&dealTime)
			}
		}

		resultBean.RET_CODE = consts.REVOKE_OK
		resultBean.RET_INFO = data
	} else {
		errInfo := fmt.Sprintf("GetAlarmList fail, %v", err.Error())
		utils.LOGGER.Error(errInfo)

		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
	}
}

func ClearAlarm(clearAlarmParam *proto.ClearAlarmParam, resultBean *result.ResultBean) {
	key := fmt.Sprintf("alarm-%s-%d", clearAlarmParam.INST_ID, clearAlarmParam.ALARM_TYPE)
	err := redisdao.Del(key)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = consts.ERR_CLEAR_ALARM_REDIS_CACHE_FAIL
		return
	}
	// SQL_UPD_ALARM_STATE_BY_ALARMID = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where ALARM_ID = ?"
	alarmId := clearAlarmParam.ALARM_ID
	dealTime := utils.CurrentTimeMilli()
	accName := clearAlarmParam.DEAL_ACC_NAME
	isDealed := consts.ALARM_DEALED

	dbPool := global.GLOBAL_RES.GetDbPool()
	_, err = crud.Update(dbPool, &SQL_UPD_ALARM_STATE_BY_ALARMID, dealTime, accName, isDealed, alarmId)
	if err != nil {
		resultBean.RET_CODE = consts.REVOKE_NOK
		resultBean.RET_INFO = err.Error()
		return
	}

	resultBean.RET_CODE = consts.REVOKE_OK
	resultBean.RET_INFO = ""
}
