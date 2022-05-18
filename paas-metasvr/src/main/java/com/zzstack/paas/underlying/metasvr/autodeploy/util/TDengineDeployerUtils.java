package com.zzstack.paas.underlying.metasvr.autodeploy.util;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TDengineDeployerUtils {

    private static final Logger logger = LoggerFactory.getLogger(TDengineDeployerUtils.class);

    public static String getArbitratorAddr(JsonObject arbitrator) {
        String sshId = arbitrator.getString(FixHeader.HEADER_SSH_ID);
        String port = arbitrator.getString(FixHeader.HEADER_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
        String ip = ssh.getServerIp();
        
        return String.format("%s:%s", ip, port);
    }
    
    public static String getFirstNode(JsonArray dnodeArr) {
        if (dnodeArr == null || dnodeArr.isEmpty())
            return null;
        
        JsonObject firstNode = dnodeArr.getJsonObject(0);
        String sshId = firstNode.getString(FixHeader.HEADER_SSH_ID);
        String port = firstNode.getString(FixHeader.HEADER_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, null, null);
        String ip = ssh.getServerIp();
        
        return String.format("%s:%s", ip, port);
    }
    
    public static boolean deployArbitrator(JsonObject tdArbitrator, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        String port = tdArbitrator.getString(FixHeader.HEADER_PORT);
        String instId = tdArbitrator.getString(FixHeader.HEADER_INST_ID);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "tarbitrator", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("tarbitrator.服务端口 is in use");
            return false;
        }
        
        //获取tdengine文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_TDENGINE_FILE_ID, FixDefs.DB_TDENGINE_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_TDENGINE_FILE_ID, version, logKey, result);
        String newName = "arbitrator_" + port;
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

        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");

        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, FixDefs.ARBITRATOR_START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, FixDefs.ARBITRATOR_STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.rm(ssh2, "taosd_*.sh", logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start tarbitrator ......");
        String cmd = String.format("./%s", FixDefs.ARBITRATOR_START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "tarbitrator", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "init tarbitrator success ......");
        ssh2.close();
        return true;
    }

    public static boolean deployDnode(JsonObject tdArbitrator, String arbitratorAddr, String firstNode,
                                      boolean bIsFirst, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        String port = tdArbitrator.getString(FixHeader.HEADER_PORT);
        String instId = tdArbitrator.getString(FixHeader.HEADER_INST_ID);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "taosd", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("taosd.服务端口 is in use");
            return false;
        }
        
        //获取tdengine文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_TDENGINE_FILE_ID, FixDefs.DB_TDENGINE_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_TDENGINE_FILE_ID, version, logKey, result);
        String newName = String.format("tdengine_%s", port);
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
        DeployLog.pubLog(logKey, "modify taos configure files ......");
        String newConf = "etc/taos.cfg";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_FIRSTEP, firstNode, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_FQDN, servIp, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ARBITRATOR_ADDR, arbitratorAddr, newConf, logKey, result)) {
            ssh2.close();
            return false;
        }

        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INST_ID, instId, FixDefs.TAOSD_START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INST_ID, instId, FixDefs.TAOSD_STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.rm(ssh2, "arbitrator_*.sh", logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start taosd ......");
        String cmd = String.format("./%s", FixDefs.TAOSD_START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "taosd", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!bIsFirst) { //不为第一个节点时
            String export = String.format("export LANG=zh_CN.UTF-8 %s" +
                            "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib"
                    , CONSTS.LINE_SEP);
            if (!DeployUtils.execSimpleCmd(ssh2, export, logKey, result)) {
                ssh2.close();
                return false;
            }
            
            String[] strNodeIp = firstNode.split(":");
            String loginFirstNode = String.format("./bin/taos -h %s -P %s -c ./etc", strNodeIp[0], strNodeIp[1]);
            if (!loginTaosShell(ssh2, loginFirstNode, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "login taosd failed ......");
                ssh2.close();
                return false;
            }
            String createNode = String.format("CREATE DNODE \"%s:%s\" ;", servIp, port);
            if (!createOrDropNode(ssh2, createNode, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "taosd create node is failed ......");
                ssh2.close();
                return false;
            }
            //解析show dnodes;
            String showNodes = String.format("show dnodes;");

            if (!checkNodeOnline(ssh2, showNodes, firstNode, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "taosd status is offline ......");
                ssh2.close();
                return false;
            }
        }

        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;
        DeployLog.pubLog(logKey, "init taosd success ......");
        ssh2.close();
        return true;
    }

    public static boolean undeployArbitrator(JsonObject tdArbitrator, String logKey, String magicKey, ResultBean result) {
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        String port = tdArbitrator.getString(FixHeader.HEADER_PORT);
        String instId = tdArbitrator.getString(FixHeader.HEADER_INST_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy arbitrator, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        String strName = "arbitrator_" + port;
        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey, "exec pwd error ......");
            ssh2.close();
            return false;
        }
        String arbitratorRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.DB_TDENGINE_ROOT, strName);
        if (!DeployUtils.cd(ssh2, arbitratorRoot, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop arbitrator ......");
        String cmd = String.format("./%s", FixDefs.ARBITRATOR_STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortDown(ssh2, "arbitrator", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, arbitratorRoot, logKey, result)) {
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

    public static boolean undeployDnode(JsonObject tdArbitrator, String logKey, ResultBean result, boolean dropFlag, String magicKey) {
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        String port = tdArbitrator.getString(FixHeader.HEADER_PORT);
        String instId = tdArbitrator.getString(FixHeader.HEADER_INST_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy taosd, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        
        String strName = String.format("tdengine_%s", port);
        String root = DeployUtils.pwd(ssh2, logKey, result);//获取路径

        if (result.getRetCode() == CONSTS.REVOKE_NOK) {
            DeployLog.pubErrorLog(logKey, "exec pwd error ......");
            ssh2.close();
            return false;
        }
        String taosdRoot = String.format("%s/%s/%s/%s", root, FixDefs.PAAS_ROOT, FixDefs.DB_TDENGINE_ROOT, strName);
        if (!DeployUtils.cd(ssh2, taosdRoot, logKey, result)) {
            ssh2.close();
            return false;
        }
        // 删除服务不需要考虑节点
        if (dropFlag) {
            // 删除单节点时操作，集群中删除该节点
            String export = String.format("export LANG=zh_CN.UTF-8 %s" +
                            "export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:./lib"
                    , CONSTS.LINE_SEP);
            if (!DeployUtils.execSimpleCmd(ssh2, export, logKey, result)) {
                ssh2.close();
                return false;
            }
            String loginFirstNode = String.format("./bin/taos -h %s -P %s -c ./etc", servIp, port);
            if (!loginTaosShell(ssh2, loginFirstNode, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "login taosd failed ......");
                ssh2.close();
                return false;
            }

            String dropNode = String.format("DROP DNODE \"%s:%s\" ;", servIp, port);
            if (!createOrDropNode(ssh2, dropNode, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "taosd drop node is failed ......");
                ssh2.close();
                return false;
            }

            String showdNodes = "show dnodes;";

            if (!checkNodeOffLine(ssh2, showdNodes, servIp + ":" + port, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "taosd status is offline ......");
                ssh2.close();
                return false;
            }

            if (!DeployUtils.execSimpleCmd(ssh2, "quit;", logKey, result)) {
                ssh2.close();
                return false;
            }
        }

        // stop
        DeployLog.pubLog(logKey, "stop taosd ......");
        String cmd = String.format("./%s", FixDefs.TAOSD_STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortDown(ssh2, "taosd", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.rm(ssh2, taosdRoot, logKey, result)) {
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

    public static boolean checkNodeOnline(SSHExecutor ssh2, String cmd, String nodeIp, String logKey, ResultBean result) {
        boolean res = true;
        try {
            res = ssh2.checkNodeOnline(cmd, nodeIp, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean checkNodeOffLine(SSHExecutor ssh2, String cmd, String nodeIp, String logKey, ResultBean result) {
        boolean res = true;
        try {
            res = ssh2.checkNodeOffLine(cmd, nodeIp, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean loginTaosShell(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            res = ssh2.loginTaosShell(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }

    public static boolean createOrDropNode(SSHExecutor ssh2, String cmd, String logKey, ResultBean result) {
        boolean res = true;
        try {
            res = ssh2.createOrDropNode(cmd, logKey);
        } catch (SSHException e) {
            res = false;
            logger.error(e.getMessage(), e);
            DeployLog.pubErrorLog(logKey, e.getMessage());

            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        }

        return res;
    }
}
