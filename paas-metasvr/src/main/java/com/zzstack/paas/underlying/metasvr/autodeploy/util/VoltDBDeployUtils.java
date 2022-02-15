package com.zzstack.paas.underlying.metasvr.autodeploy.util;

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

public class VoltDBDeployUtils {
    
    public static boolean deployVoltDBServer(JsonObject voltdb, String version, String hosts, String userName, String userPasswd,
            String logKey, String magicKey, ResultBean result) {
        
        String instId = voltdb.getString(FixHeader.HEADER_INST_ID);
        String sshId = voltdb.getString(FixHeader.HEADER_SSH_ID);
        String clientPort = voltdb.getString(FixHeader.HEADER_VOLT_CLIENT_PORT);
        String adminPort = voltdb.getString(FixHeader.HEADER_VOLT_ADMIN_PORT);
        String webPort = voltdb.getString(FixHeader.HEADER_VOLT_WEB_PORT);
        String internalPort = voltdb.getString(FixHeader.HEADER_VOLT_INTERNAL_PORT);
        String replicaPort = voltdb.getString(FixHeader.HEADER_VOLT_REPLI_PORT);
        String zkPort = voltdb.getString(FixHeader.HEADER_VOLT_ZK_PORT);
        String sitesPerHost = voltdb.getString(FixHeader.HEADER_SITES_PER_HOST);
        String kfactor = voltdb.getString(FixHeader.HEADER_KFACTOR);
        String memLimit = voltdb.getString(FixHeader.HEADER_MEM_LIMIT);
        String heartBeatTimeout = voltdb.getString(FixHeader.HEADER_HEARTBEAT_TIMEOUT);
        String temptablesMaxSize = voltdb.getString(FixHeader.HEADER_TEMPTABLES_MAXSIZE);
        String elasticDuration = voltdb.getString(FixHeader.HEADER_ELASTIC_DURATION);
        String elasticThroughput = voltdb.getString(FixHeader.HEADER_ELASTIC_THROUGHPUT);
        String queryTimeout = voltdb.getString(FixHeader.HEADER_QUERY_TIMEOUT);
        String procedureLoginfo = voltdb.getString(FixHeader.HEADER_PROCEDURE_LOGINFO);
        String memAlert = voltdb.getString(FixHeader.HEADER_MEM_ALERT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy voltdb: %s:%s, instId:%s", servIp, internalPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, clientPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.client_port is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, adminPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.admin_port is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, webPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.web_port is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, internalPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.internal_port is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, replicaPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.repli_port is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "voltdb", instId, servIp, zkPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("voltdb.zk_port is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_VOLTDB_FILE_ID, FixDefs.DB_VOLTDB_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.DB_VOLTDB_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + internalPort;
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
        
        // modify deployment.xml
        // <cluster sitesperhost="%SITES_PER_HOST%"
        //          kfactor="%KFACTOR%"/>
        // <heartbeat timeout="%HEARTBEAT_TIMEOUT%"/>
        // <user name="%ADMIN_NAME%" roles="dev,ops,administrator" password="%ADMIN_PWD%"/>
        // <user name="%USER_NAME%" roles="user" password="%USER_PASSWORD%"/>
        // <temptables maxsize="%TEMPTABLES_MAXSIZE%"/>
        // <elastic duration="%ELASTIC_DURATION%" throughput="%ELASTIC_THROUGHPUT%"/>
        // <query timeout="%QUERY_TIMEOUT%"/>
        // <procedure loginfo="%PROCEDURE_LOGINFO%"/>
        // <memorylimit size="%MEMORYLIMIT_SIZE%" alert="%MEMORYLIMIT_ALERT%"/>
        DeployLog.pubLog(logKey, "modify deployment.xml configures ......");
        String deploymentFile = "./deployment.xml";
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_SITES_PER_HOST, sitesPerHost, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_KFACTOR, kfactor, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_HEARTBEAT_TIMEOUT, heartBeatTimeout, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ADMIN_NAME, FixDefs.VOLTDB_ADMIN_NAME, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ADMIN_PWD, FixDefs.VOLTDB_ADMIN_PWD, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_USER_NAME, userName, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_USER_PASSWORD, userPasswd, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_TEMPTABLES_MAXSIZE, temptablesMaxSize, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ELASTIC_DURATION, elasticDuration, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ELASTIC_THROUGHPUT, elasticThroughput, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_QUERY_TIMEOUT, queryTimeout, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PROCEDURE_LOGINFO, procedureLoginfo, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_MEMORYLIMIT_SIZE, memLimit, deploymentFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_MEMORYLIMIT_ALERT, memAlert, deploymentFile, logKey, result)) return false;
        
