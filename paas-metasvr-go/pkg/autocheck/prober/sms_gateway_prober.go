package prober

import (
	"fmt"
	"strconv"

	"github.com/maoge/paas-metasvr-go/pkg/autodeploy/maintainer"
	"github.com/maoge/paas-metasvr-go/pkg/config"
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/dao/redisdao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"

	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/sequence"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

type SmsGatewayProber struct {
}

func (h *SmsGatewayProber) DoCheck(servInstID, servType string) bool {
	paasResult := result.NewResultBean()
	servJson, _, ok := DeployUtils.LoadServTopo(servInstID, "", true, paasResult)
	if !ok {
		return false
	}

	serverContainer := servJson[consts.HEADER_SMS_SERVER_CONTAINER].(map[string]interface{})
	processContainer := servJson[consts.HEADER_SMS_PROCESS_CONTAINER].(map[string]interface{})
	clientContainer := servJson[consts.HEADER_SMS_CLIENT_CONTAINER].(map[string]interface{})
	batsaveContainer := servJson[consts.HEADER_SMS_BATSAVE_CONTAINER].(map[string]interface{})
	statsContainer := servJson[consts.HEADER_SMS_STATS_CONTAINER].(map[string]interface{})

	h.checkSmsInstanceArr(consts.HEADER_SMS_SERVER, servInstID, servType, serverContainer)
	h.checkSmsInstanceArr(consts.HEADER_SMS_PROCESS, servInstID, servType, processContainer)
	h.checkSmsInstanceArr(consts.HEADER_SMS_CLIENT, servInstID, servType, clientContainer)
	h.checkSmsInstanceArr(consts.HEADER_SMS_BATSAVE, servInstID, servType, batsaveContainer)
	h.checkSmsInstanceArr(consts.HEADER_SMS_STATS, servInstID, servType, statsContainer)

	return true
}

func (h *SmsGatewayProber) checkSmsInstanceArr(header, servInstId, servType string, container map[string]interface{}) {
	smsInstanceArrRaw := container[header]
	if smsInstanceArrRaw == nil {
		return
	}

	smsInstanceArr := smsInstanceArrRaw.([]map[string]interface{})
	if len(smsInstanceArr) == 0 {
		return
	}

	for _, item := range smsInstanceArr {
		instId := item[consts.HEADER_INST_ID].(string)
		instance := meta.CMPT_META.GetInstance(instId)
		// 未部署的实例不用检测
		if !instance.IsDeployed() {
			continue
		}

		// 停机维护的实例不检测
		// 预埋的不检测
		if instance.STATUS == consts.STR_WARN || instance.STATUS == consts.STR_PRE_EMBADDED {
			continue
		}

		cmptId := instance.CMPT_ID
		cmpt := meta.CMPT_META.GetCmptById(cmptId)
		cmptName := cmpt.CMPT_NAME

		ip := item[consts.HEADER_IP].(string)
		passwd := consts.SMS_CONSOLE_PASSWD
		realPort := DeployUtils.GetRealPortAsInt(item)

		key := fmt.Sprintf("alarm-%s-%d", instId, consts.ALARM_APP_PROC_DOWN)
		if !h.pingSmsGateway(cmptName, instId, ip, passwd, realPort) {
			// 在告警窗口内不重复生成
			h.generateAlarm(key, servInstId, servType, instId, cmptName, consts.ALARM_APP_PROC_DOWN)
		} else {
			// 如果已经恢复则复位
			if instance.STATUS == consts.STR_ALARM {
				ClearAlarm(key, instId)

				errMsg := fmt.Sprintf("proc recoverd, SERV_INST_ID:%s, INST_ID:%s, CMPT_NAME:%s", servInstId, instId, cmptName)
				utils.LOGGER.Error(errMsg)
			}
		}
	}
}

func (h *SmsGatewayProber) generateAlarm(key, servInstId, servType, instId, cmptName string, alarmCode int) {
	paasResult := result.NewResultBean()
	metadao.UpdateInstanceDeployFlag(instId, consts.STR_ALARM, "", "", paasResult)

	exist, err := redisdao.Exists(key)
	if err != nil {
		utils.LOGGER.Error(err.Error())
		return
	}

	if !exist {
		alarmId, err := sequence.SEQ.NextId(consts.SEQ_ALARM)
		if err != nil {
			err := fmt.Sprintf("get sequence error: %s", err.Error())
			utils.LOGGER.Error(err)
		} else {
			errMsg := fmt.Sprintf("proc down, SERV_INST_ID:%s, INST_ID:%s, CMPT_NAME:%s", servInstId, instId, cmptName)
			utils.LOGGER.Error(errMsg)
			redisdao.Set(key, strconv.FormatInt(alarmId, 10))
			redisdao.Pexpire(key, config.META_SVR_CONFIG.AlarmTimeWindow)

			metadao.InsertAlarm(alarmId, servInstId, servType, instId, cmptName, alarmCode, utils.CurrentTimeMilli())

			if config.META_SVR_CONFIG.AlarmNotifyEnabled {
				sshId := meta.CMPT_META.GetInstAttr(instId, 116).ATTR_VALUE // 116 -> 'SSH_ID'
				ip := meta.CMPT_META.GetSshById(sshId).SERVER_IP
				alarmInfo := GetAlarmInfo(alarmCode)
				msg := fmt.Sprintf("servInstId: %s, instId: %s, cmptName: %s, ip: %s", servInstId, instId, cmptName, ip)
				NotifyAlarmCenter(consts.SMS_APP_CODE, alarmInfo, instId, alarmInfo, msg)
			}
		}
	}

	if alarmCode == consts.ALARM_APP_PROC_DOWN {
		h.doRecover(servInstId, servType, instId, cmptName)
	}
}

func (h *SmsGatewayProber) pingSmsGateway(cmptName, instId, ip, passwd string, realPort int) bool {
	result := false
	retry := 0

	for {
		if retry > consts.CONSOLE_MAX_RETRY {
			break
		}

		var connector = utils.SmsWebConsoleConnector{
			IP:     ip,
			Port:   realPort,
			Passwd: passwd,
		}

		if connector.Connect() {
			if connector.SendData([]byte(consts.CMD_PING)) {
				result = true
				break
			}
			connector.Close()
		}

		retry++
	}

	return result
}

func (h *SmsGatewayProber) doRecover(servInstId, servType, instId, cmptName string) {
	// 先尝试恢复异常的进程，异常进程不能恢复的情况下再通过拉起预埋的配置来达到异常隔离
	if RecoverCashedProc(servInstId, servType, instId, cmptName) {
		return
	}

	if RecoverBackupProc(servInstId, servType, instId, cmptName) {
		utils.LOGGER.Info(fmt.Sprintf("servType: %s, cmptName: %s, instId: %s, 异常恢复失败，尝试拉起备份进程成功", servType, cmptName, instId))
	} else {
		utils.LOGGER.Info(fmt.Sprintf("servType: %s, cmptName: %s, instId: %s, 异常恢复失败，尝试拉起备份进程失败", servType, cmptName, instId))
	}
}

func RecoverCashedProc(servInstId, servType, instId, cmptName string) bool {
	paasResult := result.NewResultBean()
	smsServiceMaintainer := new(maintainer.SmsGatewayMaintainer)

	op := &consts.INSTANCE_OPERATION_START
	startResult := smsServiceMaintainer.MaintainInstance(servInstId, instId, servType, op, false, "", "", paasResult)

	return startResult
}

func RecoverBackupProc(servInstId, servType, instId, cmptName string) bool {
	if cmptName == consts.HEADER_SMS_SERVER {
		// smsserver 运维需求不需要拉起备份，因要手工配套修改防火墙路由配置
		return false
	}

	info := fmt.Sprintf("servType: %s, cmptName: %s, instId: %s 异常恢复失败，尝试拉起备份进程", servType, cmptName, instId)
	utils.LOGGER.Info(info)

	// 首先查找对应的预埋配置
	preEmbeddedInstId := findPreEmbeddedInst(servInstId, servType, instId, cmptName)
	// 拉起预埋配置对应的实例, 预埋配置已经提前做了包安装以便缩短恢复需要的时间
	if preEmbeddedInstId == "" {
		errMsg := fmt.Sprintf("servType: %s, cmptName: %s, instId: %s, 异常恢复失败，找不到预埋配置", servType, cmptName, instId)
		utils.LOGGER.Error(errMsg)
		return false
	}

	preEmbaddedInst := meta.CMPT_META.GetInstance(preEmbeddedInstId)
	if !preEmbaddedInst.IsDeployed() {
		errMsg := fmt.Sprintf("servType: %s, cmptName: %s, instId: %s, 异常恢复失败，预埋实例未部署 %s", servType, cmptName, instId, preEmbeddedInstId)
		utils.LOGGER.Error(errMsg)
		return false
	}

	recoverResult := RecoverCashedProc(servInstId, servType, preEmbeddedInstId, cmptName)
	if recoverResult {
		// 预埋实例拉起成功，修改PRE_EMBEDDED属性为S_FALSE，视为与正常实例一样
		result := result.NewResultBean()
		// String magicKey = "";
		recoverResult = metadao.UpdateInstancePreEmbadded(preEmbeddedInstId, consts.S_FALSE, "", "", result)

		// 将老的置成预埋状态
		metadao.UpdateInstancePreEmbadded(instId, consts.S_TRUE, "", "", result)
		metadao.UpdateInstanceDeployFlag(instId, consts.STR_PRE_EMBADDED, "", "", result)
	}

	return recoverResult
}

func findPreEmbeddedInst(servInstId, servType, instId, cmptName string) string {
	topoList := meta.CMPT_META.GetSameLevelInstList(servInstId, instId)
	if topoList == nil || len(topoList) == 0 {
		return ""
	}

	embaddedInstId := ""
	switch cmptName {
	case consts.HEADER_SMS_PROCESS:
		embaddedInstId = GetPreEmbaddedSmsProcess(topoList, instId)
		break
	case consts.HEADER_SMS_CLIENT:
		embaddedInstId = GetPreEmbaddedSmsClient(topoList, instId)
		break
	case consts.HEADER_SMS_BATSAVE:
		embaddedInstId = GetPreEmbaddedSmsBatSave(topoList, instId)
		break
	case consts.HEADER_SMS_STATS:
		embaddedInstId = GetPreEmbaddedSmsStats(topoList, instId)
		break
	case consts.HEADER_SMS_QUERY:
		embaddedInstId = GetPreEmbaddedSmsQuery(topoList, instId)
		break
	default:
		break
	}

	return embaddedInstId
}
