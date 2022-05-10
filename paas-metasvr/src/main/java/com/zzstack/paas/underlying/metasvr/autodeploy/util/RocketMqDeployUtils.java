package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import java.util.StringJoiner;

import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.exception.SSHException;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RocketMqDeployUtils {

    public static boolean deployNamesrv(JsonObject nameSrv, String version, String logKey, String magicKey, ResultBean result){
        String  sshId      = nameSrv.getString(FixHeader.HEADER_SSH_ID);
        String  port       = nameSrv.getString(FixHeader.HEADER_LISTEN_PORT);
        String  instId     = nameSrv.getString(FixHeader.HEADER_INST_ID);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "nameserv", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("nameserv.listen-port is in use");
            return false;
        }
        
        //判断有没有JDK及JDK版本(严格用1.8)
        //isDirExistInCurrPath 判断jdk路径是否存在
        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey,"exec pwd error ......");
            ssh2.close();
            return false;
        }
        //jdk路径
        String strFilePath = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, "jdk");
        try {
            if (!ssh2.isDirExistInCurrPath(strFilePath,logKey)) {
                if (!DeployUtils.cd(ssh2, root, logKey, result)) { ssh2.close(); return false; }
                //获取java文件,解压文件
                if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.COMMON_TOOLS_JDK_FILE_ID, FixDefs.COMMON_TOOLS_ROOT, "", logKey, result)) return false;
                //修改文件名
                PaasDeployFile jdkDeployFile = DeployUtils.getDeployFile(FixDefs.COMMON_TOOLS_JDK_FILE_ID, logKey, result);
                String srcJdkFileName   = jdkDeployFile.getFileName();
                int idx = srcJdkFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
                String oldJdkName = srcJdkFileName.substring(0, idx);

                String newJdkName = "jdk";
                if (!DeployUtils.rm(ssh2, newJdkName, logKey, result)) { ssh2.close(); return false; }
                if (!DeployUtils.mv(ssh2, newJdkName, oldJdkName, logKey, result)) { ssh2.close(); return false; }
            }
        } catch (SSHException e) {
            DeployLog.pubLog(logKey, e.getMessage());
        }

        if (!DeployUtils.cd(ssh2, "~/", logKey, result)) { ssh2.close(); return false; }

        //获取mq文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.MQ_ROCKET_MQ_FILE_ID, FixDefs.MQ_ROCKETMQ_ROOT, version, logKey, result)) return false;

        String oldName = DeployUtils.getVersionedFileName(FixDefs.MQ_ROCKET_MQ_FILE_ID, version, logKey, result);
        String newName = "rocketmq_namesrv_" + port;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify rocketmq_namesrv configure files ......");
        String newConf = "conf/namesrv.properties";
        String mqRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.MQ_ROCKETMQ_ROOT, newName);
        String newMqRoot = mqRoot.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ROCKET_HOME, newMqRoot, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_KV_CONFIG_PATH, newMqRoot + "\\/conf\\/kvConfig.json", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_PORT, port, newConf, logKey, result)) { ssh2.close(); return false; }

        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");

        String jdkHome = strFilePath;
        String startShell = String.format("export JAVA_HOME=%s %s" +
                        "export ROCKETMQ_HOME=%s %s" +
                        "nohup sh ./bin/mqnamesrv -c %s/conf/namesrv.properties > %s/logs/mqnamesrv.log >/dev/null 2>&1 & %s",
                jdkHome, CONSTS.LINE_SEP,
                mqRoot, CONSTS.LINE_SEP,
                mqRoot, mqRoot, CONSTS.LINE_SEP);


        if (!DeployUtils.createShell(ssh2, FixDefs.START_SHELL, startShell, logKey, result)) {
            ssh2.close();
            return false;
        }


        String stopShell = String.format("export JAVA_HOME=%s %s" +
                        "nohup sh bin/mqshutdown namesrv >/dev/null 2>&1 & ",
                jdkHome, CONSTS.LINE_SEP);
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_SHELL, stopShell, logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start namesrv ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "rocketmq", instId, servIp, port, logKey, result)) {
            ssh2.close();
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

    public static boolean deployBroker(JsonObject rocketMqJson, String servInstID, String namesrvAddrs, String brokerId,
            String version, String logKey, String magicKey, ResultBean result) {
        
        String  sshId      = rocketMqJson.getString(FixHeader.HEADER_SSH_ID);
        String  port       = rocketMqJson.getString(FixHeader.HEADER_LISTEN_PORT);
        String  instId     = rocketMqJson.getString(FixHeader.HEADER_INST_ID);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);

        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "rocketmq", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("rocketmq.listen-port is in use");
            return false;
        }

        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey,"exec pwd error ......");
            ssh2.close();
            return false;
        }

        //jdk路径
        String strFilePath = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, "jdk");
        try {
            if (!ssh2.isDirExistInCurrPath(strFilePath, logKey)) {
                if (!DeployUtils.cd(ssh2, root, logKey, result)) {
                    ssh2.close();
                    return false;
                }
                //获取java文件,解压文件
                if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.COMMON_TOOLS_JDK_FILE_ID,
                        FixDefs.COMMON_TOOLS_ROOT, version, logKey, result)) {
                    return false;
                }
                
                //修改文件名
                PaasDeployFile jdkDeployFile = DeployUtils.getDeployFile(FixDefs.COMMON_TOOLS_JDK_FILE_ID, logKey, result);
                String srcJdkFileName = jdkDeployFile.getFileName();
                int idx = srcJdkFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
                String oldJdkName = srcJdkFileName.substring(0, idx);

                String newJdkName = "jdk";
                if (!DeployUtils.rm(ssh2, newJdkName, logKey, result)) {
                    ssh2.close();
                    return false;
                }
                if (!DeployUtils.mv(ssh2, newJdkName, oldJdkName, logKey, result)) {
                    ssh2.close();
                    return false;
                }
            }
        } catch (SSHException e) {
            DeployLog.pubLog(logKey, e.getMessage());
        }

        if (!DeployUtils.cd(ssh2, "~/", logKey, result)) { ssh2.close(); return false; }

        //获取mq文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.MQ_ROCKET_MQ_FILE_ID, FixDefs.MQ_ROCKETMQ_ROOT,
                version, logKey, result)) {
            return false;
        }

        String oldName = DeployUtils.getVersionedFileName(FixDefs.MQ_ROCKET_MQ_FILE_ID, version, logKey, result);
        String newName = "rocketmq_broker_" + port;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify rocketmq_broker configure files ......");
        String newConf = "conf/broker.conf";
        String mqRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.MQ_ROCKETMQ_ROOT, newName);
        String newMqRoot = mqRoot.replace("/", "\\/");

        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_CLUSTER_NAME, servInstID, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_NAME, instId, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_ID, brokerId, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_NAMESRV_ADDR, namesrvAddrs, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_IP, servIp, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_PORT, port, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_STORE_ROOT, newMqRoot + "\\/store", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_COMMIT_LOG_PATH, newMqRoot + "\\/store\\/commitlog", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CONSUME_QUEUE_PATH, newMqRoot + "\\/store\\/consumequeue", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INDEX_PATH, newMqRoot + "\\/store\\/index", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CHECKPOINT_PATH, newMqRoot + "\\/store\\/checkpoint", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ABORT_FILE_PATH, newMqRoot + "\\/store\\/abort", newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_ROLE, rocketMqJson.getString("BROKER_ROLE"), newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_FLUSH_DISK_TYPE, rocketMqJson.getString("FLUSH_DISK_TYPE"), newConf, logKey, result)) {
            ssh2.close();
            return false;
        }

        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");

        String jdkHome = strFilePath;
        String startShell = String.format("export JAVA_HOME=%s %s" +
                        "export ROCKETMQ_HOME=%s %s" +
                        "nohup sh bin/mqbroker -c conf/broker.conf >/dev/null 2>&1 & %s",
                jdkHome, CONSTS.LINE_SEP,
                mqRoot, CONSTS.LINE_SEP,
                CONSTS.LINE_SEP);
        if (!DeployUtils.createShell(ssh2, FixDefs.START_SHELL, startShell, logKey, result)) { ssh2.close(); return false; }

        StringJoiner stopShell = new StringJoiner(CONSTS.LINE_END);
        stopShell.add(String.format(" PIDS=\\`ps -ef | grep java | grep -v grep | grep %s |awk '{print \\$2}'\\`", newName))
              .add(" if [ -z \"\\$PIDS\" ]; then")
              .add("    echo \"ERROR: broker does not started!\"")
              .add("    exit 127")
              .add(" fi")
              .add(" echo -e \"Stopping broker ...\"")
              .add(" for PID in \\$PIDS ; do")
              .add("    kill \\$PID > /dev/null 2>&1")
              .add(" done ")
              .add(" sleep 2 ")
              .add(" for PID in \\$PIDS ; do")
              .add("    PID_EXIST=\\`ps -f -p \\$PID | grep java\\`")
              .add("    if [ -n \"\\$PID_EXIST\" ]; then")
              .add("        kill -9 \\$PID > /dev/null 2>&1")
              .add(" fi")
              .add(" done ")
              .add(" echo \"OK!\" ")
              .add(" echo \"PID: \\$PIDS\" ");
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_SHELL, stopShell.toString(), logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start rocketmq broker ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.checkPortUp(ssh2, "rocketmq", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        // 3. update t_meta_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey))
            return false;

        DeployLog.pubLog(logKey, "deploy rocketmq broker success ......");
        ssh2.close();
        return true;
    }

    public static boolean undeployNamesrv(JsonObject rocketMqJson, String logKey, String magicKey, ResultBean result){
        String  sshId      = rocketMqJson.getString(FixHeader.HEADER_SSH_ID);
        String  port       = rocketMqJson.getString(FixHeader.HEADER_LISTEN_PORT);
        String  instId     = rocketMqJson.getString(FixHeader.HEADER_INST_ID);
        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy rocketmq-namesrv, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        String strName = "rocketmq_namesrv_" + port;
        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey,"exec pwd error ......");
            ssh2.close();
            return false;
        }
        String mqRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.MQ_ROCKETMQ_ROOT, strName);
        if (!DeployUtils.cd(ssh2, mqRoot, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop rocketmq namesrv ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.checkPortDown(ssh2, "rocketmq", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, mqRoot, logKey, result)) { ssh2.close(); return false; }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        ssh2.close();
        return true;
    }

    public static boolean undeployBroker(JsonObject rocketMqJson, String logKey, String magicKey, ResultBean result){
        String  sshId      = rocketMqJson.getString(FixHeader.HEADER_SSH_ID);
        String  port       = rocketMqJson.getString(FixHeader.HEADER_LISTEN_PORT);
        String  instId     = rocketMqJson.getString(FixHeader.HEADER_INST_ID);
        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy rocketmq-broker, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        String fileName = "rocketmq_broker_" + port;
        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey,"exec pwd error ......");
            ssh2.close();
            return false;
        }
        String mqRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.MQ_ROCKETMQ_ROOT, fileName);
        if (!DeployUtils.cd(ssh2, mqRoot, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop rocketmq broker ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.checkPortDown(ssh2, "rocketmq", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        
        // rocketmq 文件句柄释放较慢，进程停完任然占用，导致文件删除不彻底
        if (!DeployUtils.rm(ssh2, mqRoot, logKey, result)) { ssh2.close(); return false; }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        ssh2.close();
        return true;
    }

    //伪部署
    public static boolean deployFakeService(JsonObject servJson, String logKey, String servInstID, ResultBean result, String magicKey) {
        JsonObject rocketMqNameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray rocketMqNameSrv = rocketMqNameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);

        for (int i = 0; i < rocketMqNameSrv.size(); i++) {
            JsonObject jsonRokectMq = rocketMqNameSrv.getJsonObject(i);
            if (!MetaDataDao.updateInstanceDeployFlag(jsonRokectMq.getString(FixHeader.HEADER_INST_ID), FixDefs.STR_TRUE, result, magicKey)) {
                DeployLog.pubFailLog(logKey, "namesrv fake deploy failed ......");
                return false;
            }
            DeployLog.pubLog(logKey, "namesrv fake deploy success ......");
        }

        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray rocketMqVBroker = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);

        //部署broker服务
        for (int i = 0; i < rocketMqVBroker.size(); i++) {
            JsonObject jsonRocketbroker = rocketMqVBroker.getJsonObject(i);
            String vbrokerInstId = jsonRocketbroker.getString(FixHeader.HEADER_INST_ID);
            JsonArray jsonBrokerArray = jsonRocketbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            for (int j = 0; j < jsonBrokerArray.size(); j++) {
                JsonObject jsonRokectBroker = jsonBrokerArray.getJsonObject(j);
                String brokerID = jsonRokectBroker.getString(FixHeader.HEADER_INST_ID);
                if (!MetaDataDao.updateInstanceDeployFlag(brokerID, FixDefs.STR_TRUE, result, magicKey)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker start failed ......");
                    return false;
                }
                if (!MetaDataDao.updateServiceDeployFlag(vbrokerInstId, FixDefs.STR_TRUE, result, magicKey)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker start failed ......");
                    return false;
                }

                DeployLog.pubLog(logKey, "init rocketmq broker success ......");

            }

        }
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PSEUDO, magicKey)) {
            return false;
        }

        return true;
    }

    //卸载伪部署
    public static boolean undeployFakeService(JsonObject servJson, String logKey, String servInstID, ResultBean result, String magicKey) {

        JsonObject rocketMqNameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray rocketMqNameSrv = rocketMqNameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);

        for (int i = 0; i < rocketMqNameSrv.size(); i++) {
            JsonObject jsonRokectMq = rocketMqNameSrv.getJsonObject(i);
            String brokerID = jsonRokectMq.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(brokerID, FixDefs.STR_FALSE, result, magicKey)) {
                DeployLog.pubFailLog(logKey, "rocketmq namesrv undeploy failed ......");
                return false;
            }
            DeployLog.pubLog(logKey, "undeploy rocketmq namesrv success ......");

        }

        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray rocketMqVBroker = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);

        //部署broker服务
        for (int i = 0; i < rocketMqVBroker.size(); i++) {
            JsonObject jsonRocketbroker = rocketMqVBroker.getJsonObject(i);
            String vbrokerInstId = jsonRocketbroker.getString(FixHeader.HEADER_INST_ID);
            JsonArray jsonBrokerArray = jsonRocketbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            for (int j = 0; j < jsonBrokerArray.size(); j++) {
                JsonObject jsonRokectBroker = jsonBrokerArray.getJsonObject(j);
                String brokerID = jsonRokectBroker.getString(FixHeader.HEADER_INST_ID);
                if (!MetaDataDao.updateInstanceDeployFlag(brokerID, FixDefs.STR_FALSE, result, magicKey)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker undeploy failed ......");
                    return false;
                }
                if (!MetaDataDao.updateServiceDeployFlag(vbrokerInstId, CONSTS.DEPLOY_FLAG_PSEUDO, result, magicKey)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker undeploy failed ......");
                    return false;
                }

                DeployLog.pubLog(logKey, "undeploy rocketmq broker success ......");

            }

        }
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PHYSICAL, magicKey)) {
            return false;
        }

        return true;
    }

    public static boolean deployConsole(JsonObject console, String servInstID, String namesrvAddrs, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = console.getString(FixHeader.HEADER_SSH_ID);
        String consolePort = console.getString(FixHeader.HEADER_CONSOLE_PORT);
        String instId = console.getString(FixHeader.HEADER_INST_ID);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed rocketmq-console, inst_id:%s", instId);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null)
            return false;
        
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "rocketmq-console", instId, servIp, consolePort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("rocketmq-console.console端口 is in use");
            return false;
        }
        
        //获取mq文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.ROCKETMQ_CONSOLE_FILE_ID, FixDefs.MQ_ROCKETMQ_ROOT, version, logKey, result)) return false;

        //修改文件名
        PaasDeployFile rocketMqDeployFile = DeployUtils.getDeployFile(FixDefs.ROCKETMQ_CONSOLE_FILE_ID, logKey, result);
        String srcFileName   = rocketMqDeployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = rocketMqDeployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);

        String newName = String.format("rocketmq-console_%s", consolePort);
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        
        DeployLog.pubLog(logKey, "modify rocketmq-console configure ......");
        
        // sed 替换 start.sh 中的变量 
        // -Drocketmq.namesrv.addr=%NAMESRV_ADDR%
        // -Dserver.port=%CONSOLE_PORT%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_NAMESRV_ADDR, namesrvAddrs, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CONSOLE_PORT, consolePort, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start rocketmq-console ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "rocketmq-console", instId, servIp, consolePort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        DeployLog.pubLog(logKey, "deploy rocketmq-console success ......");
        ssh2.close();
        return true;
    }

    public static boolean undeployConsole(JsonObject console, String logKey, String magicKey, ResultBean result) {
        String sshId = console.getString(FixHeader.HEADER_SSH_ID);
        String consolePort = console.getString(FixHeader.HEADER_CONSOLE_PORT);
        String instId = console.getString(FixHeader.HEADER_INST_ID);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;

        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (!DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed rocketmq-console, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }

        {
            String info = String.format("start undeploy rocketmq-console, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = String.format("rocketmq-console_%s", consolePort);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.MQ_ROCKETMQ_ROOT, newName);

        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop rocketmq-console ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortDown(ssh2, "rocketmq-console", instId, servIp, consolePort, logKey, result)) {
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
    
    public static String getNameSrvAddrs(JsonArray mameSrvArr) {
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < mameSrvArr.size();i++){
            JsonObject nameSrv = mameSrvArr.getJsonObject(i);
            
            String sshId = nameSrv.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String servIp = ssh.getServerIp();
            String port = nameSrv.getString(FixHeader.HEADER_LISTEN_PORT);
            
            if (sb.length() > 0)
                sb.append(";");
            
            String line = String.format("%s:%s", servIp, port);
            sb.append(line);
        }
        
        return sb.toString();
    }
    
    public static String getSingleNameSrvAddrs(JsonArray mameSrvArr) {
        StringBuilder sb = new StringBuilder();
        
        for(int i = 0; i < mameSrvArr.size();i++){
            JsonObject nameSrv = mameSrvArr.getJsonObject(i);
            
            String sshId = nameSrv.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
            if (ssh == null)
                continue;
            
            String servIp = ssh.getServerIp();
            String port = nameSrv.getString(FixHeader.HEADER_LISTEN_PORT);
            
            String line = String.format("%s:%s", servIp, port);
            sb.append(line);
            
            break;
        }
        
        return sb.toString();
    }
    
}
