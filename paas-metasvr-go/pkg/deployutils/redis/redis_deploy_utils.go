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
	"github.com/maoge/paas-metasvr-go/pkg/proto"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func RedisMasterSlaveServiceFakeDeploy(servJson map[string]interface{}, servInstID, logKey, magicKey,
	operType string, paasResult *result.ResultBean) bool {

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	redisNodeArr := nodeContainer[consts.HEADER_REDIS_NODE].([]map[string]interface{})

	var deployFlag string = ""
	var pseudoFlag string = ""
	if operType == consts.STR_DEPLOY {
		deployFlag = consts.STR_TRUE
		pseudoFlag = consts.DEPLOY_FLAG_PSEUDO
	} else {
		deployFlag = consts.STR_FALSE
		pseudoFlag = consts.DEPLOY_FLAG_PHYSICAL
	}

	for _, redisNode := range redisNodeArr {
		redisNodeID := redisNode[consts.HEADER_INST_ID].(string)
		if !metadao.UpdateInstanceDeployFlag(redisNodeID, deployFlag, logKey, magicKey, paasResult) {
			return false
		}
	}

	if !metadao.UpdateInstanceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}

	if !metadao.UpdateServiceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}

	if !metadao.ModServicePseudoFlag(servInstID, pseudoFlag, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func RedisClusterServiceFakeDeploy(servJson map[string]interface{}, servInstID, logKey, magicKey,
	operType string, paasResult *result.ResultBean) bool {

	nodeContainer := servJson[consts.HEADER_REDIS_NODE_CONTAINER].(map[string]interface{})
	proxyContainer := servJson[consts.HEADER_REDIS_PROXY_CONTAINER].(map[string]interface{})

	redisNodeArrRaw := nodeContainer[consts.HEADER_REDIS_NODE]
	proxyArrRaw := proxyContainer[consts.HEADER_REDIS_PROXY]

	var deployFlag string = ""
	var pseudoFlag string = ""
	if operType == consts.STR_DEPLOY {
		deployFlag = consts.STR_TRUE
		pseudoFlag = consts.DEPLOY_FLAG_PSEUDO
	} else {
		deployFlag = consts.STR_FALSE
		pseudoFlag = consts.DEPLOY_FLAG_PHYSICAL
	}

	// 1. undeploy redis nodes
	if redisNodeArrRaw != nil {
		redisNodeArr := redisNodeArrRaw.([]map[string]interface{})
		for _, redisJson := range redisNodeArr {
			if len(redisJson) == 0 {
				continue
			}

			redisNodeID := redisJson[consts.HEADER_INST_ID].(string)
			if !metadao.UpdateInstanceDeployFlag(redisNodeID, deployFlag, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 2. undeploy proxy
	if proxyArrRaw != nil {
		proxyArr := proxyArrRaw.([]map[string]interface{})
		for _, proxyJson := range proxyArr {
			if len(proxyJson) == 0 {
				continue
			}

			proxyID := proxyJson[consts.HEADER_INST_ID].(string)
			if !metadao.UpdateInstanceDeployFlag(proxyID, deployFlag, logKey, magicKey, paasResult) {
				return false
			}
		}
	}

	// 3. update t_service.is_deployed and local cache
	if !metadao.UpdateInstanceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}
	if !metadao.UpdateServiceDeployFlag(servInstID, deployFlag, logKey, magicKey, paasResult) {
		return false
	}

	if !metadao.ModServicePseudoFlag(servInstID, pseudoFlag, logKey, magicKey, paasResult) {
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

func DeploySingleRedisNode(redisNode map[string]interface{}, redisNodeArr []map[string]interface{}, isMaster bool,
	version, logKey, magicKey string, paasResult *result.ResultBean) bool {

	maxConn := redisNode[consts.HEADER_MAX_CONN].(string)
	maxMem := redisNode[consts.HEADER_MAX_MEMORY].(string) // unit: GB
	port := redisNode[consts.HEADER_PORT].(string)
	instId := redisNode[consts.HEADER_INST_ID].(string)
	sshId := redisNode[consts.HEADER_SSH_ID].(string)

	lMaxMem, err := strconv.ParseInt(maxMem, 10, 64)
	if err != nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_PARSE_REDIS_MAX_MEM
		return false
	}
	lMaxMem = lMaxMem * consts.UNIT_G

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	info := fmt.Sprintf("start deploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
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

	// CACHE_REDIS_SERVER_FILE_ID -> 'redis-xxx.tar.gz'
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

	if !DeployUtils.SED(sshClient, consts.CONF_SERV_IP, ssh.SERVER_IP, newConf, logKey, paasResult) {
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

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create start and stop shell ......")
	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}

	startShell := fmt.Sprintf("./bin/redis-server ./etc/%s", newConf)
	if !DeployUtils.CreateShell(sshClient, consts.START_SHELL, startShell, logKey, paasResult) {
		return false
	}

	stopShell := fmt.Sprintf("./bin/redis-cli -h %s -p %s -a %s -c --no-auth-warning shutdown", ssh.SERVER_IP, port, consts.ZZSOFT_REDIS_PASSWD)
	if !DeployUtils.CreateShell(sshClient, consts.STOP_SHELL, stopShell, logKey, paasResult) {
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

	// slave node exec slaveof
	if !isMaster {
		masterNode := getMasNode(redisNodeArr)
		if masterNode == nil {
			global.GLOBAL_RES.PubErrorLog(logKey, consts.ERR_NO_REDIS_MASTER_NODE)
			return false
		}

		masterPort := masterNode[consts.HEADER_PORT].(string)
		masterSshId := masterNode[consts.HEADER_SSH_ID].(string)

		masterSSH := meta.CMPT_META.GetSshById(sshId)
		if masterSSH == nil {
			errMsg := fmt.Sprintf("%s, ssh_id: %s", consts.ERR_METADATA_NOT_FOUND, masterSshId)
			global.GLOBAL_RES.PubErrorLog(logKey, errMsg)

			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = errMsg
			return false
		}
		masterHost := masterSSH.SERVER_IP

		cmdSlaveOf := fmt.Sprintf("./bin/redis-cli -h %s -p %s slaveof %s %s", sshClient.Ip, port, masterHost, masterPort)
		global.GLOBAL_RES.PubLog(logKey, cmdSlaveOf)

		if !DeployUtils.RedisSlaveOf(sshClient, cmdSlaveOf, logKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "join as slave node NOK ......")

			cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
			DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult)

			return false
		}
		global.GLOBAL_RES.PubSuccessLog(logKey, "join as slave node OK ......")
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeploySingleRedisNode(redisNode map[string]interface{}, shrink bool, logKey, magicKey string, paasResult *result.ResultBean) bool {
	port := redisNode[consts.HEADER_PORT].(string)
	instId := redisNode[consts.HEADER_INST_ID].(string)
	sshId := redisNode[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	info := fmt.Sprintf("start undeploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	redisDir := fmt.Sprintf("redis_%s", port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.CACHE_REDIS_ROOT, redisDir)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop redis-server ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "redis-server", instId, port, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, redisDir, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func getMasNode(redisNodeArr []map[string]interface{}) map[string]interface{} {
	if redisNodeArr == nil || len(redisNodeArr) == 0 {
		return nil
	}

	for _, node := range redisNodeArr {
		nodeTypeRaw := node[consts.ATTR_NODE_TYPE]
		if nodeTypeRaw == nil {
			continue
		}

		nodeType := nodeTypeRaw.(string)
		if nodeType == consts.TYPE_REDIS_MASTER_NODE {
			return node
		}
	}

	return nil
}

func DeployRedisNode(redisNode map[string]interface{}, init, expand, isCluster bool, join bool,
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

	info := fmt.Sprintf("start deploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
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

	// CACHE_REDIS_SERVER_FILE_ID -> 'redis-xxx.tar.gz'
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

	if !DeployUtils.SED(sshClient, consts.CONF_SERV_IP, ssh.SERVER_IP, newConf, logKey, paasResult) {
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

	stopShell := fmt.Sprintf("./bin/redis-cli -h %s -p %s -a %s -c --no-auth-warning shutdown", ssh.SERVER_IP, port, consts.ZZSOFT_REDIS_PASSWD)
	if !DeployUtils.CreateShell(sshClient, consts.STOP_SHELL, stopShell, logKey, paasResult) {
		return false
	}

	stopNoAuthShell := fmt.Sprintf("./bin/redis-cli -h %s -p %s -c shutdown", ssh.SERVER_IP, port)
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

	// init redis cluster when the last node deploy ok
	if init {
		initCmd := fmt.Sprintf("./bin/redis-cli --cluster create %s --cluster-replicas %d", node4cluster, consts.REDIS_CLUSTER_REPLICAS)
		global.GLOBAL_RES.PubLog(logKey, "init redis cluster ......")

		if !DeployUtils.InitRedisCluster(sshClient, initCmd, logKey, paasResult) {
			return false
		}
	}

	// elasticlly add new node, need join to the cluster.
	// join as MASTER/SLAVE role relly on cluster node state
	if join {
		joinIp, joinPort, ok := getOneNodeForJoin(node4cluster, ssh.SERVER_IP+":"+port, paasResult)
		if !ok {
			return false
		}

		cmd = fmt.Sprintf("./bin/redis-cli -h %s -p %s -c --no-auth-warning cluster nodes", joinIp, joinPort)
		if !DeployUtils.GetRedisClusterNode(sshClient, cmd, logKey, paasResult) {
			return false
		}
		cluster := new(proto.PaasRedisCluster)
		cluster.Parse(paasResult.RET_INFO.(string))
		paasResult.RET_INFO = ""

		joinMasterAddr, ok := cluster.GetAnyMasterAddr()
		if !ok {
			paasResult.RET_CODE = consts.REVOKE_NOK
			paasResult.RET_INFO = consts.ERR_NO_REDIS_MASTER_NODE
			return false
		}

		// first check whether REDIS_CLUSTER_REPLICAS > 0 and have mater node with no slave
		// if REDIS_CLUSTER_REPLICAS == 0 new node is joined with master role,
		// or else need to check cluster status to jutify which role.
		if consts.REDIS_CLUSTER_REPLICAS > 0 {
			aloneMasterId := cluster.GetAloneMaster()
			if aloneMasterId == "" {
				// join as master node
				if !joinAsMasterNode(sshClient, joinMasterAddr, ssh.SERVER_IP, port, logKey, cluster, paasResult) {
					return false
				}
			} else {
				// join as slave node
				if !joinAsSlaveNode(sshClient, joinMasterAddr, aloneMasterId, ssh.SERVER_IP, port, logKey, paasResult) {
					return false
				}
			}
		}
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func joinAsMasterNode(sshClient *DeployUtils.SSHClient, joinMasterAddr, ip, port, logKey string,
	cluster *proto.PaasRedisCluster, paasResult *result.ResultBean) bool {

	cmdJoin := fmt.Sprintf("./bin/redis-cli --cluster add-node %s:%s %s -c --no-auth-warning", ip, port, joinMasterAddr)
	global.GLOBAL_RES.PubLog(logKey, cmdJoin)

	if !DeployUtils.JoinRedisCluster(sshClient, cmdJoin, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "join as master node NOK ......")
		return false
	}

	// migrate slot from exist master nodes to newer
	// get self node id
	cmdSelf := fmt.Sprintf("./bin/redis-cli -h %s -p %s -a %s -c --no-auth-warning cluster nodes | grep myself", ip, port, consts.ZZSOFT_REDIS_PASSWD)
	if !DeployUtils.GetRedisClusterNode(sshClient, cmdSelf, logKey, paasResult) {
		return false
	}

	masterCnt := len(cluster.MasterNodes)
	slotAvg := consts.REDIS_CLUSTER_TTL_SLOT / (masterCnt + 1)
	moveSlotCnt := slotAvg / masterCnt

	// b91f8eec7570e89c8f24ce97f2cb5d18c588415c 172.20.0.171:13001@23001 myself,master - 0 1649765390000 1 connected 0-5460
	slefInfo := paasResult.RET_INFO.(string)
	idx := strings.Index(slefInfo, " ")
	selfId := slefInfo[:idx]
	for _, masterNode := range cluster.MasterNodes {
		srcNodeId := masterNode.NodeId
		// do migrate slot from exist master nodes to newer
		cmdMig := fmt.Sprintf("./bin/redis-cli --cluster reshard %s:%s --cluster-from %s --cluster-to %s --cluster-slots %d -c -a %s --no-auth-warning --cluster-yes",
			masterNode.Ip, masterNode.Port, srcNodeId, selfId, moveSlotCnt, consts.ZZSOFT_REDIS_PASSWD)
		if !DeployUtils.ReshardingRedisSlot(sshClient, cmdMig, logKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "resharding slot NOK ......")
			return false
		}
	}

	global.GLOBAL_RES.PubSuccessLog(logKey, "join as master node OK ......")
	return true
}

func joinAsSlaveNode(sshClient *DeployUtils.SSHClient, joinMasterAddr, aloneMasterId, ip, port, logKey string, paasResult *result.ResultBean) bool {
	cmdJoin := fmt.Sprintf("./bin/redis-cli --cluster add-node %s:%s %s -c --cluster-slave --no-auth-warning --cluster-master-id %s", ip, port, joinMasterAddr, aloneMasterId)
	global.GLOBAL_RES.PubLog(logKey, cmdJoin)

	if !DeployUtils.JoinRedisCluster(sshClient, cmdJoin, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "join as slave node NOK ......")
		return false
	}

	global.GLOBAL_RES.PubSuccessLog(logKey, "join as slave node OK ......")
	return true
}

func getOneNodeForJoin(node4cluster, exclude string, paasResult *result.ResultBean) (string, string, bool) {
	arr := strings.Split(node4cluster, " ")
	for _, str := range arr {
		if str != exclude {
			addrSlice := strings.Split(str, ":")
			return addrSlice[0], addrSlice[1], true
		}
	}

	paasResult.RET_CODE = consts.REVOKE_NOK
	paasResult.RET_INFO = consts.ERR_NO_EXISTING_REDIS_NODE

	return "", "", false
}

func UndeployRedisNode(redisJson map[string]interface{}, shrink bool, logKey, magicKey string, paasResult *result.ResultBean) bool {
	port := redisJson[consts.HEADER_PORT].(string)
	instId := redisJson[consts.HEADER_INST_ID].(string)
	sshId := redisJson[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	servIp := ssh.SERVER_IP

	info := fmt.Sprintf("start undeploy redis-server, inst_id:%s, serv_ip:%s, port:%d", instId, servIp, ssh.SSH_PORT)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	redisDir := fmt.Sprintf("redis_%s", port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.CACHE_REDIS_ROOT, redisDir)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	if shrink {
		redisDir := fmt.Sprintf("redis_%s", port)
		rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.CACHE_REDIS_ROOT, redisDir)
		cmd := fmt.Sprintf("./bin/redis-cli -h %s -p %s -c --no-auth-warning cluster nodes", ssh.SERVER_IP, port)
		if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
			return false
		}
		if !DeployUtils.GetRedisClusterNode(sshClient, cmd, logKey, paasResult) {
			return false
		}
		cluster := new(proto.PaasRedisCluster)
		cluster.Parse(paasResult.RET_INFO.(string))
		paasResult.RET_INFO = ""

		// if self node is master role, must undeploy relate slave node.
		self := cluster.GetSelfInfo(ssh.SERVER_IP, port)
		if self == nil {
			global.GLOBAL_RES.PubLog(logKey, "cluster getSelfInfo no data return ......")
			return false
		}

		selfId := self.NodeId
		slotRange := self.SlotRange

		info := fmt.Sprintf("shrink, id:%s, role:%d", selfId, self.RedisRole)
		global.GLOBAL_RES.PubLog(logKey, info)

		if self.RedisRole == consts.REDIS_ROLE_MASTER {
			// remove slaves from cluster
			slaves := cluster.GetSlaves(selfId)

			for _, slaveNode := range slaves {
				info := fmt.Sprintf("remove sub-slave node:{%s:%s %s} from cluster", slaveNode.Ip, slaveNode.Port, slaveNode.NodeId)
				global.GLOBAL_RES.PubLog(logKey, info)

				if !DeployUtils.RemoveRedisNodeFromCluster(sshClient, slaveNode.Ip, slaveNode.Port, slaveNode.NodeId, logKey, paasResult) {
					global.GLOBAL_RES.PubFailLog(logKey, "remove slave node NOK ......")
					return false
				}
				global.GLOBAL_RES.PubLog(logKey, "remove slave node OK ......")
			}

			// migrate master slot to other masters
			if slotRange != "" {
				masters := cluster.GetMasters()
				avgMoveSlotCnt := self.GetSlotCount() / (len(masters) - 1)

				if len(masters) <= consts.REDIS_CLUSTER_MIN_MASTER_NODES {
					info := fmt.Sprintf("master node size: %d, cannot be shrinked", len(masters))
					global.GLOBAL_RES.PubLog(logKey, info)
					return true
				}

				info := fmt.Sprintf("slot <<%s>> not null, need to migrate slot by tools first", slotRange)
				global.GLOBAL_RES.PubLog(logKey, info)

				for _, node := range masters {
					desNodeId := node.NodeId
					if desNodeId == selfId {
						continue
					}

					// redis cluster slot resharding
					cmd := fmt.Sprintf("./bin/redis-cli --cluster reshard %s:%s --cluster-from %s --cluster-to %s --cluster-slots %d -c --no-auth-warning --cluster-yes",
						node.Ip, node.Port, selfId, desNodeId, avgMoveSlotCnt)
					if !sshClient.MigrateRedisClusterSlot(cmd, logKey, paasResult) {
						return false
					}
				}
			}

			// remove master from cluster
			removeInfo := fmt.Sprintf("remove master node:{%s:%s %s} from cluster", servIp, port, selfId)
			global.GLOBAL_RES.PubLog(logKey, removeInfo)

			if !DeployUtils.RemoveRedisNodeFromCluster(sshClient, servIp, port, selfId, logKey, paasResult) {
				global.GLOBAL_RES.PubLog(logKey, "remove master node NOK ......")
				return false
			}
			global.GLOBAL_RES.PubLog(logKey, "remove master node from cluster OK ......")
		} else {
			// remove SLAVE ROLE Redis Node
			if !DeployUtils.RemoveRedisNodeFromCluster(sshClient, servIp, port, selfId, logKey, paasResult) {
				global.GLOBAL_RES.PubFailLog(logKey, "remove slave node from cluster NOK ......")
				return false
			}
			global.GLOBAL_RES.PubLog(logKey, "remove slave node from cluster OK ......")
		}

	}

	// stop
	// global.GLOBAL_RES.PubLog(logKey, "stop redis-server ......")
	shell := fmt.Sprintf("./%s", consts.STOP_NOAUTH_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, shell, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "redis-server", instId, port, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, redisDir, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployProxyNode(proxy map[string]interface{}, nodes4proxy, logKey, magicKey string, paasResult *result.ResultBean) bool {
	// "REDIS_PROXY": [
	//     {
	//         "INST_ID": "955518f4-775b-bdf2-57ea-564cb197bb01",
	//         "MAX_CONN": "10000",
	//         "NODE_CONN_POOL_SIZE": "10",
	//         "PORT": "8000",
	//         "POS": { },
	//         "SSH_ID": "24b06d9d-624e-4e69-8e9d-ac957754b8ee"
	//     }
	// ]

	instId := proxy[consts.HEADER_INST_ID].(string)
	maxConn := proxy[consts.HEADER_MAX_CONN].(string)
	connPoolSize := proxy[consts.HEADER_NODE_CONN_POOL_SIZE].(string)
	proxyThreads := proxy[consts.HEADER_PROXY_THREADS].(string)
	port := proxy[consts.HEADER_PORT].(string)
	sshId := proxy[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	info := fmt.Sprintf("start deploy redis-proxy, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}

	deployFile := DeployUtils.GetDeployFile(consts.CACHE_REDIS_PROXY_FILE_ID, logKey, paasResult)
	srcFileName := deployFile.FILE_NAME
	version := deployFile.VERSION

	// CACHE_REDIS_PROXY_FILE_ID -> 'redis-cluster-proxy-1.0.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.CACHE_REDIS_PROXY_FILE_ID, consts.CACHE_REDIS_ROOT, version, logKey, paasResult) {
		return false
	}

	// 替换 %VERSION% 为真实版本
	if strings.Index(srcFileName, consts.REG_VERSION) != -1 && version != "" {
		srcFileName = strings.Replace(srcFileName, consts.REG_VERSION, version, -1)
	}

	idx := strings.Index(srcFileName, consts.TAR_GZ_SURFIX)
	oldName := srcFileName[:idx]
	newName := fmt.Sprintf("%s%s", consts.CACHE_REDIS_PROXY_PREFIX, port)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "modify redis_proxy configure file ......")
	newConf := fmt.Sprintf("proxy_%s.conf", port)
	if !DeployUtils.CD(sshClient, newName+"/etc", logKey, paasResult) {
		return false
	}

	if !DeployUtils.MV(sshClient, newConf, consts.PROXY_CONF, logKey, paasResult) {
		return false
	}

	// cluster %CLUSTER_NODES%
	// port %SERV_PORT%
	// bind %SERV_IP%
	// connections-pool-size %CONN_POOL_SIZE%
	// pidfile ./data/%PID_FILE%
	// logfile ./log/%LOG_FILE%
	// maxclients %MAX_CONN%
	// threads %PROXY_THREADS%
	// auth %PASSWORD%
	pidFile := fmt.Sprintf("proxy_%s.pid", port)
	logFile := fmt.Sprintf("proxy_%s.log", port)

	if !DeployUtils.SED(sshClient, consts.CONF_CLUSTER_NODES, nodes4proxy, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_SERV_PORT, port, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_SERV_IP, ssh.SERVER_IP, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_CONN_POOL_SIZE, connPoolSize, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PID_FILE, pidFile, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_LOG_FILE, logFile, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_MAX_CONN, maxConn, newConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PROXY_THREADS, proxyThreads, newConf, logKey, paasResult) {
		return false
	}

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create proxy start and stop shell ......")
	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}

	if !DeployUtils.SED(sshClient, consts.CONF_PORT, port, consts.START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PORT, port, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start redis cluster proxy ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "redis-proxy", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployProxyNode(proxy map[string]interface{}, force bool, logKey, magicKey string, paasResult *result.ResultBean) bool {
	instId := proxy[consts.HEADER_INST_ID].(string)
	port := proxy[consts.HEADER_PORT].(string)
	sshId := proxy[consts.HEADER_SSH_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

	info := fmt.Sprintf("start undeploy redis-proxy, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	proxyDir := fmt.Sprintf("%s%s", consts.CACHE_REDIS_PROXY_PREFIX, port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.CACHE_REDIS_ROOT, proxyDir)
	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop redis-proxy ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !force && !DeployUtils.CheckPortDown(sshClient, "redis-server", instId, port, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CD(sshClient, "..", logKey, paasResult) {
		return false
	}
	if !DeployUtils.RM(sshClient, proxyDir, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	info = fmt.Sprintf("undeploy redis-proxy success, inst_id:%s, serv_ip:%s, port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubSuccessLog(logKey, info)

	return true
}

func CheckMasterNode(arr []map[string]interface{}, paasResult *result.ResultBean) bool {
	count := 0
	ret := false
	for _, item := range arr {
		strNodeType := item[consts.ATTR_NODE_TYPE].(string)
		if strNodeType == consts.TYPE_REDIS_MASTER_NODE {
			count++
		}
	}

	if count == 0 {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_NO_REDIS_MASTER_NODE
	} else if count == 1 {
		ret = true
	} else if count > 1 {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_TOO_MUCH_REDIS_MASTER_NODE
	}

	return ret
}

func GetSelfRedisNode(redisNodeArr []map[string]interface{}, instID string) map[string]interface{} {
	for _, node := range redisNodeArr {
		currIDRaw := node[consts.HEADER_INST_ID]
		if currIDRaw != nil {
			currID := currIDRaw.(string)
			if currID == instID {
				return node
			}
		}
	}

	return nil
}
