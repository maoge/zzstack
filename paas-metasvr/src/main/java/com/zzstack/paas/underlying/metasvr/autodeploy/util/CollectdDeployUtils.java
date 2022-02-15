package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class CollectdDeployUtils {

    public static boolean deployCollectd(JsonObject collectd, String srvInstId, String logKey, String magicKey, ResultBean result) {
        String sshId = collectd.getString(FixHeader.HEADER_SSH_ID);
        String collectdPort = collectd.getString(FixHeader.HEADER_COLLECTD_PORT);
        String instId = collectd.getString(FixHeader.HEADER_INST_ID);
        String metaSvrUrl = collectd.getString(FixHeader.HEADER_META_SVR_URL);
        String metaSvrUsr = collectd.getString(FixHeader.HEADER_META_SVR_USR);
        String metaSvrPasswd = collectd.getString(FixHeader.HEADER_META_SVR_PASSWD);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby deployed collectd, inst_id:%s", instId);
            DeployLog.pubLog(logKey, info);
            return true;
        }

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "collectd", instId, servIp, collectdPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("collectd.服务端口 is in use");
            return false;
        }

        String oldName = "paas-collectd";
        if (!DeployUtils.fetchAndExtractZipDeployFile(ssh2, FixDefs.COLLECTD_FILE_ID, FixDefs.COLLECTD_ROOT, oldName, "", logKey, result)) {
            ssh2.close();
            return false;
        }

        String newName = "collectd_" + instId;
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
        DeployLog.pubLog(logKey, "modify paas-collectd.sh env params ......");

        // 替换启停脚本中的如下变量
        // UUID=%UUID%
        // SERV_INST_ID=%SERV_INST_ID%
        // META_SVR_URL=%META_SVR_URL%
        // META_SVR_USR=%META_SVR_USR%
        // META_SVR_PASSWD=%META_SVR_PASSWD%
        // COLLECTD_PORT=%COLLECTD_PORT%
        String file = "./bin/paas-collectd.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_UUID, instId, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_INST_ID, srvInstId, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        metaSvrUrl = metaSvrUrl.replace("/", "\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_URL, metaSvrUrl, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_USR, metaSvrUsr, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_SVR_PASSWD, metaSvrPasswd, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_COLLECTD_PORT, collectdPort, file, logKey, result)) {
            ssh2.close();
            return false;
        }

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
        DeployLog.pubLog(logKey, "start collectd ......");
        String cmd = String.format("./bin/paas-collectd.sh start");
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "paas-collectd", instId, servIp, collectdPort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        DeployLog.pubLog(logKey, "init collectd success ......");
        ssh2.close();
        return true;
    }

    public static boolean undeployCollectd(JsonObject collectd, String logKey, String magicKey, ResultBean result) {
        String instId = collectd.getString(FixHeader.HEADER_INST_ID);
        String sshId = collectd.getString(FixHeader.HEADER_SSH_ID);
        String collectdPort = collectd.getString(FixHeader.HEADER_COLLECTD_PORT);

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;

        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (!DeployUtils.isInstanceDeployed(inst, logKey, result)) {
            String info = String.format("passby undeployed collectd, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
            return true;
        }

        {
            String info = String.format("start undeploy collectd, inst_id:%s, serv_ip:%s", instId, servIp);
            DeployLog.pubLog(logKey, info);
        }

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = String.format("collectd_%s", instId);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COLLECTD_ROOT, newName);

        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop collectd ......");
        String cmd = String.format("./bin/paas-collectd.sh stop");
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

        if (!DeployUtils.checkPortDown(ssh2, "paas-collectd", instId, servIp, collectdPort, logKey, result)) {
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


}
