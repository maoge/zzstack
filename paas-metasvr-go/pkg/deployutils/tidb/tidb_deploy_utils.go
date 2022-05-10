package tidb

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
)

func GetPDLongAddress(pdArr []map[string]interface{}) string {
	result := ""
	for idx, pd := range pdArr {
		sshID := pd[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshID)
		if ssh == nil {
			continue
		}

		servIp := ssh.SERVER_IP
		peerPort := pd[consts.HEADER_PEER_PORT].(string)
		instId := pd[consts.HEADER_INST_ID].(string)

		if idx > 0 {
			result += ","
		}

		line := fmt.Sprintf("%s=http://%s:%s", instId, servIp, peerPort)
		result += line
	}

	return result
}

func GetPDShortAddress(pdArr []map[string]interface{}) string {
	result := ""
	for idx, pd := range pdArr {
		sshID := pd[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshID)
		if ssh == nil {
			continue
		}

		servIp := ssh.SERVER_IP
		clientPort := pd[consts.HEADER_CLIENT_PORT].(string)

		if idx > 0 {
			result += ","
		}

		line := fmt.Sprintf("%s:%s", servIp, clientPort)
		result += line
	}

	return result
}

func GetFirstPDAddress(pdArr []map[string]interface{}) string {
	pd := pdArr[0]
	sshID := pd[consts.HEADER_SSH_ID].(string)
	ssh := meta.CMPT_META.GetSshById(sshID)
	pdServIp := ssh.SERVER_IP
	pdPort := pd[consts.HEADER_CLIENT_PORT].(string)
	return fmt.Sprintf("%s:%s", pdServIp, pdPort)
}