        // modify start.sh
        // nohup ./bin/voltdb start --dir=./database --host=%HOSTS% \
        //     --client=%VOLT_CLIENT_PORT% --admin=%VOLT_ADMIN_PORT% \
        //     --http=%VOLT_WEB_PORT% --internal=%VOLT_INTERNAL_PORT% \
        //     --replication=%VOLT_REPLI_PORT% --zookeeper=%VOLT_ZK_PORT% > /dev/null 2>&1 &
        String zkAddr = String.format("%s:%s", servIp, zkPort);
        String startFile = "./start.sh";
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_HOSTS, hosts, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_CLIENT_PORT, clientPort, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_ADMIN_PORT, adminPort, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_WEB_PORT, webPort, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_INTERNAL_PORT, internalPort, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_REPLI_PORT, replicaPort, startFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_VOLT_ZK_PORT, zkAddr, startFile, logKey, result)) return false;

        // modify stop.sh
        // --host=%HOST% --user=%USER_NAME% --password=%USER_PASSWORD%
        String stopFile = "./stop.sh";
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_HOST, servIp, stopFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_USER_NAME, FixDefs.VOLTDB_ADMIN_NAME, stopFile, logKey, result)) return false;
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_USER_PASSWORD, FixDefs.VOLTDB_ADMIN_PWD, stopFile, logKey, result)) return false;
        
        // init database
        String initDBCmd = "./bin/voltdb init --dir=./database --config=deployment.xml";
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, initDBCmd, logKey, result)) return false;
        
        // start
        DeployLog.pubLog(logKey, "start voltdb-server ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) return false;
        
        if (!DeployUtils.checkPortUpOnFailClose(ssh2, "voltdb", instId, servIp, internalPort, logKey, result)) return false;

        // update t_meta_instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeployVoltDBServer(JsonObject voltdb, String version, String logKey, String magicKey, ResultBean result) {
        String instId = voltdb.getString(FixHeader.HEADER_INST_ID);
        String sshId = voltdb.getString(FixHeader.HEADER_SSH_ID);
        String internalPort = voltdb.getString(FixHeader.HEADER_VOLT_INTERNAL_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy voltdb, inst_id:%s, serv_ip:%s, internalPort:%s", instId, servIp, internalPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.DB_VOLTDB_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + internalPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_VOLTDB_ROOT, newName);
        if (!DeployUtils.cdOnFailClose(ssh2, rootDir, logKey, result)) {
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop voltdb-server ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) {
            return false;
        }
        
        if (!DeployUtils.checkPortDownOnFailClose(ssh2, "voltdb", instId, servIp, internalPort, logKey, result)) {
            return false;
        }
        
        if (!DeployUtils.cdOnFailClose(ssh2, "..", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, newName, logKey, result)) {
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
    
    public static String getVoltDBHosts(JsonArray voltdbServerArr, String logKey, ResultBean result) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < voltdbServerArr.size(); ++i) {
            JsonObject voltdb = voltdbServerArr.getJsonObject(i);
            String sshId = voltdb.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
            if (ssh == null)
                continue;
            
            String servIp = ssh.getServerIp();
            
            if (sb.length() > 0)
                sb.append(",");
            
            sb.append(servIp);
        }
        
        return sb.toString();
    }
    
    public static boolean createValidationTable(JsonObject voltdb, String version, String logKey, String magicKey, ResultBean result) {
        String instId = voltdb.getString(FixHeader.HEADER_INST_ID);
        String sshId = voltdb.getString(FixHeader.HEADER_SSH_ID);
        String internalPort = voltdb.getString(FixHeader.HEADER_VOLT_INTERNAL_PORT);
        String clientPort = voltdb.getString(FixHeader.HEADER_VOLT_CLIENT_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.DB_VOLTDB_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + internalPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_VOLTDB_ROOT, newName);
        if (!DeployUtils.cdOnFailClose(ssh2, rootDir, logKey, result)) {
            return false;
        }
        
        // ./bin/sqlcmd --servers=127.0.0.1 --port=21212 --user=admin --password=a123456
        String sqlCmd = String.format("./bin/sqlcmd --servers=%s --port=%s --user=%s --password=%s ",
                servIp, clientPort, FixDefs.VOLTDB_ADMIN_NAME, FixDefs.VOLTDB_ADMIN_PWD);
        if (!DeployUtils.createValidationTable(ssh2, sqlCmd, logKey, result)) {
            return false;
        }
        
        ssh2.close();
        return true;
    }

}
