package deployutils

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func GetServiceTopo(servInstID, logKey string, paasResult *result.ResultBean) bool {
	if !metadao.LoadServiceTopo(servInstID, paasResult) {
		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, paasResult.RET_INFO.(string))
		}

		return false
	}

	return true
}

func GetService(instID string, logKey string, paasResult *result.ResultBean) (*proto.PaasService, bool) {
	service := meta.CMPT_META.GetService(instID)
	if service == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_FOUND

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_FOUND)
		}

		return nil, false
	}

	return service, true
}

func GetInstance(instID string, logKey string, paasResult *result.ResultBean) (*proto.PaasInstance, bool) {
	inst := meta.CMPT_META.GetInstance(instID)
	if inst == nil {
		errMsg := fmt.Sprintf("%s, instID:%s", consts.ERR_INSTANCE_NOT_FOUND, instID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_FOUND

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return nil, false
	}

	return inst, true
}

func IsServiceDeployed(logKey string, service *proto.PaasService, paasResult *result.ResultBean) bool {
	if service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_ALLREADY_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_ALLREADY_DEPLOYED)
		}

		return true
	}
	return false
}

func IsServiceNotDeployed(logKey string, service *proto.PaasService, paasResult *result.ResultBean) bool {
	if !service.IsDeployed() {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SERVICE_NOT_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, consts.ERR_SERVICE_NOT_DEPLOYED)
		}

		return true
	}
	return false
}

func IsInstanceDeployed(logKey string, inst *proto.PaasInstance, paasResult *result.ResultBean) bool {
	if inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is allready deployed, inst_id:%s", inst.INST_ID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_ALLREADY_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return true
	}

	return false
}

