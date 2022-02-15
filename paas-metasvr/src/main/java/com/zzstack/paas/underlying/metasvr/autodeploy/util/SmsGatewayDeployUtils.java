package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
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

public class SmsGatewayDeployUtils {
    
    private static final long WAIT_MILLI_SECONDS = 200;
    
    public static boolean deploySmsInstanceArr(String header, String servInstId, JsonObject container, String logKey, String magicKey, ResultBean result) {
        JsonArray smsInstanceArr = container.getJsonArray(header);
        if (smsInstanceArr == null || smsInstanceArr.isEmpty())
            return true;
        
        String containerInstId = container.getString(FixHeader.HEADER_INST_ID);
        
        int size = smsInstanceArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject item = smsInstanceArr.getJsonObject(i);
            boolean ret = true;
            
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            String version = DeployUtils.getVersion(servInstId, containerInstId, instId);
            
            switch (header) {
            case FixHeader.HEADER_SMS_SERVER:
            case FixHeader.HEADER_SMS_SERVER_EXT:
            	ret = deploySmsServerNode(item, version, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_PROCESS:
            	ret = deploySmsProcessNode(item, version, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_CLIENT:
            	ret = deploySmsClientNode(item, version, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_BATSAVE:
            	ret = deploySmsBatSaveNode(item, version, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_STATS:
            	ret = deploySmsStatsNode(item, version, logKey, magicKey, result);
            	break;
            default:
            	break;
            }
            
            if (!ret) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean deploySmsServerNode(JsonObject serverItem, String version, String logKey, String magicKey, ResultBean result) {
        
        String instId        = serverItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = serverItem.getString(FixHeader.HEADER_SSH_ID);
        String metaSvrUrl    = serverItem.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr    = serverItem.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd = serverItem.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String jvmOps        = serverItem.getString(FixHeader.HEADER_JVM_OPS);
        String webConsolePort= serverItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        String cmpp20Port    = serverItem.getString(FixHeader.HEADER_CMPP20_PORT);
        String cmpp30Port    = serverItem.getString(FixHeader.HEADER_CMPP30_PORT);
        String sgip12Port    = serverItem.getString(FixHeader.HEADER_SGIP12_PORT);
        String smpp34Port    = serverItem.getString(FixHeader.HEADER_SMPP34_PORT);
        String smgp30Port    = serverItem.getString(FixHeader.HEADER_SMGP30_PORT);
        String httpPort      = serverItem.getString(FixHeader.HEADER_HTTP_PORT);
        String http2Port     = serverItem.getString(FixHeader.HEADER_HTTP_PORT2);
        String httpsPort     = serverItem.getString(FixHeader.HEADER_HTTPS_PORT);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed sms-server, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy sms-server, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, webConsolePort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.web控制台命令接收端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, cmpp20Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.cmpp20端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, cmpp30Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.cmpp30端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, sgip12Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.sgip12端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, smpp34Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.smpp34端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, smgp30Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.smgp30端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.http端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, http2Port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.http2端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-server", instId, servIp, httpsPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-server.https端口 is in use");
            return false;
        }
        
        // SMS_SERVER_FILE_ID -> 'smsserver-xxx.zip'
        String oldName = "smsserver";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_SERVER_FILE_ID, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String newName = "smsserver_" + instId;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsserver.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // JAVA_OPTS="%JVM_OPS%"
        String file = "./bin/smsserver.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) { ssh2.close(); return false; }
        jvmOps = jvmOps.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JVM_OPS, jvmOps, file, logKey, result)) { ssh2.close(); return false; }

        //执行权限
        if (!DeployUtils.chmod(ssh2, file, "+x", logKey, result)) {
            ssh2.close();
            return false;
        }

        //执行unix脚本命令
        if (!DeployUtils.dos2unix(ssh2, file, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "dos2unix failed......");
            ssh2.close();
            return false;
        }

        // start
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "pre_embadded instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsserver", instId, servIp, webConsolePort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start sms-server ......");
            String cmd = String.format("./bin/smsserver.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (!DeployUtils.checkPortUp(ssh2, "smsserver", instId, servIp, webConsolePort, logKey, result)) { ssh2.close(); return false; }
            
            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySmsProcessNode(JsonObject processItem, String version, String logKey, String magicKey, ResultBean result) {
        String instId         = processItem.getString(FixHeader.HEADER_INST_ID);
        String sshId          = processItem.getString(FixHeader.HEADER_SSH_ID);
        String metaSvrUrl     = processItem.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr     = processItem.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd  = processItem.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String rocketMQServ   = processItem.getString(FixHeader.HEADER_ROCKETMQ_SERV);
        String processor      = processItem.getString(FixHeader.HEADER_PROCESSOR);
        String jvmOps         = processItem.getString(FixHeader.HEADER_JVM_OPS);
        String webConsolePort = processItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        String realPort       = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));
        
        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed sms-process, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy sms-process, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-process", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-process.web控制台命令接收端口 is in use");
            return false;
        }
        
