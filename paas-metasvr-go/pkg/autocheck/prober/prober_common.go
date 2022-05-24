package prober

import (
	"strconv"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func GetAlarmInfo(alarmCode int) string {
	result := ""
	switch alarmCode {
	case consts.ALARM_NONE:
		result = "alarm-none"
		break
	case consts.ALARM_APP_PROC_DOWN:
		result = "app-process-down"
		break
	case consts.ALARM_DISK_HIGH_WATERMARK:
		result = "disk-high-watermark"
		break
	case consts.ALARM_MEM_HIGH_WATERMARK:
		result = "memory-high-watermark"
		break
	default:
		break
	}

	return result
}

func NotifyAlarmCenter(appCode, alarmCode, variable, abstractMsg, msg string) bool {
	return true
}

func ClearAlarm(key, instId string) {
	exists, err := redisdao.Exists(key)
	if err != nil {
		return
	}

	if exists {
		s, _ := redisdao.Get(key)
		if s != nil {
			alarmId, _ := strconv.ParseInt(s.(string), 10, 64)

			paasResult := result.NewResultBean()
			metadao.UpdateAlarmStateByAlarmId(alarmId, utils.CurrentTimeMilli(), consts.SYS_USER, consts.ALARM_DEALED, paasResult)

			redisdao.Del(key)
		}
	}
}