func DeployDashboard(dashboard map[string]interface{}, version, pdAddress, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := dashboard[consts.HEADER_INST_ID].(string)
	sshId := dashboard[consts.HEADER_SSH_ID].(string)
	dashboardPort := dashboard[consts.HEADER_DASHBOARD_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

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

	info := fmt.Sprintf("deploy dashboard: %s:%s, instId:%s", ssh.SERVER_IP, dashboardPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, dashboardPort, logKey, paasResult) {
		return false
	}

	// DB_TIDB_DASHBOARD_PROXY_FILE_ID -> 'dashboard-proxy-5.0.1.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_TIDB_DASHBOARD_PROXY_FILE_ID, consts.DB_TIDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIDB_DASHBOARD_PROXY_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, dashboardPort)
	dashboardAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, dashboardPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// etc/dashboard_proxy.cfg
	// %DASHBOARD_ADDR% 替换为$SSH_IP:$DASHBOARD_PORT
	// %PD_ADDRESS% 替换为pdAddress
	global.GLOBAL_RES.PubLog(logKey, "modify dashboard_proxy.cfg env params ......")
	confFile := "./etc/dashboard_proxy.cfg"
	if !DeployUtils.SED(sshClient, consts.CONF_DASHBOARD_ADDR, dashboardAddr, confFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PD_ADDRESS, pdAddress, confFile, logKey, paasResult) {
		return false
	}

	newConfFile := fmt.Sprintf("./etc/dashboard_proxy_%s.cfg", dashboardPort)
	if !DeployUtils.MV(sshClient, newConfFile, confFile, logKey, paasResult) {
		return false
	}

	// start.sh
	// %DASHBOARD_PORT% 替换为$DASHBOARD_PORT
	if !DeployUtils.SED(sshClient, consts.CONF_DASHBOARD_PORT, dashboardPort, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	// stop.sh
	// %DASHBOARD_PORT% 替换为$DASHBOARD_PORT
	if !DeployUtils.SED(sshClient, consts.CONF_DASHBOARD_PORT, dashboardPort, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// start
	global.GLOBAL_RES.PubLog(logKey, "start dashboard proxy ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "dashboard-proxy", instId, dashboardPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployDashboard(dashboard map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := dashboard[consts.HEADER_INST_ID].(string)
	sshId := dashboard[consts.HEADER_SSH_ID].(string)
	dashboardPort := dashboard[consts.HEADER_DASHBOARD_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy dashboard-proxy, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, dashboardPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIDB_DASHBOARD_PROXY_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, dashboardPort)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TIDB_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop dashboard-proxy ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "clickhouse", instId, dashboardPort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployPdServer(pdServer map[string]interface{}, version, pdLongAddr, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := pdServer[consts.HEADER_SSH_ID].(string)
	port := pdServer[consts.HEADER_CLIENT_PORT].(string)
	peerPort := pdServer[consts.HEADER_PEER_PORT].(string)
	instId := pdServer[consts.HEADER_INST_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

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

	info := fmt.Sprintf("deploy pd-server: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{port, peerPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	// DB_PD_SERVER_FILE_ID -> 'pd-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_PD_SERVER_FILE_ID, consts.DB_TIDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_PD_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// %INST_ID% 替换为$INST_ID
	// %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
	// %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
	// %PD_LIST% 替换为上面拼接的PD集群地址,格式1
	global.GLOBAL_RES.PubLog(logKey, "modify start.sh and stop.sh env params ......")
	clientUrls := fmt.Sprintf("%s:%s", ssh.SERVER_IP, port)
	peerUrls := fmt.Sprintf("%s:%s", ssh.SERVER_IP, peerPort)
	startFile := consts.START_SHELL
	pdAddr := strings.ReplaceAll(pdLongAddr, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_INST_ID, instId, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_CLIENT_URLS, clientUrls, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PEER_URLS, peerUrls, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_ADVERTISE_PEER_URLS, peerUrls, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PD_LIST, pdAddr, startFile, logKey, paasResult) {
		return false
	}

	stopFile := consts.STOP_SHELL
	if !DeployUtils.SED(sshClient, consts.CONF_INST_ID, instId, stopFile, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start pd-server ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "pd-server", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployPdServer(pd map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := pd[consts.HEADER_SSH_ID].(string)
	port := pd[consts.HEADER_CLIENT_PORT].(string)
	instId := pd[consts.HEADER_INST_ID].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy pd-server, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_PD_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TIDB_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop pd-server ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "pd-server", instId, port, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployTikvServer(tikv map[string]interface{}, version, pdShortAddr, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := tikv[consts.HEADER_SSH_ID].(string)
	port := tikv[consts.HEADER_PORT].(string)
	instId := tikv[consts.HEADER_INST_ID].(string)
	statPort := tikv[consts.HEADER_STAT_PORT].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

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

	info := fmt.Sprintf("deploy tikv-server: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}
	if DeployUtils.CheckPortUpPredeploy(sshClient, statPort, logKey, paasResult) {
		return false
	}

	// DB_TIKV_SERVER_FILE_ID -> 'tikv-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_TIKV_SERVER_FILE_ID, consts.DB_TIDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIKV_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// %INST_ID% 替换为$INST_ID
	// %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
	// %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
	// %PD_LIST% 替换为上面拼接的PD集群地址,格式1
	global.GLOBAL_RES.PubLog(logKey, "modify start.sh and stop.sh env params ......")
	startFile := consts.START_SHELL
	tikvAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, port)
	statAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, statPort)

	if !DeployUtils.SED(sshClient, consts.CONF_PD_LIST, pdShortAddr, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_TIKV_ADDR, tikvAddr, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_STAT_ADDR, statAddr, startFile, logKey, paasResult) {
		return false
	}

	stopFile := consts.STOP_SHELL
	if !DeployUtils.SED(sshClient, consts.CONF_PORT, port, stopFile, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start tikv-server ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "tikv-server", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployTikvServer(tikv map[string]interface{}, pdCtl map[string]interface{}, version, logKey, magicKey string,
	isUndeployService bool, paasResult *result.ResultBean) bool {

	sshId := tikv[consts.HEADER_SSH_ID].(string)
	ip := tikv[consts.HEADER_IP].(string)
	port := tikv[consts.HEADER_PORT].(string)
	instId := tikv[consts.HEADER_INST_ID].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy tikv-server, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIKV_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TIDB_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop tikv-server ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "clickhouse", instId, port, logKey, paasResult) {
		return false
	}

	if !isUndeployService {
		// pd-server delete tikv-instance
		// String.format("./bin/pd-ctl -u http://%s:%s -d store delete %s \n", servIp, port, instId)
		// pdServrSshId := pdCtl[consts.HEADER_SSH_ID].(string)
		pdIp := pdCtl[consts.HEADER_IP].(string)
		pdClientPort := pdCtl[consts.HEADER_CLIENT_PORT].(string)
		storeId, ok := DeployUtils.GetStoreId(sshClient, pdIp, pdClientPort, ip, port, logKey, paasResult)
		if !ok {
			global.GLOBAL_RES.PubFailLog(logKey, "pdctl get store id fail ......")
			return false
		}
		if !DeployUtils.PdctlDeleteTikvStore(sshClient, pdIp, pdClientPort, storeId, logKey, paasResult) {
			global.GLOBAL_RES.PubFailLog(logKey, "pdctl Delete Tikv Store is failed ......")
			return false
		}
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployTidbServer(tidb map[string]interface{}, version, pdShortAddr, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	sshId := tidb[consts.HEADER_SSH_ID].(string)
	port := tidb[consts.HEADER_PORT].(string)
	statPort := tidb[consts.HEADER_STAT_PORT].(string)
	instId := tidb[consts.HEADER_INST_ID].(string)

	ssh := meta.CMPT_META.GetSshById(sshId)
	if ssh == nil {
		paasResult.RET_CODE = consts.REVOKE_NOK
		paasResult.RET_INFO = consts.ERR_SSH_NOT_FOUND
		return false
	}

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

	info := fmt.Sprintf("deploy tidb-server: %s:%s, instId:%s", ssh.SERVER_IP, port, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, port, logKey, paasResult) {
		return false
	}
	if DeployUtils.CheckPortUpPredeploy(sshClient, statPort, logKey, paasResult) {
		return false
	}

	// DB_TIDB_SERVER_FILE_ID -> 'tidb-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.DB_TIDB_SERVER_FILE_ID, consts.DB_TIDB_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIDB_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// %INST_ID% 替换为$INST_ID
	// %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
	// %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
	// %PD_LIST% 替换为上面拼接的PD集群地址,格式1
	global.GLOBAL_RES.PubLog(logKey, "modify start.sh and stop.sh env params ......")
	startFile := consts.START_SHELL
	if !DeployUtils.SED(sshClient, consts.CONF_PD_LIST, pdShortAddr, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_HOST, ssh.SERVER_IP, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_PORT, port, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_STAT_HOST, ssh.SERVER_IP, startFile, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_STAT_PORT, statPort, startFile, logKey, paasResult) {
		return false
	}

	stopFile := consts.STOP_SHELL
	if !DeployUtils.SED(sshClient, consts.CONF_PORT, port, stopFile, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start tidb-server ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "tidb-server", instId, port, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployTidbServer(tidb map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := tidb[consts.HEADER_INST_ID].(string)
	sshId := tidb[consts.HEADER_SSH_ID].(string)
	port := tidb[consts.HEADER_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceNotDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("start undeploy tidb-server, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, port)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.DB_TIDB_SERVER_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, port)
	rootDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.DB_TIDB_ROOT, newName)

	if !DeployUtils.CD(sshClient, rootDir, logKey, paasResult) {
		return false
	}

	// stop
	global.GLOBAL_RES.PubLog(logKey, "stop tidb-server ......")
	cmd := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortDown(sshClient, "tidb-server", instId, port, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}
