package apisix

import (
	"fmt"
	"strings"

	"github.com/maoge/paas-metasvr-go/pkg/consts"
	"github.com/maoge/paas-metasvr-go/pkg/dao/metadao"
	DeployUtils "github.com/maoge/paas-metasvr-go/pkg/deployutils"
	"github.com/maoge/paas-metasvr-go/pkg/global"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/result"
	"github.com/maoge/paas-metasvr-go/pkg/utils"
)

func GetApisixMetricList(apiSixNodeArr []map[string]interface{}) string {
	result := ""
	for idx, apisixNode := range apiSixNodeArr {
		sshId := apisixNode[consts.HEADER_SSH_ID].(string)
		ssh := meta.CMPT_META.GetSshById(sshId)
		if ssh == nil {
			continue
		}

		ip := ssh.SERVER_IP
		metricPort := apisixNode[consts.HEADER_METRIC_PORT].(string)
		metricAddr := fmt.Sprintf("%s:%s", ip, metricPort)

		if idx > 0 {
			result += ","
		}

		result += metricAddr
	}

	return result
}

func DeployPrometheus(prometheus map[string]interface{}, clusterName, apisixMetricList, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := prometheus[consts.HEADER_INST_ID].(string)
	sshId := prometheus[consts.HEADER_SSH_ID].(string)
	prometheusPort := prometheus[consts.HEADER_PROMETHEUS_PORT].(string)

	inst := meta.CMPT_META.GetInstance(instId)
	if DeployUtils.IsInstanceDeployed(logKey, inst, paasResult) {
		return true
	}

	ssh := meta.CMPT_META.GetSshById(sshId)
	sshClient := DeployUtils.NewSSHClientBySSH(ssh)
	if !DeployUtils.ConnectSSH(sshClient, logKey, paasResult) {
		return false
	} else {
		defer sshClient.Close()
	}

	info := fmt.Sprintf("deploy prometheus: %s:%s, instId:%s", ssh.SERVER_IP, prometheusPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	if DeployUtils.CheckPortUpPredeploy(sshClient, prometheusPort, logKey, paasResult) {
		return false
	}

	// PROMETHEUS_FILE_ID -> 'prometheus-2.27.1.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.PROMETHEUS_FILE_ID, consts.COMMON_TOOLS_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.PROMETHEUS_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, prometheusPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	// start.sh stop.sh %LISTEN_ADDRESS%
	prometheusAddr := fmt.Sprintf("%s:%s", ssh.SERVER_IP, prometheusPort)
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.START_SHELL, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_LISTEN_ADDRESS, prometheusAddr, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// scp prometheus_apisix.yml
	if !DeployUtils.FetchFile(sshClient, consts.PROMETHEUS_APISIX_YML_FILE_ID, logKey, paasResult) {
		global.GLOBAL_RES.PubFailLog(logKey, "scp prometheus_apisix.yml fail ......")
		return false
	}

	// cluster: %CLUSTER_NAME%
	// - targets: [%APISIX_LIST%]
	if !DeployUtils.SED(sshClient, consts.CONF_CLUSTER_NAME, clusterName, consts.PROMETHEUS_APISIX_YML, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_APISIX_LIST, apisixMetricList, consts.PROMETHEUS_APISIX_YML, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, consts.PROMETHEUS_YML, consts.PROMETHEUS_APISIX_YML, logKey, paasResult) {
		return false
	}

	global.GLOBAL_RES.PubLog(logKey, "start prometheus ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "clickhouse", instId, prometheusPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func DeployApiSixNode(apiSixNode map[string]interface{}, groupId, etcdLongAddr, etcdShortAddr, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := apiSixNode[consts.HEADER_INST_ID].(string)
	sshId := apiSixNode[consts.HEADER_SSH_ID].(string)
	httpPort := apiSixNode[consts.HEADER_HTTP_PORT].(string)
	sslPort := apiSixNode[consts.HEADER_SSL_PORT].(string)
	apiSixDashBoardPort := apiSixNode[consts.HEADER_DASHBOARD_PORT].(string)
	apiSixControlPort := apiSixNode[consts.HEADER_CONTROL_PORT].(string)
	metricPort := apiSixNode[consts.HEADER_METRIC_PORT].(string)
	servInstIdMd5 := utils.MD5V(groupId)

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

	info := fmt.Sprintf("deploy apisix: %s:%s, instId:%s", ssh.SERVER_IP, httpPort, instId)
	global.GLOBAL_RES.PubLog(logKey, info)

	checkPorts := []string{httpPort, sslPort, apiSixControlPort, apiSixDashBoardPort, metricPort}
	if DeployUtils.CheckPortsUpPredeploy(sshClient, checkPorts, logKey, paasResult) {
		return false
	}

	homePath, _ := DeployUtils.PWD(sshClient, logKey, paasResult)

	// SERVERLESS_APISIX_FILE_ID -> 'apisix-%VERSION%.tar.gz'
	if !DeployUtils.FetchAndExtractTgzDeployFile(sshClient, consts.SERVERLESS_APISIX_FILE_ID, consts.SERVERLESS_ROOT, version, logKey, paasResult) {
		return false
	}

	oldName := DeployUtils.GetVersionedFileName(consts.SERVERLESS_APISIX_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, httpPort)

	if !DeployUtils.RM(sshClient, newName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.MV(sshClient, newName, oldName, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CD(sshClient, newName, logKey, paasResult) {
		return false
	}

	configFile := fmt.Sprintf("%s%s", "apisix/conf/", consts.APISIX_CONFIG)
	DeployUtils.AppendMultiLine(sshClient, consts.CONF_ETCD_ADDR_LIST, etcdLongAddr, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_HTTP_PORT, httpPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_SSL_PORT, sslPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_CONTROL_PORT, apiSixControlPort, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_APISIX_IP, ssh.SERVER_IP, configFile, logKey, paasResult)
	DeployUtils.SED(sshClient, consts.CONF_INST_ID_MD5, servInstIdMd5, configFile, logKey, paasResult)

	// create $HOME/paas/serverless/apisix-%VERSION%_%HTTP_PORT%/luarocks/etc/luarocks/config-5.1.lua
	luaEnv := ""
	luaEnv += "rocks_trees = {" + consts.LINE_END
	luaEnv += "   { name = \"user\", root = home .. \"/.luarocks\" };" + consts.LINE_END
	luaEnv += "   { name = \"system\", root = \"%APISIX_ROOT%/luarocks\" };" + consts.LINE_END
	luaEnv += "}" + consts.LINE_END
	luaEnv += "lua_interpreter = \"lua\";" + consts.LINE_END
	luaEnv += "variables = {" + consts.LINE_END
	luaEnv += "   LUA_DIR = \"%APISIX_ROOT%/%APISIX_LUA%\";" + consts.LINE_END
	luaEnv += "   LUA_INCDIR = \"%APISIX_ROOT%/%APISIX_LUA%/include\";" + consts.LINE_END
	luaEnv += "   LUA_BINDIR = \"%APISIX_ROOT%/%APISIX_LUA%/bin\";" + consts.LINE_END
	luaEnv += "   LUA_LIBDIR = \"%APISIX_ROOT%/%APISIX_LUA%/lib\";" + consts.LINE_END
	luaEnv += "   OPENSSL_INCDIR = \"%APISIX_ROOT%/openssl/include\";" + consts.LINE_END
	luaEnv += "   OPENSSL_LIBDIR = \"%APISIX_ROOT%/openssl/lib\"" + consts.LINE_END
	luaEnv += "}"

	openrestyHome := fmt.Sprintf("%s/%s/%s/%s/%s", homePath, consts.PAAS_ROOT, consts.SERVERLESS_ROOT, newName, consts.APISIX_OPENRESTY)

	luaRocksConfigPath := fmt.Sprintf("%s/%s/%s/%s/%s/%s", homePath, consts.PAAS_ROOT, consts.SERVERLESS_ROOT,
		newName, "luarocks/etc/luarocks", consts.APISIX_LUAROCKS_CONFIG)

	apisixHome := fmt.Sprintf("%s/%s/%s/%s", homePath, consts.PAAS_ROOT, consts.SERVERLESS_ROOT, newName)

	fileContent := strings.ReplaceAll(luaEnv, consts.CONF_APISIX_ROOT, apisixHome)
	fileContent = strings.ReplaceAll(fileContent, consts.CONF_APISIX_LUA, consts.APISIX_LUA)
	if !DeployUtils.CreateFile(sshClient, luaRocksConfigPath, fileContent, logKey, paasResult) {
		return false
	}

	// create start and stop shell
	global.GLOBAL_RES.PubLog(logKey, "create apisix start and stop shell ......")
	if !DeployUtils.CD(sshClient, "./apisix", logKey, paasResult) {
		return false
	}

	apisixHomeNew := strings.ReplaceAll(apisixHome, "/", "\\/")

	// 替换 conf/config-default.yaml
	// extra_lua_path: "%APISIX_HOME%/openresty/lualib/?.lua"
	// extra_lua_cpath: "%APISIX_HOME%/openresty/lualib/?.so;%APISIX_HOME%/openresty/lualib/redis/?.so;%APISIX_HOME%/openresty/lualib/rds/?.so"
	defaultConf := "./conf/config-default.yaml"
	if !DeployUtils.SED(sshClient, consts.CONF_APISIX_HOME, apisixHomeNew, defaultConf, logKey, paasResult) {
		return false
	}

	// 替换 ./bin/apisix
	apisixShell := fmt.Sprintf("./bin/%s", consts.APISIX_APISIX)
	openrestyHomeNew := strings.ReplaceAll(openrestyHome, "/", "\\/")
	if !DeployUtils.SED(sshClient, consts.CONF_OPENRESTY_HOME, openrestyHomeNew, apisixShell, logKey, paasResult) {
		return false
	}

	// 替换 start.sh
	// export APISIX_HOME=%APISIX_HOME%
	if !DeployUtils.SED(sshClient, consts.CONF_APISIX_HOME, apisixHomeNew, consts.START_SHELL, logKey, paasResult) {
		return false
	}

	// 替换 stop.sh
	// export APISIX_HOME=%APISIX_HOME%
	if !DeployUtils.SED(sshClient, consts.CONF_APISIX_HOME, apisixHomeNew, consts.STOP_SHELL, logKey, paasResult) {
		return false
	}

	// start apisix
	global.GLOBAL_RES.PubLog(logKey, "start apisix ......")
	cmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, cmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "apisix", instId, httpPort, logKey, paasResult) {
		return false
	}

	// create apisix-dashboard
	if !DeployUtils.CD(sshClient, "../apisix-dashboard", logKey, paasResult) {
		errinfo := fmt.Sprintf("service inst_id:%s, cd apisix-dashboard fail ......", instId)
		global.GLOBAL_RES.PubErrorLog(logKey, errinfo)
	}

	// modify config
	dashboardConf := fmt.Sprintf("conf/%s", consts.APISIX_DASHBOARD_CONF)
	if !DeployUtils.SED(sshClient, consts.CONF_DASHBOARD_IP, ssh.SERVER_IP, dashboardConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.SED(sshClient, consts.CONF_DASHBOARD_PORT, apiSixDashBoardPort, dashboardConf, logKey, paasResult) {
		return false
	}
	if !DeployUtils.AppendMultiLine(sshClient, consts.CONF_ETCD_ADDR, etcdShortAddr, dashboardConf, logKey, paasResult) {
		return false
	}

	// start apisix-dashboard
	global.GLOBAL_RES.PubLog(logKey, "start apisix-dashboard ......")
	dashboardCmd := fmt.Sprintf("./%s", consts.START_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, dashboardCmd, logKey, paasResult) {
		return false
	}

	if !DeployUtils.CheckPortUp(sshClient, "apisix-dashboard", instId, apiSixDashBoardPort, logKey, paasResult) {
		return false
	}

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_TRUE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}

func UndeployApiSixNode(apiSixNode map[string]interface{}, version, logKey, magicKey string,
	paasResult *result.ResultBean) bool {

	instId := apiSixNode[consts.HEADER_INST_ID].(string)
	sshId := apiSixNode[consts.HEADER_SSH_ID].(string)
	httpPort := apiSixNode[consts.HEADER_HTTP_PORT].(string)
	apiSixDashBoardPort := apiSixNode[consts.HEADER_DASHBOARD_PORT].(string)

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

	info := fmt.Sprintf("start undeploy apisix, inst_id:%s, serv_ip:%s, http_port:%s", instId, ssh.SERVER_IP, httpPort)
	global.GLOBAL_RES.PubLog(logKey, info)

	oldName := DeployUtils.GetVersionedFileName(consts.SERVERLESS_APISIX_FILE_ID, version, logKey, paasResult)
	newName := fmt.Sprintf("%s_%s", oldName, httpPort)
	baseDir := fmt.Sprintf("%s/%s/%s", consts.PAAS_ROOT, consts.SERVERLESS_ROOT, newName)

	if !DeployUtils.CD(sshClient, baseDir, logKey, paasResult) {
		return false
	}

	// stop apisix
	global.GLOBAL_RES.PubLog(logKey, "stop apisix ......")
	apisixStopShell := fmt.Sprintf("./%s", consts.STOP_SHELL)
	DeployUtils.CD(sshClient, "apisix", logKey, paasResult)
	if !DeployUtils.ExecSimpleCmd(sshClient, apisixStopShell, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CheckPortDown(sshClient, "apisix", instId, httpPort, logKey, paasResult) {
		return false
	}

	// stop apisix-dashboard
	global.GLOBAL_RES.PubLog(logKey, "stop apisix-dashboard ......")
	DeployUtils.CD(sshClient, "../apisix-dashboard", logKey, paasResult)
	apisixDashboardStopShell := fmt.Sprintf("./%s", consts.STOP_SHELL)
	if !DeployUtils.ExecSimpleCmd(sshClient, apisixDashboardStopShell, logKey, paasResult) {
		return false
	}
	if !DeployUtils.CheckPortDown(sshClient, "apisix-dashboard", instId, apiSixDashBoardPort, logKey, paasResult) {
		return false
	}

	DeployUtils.CD(sshClient, "../..", logKey, paasResult)
	DeployUtils.RM(sshClient, newName, logKey, paasResult)

	// update instance deploy flag
	if !metadao.UpdateInstanceDeployFlag(instId, consts.STR_FALSE, logKey, magicKey, paasResult) {
		return false
	}

	return true
}
