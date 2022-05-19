package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import java.util.StringJoiner;

import org.apache.pulsar.shade.org.apache.commons.codec.digest.DigestUtils;

import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApiSixDeployUtils {

    public static boolean deployApiSixNode(JsonObject apiSixJson, String servInstId, String ectdAddrList,
                                           String ectdAddr, String version, String logKey, String magicKey, ResultBean result) {
        // %ETCD_ADDR_LIST% 替换为etcd地址列表
        // %HTTP_PORT% 替换为HTTP_PORT属性值
        // %SSL_PORT% 替换为SSL_PORT属性值
        // %APISIX_IP% 替换为SSH_IP值
        // %CONTROL_PORT% 替换为CONTROL_PORT属性值
        // %INST_ID_MD5% 替换为md5(apisix_contatiner.INST_ID)值
        String instId = apiSixJson.getString(FixHeader.HEADER_INST_ID);
        String sshId = apiSixJson.getString(FixHeader.HEADER_SSH_ID);
        String httpPort = apiSixJson.getString(FixHeader.HEADER_HTTP_PORT);
        String sslPort = apiSixJson.getString(FixHeader.HEADER_SSL_PORT);
        String apiSixDashBoardPort = apiSixJson.getString(FixHeader.HEADER_DASHBOARD_PORT);
        String apiSixControlPort = apiSixJson.getString(FixHeader.HEADER_CONTROL_PORT);
        String metricPort = apiSixJson.getString(FixHeader.HEADER_METRIC_PORT);
        String servInstIdMd5 = DigestUtils.md5Hex(servInstId);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) {
            return false;
        }
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        String info = String.format("start deploy apisix, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, httpPort);
        DeployLog.pubLog(logKey, info);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            return true;
        }
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) {
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "apisix", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("apisix.httpPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "apisix", instId, servIp, sslPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("apisix.sslPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "apisix", instId, servIp, apiSixControlPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("apisix.controlPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "apisix", instId, servIp, apiSixDashBoardPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("apisix.dashBoardPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "apisix", instId, servIp, metricPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("apisix.metricPort is in use");
            return false;
        }
        
        String homePath = DeployUtils.pwd(ssh2, logKey, result);

        // SERVERLESS_APISIX FILE_ID -> 'apisix-%VERSION%.tar.gz'
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.SERVERLESS_APISIX_FILE_ID, FixDefs.SERVERLESS_ROOT, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.SERVERLESS_APISIX_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + httpPort;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        String configFile = String.format("%s%s", "apisix/conf/", FixDefs.APISIX_CONFIG);
        // 替换config文件
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ETCD_ADDR_LIST, ectdAddrList, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HTTP_PORT, httpPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SSL_PORT, sslPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CONTROL_PORT, apiSixControlPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APISIX_IP, servIp, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INST_ID_MD5, servInstIdMd5, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // create $HOME/paas/serverless/apisix-%VERSION%_%HTTP_PORT%/luarocks/etc/luarocks/config-5.1.lua
        StringJoiner fileContentSj = new StringJoiner(CONSTS.LINE_END);
        fileContentSj.add("rocks_trees = {");
        fileContentSj.add("   { name = \"user\", root = home .. \"/.luarocks\" };");
        fileContentSj.add("   { name = \"system\", root = \"%APISIX_ROOT%/luarocks\" };");
        fileContentSj.add("}");
        fileContentSj.add("lua_interpreter = \"lua\";");
        fileContentSj.add("variables = {");
        fileContentSj.add("   LUA_DIR = \"%APISIX_ROOT%/%APISIX_LUA%\";");
        fileContentSj.add("   LUA_INCDIR = \"%APISIX_ROOT%/%APISIX_LUA%/include\";");
        fileContentSj.add("   LUA_BINDIR = \"%APISIX_ROOT%/%APISIX_LUA%/bin\";");
        fileContentSj.add("   LUA_LIBDIR = \"%APISIX_ROOT%/%APISIX_LUA%/lib\";");
        fileContentSj.add("   OPENSSL_INCDIR = \"%APISIX_ROOT%/openssl/include\";");
        fileContentSj.add("   OPENSSL_LIBDIR = \"%APISIX_ROOT%/openssl/lib\"");
        fileContentSj.add("}");
        
        String openrestyHome = String.format("%s/%s/%s/%s/%s", homePath, FixDefs.PAAS_ROOT,
                FixDefs.SERVERLESS_ROOT, newName, FixDefs.APISIX_OPENRESTY);
        
        String luaRocksConfigPath = String.format("%s/%s/%s/%s/%s/%s", homePath, FixDefs.PAAS_ROOT,
                FixDefs.SERVERLESS_ROOT, newName, "luarocks/etc/luarocks", FixDefs.APISIX_LUAROCKS_CONFIG);
        
        String apisixHome = String.format("%s/%s/%s/%s", homePath, FixDefs.PAAS_ROOT, FixDefs.SERVERLESS_ROOT, newName);
        
        String fileContent = fileContentSj.toString()
                .replace(FixDefs.CONF_APISIX_ROOT, apisixHome)
                .replace(FixDefs.CONF_APISIX_LUA, FixDefs.APISIX_LUA);
        if (!DeployUtils.createFile(ssh2, luaRocksConfigPath, fileContent, logKey, result)) {
            ssh2.close();
            return false;
        }

        // create start and stop shell
        DeployLog.pubLog(logKey, "create apisix start and stop shell ......");
        if (!DeployUtils.cd(ssh2, "./apisix", logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 替换 conf/config-default.yaml
        // extra_lua_path: "%APISIX_HOME%/openresty/lualib/?.lua"
        // extra_lua_cpath: "%APISIX_HOME%/openresty/lualib/?.so;%APISIX_HOME%/openresty/lualib/redis/?.so;%APISIX_HOME%/openresty/lualib/rds/?.so"
        String defaultConf = String.format("./conf/config-default.yaml");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APISIX_HOME, apisixHome.replace("/", "\\/"), defaultConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 替换 ./bin/apisix
        String apisixShell = String.format("./bin/%s", FixDefs.APISIX_APISIX);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_OPENRESTY_HOME, openrestyHome.replace("/", "\\/"), apisixShell, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 替换 start.sh
        // export APISIX_HOME=%APISIX_HOME%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APISIX_HOME, apisixHome.replace("/", "\\/"), FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 替换 stop.sh
        // export APISIX_HOME=%APISIX_HOME%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APISIX_HOME, apisixHome.replace("/", "\\/"), FixDefs.STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start apisix
        DeployLog.pubLog(logKey, "start apisix ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        DeployLog.pubLog(logKey, "checkPortUp apisix ......");
        if (!DeployUtils.checkPortUp(ssh2, "apisix", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // create apisix-dashboard
        if (!DeployUtils.cd(ssh2, "../apisix-dashboard", logKey, result)) {
            ssh2.close();
            String createStartShellErrorinfo = String.format("service inst_id:%s, cd apisix-dashboard fail ......", instId);
            DeployLog.pubFailLog(logKey, createStartShellErrorinfo);
            return false;
        }
        // modify config
        String dashboardConf = String.format("conf/%s", FixDefs.APISIX_DASHBOARD_CONF);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DASHBOARD_IP, servIp, dashboardConf, logKey, result)) {
            ssh2.close();
            String modifyErrorinfo = String.format("service inst_id:%s, modify apisix-dashboard config %s fail ......", instId, FixDefs.CONF_DASHBOARD_IP);
            DeployLog.pubFailLog(logKey, modifyErrorinfo);
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DASHBOARD_PORT, apiSixDashBoardPort, dashboardConf, logKey, result)) {
            ssh2.close();
            String modifyErrorinfo = String.format("service inst_id:%s, modify apisix-dashboard config %s fail ......", instId, FixDefs.CONF_DASHBOARD_PORT);
            DeployLog.pubFailLog(logKey, modifyErrorinfo);
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ETCD_ADDR, ectdAddr, dashboardConf, logKey, result)) {
            ssh2.close();
            String modifyErrorinfo = String.format("service inst_id:%s, modify apisix-dashboard config %s fail ......", instId, FixDefs.CONF_ETCD_ADDR);
            DeployLog.pubFailLog(logKey, modifyErrorinfo);
            return false;
        }
        
        // start apisix-dashboard
        DeployLog.pubLog(logKey, "start apisix-dashboard ......");
        String dashboardCmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, dashboardCmd, logKey, result)) {
            ssh2.close();
            String startShellErrorinfo = String.format("service inst_id:%s, start apisix-dashboard fail ......", instId);
            DeployLog.pubFailLog(logKey, startShellErrorinfo);
            return false;
        }
        if (!DeployUtils.checkPortUp(ssh2, "apisix-dashboard", instId, servIp, apiSixDashBoardPort, logKey, result)) {
            ssh2.close();
            String checkPortUpErrorinfo = String.format("service inst_id:%s, checkPortUp apisix-dashboard fail ......", instId);
            DeployLog.pubFailLog(logKey, checkPortUpErrorinfo);
            return false;
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        ssh2.close();
        return true;
    }

    public static boolean undeployApiSixNode(JsonObject apiSixJson, String version, String logKey, String magicKey, ResultBean result) {
        String instId = apiSixJson.getString(FixHeader.HEADER_INST_ID);
        String sshId = apiSixJson.getString(FixHeader.HEADER_SSH_ID);
        String httpPort = apiSixJson.getString(FixHeader.HEADER_HTTP_PORT);
        String apiSixDashBoardPort = apiSixJson.getString(FixHeader.HEADER_DASHBOARD_PORT);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) {
            return false;
        }
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        String info = String.format("start undeploy apisix, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, httpPort);
        DeployLog.pubLog(logKey, info);
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            return true;
        }
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) {
            return false;
        }
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.SERVERLESS_APISIX_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);
        String newName = oldName + "_" + httpPort;
        
        String basePath = DeployUtils.pwd(ssh2, logKey, result);
        String apiSixDir = String.format("%s/%s/%s/%s", basePath, FixDefs.PAAS_ROOT, FixDefs.SERVERLESS_ROOT, newName);
        
        String rootDir = String.format("%s/%s", apiSixDir, "apisix");
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop apisix
        DeployLog.pubLog(logKey, "stop apisix ......");
        String apisixStopShell = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, apisixStopShell, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.checkPortDown(ssh2, "apisix", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop apisix-dashboard
        DeployLog.pubLog(logKey, "stop apisix-dashboard ......");
        if (!DeployUtils.cd(ssh2, "../apisix-dashboard", logKey, result)) {
            ssh2.close();
            return false;
        }
        String apisixDashboardStopShell = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, apisixDashboardStopShell, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.checkPortDown(ssh2, "apisix-dashboard", instId, servIp, apiSixDashBoardPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.cd(ssh2, "../..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, apiSixDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        ssh2.close();
        return true;
    }
    
    public static String getApisixMetricList(JsonArray apiSixNodeArr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < apiSixNodeArr.size(); ++i) {
            JsonObject apisixNode = apiSixNodeArr.getJsonObject(i);
            String sshId = apisixNode.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String ip = ssh.getServerIp();
            String metricPort = apisixNode.getString(FixHeader.HEADER_METRIC_PORT);
            String metricAddr = String.format("%s:%s", ip, metricPort);
            
            if (sb.length() > 0)
                sb.append(",");
            
            sb.append(metricAddr);
        }
        
        return sb.toString();
    }
    
    public static boolean deployPrometheus(JsonObject prometheus, String clusterName, String apisixMetricList,
            String version, String logKey, String magicKey, ResultBean result) {

        String instId = prometheus.getString(FixHeader.HEADER_INST_ID);
        String sshId = prometheus.getString(FixHeader.HEADER_SSH_ID);
        String prometheusPort = prometheus.getString(FixHeader.HEADER_PROMETHEUS_PORT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy prometheus: %s:%s, instId:%s", servIp, prometheusPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "prometheus", instId, servIp, prometheusPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("prometheus.prometheusPort is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.PROMETHEUS_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PROMETHEUS_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + prometheusPort;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start.sh stop.sh %LISTEN_ADDRESS%
        String prometheusAddr = String.format("%s:%s", servIp, prometheusPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_ADDRESS, prometheusAddr, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_ADDRESS, prometheusAddr, FixDefs.STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // scp prometheus_apisix.yml
        if (!DeployUtils.fetchFile(ssh2, FixDefs.PROMETHEUS_APISIX_YML_FILE_ID, logKey, result)) {
            DeployLog.pubFailLog(logKey, "scp prometheus_apisix.yml fail ......");
            return false;
        }
        
        // cluster: %CLUSTER_NAME%
        // - targets: [%APISIX_LIST%]
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NAME, clusterName, FixDefs.PROMETHEUS_APISIX_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APISIX_LIST, apisixMetricList, FixDefs.PROMETHEUS_APISIX_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, FixDefs.PROMETHEUS_YML, FixDefs.PROMETHEUS_APISIX_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start prometheus ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "prometheus", instId, servIp, prometheusPort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update t_meta_instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        return true;
    }
    
}
