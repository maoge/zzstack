package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import java.util.Vector;

import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
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

public class SmsQueryServiceDeployUtils {

    public static boolean deployNgxArr(String servInstId, String containerInstId, JsonArray ngxArr, 
            String servList, String logKey, String magicKey, ResultBean result) {

        for (int i = 0; i < ngxArr.size(); ++i) {
            JsonObject item = ngxArr.getJsonObject(i);
            
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            String version = DeployUtils.getVersion(servInstId, containerInstId, instId);
            
            if (!deployNgxNode(item, servList, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean undeployNgxArr(String servInstId, String containerInstId, JsonArray ngxArr,
            String logKey, String magicKey, ResultBean result) {
        
        for (int i = 0; i < ngxArr.size(); ++i) {
            JsonObject item = ngxArr.getJsonObject(i);
            
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            String version = DeployUtils.getVersion(servInstId, containerInstId, instId);
            
            if (!undeployNgxNode(item, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean deploySmsQueryArr(String servInstId, String containerInstId, JsonArray smsQueryArr,
            String logKey, String magicKey, ResultBean result) {
        
        for (int i = 0; i < smsQueryArr.size(); ++i) {
            JsonObject item = smsQueryArr.getJsonObject(i);
            
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            String version = DeployUtils.getVersion(servInstId, containerInstId, instId);
            
            if (!deploySmsQueryNode(item, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean undeploySmsQueryArr(String servInstId, String containerInstId, JsonArray smsQueryArr,
            String logKey, String magicKey, ResultBean result) {
        
        for (int i = 0; i < smsQueryArr.size(); ++i) {
            JsonObject item = smsQueryArr.getJsonObject(i);
            
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            String version = DeployUtils.getVersion(servInstId, containerInstId, instId);
            
            if (!undeploySmsQueryNode(item, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        return true;
    }
    
    public static boolean deploySmsQueryNode(JsonObject item, String version, String logKey, String magicKey, ResultBean result) {
        String instId        = item.getString(FixHeader.HEADER_INST_ID);
        String sshId         = item.getString(FixHeader.HEADER_SSH_ID);
        String vertxPort     = item.getString(FixHeader.HEADER_VERTX_PORT);
        String metaSvrUrl    = item.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr    = item.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPwd    = item.getString(FixHeader.HEADER_META_SVR_PASSWD);
        String jvmOps        = item.getString(FixHeader.HEADER_JVM_OPS);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed smsqueryserver, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy smsqueryserver, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "smsqueryserver", instId, servIp, vertxPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("smsqueryserver.vertxPort端口 is in use");
            return false;
        }
        
        // SMS_QUERY_SERVER_FILE_ID -> 'smsqueryserver-%VERSION%.zip'
        String oldName = "smsqueryserver";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.SMS_QUERY_SERVER_FILE_ID, FixDefs.SMS_QUERY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String newName = "smsqueryserver_" + instId;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify smsqueryserver.sh env params ......");
        
        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // JAVA_OPTS="%JVM_OPS%"
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        jvmOps = jvmOps.replace("/", "\\/");
        
        String file = "./bin/smsqueryserver.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPwd, file, logKey, result)) { ssh2.close(); return false; }
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
            
            String info = String.format("deploy pre_embadded %s success, inst_id:%s, serv_ip:%s, port:%s", "smsqueryserver", instId, servIp, vertxPort);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            DeployLog.pubLog(logKey, "start smsqueryserver ......");
            String cmd = String.format("./bin/smsqueryserver.sh start");
            if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
            
            if (!DeployUtils.checkPortUp(ssh2, "smsqueryserver", instId, servIp, vertxPort, logKey, result)) { ssh2.close(); return false; }
            
            // update instance deploy flag
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeploySmsQueryNode(JsonObject instItem, String version, String logKey, String magicKey, ResultBean result) {
        String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String vertxPort     = instItem.getString(FixHeader.HEADER_VERTX_PORT);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed smsqueryserver, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy smsqueryserver, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = "smsqueryserver_" + instId;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_QUERY_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }
        // stop
        DeployLog.pubLog(logKey, "stop smsqueryserver ......");
        String cmd = String.format("./bin/smsqueryserver.sh stop");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortDown(ssh2, "smsqueryserver", instId, servIp, vertxPort, logKey, result)) { ssh2.close(); return false; }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_SAVED, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deployNgxNode(JsonObject item, String servList, String version, String logKey, String magicKey, ResultBean result) {
        String instId        = item.getString(FixHeader.HEADER_INST_ID);
        String sshId         = item.getString(FixHeader.HEADER_SSH_ID);
        String wrokerProcess = item.getString(FixHeader.HEADER_WORKER_PROCESSES);
        String lisnPort      = item.getString(FixHeader.HEADER_LISTEN_PORT);

        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed nginx, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start deploy nginx, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "nginx", instId, servIp, lisnPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("nginx.listen_port端口 is in use");
            return false;
        }
        
        // NGX_FILE_ID -> 'nginx-1.19.6.tar.gz'         
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.NGX_FILE_ID, FixDefs.COMMON_TOOLS_ROOT,
                version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.NGX_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + lisnPort;
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
        
        DeployLog.pubLog(logKey, "fetch and replace nginx.conf configure ......");
        // scp nginx_sms_query.conf
        if (!DeployUtils.fetchFile(ssh2, FixDefs.NGX_SMS_QUERY_CONF_FILE_ID, logKey, result)) {
            DeployLog.pubFailLog(logKey, "scp nginx_sms_query.conf fail ......");
            return false;
        }
        // worker_processes  %WORKER_PROCESSES%;
        // %SERVER_LIST%
        // listen       %LISTEN_PORT%;
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_WORKER_PROCESSES, wrokerProcess, FixDefs.NGX_SMS_QUERY_CONF, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.appendMultiLine(ssh2, FixDefs.CONF_SERVER_LIST, servList, FixDefs.NGX_SMS_QUERY_CONF, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_PORT, lisnPort, FixDefs.NGX_SMS_QUERY_CONF, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.mv(ssh2, "./conf/" + FixDefs.NGX_CONF, FixDefs.NGX_SMS_QUERY_CONF, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start nginx ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "nginx", instId, servIp, lisnPort, logKey, result)) {
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
    
    public static boolean undeployNgxNode(JsonObject instItem, String version, String logKey, String magicKey, ResultBean result) {
        String instId        = instItem.getString(FixHeader.HEADER_INST_ID);
        String sshId         = instItem.getString(FixHeader.HEADER_SSH_ID);
        String lisnPort      = instItem.getString(FixHeader.HEADER_LISTEN_PORT);
        
        PaasSsh ssh          = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp       = ssh.getServerIp();
        String  sshName      = ssh.getSshName();
        String  sshPwd       = ssh.getSshPwd();
        int     sshPort      = ssh.getSshPort();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed nginx, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }
        
        {
            String info = String.format("start undeploy nginx, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.NGX_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + lisnPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }
        
        // stop
        DeployLog.pubLog(logKey, "stop nginx ......");
        String cmd = String.format("./stop.sh");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortDown(ssh2, "nginx", instId, servIp, lisnPort, logKey, result)) { ssh2.close(); return false; }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) { ssh2.close(); return false; }
        
        ssh2.close();
        return true;
    }
    
    public static boolean maintainInstance(String servInstID, String instID, String servType,
            InstanceOperationEnum op, String logKey, String magicKey, ResultBean result) {
        
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = meta.getInstance(instID);
        PaasMetaCmpt cmpt = meta.getCmptById(inst.getCmptId());
        
        JsonObject retval = new JsonObject();
        MetaDataDao.loadInstanceMeta(retval, instID);
        JsonObject instItem = retval.getJsonObject(FixHeader.HEADER_RET_INFO);
        String version = DeployUtils.getServiceVersion(servInstID, instID);
        
        if (op == InstanceOperationEnum.INSTANCE_OPERATION_UPDATE) {
            return updateNode(instItem, instID, version, cmpt.getCmptName(), op, logKey, magicKey, result);
        } else {
            return maintainNode(instItem, instID, cmpt.getCmptName(), version, op, logKey, magicKey, result);
        }
    }
    
    private static boolean updateNode(JsonObject item, String instID, String version, String cmptName, InstanceOperationEnum op,
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
        
        String newName = null, stopCmd = null, startCmd = null, oldName = null;
        int fileId = 0;
        
        String baseDir = String.format("%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_QUERY_ROOT);
        
        switch (cmptName) {
        case FixHeader.HEADER_SMS_QUERY:
            oldName = "smsqueryserver";
            newName = "smsqueryserver_" + instId;
            fileId = FixDefs.SMS_QUERY_SERVER_FILE_ID;
            stopCmd = String.format("./%s/bin/smsqueryserver.sh stop", newName);
            startCmd = String.format("./%s/bin/smsqueryserver.sh start", newName);
            break;
        default:
            break;
        }
        
        // 1. scp deploy file and unzip
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, fileId, FixDefs.SMS_QUERY_ROOT,
                oldName, version, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.cd(ssh2, baseDir, logKey, result)) { ssh2.close(); return false; }
        
        // 2. stop instance
        DeployLog.pubLog(logKey, stopCmd);
        if (!DeployUtils.execSimpleCmd(ssh2, stopCmd, logKey, result)) { ssh2.close(); return false; }
        
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
        if (!DeployUtils.execSimpleCmd(ssh2, startCmd, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, oldName, logKey, result)) { ssh2.close(); return false; }
        
        // 5. 修改实例的version属性
        // 227 -> 'VERSION'
        if (!MetaDataDao.modInstanceAttr(instId, 227, "VERSION", version, magicKey)) { ssh2.close(); return false; }

        boolean res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey);
        DeployLog.pubSuccessLog(logKey, String.format("%s %s %s", op.getAction(), cmptName, res ? "success" : "fail"));
        
        ssh2.close();
        result.setRetInfo(version);
        return res;
    }
    
    private static boolean maintainNode(JsonObject item, String instID, String cmptName, String version,
            InstanceOperationEnum op, String logKey, String magicKey, ResultBean result) {
        
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
        
        String oldName = null, newName = null, dir = null, cmd = null, lisnPort = null;
        switch (cmptName) {
        case FixHeader.HEADER_NGX:
            oldName = DeployUtils.getVersionedFileName(FixDefs.NGX_FILE_ID, version, logKey, result);
            lisnPort = item.getString(FixHeader.HEADER_LISTEN_PORT);
            newName = oldName + "_" + lisnPort;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
            cmd = (op == InstanceOperationEnum.INSTANCE_OPERATION_START) ? "start.sh" : "stop.sh";
            break;
        case FixHeader.HEADER_SMS_QUERY:
            newName = "smsqueryserver_" + instId;
            dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_QUERY_ROOT, newName);
            cmd = String.format("./bin/smsqueryserver.sh %s", op.getAction());
            break;
        default:
            break;
        }
        
        if (!DeployUtils.cd(ssh2, dir, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        boolean res = false;
        switch (op) {
        case INSTANCE_OPERATION_STOP:
            res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_WARN, result, magicKey);
            break;
        case INSTANCE_OPERATION_START:
        case INSTANCE_OPERATION_RESTART:
            res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey);
            break;
        default:
            break;
        }
        
        DeployLog.pubSuccessLog(logKey, String.format("%s %s %s", op.getAction(), cmptName, res ? "success" : "fail"));
        
        ssh2.close();
        return res;
    }
    
    public static boolean updateInstanceForBatch(String servInstID, String instID, String servType, 
            boolean loadDeployFile, boolean rmDeployFile, String logKey, String magicKey, ResultBean result) {
        
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
        
        String newName = null, stopCmd = null, startCmd = null, oldName = null;
        int fileId = 0;
        
        String baseDir = String.format("%s/%s", FixDefs.PAAS_ROOT, FixDefs.SMS_QUERY_ROOT);
        
        switch (cmptName) {
        case FixHeader.HEADER_SMS_QUERY:
            oldName = "smsqueryserver";
            newName = "smsqueryserver_" + instId;
            fileId = FixDefs.SMS_QUERY_SERVER_FILE_ID;
            stopCmd = String.format("./%s/bin/smsqueryserver.sh stop", newName);
            startCmd = String.format("./%s/bin/smsqueryserver.sh start", newName);
            break;
        default:
            break;
        }
        
        // 1. scp deploy file and unzip
        if (loadDeployFile) {
            if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, fileId, FixDefs.SMS_QUERY_ROOT,
                    oldName, version, logKey, result)) {
                ssh2.close();
                return false;
            }
        }
        
        if (!DeployUtils.cd(ssh2, baseDir, logKey, result)) { ssh2.close(); return false; }
        
        // 2. stop instance
        DeployLog.pubLog(logKey, stopCmd);
        if (!DeployUtils.execSimpleCmd(ssh2, stopCmd, logKey, result)) { ssh2.close(); return false; }
        
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
        if (!DeployUtils.execSimpleCmd(ssh2, startCmd, logKey, result)) { ssh2.close(); return false; }
        if (rmDeployFile) {
            if (!DeployUtils.rm(ssh2, oldName, logKey, result)) { ssh2.close(); return false; }
        }
        
        // 5. 修改实例的version属性
        // 227 -> 'VERSION'
        if (!MetaDataDao.modInstanceAttr(instId, 227, "VERSION", version, magicKey)) { ssh2.close(); return false; }

        boolean res = MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey);
        DeployLog.pubSuccessLog(logKey, String.format("update %s to version:%s %s", cmptName, version, res ? "success" : "fail"));
        
        ssh2.close();
        result.setRetInfo(version);
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
        
        String port = null;
        boolean ret = true;
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_NGX:
            port = meta.getInstAttr(instID, 126).getAttrValue();  // 126 -> 'LISTEN_PORT'
            ret = DeployUtils.checkPortUp(ssh2, "nginx", instID, servIp, port, null, result);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            port = meta.getInstAttr(instID, 296).getAttrValue();  // 296 -> 'VERTX_PORT'
            ret = DeployUtils.checkPortUp(ssh2, "smsqueryserver", instID, servIp, port, null, result);
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
    
    public static String getSmsQueryServList(String header, JsonObject container) {
        JsonArray instanceArr = container.getJsonArray(header);
        if (instanceArr == null || instanceArr.isEmpty())
            return "";
        
        StringBuilder sb = new StringBuilder();
        int size = instanceArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject item = instanceArr.getJsonObject(i);
            String sshId = item.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, "", null);
            if (ssh == null) continue;
            
            String servIp = ssh.getServerIp();
            String vertxPort = item.getString(FixHeader.HEADER_VERTX_PORT);
            String line = String.format("         server %s:%s;", servIp, vertxPort);
            if (sb.length() > 0) sb.append(FixDefs.LINE_SEP);
            
            sb.append(line);
        }
        
        return sb.toString();
    }
    
    public static String getSmsQueryServList(String servInstID) {
        StringBuilder servList = new StringBuilder();
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        Vector<PaasTopology> servRelations = new Vector<PaasTopology>();
        meta.getInstRelations(servInstID, servRelations);
        for (PaasTopology topo : servRelations) {
            String toeId = topo.getToe(servInstID);
            if (toeId == null || toeId.isEmpty())
                continue;
            
            PaasInstance container = meta.getInstance(toeId);
            if (container.getCmptId() != 721) // 721 -> 'SMS_QUERY_CONTAINER'
                continue;
            
            Vector<PaasTopology> containerRelations = new Vector<PaasTopology>();
            meta.getInstRelations(toeId, containerRelations);
            
            for (PaasTopology subTopo : containerRelations) {
                String subInstId = subTopo.getToe(toeId);
                if (subInstId == null)
                    continue;
                
                String vertxPort = meta.getInstAttr(subInstId, 116).getAttrValue(); // 296 -> 'VERTX_PORT'
                String sshId = meta.getInstAttr(subInstId, 116).getAttrValue();     // 116 -> 'SSH_ID'
                PaasSsh ssh = meta.getSshById(sshId);
                String servIp = ssh.getServerIp();
                
                String line = String.format("         server %s:%s;", servIp, vertxPort);
                if (servList.length() > 0) servList.append(FixDefs.LINE_SEP);
                
                servList.append(line);
            }
        }
        
        return servList.toString();
    }

}
