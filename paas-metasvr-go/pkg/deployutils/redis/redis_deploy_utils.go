package redis

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func DeployFakeClusterService(servInstID, logKey, magicKey string, servJson map[string]interface{}, paasResult *result.ResultBean) bool {
	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArrRaw := nodeContainer[consts.HEADER_REDIS_NODE]
	proxyArrRaw := proxyContainer[consts.HEADER_REDIS_PROXY]

	// 1. deploy redis nodes
	if redisNodeArrRaw != nil {
		redisNodeArr := redisNodeArrRaw.([]map[string]interface{})
		for _, redisJson := range redisNodeArr {
			if len(redisJson) == 0 {
				continue
			}

			redisNodeID := redisJson[consts.HEADER_INST_ID].(string)
			if !metadao.UpdateInstanceDeployFlag(redisNodeID, consts.STR_TRUE, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 2. deploy proxy
	if proxyArrRaw != nil {
		proxyArr := proxyArrRaw.([]map[string]interface{})
		for _, proxyJson := range proxyArr {
			if len(proxyJson) == 0 {
				continue
			}

			proxyID := proxyJson[consts.HEADER_INST_ID].(string)
			if !metadao.UpdateInstanceDeployFlag(proxyID, consts.STR_TRUE, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 3. update t_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	if !metadao.ModServicePseudoFlag(servInstID, consts.DEPLOY_FLAG_PSEUDO, logKey, magicKey, paasResult) {
		return false
	}
	return true
}

func GetClusterNodes(redisNodeArr *[]map[string]interface{}) (string, string) {
	var nodes4cluster string = ""
	var nodes4proxy string = ""

	first := true

	for _, redisJson := range *redisNodeArr {
		sshId := redisJson[consts.HEADER_SSH_ID].(string)
		port := redisJson[consts.HEADER_PORT].(string)

		ssh := meta.CMPT_META.GetSshById(sshId)
		servIp := ssh.SERVER_IP

		node := fmt.Sprintf("%s:%s", servIp, port)

		if !first {
			nodes4cluster += " "
			nodes4proxy += ","
		} else {
			first = false
		}

		nodes4cluster += node
		nodes4proxy += node
	}

	return nodes4cluster, nodes4proxy
}

func DeployRedisNode(redisNode map[string]interface{}, init, expand, isCluster bool,
	node4cluster, version, logKey, magicKey string, paasResult *result.ResultBean) bool {

	maxConn := redisNode[consts.HEADER_MAX_CONN].(string)
	sMaxMem := redisNode[consts.HEADER_MAX_MEMORY].(string) // unit: GB
	tempMaxMem, _ := strconv.Atoi(sMaxMem)

	port := redisNode[consts.HEADER_PORT].(string)
	instId := redisNode[consts.HEADER_INST_ID].(string)
	sshId := redisNode[consts.HEADER_SSH_ID].(string)
	maxMem := strconv.Itoa(tempMaxMem * consts.UNIT_G)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	servIp := ssh.SERVER_IP
	sshName := ssh.SSH_NAME
	sshPwd := ssh.SSH_PWD
	sshPort := ssh.SSH_PORT

	info := fmt.Sprintf("start deploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClient(servIp, sshPort, sshName, sshPwd)
	if !sshClient.Connect() {
		errMsg := fmt.Sprintf("ssh connect: %s:%d", servIp, sshPort)
		utils.LOGGER.Error(errMsg)
		global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = errMsg
		return false
	} else {
		defer sshClient.Close()
	}

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	deployFile := DeployUtils.GetDeployFile(consts.CACHE_REDIS_SERVER_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME

	// 版本优先级: service.VERSION > deploy_file.VERSION
	if version == "" {
		version = deployFile.VERSION
	}

	// CACHE_REDIS_SERVER_FILE_ID -> 'redis-6.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.CACHE_REDIS_SERVER_FILE_ID, consts.CACHE_REDIS_ROOT, version, logKey, paasResult) {
		return false
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	idx := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[0:idx]

	newName := "redis_" + port
	// rm exists dir before deploy
	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}

	// mv to new name
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify redis configure files ......")

	if !DeployUtils.CD(sshClient, newName+"/etc", logKey, paasResult) {
		return false
	}

	newConf := fmt.Sprintf("redis_%s.conf", port)
	if !DeployUtils.MV(sshClient, newConf, consts.REDIS_CONF, logKey, paasResult) {
		return false
	}

	pidFile := fmt.Sprintf("redis_%s.pid", port)
	aofFile := fmt.Sprintf("appendonly_%s.aof", port)
	confFile := fmt.Sprintf("nodes_%s.conf", port)

	// bind %SERV_IP%
	// port %SERV_PORT%
	// pidfile ./data/%PID_FILE%
	// requirepass %PASSWORD%
	// maxclients %MAX_CONN%
	// maxmemory %MAX_MEMORY%
	// appendfilename %APPENDONLY_FILENAME%
	// cluster-config-file %REDIS_CONF_FILENAME%
	// cluster-enabled %CLUSTER_ENABLED%
	clusterEnabledFlag := "no"
	if isCluster {
		clusterEnabledFlag = "yes"
	}

	if !DeployUtils.SED(sshClient, consts.CONF_SERV_IP, servIp, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_SERV_PORT, port, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PID_FILE, pidFile, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_CONN, maxConn, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_MEMORY, maxMem, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_APPENDONLY_FILENAME, aofFile, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.REDIS_CLUSTER_CONF_FILENAME, confFile, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_CLUSTER_ENABLED, clusterEnabledFlag, newConf, logKey, paasResult) {
		return false
	}

	// if needs auth, new node added to redis cluster need set auth setting before join.
	if expand {
		global.GLOBAL_RES.PubLog(logKey, "set requirepass and masterauth ......")
		requirepass := "requirepass " + consts.ZZSOFT_REDIS_PASSWD
		masterauth := "masterauth " + consts.ZZSOFT_REDIS_PASSWD
		if !DeployUtils.AddLine(sshClient, requirepass, newConf, logKey, paasResult) {
			return false
		}
		if !DeployUtils.AddLine(sshClient, masterauth, newConf, logKey, paasResult) {
			return false
		}
	}

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")
	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}

	startShell := fmt.Sprintf("./bin/redis-server ./etc/%s", newConf)
	if !DeployUtils.CreateShell(sshClient, consts.START_SHELL, startShell, logKey, paasResult) {
		return false
	}

	stopShell := fmt.Sprintf("./bin/redis-cli -h %s -p %s -a %s -c shutdown", servIp, port, consts.ZZSOFT_REDIS_PASSWD)
	if !DeployUtils.CreateShell(sshClient, consts.STOP_SHELL, stopShell, logKey, paasResult) {
		return false
	}

	stopNoAuthShell := fmt.Sprintf("./bin/redis-cli -h %s -p %s -c shutdown", servIp, port)
	if !DeployUtils.CreateShell(sshClient, consts.STOP_NOAUTH_SHELL, stopNoAuthShell, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start redis-server ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "redis-server", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	// init redis cluster when the last node deploy ok
	if init {
		initCmd := fmt.Sprintf("./bin/redis-cli --cluster create %s --cluster-replicas %d", node4cluster, consts.REDIS_CLUSTER_REPLICAS)
		global.GLOBAL_RES.PubLog(logKey, "init redis cluster ......")

		if !DeployUtils.InitRedisCluster(sshClient, initCmd, logKey, paasResult) {
			return false
		}
	}

	return true
}

func DeployProxyNode(proxy map[string]interface{}, nodes4proxy, logKey, magicKey string, paasResult *result.ResultBean) bool {

	return true
}