func IsInstanceNotDeployed(logKey string, inst *proto.PaasInstance, paasResult *result.ResultBean) bool {
	if !inst.IsDeployed() {
		errMsg := fmt.Sprintf("instance is not deployed, inst_id:%s", inst.INST_ID)
		utils.LOGGER.Error(errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INSTANCE_NOT_DEPLOYED

		if logKey != "" {
			global.GLOBAL_RES.PubFailLog(logKey, errMsg)
		}

		return true
	}

	return false
}

func GetCmptById(servInstID, instID string, cmptID int, paasResult *result.ResultBean) *proto.PaasMetaCmpt {
	cmpt := meta.CMPT_META.GetCmptById(cmptID)
	if cmpt == nil {
		errMsg := fmt.Sprintf("service type not found, service_id:%s, inst_id:%s, cmpt_id:%d", servInstID, instID, cmptID)
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return cmpt
}

func GetDeployFile(fileId int, logKey string, paasResult *result.ResultBean) *proto.PaasDeployFile {
	deployFile := meta.CMPT_META.GetDeployFile(fileId)
	if deployFile == nil {
		errMsg := fmt.Sprintf("deploy file id: %d not found ......", fileId)
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return deployFile
}

func CheckPortUpPredeploy(sshClient *SSHClient, port, logKey string, paasResult *result.ResultBean) bool {
	using, err := sshClient.IsPortUsed(port)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if using {
		errMsg := fmt.Sprintf("redis-server: %s, port: %s is in using", sshClient.Ip, port)
		global.GLOBAL_RES.PubFailLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
	}

	return using
}

func FetchAndExtractTgzDeployFile(sshClient *SSHClient, fileId int, subPath, version, logKey string, paasResult *result.ResultBean) bool {
	deployFile := GetDeployFile(fileId, logKey, paasResult)
	if deployFile == nil {
		return false
	}

	hostId := deployFile.HOST_ID
	srcFileName := deployFile.FILE_NAME
	srcFileDir := deployFile.FILE_DIR

	if version == "" {
		version = deployFile.VERSION
	}

	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	deployHost := meta.CMPT_META.GetDeployHost(hostId)

	srcIp := deployHost.IP_ADDRESS
	srcPort := deployHost.SSH_PORT
	srcUser := deployHost.USER_NAME
	srcPwd := deployHost.USER_PWD

	rootDir := fmt.Sprintf("%s/%s", consts.PAAS_ROOT, subPath)

	global.GLOBAL_RES.PubLog(logKey, "create install dir ......")
	if !MkDir(sshClient, rootDir, logKey, paasResult) {
		return false
	}
	if !CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	srcFile := srcFileDir + srcFileName
	desFile := "./" + srcFileName
	global.GLOBAL_RES.PubLog(logKey, "scp deploy file ......")
	if !sshClient.SCP(srcUser, srcPwd, srcIp, srcPort, srcFile, desFile, logKey, paasResult) {
		return false
	}

	// 防止文件没有下载下来
	if !IsFileExist(sshClient, desFile, false, logKey, paasResult) {
		return false
	}

	idx := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:idx]

	global.GLOBAL_RES.PubLog(logKey, "unpack install tar file ......")
	if !TAR(sshClient, consts.TAR_ZXVF, srcFileName, oldName, logKey, paasResult) {
		return false
	}
	if !RM(sshClient, srcFileName, logKey, paasResult) {
		return false
	}

	return true
}

func InitRedisCluster(sshClient *SSHClient, initCmd, logKey string, paasResult *result.ResultBean) bool {
	bytes, ok, err := sshClient.InitRedisCluster(initCmd)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if ok {
		global.GLOBAL_RES.PubLog(logKey, string(bytes))
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_INIT_REDIS_CLUSTER_FAIL
	}

	return ok
}

func JoinRedisCluster(sshClient *SSHClient, cmdJoin, logKey string, paasResult *result.ResultBean) bool {
	bytes, ok, err := sshClient.JoinRedisCluster(cmdJoin)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if ok {
		global.GLOBAL_RES.PubLog(logKey, string(bytes))
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_JOIN_REDIS_CLUSTER_FAIL
	}

	return true
}

func RedisSlaveOf(sshClient *SSHClient, slaveCmd, logKey string, paasResult *result.ResultBean) bool {
	bytes, ok, err := sshClient.RedisSlaveOf(slaveCmd)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if ok {
		global.GLOBAL_RES.PubLog(logKey, string(bytes))
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_JOIN_REDIS_CLUSTER_FAIL
	}

	return true
}

func ReshardingRedisSlot(sshClient *SSHClient, cmdMig, logKey string, paasResult *result.ResultBean) bool {
	bytes, ok, err := sshClient.ReshardingRedisSlot(cmdMig)
	if err != nil {
		global.GLOBAL_RES.PubErrorLog(logKey, err.Error())
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = err.Error()
		return false
	}

	if ok {
		global.GLOBAL_RES.PubLog(logKey, string(bytes))
	} else {
		global.GLOBAL_RES.PubErrorLog(logKey, string(bytes))
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_RESHARDING_REDIS_SLOT_FAIL
	}

	return true
}

func ConnectSSH(sshClient *SSHClient, logKey string, paasResult *result.ResultBean) bool {
	if !sshClient.Connect() {
		errMsg := fmt.Sprintf("ssh connect: %s:%d", sshClient.Ip, sshClient.SshPort)
		utils.LOGGER.Error(errMsg)
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return false
	}

	return true
}

func GetZKAddress(zkArr []map[string]interface{}) string {
	result := ""
	for idx, item := range zkArr {
		sshID := item[consts.HEADER_SSH_ID].(string)
		clientPort1 := item[consts.HEADER_ZK_CLIENT_PORT1].(string)
		clientPort2 := item[consts.HEADER_ZK_CLIENT_PORT2].(string)

		ssh := meta.CMPT_META.GetSshById(sshID)
		servIP := ssh.SERVER_IP

		// server.1=host1:2888:3888
		// server.2=host2:2888:3888
		// server.3=host3:2888:3888
		line := fmt.Sprintf("server.%d=%s:%s:%s", (idx + 1), servIP, clientPort1, clientPort2)
		if idx > 0 {
			result += "\n"
		}

		result += line
	}

	return result
}

func GetZkCluster(zkArr []map[string]interface{}) string {
	//    <node>
	//        <host>172.20.0.41</host>
	//        <port>24003</port>
	//    </node>
	//    <node>
	//        <host>172.20.0.42</host>
	//        <port>24003</port>
	//    </node>
	//    <node>
	//        <host>172.20.0.43</host>
	//        <port>24003</port>
	//    </node>

	result := ""
	size := len(zkArr)
	for i, zk := range zkArr {
		sshID := zk[consts.HEADER_SSH_ID].(string)
		clientPort := zk[consts.HEADER_CLIENT_PORT].(string)

		ssh := meta.CMPT_META.GetSshById(sshID)
		if ssh == nil {
			continue
		}

		host := fmt.Sprintf("                <host>%s</host>", ssh.SERVER_IP)
		port := fmt.Sprintf("                <port>%s</port>", clientPort)

		zkNode := ""
		zkNode += "            <node>"
		zkNode += consts.LINE_END

		zkNode += host
		zkNode += consts.LINE_END

		zkNode += port
		zkNode += consts.LINE_END

		zkNode += "            </node>"
		zkNode += consts.LINE_END

		result += zkNode
		if i < size-1 {
			result += consts.LINE_END
		}

	}

	return result
}

func GetSpecifiedItem(jsonArr []map[string]interface{}, instID string) map[string]interface{} {
	for _, item := range jsonArr {
		id := item[consts.HEADER_INST_ID].(string)
		if id == instID {
			return item
		}
	}
	return nil
}

func GetSpecifiedClickHouseItem(jsonArr []map[string]interface{}, instID string) (map[string]interface{}, string) {
	for _, replicas := range jsonArr {
		clickHouseArr := replicas[consts.HEADER_CLICKHOUSE_SERVER].([]map[string]interface{})

		for _, clickhouse := range clickHouseArr {
			id := clickhouse[consts.HEADER_INST_ID].(string)
			if id == instID {
				replicasID := replicas[consts.HEADER_INST_ID].(string)
				return clickhouse, replicasID
			}
		}
	}
	return nil, ""
}

func GetVersionedFileName(fileId int, version, logKey string, paasResult *result.ResultBean) string {
	deployFile := GetDeployFile(fileId, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	if version == "" || strings.Trim(version, " ") == "" {
		version = deployFile.VERSION
	}

	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	idx := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	return srcFileName[:idx]
}

func FetchFile(sshClient *SSHClient, fileId int, logKey string, paasResult *result.ResultBean) bool {
	deployFile := GetDeployFile(fileId, logKey, paasResult)
	if deployFile == nil {
		global.GLOBAL_RES.PubFailLog(logKey, fmt.Sprintf("meta file not found, fileId: %d", fileId))
		return false
	}

	hostId := deployFile.HOST_ID
	srcFileName := deployFile.FILE_NAME
	srcFileDir := deployFile.FILE_DIR

	deployHost := meta.CMPT_META.GetDeployHost(hostId) // MetaSvrGlobalRes.get().getCmptMeta().getDeployHost(hostId);
	srcIp := deployHost.IP_ADDRESS
	srcPort := deployHost.SSH_PORT
	srcUser := deployHost.USER_NAME
	srcPwd := deployHost.USER_PWD

	srcFile := srcFileDir + srcFileName
	desFile := "./" + srcFileName
	global.GLOBAL_RES.PubLog(logKey, "scp deploy file ......")

	if !sshClient.SCP(srcUser, srcPwd, srcIp, srcPort, srcFile, desFile, logKey, paasResult) {
		return false
	}

	return true
}

func DeployZookeeper(zk map[string]interface{}, idx int, version, zkAddrList, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := zk[consts.HEADER_SSH_ID].(string)
	clientPort := zk[consts.HEADER_CLIENT_PORT].(string)
	adminPort := zk[consts.HEADER_ADMIN_PORT].(string)
	instId := zk[consts.HEADER_INST_ID].(string)
	clientPort1 := zk[consts.HEADER_ZK_CLIENT_PORT1].(string)
	clientPort2 := zk[consts.HEADER_ZK_CLIENT_PORT2].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := NewSSHClientBySSH(ssh)
	if !ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy zookeeper: %s:%s, instId:%s", ssh.SERVER_IP, adminPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if CheckPortUpPredeploy(sshClient, clientPort, logKey, paasResult) {
		return false
	}

	if CheckPortUpPredeploy(sshClient, adminPort, logKey, paasResult) {
		return false
	}

	if CheckPortUpPredeploy(sshClient, clientPort1, logKey, paasResult) {
		return false
	}

	if CheckPortUpPredeploy(sshClient, clientPort2, logKey, paasResult) {
		return false
	}

	// ZK_FILE_ID -> 'zookeeper-3.7.0.tar.gz'
	if !FetchAndExtractTgzDeployFile(sshClient, consts.ZK_FILE_ID, consts.COMMON_TOOLS_ROOT, version, logKey, paasResult) {
		return false
	}

	deployFile := GetDeployFile(consts.ZK_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	// 版本优先级: service.VERSION > deploy_file.VERSION
	if version == "" {
		version = deployFile.VERSION
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	pos := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:pos]
	newName := oldName + "_" + adminPort

	// rm exists dir before deploy
	if !RM(sshClient, newName, logKey, paasResult) {
		return false
	}

	// mv to new name
	if !MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}

	if !CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// dataDir=%DATA_DIR%
	// dataLogDir=%LOG_DIR%
	// admin.serverPort=%ADMIN_PORT%
	global.GLOBAL_RES.PubLog(logKey, "modify start.sh and stop.sh env params ......")

	pwd, _ := PWD(sshClient, logKey, paasResult)
	dataDir := fmt.Sprintf("%s/%s", pwd, "data")
	logDir := fmt.Sprintf("%s/%s/%s", pwd, "data", "log")

	dataDir = strings.ReplaceAll(dataDir, "/", "\\\\/")
	logDir = strings.ReplaceAll(logDir, "/", "\\\\/")
	configFile := "./conf/zoo.cfg"

	if !SED(sshClient, consts.CONF_DATA_DIR, dataDir, configFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_LOG_DIR, logDir, configFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_ADMIN_PORT, adminPort, configFile, logKey, paasResult) {
		return false
	}
	if !AddLine(sshClient, zkAddrList, configFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_CLIENT_PORT, clientPort, configFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_CLIENT_ADDRESS, ssh.SERVER_IP, configFile, logKey, paasResult) {
		return false
	}

	myIdFile := "data/myid"
	context := fmt.Sprintf("%d", idx)
	if !AddLine(sshClient, context, myIdFile, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start zookeeper ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !CheckPortUp(sshClient, "zookeeper", instId, adminPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployZookeeper(zk map[string]interface{}, version, logKey, magicKey string, paasResult *result.ResultBean) bool {
	sshId := zk[consts.HEADER_SSH_ID].(string)
	adminPort := zk[consts.HEADER_ADMIN_PORT].(string)
	instId := zk[consts.HEADER_INST_ID].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := NewSSHClientBySSH(ssh)
	if !ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy zookeeper, inst_id:%s, serv_ip:%s, admin_port:%s", instId, ssh.SERVER_IP, adminPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	deployFile := GetDeployFile(consts.ZK_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	// 版本优先级: service.VERSION > deploy_file.VERSION
	if version == "" {
		version = deployFile.VERSION
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	pos := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:pos]
	newName := oldName + "_" + adminPort
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, newName)

	if !CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop zookeeper ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !CheckPortDown(sshClient, "zookeeper", instId, adminPort, logKey, paasResult) {
		return false
	}

	CD(sshClient, "..", logKey, paasResult)
	RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployGrafana(grafana map[string]interface{}, version, logKey, magicKey string, paasResult *result.ResultBean) bool {
	instId := grafana[consts.HEADER_INST_ID].(string)
	sshId := grafana[consts.HEADER_SSH_ID].(string)
	httpPort := grafana[consts.HEADER_HTTP_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := NewSSHClientBySSH(ssh)
	if !ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy grafana: %s:%s, instId:%s", ssh.SERVER_IP, httpPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if CheckPortUpPredeploy(sshClient, httpPort, logKey, paasResult) {
		return false
	}

	// GRAFANA_FILE_ID -> 'grafana-7.5.7.tar.gz'
	if !FetchAndExtractTgzDeployFile(sshClient, consts.GRAFANA_FILE_ID, consts.COMMON_TOOLS_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := GetVersionedFileName(consts.GRAFANA_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, httpPort)

	// rm exists dir before deploy
	if !RM(sshClient, newName, logKey, paasResult) {
		return false
	}

	// mv to new name
	if !MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}

	if !CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// conf/defaults.ini
	// http_addr = %HTTP_ADDR%
	// http_port = %HTTP_PORT%
	// domain = %DOMAIN%
	confFile := "conf/defaults.ini"
	if !SED(sshClient, consts.CONF_HTTP_ADDR, ssh.SERVER_IP, confFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_HTTP_PORT, httpPort, confFile, logKey, paasResult) {
		return false
	}
	if !SED(sshClient, consts.CONF_DOMAIN, ssh.SERVER_IP, confFile, logKey, paasResult) {
		return false
	}

	// stop.sh
	// %GRAFANA_DIR%
	if !SED(sshClient, consts.CONF_GRAFANA_DIR, newName, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start grafana ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !CheckPortUp(sshClient, "grafana", instId, httpPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployGrafana(grafana map[string]interface{}, version, logKey, magicKey string, paasResult *result.ResultBean) bool {
	instId := grafana[consts.HEADER_INST_ID].(string)
	sshId := grafana[consts.HEADER_SSH_ID].(string)
	httpPort := grafana[consts.HEADER_HTTP_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := NewSSHClientBySSH(ssh)
	if !ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy grafana, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, httpPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	deployFile := GetDeployFile(consts.GRAFANA_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	// 版本优先级: service.VERSION > deploy_file.VERSION
	if version == "" {
		version = deployFile.VERSION
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	pos := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:pos]
	newName := oldName + "_" + httpPort
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, newName)

	if !CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop grafana ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !CheckPortDown(sshClient, "grafana", instId, httpPort, logKey, paasResult) {
		return false
	}

	CD(sshClient, "..", logKey, paasResult)
	RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployPrometheus(prometheus map[string]interface{}, version, logKey, magicKey string, paasResult *result.ResultBean) bool {
	instId := prometheus[consts.HEADER_INST_ID].(string)
	sshId := prometheus[consts.HEADER_SSH_ID].(string)
	prometheusPort := prometheus[consts.HEADER_PROMETHEUS_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := NewSSHClientBySSH(ssh)
	if !ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy prometheus, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, prometheusPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	deployFile := GetDeployFile(consts.PROMETHEUS_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	// 版本优先级: service.VERSION > deploy_file.VERSION
	if version == "" {
		version = deployFile.VERSION
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	pos := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:pos]
	newName := oldName + "_" + prometheusPort
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.COMMON_TOOLS_ROOT, newName)

	if !CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop grafana ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !CheckPortDown(sshClient, "prometheus", instId, prometheusPort, logKey, paasResult) {
		return false
	}

	CD(sshClient, "..", logKey, paasResult)
	RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}