        // SMS_PROCESS_FILE_ID -> 'smsprocess-xxx.zip'
        String oldName = "smsprocess";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_PROCESS_FILE_ID, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String newName = "smsprocess_" + processor;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsprocess.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // ROCKETMQ_SERV=%ROCKETMQ_SERV%
        // PROCESSOR=%PROCESSOR%
        // JAVA_OPTS="%JVM_OPS%"
        String file = "./bin/smsprocess.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PROCESSOR, processor, file, logKey, result)) { ssh2.close(); return false; }
        jvmOps = jvmOps.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JVM_OPS, jvmOps, file, logKey, result)) { ssh2.close(); return false; }

        //执行权限
        if (!DeployUtils.chmod(ssh2, file, "+x", logKey, result)) {
            ssh2.close();
            return false;
        }

        //执行unix脚本命令
        if (!DeployUtils.dos2unix(ssh2, file, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "dos2unix failed......");
            ssh2.close();
            return false;
        }

        // start
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsserver", instId, servIp, webConsolePort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start sms-process ......");
            String cmd = String.format("./bin/smsprocess.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (!DeployUtils.checkPortUp(ssh2, "smsprocess", instId, servIp, realPort, logKey, result)) {
                ssh2.close();
                return false;
            }

            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySmsClientNode(JsonObject clientContainer, String version, String logKey, String magicKey, ResultBean result) {
    	
    	String instId        = clientContainer.getString(FixHeader.HEADER_INST_ID);
        String sshId         = clientContainer.getString(FixHeader.HEADER_SSH_ID);
        String metaSvrUrl    = clientContainer.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr    = clientContainer.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd = clientContainer.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String rocketMQServ  = clientContainer.getString(FixHeader.HEADER_ROCKETMQ_SERV);
        String processor     = clientContainer.getString(FixHeader.HEADER_PROCESSOR);
        String jvmOps        = clientContainer.getString(FixHeader.HEADER_JVM_OPS);
        String webConsolePort= clientContainer.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        String realPort      = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));
        
        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed sms-client, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy sms-client, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-client", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-client.web控制台命令接收端口 is in use");
            return false;
        }
        
        // SMS_CLIENT_FILE_ID -> 'smsclient-xxx.zip'
        String oldName = "smsclient-standard";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_CLIENT_FILE_ID, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }

        String newName = "smsclient-standard_" + processor;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsclient.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // ROCKETMQ_SERV=%ROCKETMQ_SERV%
        // PROCESSOR=%PROCESSOR%
        // JAVA_OPTS="%JVM_OPS%"
        String file = "./bin/smsclient.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PROCESSOR, processor, file, logKey, result)) { ssh2.close(); return false; }
        jvmOps = jvmOps.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JVM_OPS, jvmOps, file, logKey, result)) { ssh2.close(); return false; }

        //执行权限
        if (!DeployUtils.chmod(ssh2, file, "+x", logKey, result)) {
            ssh2.close();
            return false;
        }

        //执行unix脚本命令
        if (!DeployUtils.dos2unix(ssh2, file, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "dos2unix failed......");
            ssh2.close();
            return false;
        }

        // start
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsclient", instId, servIp, webConsolePort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start sms-client ......");
            String cmd = String.format("./bin/smsclient.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (!DeployUtils.checkPortUp(ssh2, "smsclient", instId, servIp, realPort, logKey, result)) {
                ssh2.close();
                return false;
            }
            
            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySmsBatSaveNode(JsonObject batsaveContainer, String version, String logKey, String magicKey, ResultBean result) {
        
    	String instId        = batsaveContainer.getString(FixHeader.HEADER_INST_ID);
        String sshId         = batsaveContainer.getString(FixHeader.HEADER_SSH_ID);
        String metaSvrUrl    = batsaveContainer.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr    = batsaveContainer.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd = batsaveContainer.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String rocketMQServ  = batsaveContainer.getString(FixHeader.HEADER_ROCKETMQ_SERV);
        String processor     = batsaveContainer.getString(FixHeader.HEADER_PROCESSOR);
        String jvmOps        = batsaveContainer.getString(FixHeader.HEADER_JVM_OPS);
        String webConsolePort= batsaveContainer.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        String dbInstId      = batsaveContainer.getString(FixHeader.HEADER_DB_INST_ID);
        String esServer      = batsaveContainer.getString(FixHeader.HEADER_ES_SERVER);
        String esMtServer    = batsaveContainer.getString(FixHeader.HEADER_ES_MT_SERVER);

        String realPort      = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));

        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed sms-batsave, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy sms-batsave, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-batsave", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-batsave.web控制台命令接收端口 is in use");
            return false;
        }
        
        // SMS_BATSAVE_FILE_ID -> 'smsbatsave-xxx.zip'
        String oldName = "smsbatsave";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_BATSAVE_FILE_ID, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }

        String newName = "smsbatsave_" + processor + "_" + dbInstId;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsbatsave.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // ROCKETMQ_SERV=%ROCKETMQ_SERV%
        // PROCESSOR=%PROCESSOR%
        // JAVA_OPTS="%JVM_OPS%"
        String file = "./bin/smsbatsave.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ROCKETMQ_SERV, rocketMQServ, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PROCESSOR, processor, file, logKey, result)) { ssh2.close(); return false; }
        jvmOps = jvmOps.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JVM_OPS, jvmOps, file, logKey, result)) { ssh2.close(); return false; }

        String esConfigFile = "./conf/elasticsearch.properties";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ES_SERVER, esServer, esConfigFile, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ES_MT_SERVER, esMtServer, esConfigFile, logKey, result)) { ssh2.close(); return false; }
        
        //执行权限
        if (!DeployUtils.chmod(ssh2, file, "+x", logKey, result)) {
            ssh2.close();
            return false;
        }

        //执行unix脚本命令
        if (!DeployUtils.dos2unix(ssh2, file, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "dos2unix failed......");
            ssh2.close();
            return false;
        }

        // start
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsbatsave", instId, servIp, webConsolePort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start sms-batsave ......");
            String cmd = String.format("./bin/smsbatsave.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (!DeployUtils.checkPortUp(ssh2, "smsbatsave", instId, servIp, realPort, logKey, result)) {
                ssh2.close();
                return false;
            }
            
            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySmsStatsNode(JsonObject statsContainer, String version, String logKey, String magicKey, ResultBean result) {
    	
    	String instId        = statsContainer.getString(FixHeader.HEADER_INST_ID);
        String sshId         = statsContainer.getString(FixHeader.HEADER_SSH_ID);
        String metaSvrUrl    = statsContainer.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr    = statsContainer.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd = statsContainer.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String jvmOps        = statsContainer.getString(FixHeader.HEADER_JVM_OPS);
        String webConsolePort= statsContainer.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed sms-stats, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy sms-stats, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "sms-batsave", instId, servIp, webConsolePort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("sms-batsave.web控制台命令接收端口 is in use");
            return false;
        }
        
        // SMS_STATS_FILE_ID -> 'smsstatistics-xxx.zip'
        String oldName = "smsstatistics";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_STATS_FILE_ID, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }

        String newName = "smsstatistics_" + instId;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsstatistics.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // JAVA_OPTS="%JVM_OPS%"
        String file = "./bin/smsstatistics.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) { ssh2.close(); return false; }
        jvmOps = jvmOps.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JVM_OPS, jvmOps, file, logKey, result)) { ssh2.close(); return false; }

        //执行权限
        if (!DeployUtils.chmod(ssh2, file, "+x", logKey, result)) {
            ssh2.close();
            return false;
        }

        //执行unix脚本命令
        if (!DeployUtils.dos2unix(ssh2, file, logKey, result)) {
            DeployLog.pubErrorLog(logKey, "dos2unix failed......");
            ssh2.close();
            return false;
        }

        // start
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsstats", instId, servIp, webConsolePort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start sms-statistics ......");
            String cmd = String.format("./bin/smsstatistics.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
    
            if (!DeployUtils.checkPortUp(ssh2, "smsstatistics", instId, servIp, webConsolePort, logKey, result)) { ssh2.close(); return false; }
            
            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsInstanceArr(String header, JsonObject serverContainer, String logKey, String magicKey, ResultBean result) {
        
    	JsonArray smsInstanceArr = serverContainer.getJsonArray(header);
        if (smsInstanceArr == null || smsInstanceArr.isEmpty())
            return true;
        
        int size = smsInstanceArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject item = smsInstanceArr.getJsonObject(i);
            boolean ret = true;
            
            switch (header) {
            case FixHeader.HEADER_SMS_SERVER:
            case FixHeader.HEADER_SMS_SERVER_EXT:
            	ret = undeploySmsServerNode(item, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_PROCESS:
            	ret = undeploySmsProcessNode(item, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_CLIENT:
            	ret = undeploySmsClientNode(item, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_BATSAVE:
            	ret = undeploySmsBatSaveNode(item, logKey, magicKey, result);
            	break;
            case FixHeader.HEADER_SMS_STATS:
            	ret = undeploySmsStatsNode(item, logKey, magicKey, result);
            	break;
            default:
            	break;
            }
            
            if (!ret)
            	return false;
        }
        
        return true;
    }
    
    public static boolean undeploySmsServerNode(JsonObject instItem, String logKey, String magicKey, ResultBean result) {
    	String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String webConsolePort= instItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed sms-server, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy sms-server, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = String.format("smsserver_%s", instId);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop sms-server ......");
        String cmd = String.format("./bin/smsserver.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortDown(ssh2, "smsserver", instId, servIp, webConsolePort, logKey, result)) { ssh2.close(); return false; }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsProcessNode(JsonObject instItem, String logKey, String magicKey, ResultBean result) {
    	String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String webConsolePort= instItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        String processor = instItem.getString(FixHeader.HEADER_PROCESSOR);

        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed sms-process, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy sms-process, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = String.format("smsprocess_%s", processor);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop sms-process ......");
        String cmd = String.format("./bin/smsprocess.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        String realPort = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));
        if (!DeployUtils.checkPortDown(ssh2, "smsprocess", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsClientNode(JsonObject instItem, String logKey, String magicKey, ResultBean result) {
    	String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String webConsolePort= instItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        String processor = instItem.getString(FixHeader.HEADER_PROCESSOR);

        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed sms-client, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy sms-client, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = String.format("smsclient-standard_%s", processor);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop sms-client ......");
        String cmd = String.format("./bin/smsclient.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        String realPort = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));

        if (!DeployUtils.checkPortDown(ssh2, "smsclient", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsBatSaveNode(JsonObject instItem, String logKey, String magicKey, ResultBean result) {
    	String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String webConsolePort= instItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        String processor = instItem.getString(FixHeader.HEADER_PROCESSOR);
        String dbInstId = instItem.getString(FixHeader.HEADER_DB_INST_ID);
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed sms-batsave, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy sms-batsave, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = String.format("smsbatsave_%s_%s", processor, dbInstId);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop sms-batsave ......");
        String cmd = String.format("./bin/smsbatsave.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        String realPort = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));
        if (!DeployUtils.checkPortDown(ssh2, "smsbatsave", instId, servIp, realPort, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsStatsNode(JsonObject instItem, String logKey, String magicKey, ResultBean result) {
    	String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String webConsolePort= instItem.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed sms-statistics, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy sms-statistics, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = String.format("smsstatistics_%s", instId);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop sms-statistics ......");
        String cmd = String.format("./bin/smsstatistics.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortDown(ssh2, "smsstatistics", instId, servIp, webConsolePort, logKey, result)) { ssh2.close(); return false; }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySmsGatewayInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        if (inst == null) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_FOUND);
            return false;
        }
        
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        if (inst.isDeployed()) {
            String info = String.format("%s %s is deployed ......", cmpt.getCmptName(), instID);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        JsonObject retval = new JsonObject();
        MetaDataDao.loadInstanceMeta(retval, instID);
        JsonObject instItem = retval.getJsonObject(FixHeader.HEADER_RET_INFO);
        String version = DeployUtils.getServiceVersion(servInstID, instID);

        boolean ret = true;
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            ret = deploySmsServerNode(instItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            ret = deploySmsProcessNode(instItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            ret = deploySmsClientNode(instItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            ret = deploySmsBatSaveNode(instItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_STATS:
            ret = deploySmsStatsNode(instItem, version, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        return ret;
    }
    
    public static boolean undeploySmsGatewayInstance(String instID, String logKey, String magicKey, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        if (inst == null) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_INSTANCE_NOT_FOUND);
            return false;
        }
        
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        if (!inst.isDeployed()) {
        	String info = String.format("%s %s is undeployed ......", cmpt.getCmptName(), instID);
        	DeployLog.pubLog(logKey, info);
            return true;
        }
        
        JsonObject retval = new JsonObject();
        MetaDataDao.loadInstanceMeta(retval, instID);
        JsonObject instItem = retval.getJsonObject(FixHeader.HEADER_RET_INFO);
        
        boolean ret = true;
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            ret = undeploySmsServerNode(instItem, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            ret = undeploySmsProcessNode(instItem, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            ret = undeploySmsClientNode(instItem, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            ret = undeploySmsBatSaveNode(instItem, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_STATS:
            ret = undeploySmsStatsNode(instItem, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        return ret;
    }
    
    public static boolean maintainInstance(String servInstID, String instID, String servType,
            InstanceOperationEnum op, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        
        JsonObject retval = new JsonObject();
        MetaDataDao.loadInstanceMeta(retval, instID);
        JsonObject instItem = retval.getJsonObject(FixHeader.HEADER_RET_INFO);
        
        if (op == InstanceOperationEnum.INSTANCE_OPERATION_UPDATE) {
            String version = DeployUtils.getServiceVersion(servInstID, instID);
            return updateSmsNode(instItem, instID, version, cmpt.getCmptName(), op, logKey, magicKey, result);
        } else {
            return maintainSmsNode(instItem, instID, cmpt.getCmptName(), op, isOperateByHandle, logKey, magicKey, result);
        }
    }
    
    private static boolean updateSmsNode(JsonObject item, String instID, String version, String cmptName, InstanceOperationEnum op,
            String logKey, String magicKey, ResultBean result) {
        
        String instId   = item.getString(FixHeader.HEADER_INST_ID);
        String sshId    = item.getString(FixHeader.HEADER_SSH_ID);
        
        PaasSsh ssh     = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp  = ssh.getServerIp();
        String  sshName = ssh.getSshName();
        String  sshPwd  = ssh.getSshPwd();
        int     sshPort = ssh.getSshPort();
        
        DeployLog.pubLog(logKey, String.format("%s %s, inst_id:%s, serv_ip:%s", op.getAction(), cmptName, instId, servIp));
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = null, stopCmd = null, startCmd = null, processor = null, dbInstId = null, oldName = null;
        int fileId = 0;
        
        String baseDir = String.format("%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT);
        
        switch (cmptName) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            oldName = "smsserver";
            newName = "smsserver_" + instId;
            fileId = FixDefs.SMS_SERVER_FILE_ID;
            stopCmd = String.format("./%s/bin/smsserver.sh stop", newName);
            startCmd = String.format("./%s/bin/smsserver.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            oldName = "smsprocess";
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            newName = "smsprocess_" + processor;
            fileId = FixDefs.SMS_PROCESS_FILE_ID;
            stopCmd = String.format("./%s/bin/smsprocess.sh stop", newName);
            startCmd = String.format("./%s/bin/smsprocess.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            oldName = "smsclient-standard";
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            newName = "smsclient-standard_" + processor;
            fileId = FixDefs.SMS_CLIENT_FILE_ID;
            stopCmd = String.format("./%s/bin/smsclient.sh stop", newName);
            startCmd = String.format("./%s/bin/smsclient.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            oldName = "smsbatsave";
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            dbInstId = item.getString(FixHeader.HEADER_DB_INST_ID);
            newName = "smsbatsave_" + processor + "_" + dbInstId;
            fileId = FixDefs.SMS_BATSAVE_FILE_ID;
            stopCmd = String.format("./%s/bin/smsbatsave.sh stop", newName);
            startCmd = String.format("./%s/bin/smsbatsave.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_STATS:
            oldName = "smsstatistics";
            newName = "smsstatistics_" + instId;
            fileId = FixDefs.SMS_STATS_FILE_ID;
            stopCmd = String.format("./%s/bin/smsstatistics.sh stop", newName);
            startCmd = String.format("./%s/bin/smsstatistics.sh start", newName);
            break;
        default:
            break;
        }
        
        // 1. scp deploy file and unzip
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, fileId, FixDefs.SMS_GATEWAY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.cd(ssh2, baseDir, logKey, result)) { ssh2.close(); return false; }
        
        // 2. stop instance
        DeployLog.pubLog(logKey, stopCmd);
        if (!DeployUtils.execSimpleCmd(ssh2, stopCmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.isPreEmbadded(instId)) {
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            if (DeployUtils.isProcExist(ssh2, instId, logKey, result)) {
                DeployLog.pubSuccessLog(logKey, String.format("%s %s 更新前执行进程停止失败", cmptName, instId));
                
                ssh2.close();
                return false;
            }
        }
        
        // 3. 更新文件
        String rmInstanceJar = String.format("rm ./%s/*.jar ", newName);
        String rmLibJar = String.format("rm ./%s/lib/*.jar ", newName);
        String cmdInstanceJar = String.format("cp ./%s/*.jar ./%s ", oldName, newName);
        String cmdLibJar = String.format("cp ./%s/lib/*.jar ./%s/lib ", oldName, newName);
        if (!DeployUtils.execSimpleCmd(ssh2, rmInstanceJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, rmLibJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, cmdInstanceJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, cmdLibJar, logKey, result)) { ssh2.close(); return false; }
        
        // 4. 重新拉起
        DeployLog.pubLog(logKey, startCmd);
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) {
                ssh2.close();
                return false;
            }
        } else {
            if (!DeployUtils.execSimpleCmd(ssh2, startCmd, logKey, result)) { 
                ssh2.close();
                return false;
            }
            
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            if (!DeployUtils.isProcExist(ssh2, instId, logKey, result)) {
                DeployLog.pubSuccessLog(logKey, String.format("%s %s 更新后执行进程拉起失败", cmptName, instId));
                
                ssh2.close();
                return false;
            }
        }
        if (!DeployUtils.rm(ssh2, oldName, logKey, result)) { ssh2.close(); return false; }
        
        // 5. 修改实例的version属性
        // 227 -> 'VERSION'
        if (!MetaDataDao.modInstanceAttr(instId, 227, "VERSION", version, magicKey)) { ssh2.close(); return false; }

        boolean res = true;
        if (!DeployUtils.isPreEmbadded(instId)) {
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            res = DeployUtils.isProcExist(ssh2, instId, logKey, result);
            if (res) {
                res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey);
            }
        }
        
        DeployLog.pubSuccessLog(logKey, String.format("%s %s %s", op.getAction(), cmptName, res ? "success" : "fail"));
        
        ssh2.close();
        result.setRetInfo(version);
        return res;
    }
    
    private static boolean maintainSmsNode(JsonObject item, String instID, String cmptName, InstanceOperationEnum op,
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        
        String instId   = item.getString(FixHeader.HEADER_INST_ID);
        String sshId    = item.getString(FixHeader.HEADER_SSH_ID);
        
        PaasSsh ssh     = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp  = ssh.getServerIp();
        String  sshName = ssh.getSshName();
        String  sshPwd  = ssh.getSshPwd();
        int     sshPort = ssh.getSshPort();
        
        DeployLog.pubLog(logKey, String.format("%s %s, inst_id:%s, serv_ip:%s", op.getAction(), cmptName, instId, servIp));
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName, dir = null, cmd = null, processor = null, dbInstId = null;
        switch (cmptName) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            newName = "smsserver_" + instId;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
            cmd = String.format("./bin/smsserver.sh %s", op.getAction());
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            newName = "smsprocess_" + processor;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
            cmd = String.format("./bin/smsprocess.sh %s", op.getAction());
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            newName = "smsclient-standard_" + processor;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
            cmd = String.format("./bin/smsclient.sh %s", op.getAction());
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            processor = item.getString(FixHeader.HEADER_PROCESSOR);
            dbInstId = item.getString(FixHeader.HEADER_DB_INST_ID);
            newName = "smsbatsave_" + processor + "_" + dbInstId;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
            cmd = String.format("./bin/smsbatsave.sh %s", op.getAction());
            break;
        case FixHeader.HEADER_SMS_STATS:
            newName = "smsstatistics_" + instId;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT, newName);
            cmd = String.format("./bin/smsstatistics.sh %s", op.getAction());
            break;
        default:
            break;
        }
        
        if (!DeployUtils.cd(ssh2, dir, logKey, result)) { ssh2.close(); return false; }
        
        // 非预埋或者执行故障切换时才拉起
        if (!DeployUtils.isPreEmbadded(instId) || !isOperateByHandle) {
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (op == InstanceOperationEnum.INSTANCE_OPERATION_START || op == InstanceOperationEnum.INSTANCE_OPERATION_RESTART) {
                DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
                if (!DeployUtils.isProcExist(ssh2, instId, logKey, result)) { ssh2.close(); return false; }
            }
        }
        
        boolean res = true;
        if (!DeployUtils.isPreEmbadded(instId) || !isOperateByHandle) {
            switch (op) {
            case INSTANCE_OPERATION_STOP:
                res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_WARN, result, magicKey);
                break;
            case INSTANCE_OPERATION_START:
            case INSTANCE_OPERATION_RESTART:
                res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey);
                break;
            default:
                break;
            }
        }
        
        if (res && (op == InstanceOperationEnum.INSTANCE_OPERATION_START
                || op == InstanceOperationEnum.INSTANCE_OPERATION_RESTART)) {
            // 预埋节点如果手工启动
            if (DeployUtils.isPreEmbadded(instId) && !isOperateByHandle) {
                // 预埋实例拉起成功，修改PRE_EMBEDDED属性为S_FALSE，视为与正常实例一样
                res = MetaDataDao.updateInstancePreEmbadded(instId, FixDefs.S_FALSE, result, magicKey);
            }
        }
        
        if (!DeployUtils.isPreEmbadded(instId) || !isOperateByHandle) {
            DeployLog.pubSuccessLog(logKey, String.format("%s %s %s", op.getAction(), cmptName, res ? "success" : "fail"));
            if (op == InstanceOperationEnum.INSTANCE_OPERATION_START
                    || op == InstanceOperationEnum.INSTANCE_OPERATION_RESTART) {
                DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
                res = DeployUtils.isProcExist(ssh2, instId, logKey, result);
            }
        } else {
            DeployLog.pubSuccessLog(logKey, String.format("pre-embadded instance passby %s", op.getAction()));
        }
        
        ssh2.close();
        return res;
    }
    
    public static boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        
        String sshId    = meta.getInstAttr(instID, 116).getAttrValue(); // 116 -> 'SSH_ID'
        PaasSsh ssh     = DeployUtils.getSshById(sshId, null, result);
        if (ssh == null) return false;
        
        String  servIp  = ssh.getServerIp();
        String  sshName = ssh.getSshName();
        String  sshPwd  = ssh.getSshPwd();
        int     sshPort = ssh.getSshPort();

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, null, result)) return false;
        
        String webConsolePort = meta.getInstAttr(instID, 146).getAttrValue();  // 146 -> 'WEB_CONSOLE_PORT'
        PaasInstAttr processorAttr = meta.getInstAttr(instID, 205);            // 205 -> 'PROCESSOR'
        String realPort;
        if (processorAttr != null) {
            String processor = processorAttr.getAttrValue();
            realPort = String.valueOf(Integer.parseInt(webConsolePort) + Integer.parseInt(processor));
        } else {
            realPort = webConsolePort;
        }
        
        boolean ret = true;
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            ret = DeployUtils.checkPortUp(ssh2, "smsserver", instID, servIp, realPort, null, result);
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            ret = DeployUtils.checkPortUp(ssh2, "smsprocess", instID, servIp, realPort, null, result);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            ret = DeployUtils.checkPortUp(ssh2, "smsclient", instID, servIp, realPort, null, result);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            ret = DeployUtils.checkPortUp(ssh2, "smsbatsave", instID, servIp, realPort, null, result);
            break;
        case FixHeader.HEADER_SMS_STATS:
            ret = DeployUtils.checkPortUp(ssh2, "smsstatistics", instID, servIp, realPort, null, result);
            break;
        default:
            break;
        }
        
        if (!ret) {
            result.setRetCode(CONSTS.REVOKE_NOK);
        }
        
        ssh2.close();
        return ret;
    }

    public static boolean updateInstanceForBatch(String servInstID, String instID, String servType, 
            boolean loadDeployFile, boolean rmDeployFile, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        
        String cmptName = cmpt.getCmptName();
        String version  = DeployUtils.getServiceVersion(servInstID, instID);
        String instId   = meta.getInstAttr(instID, 114).getAttrValue();  // 114 -> 'INST_ID'
        String sshId    = meta.getInstAttr(instID, 116).getAttrValue();  // 116 -> 'SSH_ID'
        
        PaasSsh ssh     = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp  = ssh.getServerIp();
        String  sshName = ssh.getSshName();
        String  sshPwd  = ssh.getSshPwd();
        int     sshPort = ssh.getSshPort();
        
        DeployLog.pubLog(logKey, String.format("update %s, inst_id:%s, serv_ip:%s", cmptName, instId, servIp));
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = null, stopCmd = null, startCmd = null, processor = null, dbInstId = null, oldName = null;
        int fileId = 0;
        
        String baseDir = String.format("%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_GATEWAY_ROOT);
        
        switch (cmptName) {
        case FixHeader.HEADER_SMS_SERVER:
        case FixHeader.HEADER_SMS_SERVER_EXT:
            oldName = "smsserver";
            newName = "smsserver_" + instId;
            fileId = FixDefs.SMS_SERVER_FILE_ID;
            stopCmd = String.format("./%s/bin/smsserver.sh stop", newName);
            startCmd = String.format("./%s/bin/smsserver.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_PROCESS:
            oldName = "smsprocess";
            processor = meta.getInstAttr(instID, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            newName = "smsprocess_" + processor;
            fileId = FixDefs.SMS_PROCESS_FILE_ID;
            stopCmd = String.format("./%s/bin/smsprocess.sh stop", newName);
            startCmd = String.format("./%s/bin/smsprocess.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            oldName = "smsclient-standard";
            processor = meta.getInstAttr(instID, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            newName = "smsclient-standard_" + processor;
            fileId = FixDefs.SMS_CLIENT_FILE_ID;
            stopCmd = String.format("./%s/bin/smsclient.sh stop", newName);
            startCmd = String.format("./%s/bin/smsclient.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            oldName = "smsbatsave";
            processor = meta.getInstAttr(instID, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            dbInstId = meta.getInstAttr(instID, 213).getAttrValue();   // 213 -> 'DB_INST_ID'
            newName = "smsbatsave_" + processor + "_" + dbInstId;
            fileId = FixDefs.SMS_BATSAVE_FILE_ID;
            stopCmd = String.format("./%s/bin/smsbatsave.sh stop", newName);
            startCmd = String.format("./%s/bin/smsbatsave.sh start", newName);
            break;
        case FixHeader.HEADER_SMS_STATS:
            oldName = "smsstatistics";
            newName = "smsstatistics_" + instId;
            fileId = FixDefs.SMS_STATS_FILE_ID;
            stopCmd = String.format("./%s/bin/smsstatistics.sh stop", newName);
            startCmd = String.format("./%s/bin/smsstatistics.sh start", newName);
            break;
        default:
            break;
        }
        
        // 1. scp deploy file and unzip
        if (loadDeployFile) {
            if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, fileId, FixDefs.SMS_GATEWAY_ROOT,
                    oldName, version, logKey, result)) {
                ssh2.close();
                return false;
            }
        }
        
        if (!DeployUtils.cd(ssh2, baseDir, logKey, result)) { ssh2.close(); return false; }
        
        // 2. stop instance
        DeployLog.pubLog(logKey, stopCmd);
        if (!DeployUtils.execSimpleCmd(ssh2, stopCmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.isPreEmbadded(instId)) {
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            if (DeployUtils.isProcExist(ssh2, instID, logKey, result)) {
                DeployLog.pubSuccessLog(logKey, String.format("%s %s 更新前执行进程停止失败", cmptName, instId));
                
                ssh2.close();
                return false;
            }
        }
        
        // 3. 更新文件
        String rmInstanceJar = String.format("rm ./%s/*.jar ", newName);
        String rmLibJar = String.format("rm ./%s/lib/*.jar ", newName);
        String cmdInstanceJar = String.format("cp ./%s/*.jar ./%s ", oldName, newName);
        String cmdLibJar = String.format("cp ./%s/lib/*.jar ./%s/lib ", oldName, newName);
        if (!DeployUtils.execSimpleCmd(ssh2, rmInstanceJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, rmLibJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, cmdInstanceJar, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, cmdLibJar, logKey, result)) { ssh2.close(); return false; }
        
        // 4. 重新拉起
        DeployLog.pubLog(logKey, startCmd);
        
        if (DeployUtils.isPreEmbadded(instId)) {
            DeployLog.pubLog(logKey, "PRE_EMBADDED instance, do not need to start ......");
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey)) { ssh2.close(); return false; }
        } else {
            if (!DeployUtils.execSimpleCmd(ssh2, startCmd, logKey, result)) {
                ssh2.close();
                return false;
            }
            
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            if (!DeployUtils.isProcExist(ssh2, instId, logKey, result)) {
                DeployLog.pubSuccessLog(logKey, String.format("%s %s 更新后执行进程拉起失败", cmptName, instId));
                
                ssh2.close();
                return false;
            }
        }
        if (rmDeployFile) {
            if (!DeployUtils.rm(ssh2, oldName, logKey, result)) { ssh2.close(); return false; }
        }
        
        // 5. 修改实例的version属性
        // 227 -> 'VERSION'
        if (!MetaDataDao.modInstanceAttr(instId, 227, "VERSION", version, magicKey)) { ssh2.close(); return false; }

        
        boolean res = true;
        // 非预埋节点且成功启动后才更新IS_DEPLOYED = 1
        if (!DeployUtils.isPreEmbadded(instId)) {
            DeployUtils.sleepMilliSeconds(WAIT_MILLI_SECONDS);
            res = DeployUtils.isProcExist(ssh2, instId, logKey, result);
            if (res)
                res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, magicKey);
        }
        
        DeployLog.pubSuccessLog(logKey, String.format("update %s to version:%s %s", cmptName, version, res ? "success" : "fail"));
        
        ssh2.close();
        result.setRetInfo(version);
        return res;
    }
}
