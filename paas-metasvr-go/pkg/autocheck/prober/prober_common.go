package prober

import (
	"fmt"
	"strconv"

	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
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
	if !config.META_SVR_CONFIG.AlarmNotifyEnabled {
		return true
	}

	url := fmt.Sprintf("%s/%s", config.META_SVR_CONFIG.AlarmNotifyUrl, consts.ADD_ALARM_EVENT_URI)
	req := make(map[string]string)
	req[consts.HEADER_APPLICATION_CODE] = appCode
	req[consts.HEADER_APP_ALARM_CODE] = alarmCode
	req[consts.HEADER_VARIABLE] = variable
	req[consts.HEADER_ABSTRACT_MSG] = abstractMsg
	req[consts.HEADER_MSG] = msg

	postData := utils.Struct2Json(req)
	bytes := utils.PostJson(url, &postData)
	if bytes != nil {
		resp := make(map[string]string)
		utils.Json2Struct(bytes, &resp)

		if resp[consts.HEADER_CODE] != "200" {
			errMsg := fmt.Sprintf("send alarm event fail: %s", resp[consts.HEADER_MESSAGE])
			utils.LOGGER.Error(errMsg)
			return false
		}
	}

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

func GetPreEmbaddedSmsProcess(topoList []*proto.PaasTopology, instId string) string {
	processor := meta.CMPT_META.GetInstAttr(instId, 205).ATTR_VALUE
	dbInstId := meta.CMPT_META.GetInstAttr(instId, 213).ATTR_VALUE
	orignalInst := meta.CMPT_META.GetInstance(instId)

	for _, topo := range topoList {
		id := (*topo).INST_ID2
		if id == instId {
			continue
		}

		preEmbaddedInst := meta.CMPT_META.GetInstance(id)
		if preEmbaddedInst == nil {
			continue
		}
		if preEmbaddedInst.CMPT_ID != orignalInst.CMPT_ID {
			continue
		}

		preEmbaddedAttr := meta.CMPT_META.GetInstAttr(id, 320) // 320 -> 'PRE_EMBEDDED'
		if preEmbaddedAttr == nil {
			continue
		}
		preEmbadded := preEmbaddedAttr.ATTR_VALUE
		if preEmbadded == consts.S_FALSE {
			continue
		}

		status := meta.CMPT_META.GetInstance(id).STATUS
		if status == consts.STR_DEPLOYED {
			continue
		}

		embaddedProcessor := meta.CMPT_META.GetInstAttr(id, 205).ATTR_VALUE // 205 -> 'PROCESSOR'
		embaddedDbInstId := meta.CMPT_META.GetInstAttr(id, 213).ATTR_VALUE  // 213 -> 'DB_INST_ID'
		if processor == embaddedProcessor && dbInstId == embaddedDbInstId {
			return id
		}
	}

	return ""
}

func GetPreEmbaddedSmsClient(topoList []*proto.PaasTopology, instId string) string {
	processor := meta.CMPT_META.GetInstAttr(instId, 205).ATTR_VALUE // 205 -> 'PROCESSOR'
	orignalInst := meta.CMPT_META.GetInstance(instId)

	for _, topo := range topoList {
		id := (*topo).INST_ID2
		if id == instId {
			continue
		}

		preEmbaddedInst := meta.CMPT_META.GetInstance(id)
		if preEmbaddedInst.CMPT_ID != orignalInst.CMPT_ID {
			continue
		}

		preEmbaddedAttr := meta.CMPT_META.GetInstAttr(id, 320) // 320 -> 'PRE_EMBEDDED'
		if preEmbaddedAttr == nil {
			continue
		}
		preEmbadded := preEmbaddedAttr.ATTR_VALUE
		if preEmbadded == consts.S_FALSE {
			continue
		}

		status := meta.CMPT_META.GetInstance(id).STATUS
		if status == consts.STR_DEPLOYED {
			continue
		}

		embaddedProcessor := meta.CMPT_META.GetInstAttr(id, 205).ATTR_VALUE // 205 -> 'PROCESSOR'
		if processor == embaddedProcessor {
			return id
		}
	}

	return ""
}

func GetPreEmbaddedSmsBatSave(topoList []*proto.PaasTopology, instId string) string {
	processor := meta.CMPT_META.GetInstAttr(instId, 205).ATTR_VALUE
	dbInstId := meta.CMPT_META.GetInstAttr(instId, 213).ATTR_VALUE
	orignalInst := meta.CMPT_META.GetInstance(instId)

	for _, topo := range topoList {
		id := (*topo).INST_ID2
		if id == instId {
			continue
		}

		preEmbaddedInst := meta.CMPT_META.GetInstance(id)
		if preEmbaddedInst.CMPT_ID != orignalInst.CMPT_ID {
			continue
		}

		preEmbaddedAttr := meta.CMPT_META.GetInstAttr(id, 320) // 320 -> 'PRE_EMBEDDED'
		if preEmbaddedAttr == nil {
			continue
		}

		preEmbadded := preEmbaddedAttr.ATTR_VALUE
		if preEmbadded == "" || preEmbadded == consts.S_FALSE {
			continue
		}

		status := meta.CMPT_META.GetInstance(id).STATUS
		if status == consts.STR_DEPLOYED {
			continue
		}

		embaddedProcessor := meta.CMPT_META.GetInstAttr(id, 205).ATTR_VALUE // 205 -> 'PROCESSOR'
		embaddedDbInstId := meta.CMPT_META.GetInstAttr(id, 213).ATTR_VALUE  // 213 -> 'DB_INST_ID'
		if processor == embaddedProcessor && dbInstId == embaddedDbInstId {
			return id
		}
	}

	return ""
}

func GetPreEmbaddedSmsStats(topoList []*proto.PaasTopology, instId string) string {
	orignalInst := meta.CMPT_META.GetInstance(instId)

	for _, topo := range topoList {
		id := (*topo).INST_ID2
		if id == instId {
			continue
		}

		preEmbaddedInst := meta.CMPT_META.GetInstance(id)
		if preEmbaddedInst.CMPT_ID != orignalInst.CMPT_ID {
			continue
		}

		preEmbaddedAttr := meta.CMPT_META.GetInstAttr(id, 320) // 320 -> 'PRE_EMBEDDED'
		if preEmbaddedAttr == nil {
			continue
		}
		preEmbadded := preEmbaddedAttr.ATTR_VALUE
		if preEmbadded == consts.S_FALSE {
			continue
		}

		status := meta.CMPT_META.GetInstance(id).STATUS
		if status == consts.STR_DEPLOYED {
			continue
		}

		return id
	}

	return ""
}

func GetPreEmbaddedSmsQuery(topoList []*proto.PaasTopology, instId string) string {
	orignalInst := meta.CMPT_META.GetInstance(instId)

	for _, topo := range topoList {
		id := (*topo).INST_ID2
		if id == instId {
			continue
		}

		preEmbaddedInst := meta.CMPT_META.GetInstance(id)
		if preEmbaddedInst.CMPT_ID != orignalInst.CMPT_ID {
			continue
		}

		preEmbaddedAttr := meta.CMPT_META.GetInstAttr(id, 320) // 320 -> 'PRE_EMBEDDED'
		if preEmbaddedAttr == nil {
			continue
		}
		preEmbadded := preEmbaddedAttr.ATTR_VALUE
		if preEmbadded == consts.S_FALSE {
			continue
		}

		status := meta.CMPT_META.GetInstance(id).STATUS
		if status == consts.STR_DEPLOYED {
			continue
		}

		return id
	}

	return ""
}
