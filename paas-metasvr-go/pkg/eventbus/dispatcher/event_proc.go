package dispatcher

import (
	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func ProcAddService(msg string) {
	service := proto.ParsePaasService(msg)
	meta.CMPT_META.AddService(service)
}

func ProcDelService(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	meta.CMPT_META.DelService(instId)
}

func ProcModService(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	meta.CMPT_META.ReloadService(instId)
}

func ProcUpdServiceDeploy(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	deployFlag := jsonMap[consts.HEADER_IS_DEPLOYED].(string)
	meta.CMPT_META.UpdServDeploy(instId, deployFlag)
}

func ProcAddInstance(msg string) {
	instance := proto.ParsePaasInstance(msg)
	meta.CMPT_META.AddInstance(instance)
}

func ProcDelInstance(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	meta.CMPT_META.DelInstance(instId)
}

func ProcUpdInstPos(msg string) {
	instance := proto.ParsePaasInstance(msg)
	meta.CMPT_META.UpdInstPos(instance)
}

func ProcUpdInstDeploy(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	deployFlag := jsonMap[consts.HEADER_IS_DEPLOYED].(string)
	meta.CMPT_META.UpdInstDeploy(instId, deployFlag)
}

func ProcAddInstAttr(msg string) {
	instAttr := proto.ParsePaasInstAttr(msg)
	meta.CMPT_META.AddInstAttr(instAttr)
}

func ProcDelInstAttr(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	meta.CMPT_META.DelInstAttr(instId)
}

func AddTopo(msg string) {
	topo := proto.ParsePaasTopology(msg)
	meta.CMPT_META.AddTopo(topo)
}

func DelTopo(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	parentId := jsonMap[consts.HEADER_PARENT_ID].(string)
	instId := jsonMap[consts.HEADER_INST_ID].(string)

	if instId == "" {
		meta.CMPT_META.DelParentTopo(parentId)
	} else {
		meta.CMPT_META.DelTopo(parentId, instId)
	}
}

func ModTopo(msg string) {
	topo := proto.ParsePaasTopology(msg)
	meta.CMPT_META.ModTopo(topo)
}

func ProcAddServer(msg string) {
	server := proto.ParsePaasServer(msg)
	meta.CMPT_META.AddServer(server)
}

func ProcDelServer(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	serverIp := jsonMap[consts.HEADER_SERVER_IP].(string)
	meta.CMPT_META.DelServer(serverIp)
}

func AddSSH(msg string) {
	ssh := proto.ParsePaasSSH(msg)
	meta.CMPT_META.AddSsh(ssh)
}

func ModSSH(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	serverIp := jsonMap[consts.HEADER_SERVER_IP].(string)
	sshId := jsonMap[consts.HEADER_SSH_ID].(string)
	sshName := jsonMap[consts.HEADER_SSH_NAME].(string)
	sshPwd := jsonMap[consts.HEADER_SSH_PWD].(string)
	sshPort := jsonMap[consts.HEADER_SSH_PORT].(int)
	meta.CMPT_META.ModSsh(serverIp, sshId, sshName, sshPwd, sshPort)
}

func DelSSH(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	serverIp := jsonMap[consts.HEADER_SERVER_IP].(string)
	sshId := jsonMap[consts.HEADER_SSH_ID].(string)
	meta.CMPT_META.DelSsh(serverIp, sshId)
}

func AddSession(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	accName := jsonMap[consts.HEADER_ACC_NAME].(string)
	magicKey := jsonMap[consts.HEADER_MAGIC_KEY].(string)
	sessionTimeOut := jsonMap[consts.HEADER_SESSION_TIMEOUT].(int64)

	session := proto.NewAccountSessionWithTTL(accName, magicKey, sessionTimeOut)
	meta.CMPT_META.AddAccSession(session, true)
}

func RemoveSession(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	accName := jsonMap[consts.HEADER_ACC_NAME].(string)
	magicKey := jsonMap[consts.HEADER_MAGIC_KEY].(string)

	meta.CMPT_META.RemoveTtlSession(accName, magicKey, true)
}

func AdjustQueueWeight(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instAId := jsonMap[consts.HEADER_INST_ID_A].(string)
	instBId := jsonMap[consts.HEADER_INST_ID_B].(string)

	weightA := jsonMap[consts.HEADER_WEIGHT_A].(string)
	weightB := jsonMap[consts.HEADER_WEIGHT_B].(string)

	meta.CMPT_META.AdjustSmsABQueueWeightInfo(instAId, weightA, instBId, weightB)
}

func SwitchDBType(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	dgContainerID := jsonMap[consts.HEADER_INST_ID].(string)
	dbType := jsonMap[consts.HEADER_ACTIVE_DB_TYPE].(string)

	meta.CMPT_META.SwitchSmsDBType(dgContainerID, dbType)
}

func AddCmptVerion(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	servType := jsonMap[consts.HEADER_SERV_TYPE].(string)
	version := jsonMap[consts.HEADER_VERSION].(string)

	meta.CMPT_META.AddCmptVersion(servType, version)
}

func DelCmptVerion(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	servType := jsonMap[consts.HEADER_SERV_TYPE].(string)
	version := jsonMap[consts.HEADER_VERSION].(string)

	meta.CMPT_META.DelCmptVersion(servType, version)
}

func ModPasswd(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	accName := jsonMap[consts.HEADER_ACC_NAME].(string)
	passwd := jsonMap[consts.HEADER_PASSWORD].(string)

	meta.CMPT_META.ModPasswd(accName, passwd)
}

func ProcUpdInstPreEmbadded(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	instId := jsonMap[consts.HEADER_INST_ID].(string)
	proEmbadded := jsonMap[consts.HEADER_PRE_EMBADDED].(string)

	meta.CMPT_META.UpdInstPreEmbadded(instId, proEmbadded)
}

func ProcReloadMetaData(msg string) {
	jsonMap := make(map[string]interface{})
	utils.Json2Struct([]byte(msg), &jsonMap)

	loadType := jsonMap[consts.HEADER_RELOAD_TYPE].(string)
	meta.CMPT_META.ReloadMetaData(loadType)
}
