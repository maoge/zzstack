package com.zzstack.paas.underlying.metasvr.autodeploy.util;

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

public class TiDBDeployerUtils {

    public static boolean deployPdServer(JsonObject pdServer, String version, String pdList, String logKey, String magicKey, ResultBean result) {
        String sshId = pdServer.getString(FixHeader.HEADER_SSH_ID);
        String port = pdServer.getString(FixHeader.HEADER_CLIENT_PORT);
        String peerPort = pdServer.getString(FixHeader.HEADER_PEER_PORT);
        String instId = pdServer.getString(FixHeader.HEADER_INST_ID);

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
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pdserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("PD.Raft_Client端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pdserver", instId, servIp, peerPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("PD.Peer端口 is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_PD_SERVER_FILE_ID, FixDefs.DB_TIDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_PD_SERVER_FILE_ID, version, logKey, result);
        String newName = "pd-" + version + "_" + port;
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
        //   %INST_ID% 替换为$INST_ID
        //   %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
        //   %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
        //   %PD_LIST% 替换为上面拼接的PD集群地址,格式1
        DeployLog.pubLog(logKey, "modify start.sh and stop.sh env params ......");
        String startFile = "./start.sh";
        String clientUrls = servIp + ":" + port;
        String peerUrls = servIp + ":" + pdServer.getString(FixHeader.HEADER_PEER_PORT);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INST_ID, instId, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLIENT_URLS, clientUrls, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PEER_URLS, peerUrls, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADVERTISE_PEER_URLS, peerUrls, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PD_LIST, pdList.replace("/", "\\/"), startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        String stopFile = "./stop.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INST_ID, instId, stopFile, logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start pdserver ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "pdserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "init pdserver success ......");
        ssh2.close();
        return true;
    }

    public static boolean deployTikvServer(JsonObject tikvServer, String version, String pdTikvList, String logKey, String magicKey, ResultBean result) {
        String sshId = tikvServer.getString(FixHeader.HEADER_SSH_ID);
        String port = tikvServer.getString(FixHeader.HEADER_PORT);
        String instId = tikvServer.getString(FixHeader.HEADER_INST_ID);

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
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "tikv-server", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("TiKV.服务端口 is in use");
            return false;
        }
        
        //获取tdengine文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_TIKV_SERVER_FILE_ID, FixDefs.DB_TIDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_TIKV_SERVER_FILE_ID, version, logKey, result);
        String newName = "tikv-" + version + "_" + port;
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

        //   %INST_ID% 替换为$INST_ID
        //   %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
        //   %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
        //   %PD_LIST% 替换为上面拼接的PD集群地址,格式1
        DeployLog.pubLog(logKey, "modify start.sh and stop.sh env params ......");
        String startFile = "./start.sh";
        String tikvAddr = servIp + ":" + port;
        String statAddr = servIp + ":" + tikvServer.getString(FixHeader.HEADER_STAT_PORT);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PD_LIST, pdTikvList, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_TIKV_ADDR, tikvAddr, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_STAT_ADDR, statAddr, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        String stopFile = "./stop.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, stopFile, logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start tikvserver ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "tikvserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "init tikvserver success ......");
        ssh2.close();
        return true;
    }

    public static boolean deployTidbServer(JsonObject tidbServer, String version, String pdList, String logKey, String magicKey, ResultBean result) {
        String sshId = tidbServer.getString(FixHeader.HEADER_SSH_ID);
        String port = tidbServer.getString(FixHeader.HEADER_PORT);
        String statPort = tidbServer.getString(FixHeader.HEADER_STAT_PORT);
        String instId = tidbServer.getString(FixHeader.HEADER_INST_ID);

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
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "tidb-server", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("TiDB.服务端口 is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "tidb-server", instId, servIp, statPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("TiDB.统计端口 is in use");
            return false;
        }
        
        //获取tidb-server文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_TIDB_SERVER_FILE_ID, FixDefs.DB_TIDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_TIDB_SERVER_FILE_ID, version, logKey, result);
        String newName = "tidb-" + version + "_" + port;
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

        //   %INST_ID% 替换为$INST_ID
        //   %CLIENT_URLS% 替换为$SSH_IP:$CLIENT_PORT
        //   %PEER_URLS% 替换为$SSH_IP:$PEER_PORT
        //   %PD_LIST% 替换为上面拼接的PD集群地址,格式1
        DeployLog.pubLog(logKey, "modify start.sh and stop.sh env params ......");
        String startFile = "./start.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PD_LIST, pdList, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HOST, servIp, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_STAT_HOST, servIp, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_STAT_PORT, tidbServer.getString(FixHeader.HEADER_STAT_PORT), startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        String stopFile = "./stop.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, stopFile, logKey, result)) {
            ssh2.close();
            return false;
        }

        // start
        DeployLog.pubLog(logKey, "start tidbserver ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "tidbserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update t_meta_instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "init tidbserver success ......");
        ssh2.close();
        return true;
    }

    public static boolean deployDashboardProxy(JsonObject dashboardProxy, String version, String pdAddress, String logKey, String magicKey, ResultBean result) {
        String instId = dashboardProxy.getString(FixHeader.HEADER_INST_ID);
        String sshId = dashboardProxy.getString(FixHeader.HEADER_SSH_ID);
        String dashboardPort = dashboardProxy.getString(FixHeader.HEADER_DASHBOARD_PORT);
        
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
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "dashboard-proxy", instId, servIp, dashboardPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("TiDB.dashboard端口 is in use");
            return false;
        }
        
        // 1. 获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_TIDB_DASHBOARD_PROXY_FILE_ID, FixDefs.DB_TIDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_TIDB_DASHBOARD_PROXY_FILE_ID, version, logKey, result);
        String newName = "dashboard-proxy_" + dashboardPort;
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
        
        String dashboardAddr = String.format("%s:%s", servIp, dashboardPort);
        
        // etc/dashboard_proxy.cfg
        // %DASHBOARD_ADDR% 替换为$SSH_IP:$DASHBOARD_PORT
        // %PD_ADDRESS% 替换为pdAddress
        DeployLog.pubLog(logKey, "modify dashboard_proxy.cfg env params ......");
        String file = "./etc/dashboard_proxy.cfg";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DASHBOARD_ADDR, dashboardAddr, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PD_ADDRESS, pdAddress, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        String newConfFile = String.format("./etc/dashboard_proxy_%s.cfg", dashboardPort);
        if (!DeployUtils.mv(ssh2, newConfFile, file, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start.sh
        // %DASHBOARD_PORT% 替换为$DASHBOARD_PORT
        String startFile = "start.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DASHBOARD_PORT, dashboardPort, startFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop.sh
        // %DASHBOARD_PORT% 替换为$DASHBOARD_PORT
        String stopFile = "stop.sh";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_DASHBOARD_PORT, dashboardPort, stopFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 2. start
        DeployLog.pubLog(logKey, "start dashboard proxy ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "dashboard-proxy", instId, servIp, dashboardPort, logKey, result)) {
            ssh2.close();
            return false;
        }

        // 3. update instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "init dashboard-proxy success ......");
        ssh2.close();
        return true;
    }

    public static boolean undeployPdServer(JsonObject pdServer, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = pdServer.getString(FixHeader.HEADER_SSH_ID);
        String port = pdServer.getString(FixHeader.HEADER_CLIENT_PORT);
        String instId = pdServer.getString(FixHeader.HEADER_INST_ID);

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy pd-Server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = "pd-" + version + "_" + port;
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_TIDB_ROOT, newName);

        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop pd-server ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortDown(ssh2, "pdserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)
                || !DeployUtils.rm(ssh2, newName, logKey, result)) {
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

    public static boolean undeployTikvServer(JsonObject tikvServer, JsonObject pdServer, String version,
            boolean isService, String logKey, String magicKey, ResultBean result) {
        
        String sshId = tikvServer.getString(FixHeader.HEADER_SSH_ID);
        String ip = tikvServer.getString(FixHeader.HEADER_IP);
        String port = tikvServer.getString(FixHeader.HEADER_PORT);
        String instId = tikvServer.getString(FixHeader.HEADER_INST_ID);

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy tikv-Server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = "tikv-" + version + "_" + port;
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_TIDB_ROOT, newName);

        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        //String.format("./bin/pd-ctl -u http://%s:%s -d store delete %s \n", servIp, port, instId);
        
        // stop
        DeployLog.pubLog(logKey, "stop tikv-server ......");
        String cmd = "./stop.sh";
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

        if (!DeployUtils.checkPortDown(ssh2, "tikvserver", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!isService) {
            //pd-server delete tikv-instance
            String pdServrSshId = pdServer.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh pdServrSsh = DeployUtils.getSshById(pdServrSshId, logKey, result);
            if (pdServrSsh == null) return false;
            String pdServIp = pdServrSsh.getServerIp();
            String pdSshName = pdServrSsh.getSshName();
            String pdSshPwd = pdServrSsh.getSshPwd();
            int pdSshPort = pdServrSsh.getSshPort();
            SSHExecutor pdSsh2 = new SSHExecutor(pdSshName, pdSshPwd, pdServIp, pdSshPort);

            if (!DeployUtils.initSsh2(pdSsh2, logKey, result)) return false;

            String pdIp = pdServer.getString(FixHeader.HEADER_IP);
            String pdClientPort = pdServer.getString(FixHeader.HEADER_CLIENT_PORT);
            String pdServerName = "pd-" + version + "_" + pdClientPort;

            String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_TIDB_ROOT, pdServerName);
            if (!DeployUtils.cd(pdSsh2, rootDir, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "rootDir is " + rootDir);
                pdSsh2.close();
                return false;
            }

            Integer storeId = DeployUtils.getStoreId(pdSsh2, pdIp, pdClientPort, ip, port, logKey, result);

            if (storeId == null) {
                DeployLog.pubErrorLog(logKey, "storeId is null......");
                pdSsh2.close();
                return false;
            }

            if (!DeployUtils.pdctlDeleteTikvStore(pdSsh2, pdIp, pdClientPort, storeId, logKey, result)) {
                DeployLog.pubErrorLog(logKey, "pdctl Delete Tikv Store is failed......");
                pdSsh2.close();
                return false;
            }
            pdSsh2.close();
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }

    public static boolean undeployTidbServer(JsonObject tidbServer, String version, String logKey, String magicKey, ResultBean result) {
        String sshId = tidbServer.getString(FixHeader.HEADER_SSH_ID);
        String port = tidbServer.getString(FixHeader.HEADER_PORT);
        String instId = tidbServer.getString(FixHeader.HEADER_INST_ID);

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy tidb-Server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = "tidb-" + version + "_" + port;
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_TIDB_ROOT, newName);

        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop tidb-server ......");
        String cmd = "./stop.sh";
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

        if (!DeployUtils.checkPortDown(ssh2, "tidbserver", instId, servIp, port, logKey, result)) {
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
    
    public static boolean undeployDashboardProxy(JsonObject dashboardProxy, String version, String logKey,
            String magicKey, ResultBean result) {
        
        String instId = dashboardProxy.getString(FixHeader.HEADER_INST_ID);
        String sshId = dashboardProxy.getString(FixHeader.HEADER_SSH_ID);
        String dashboardPort = dashboardProxy.getString(FixHeader.HEADER_DASHBOARD_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy dashboard-proxy, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, dashboardPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String newName = "dashboard-proxy_" + dashboardPort;
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_TIDB_ROOT, newName);

        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) {
            ssh2.close();
            return false;
        }
        // stop
        DeployLog.pubLog(logKey, "stop dashboard-proxy ......");
        String cmd = "./stop.sh";
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

        if (!DeployUtils.checkPortDown(ssh2, "dashboard-proxy", instId, servIp, dashboardPort, logKey, result)) {
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
    
    public static String getPDLongAddress(JsonArray pdArr) {
        StringBuilder pdList = new StringBuilder();
        for (int i = 0; i < pdArr.size(); i++) {
            JsonObject jsonPdServer = pdArr.getJsonObject(i);
            String sshId = jsonPdServer.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId);
            if (ssh == null) continue;
            String servIp = ssh.getServerIp();
            String peerPort = jsonPdServer.getString(FixHeader.HEADER_PEER_PORT);
            String instId = jsonPdServer.getString(FixHeader.HEADER_INST_ID);
            
            if (i > 0)
                pdList.append(",");
            
            String line = String.format("%s=http://%s:%s", instId, servIp, peerPort);
            pdList.append(line);
        }
        
        return pdList.toString();
    }
    
    public static String getPDShortAddress(JsonArray pdArr) {
        StringBuilder pdList = new StringBuilder();
        for (int i = 0; i < pdArr.size(); i++) {
            JsonObject jsonPdServer = pdArr.getJsonObject(i);
            String sshId = jsonPdServer.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId);
            if (ssh == null) continue;
            String servIp = ssh.getServerIp();
            String clientPort = jsonPdServer.getString(FixHeader.HEADER_CLIENT_PORT);
            
            if (i > 0)
                pdList.append(",");
            
            String line = String.format("%s:%s", servIp, clientPort);
            pdList.append(line);
        }
        
        return pdList.toString();
    }
    
    public static String getFirstPDAddress(JsonArray pdArr) {
        JsonObject pd = pdArr.getJsonObject(0);
        String sshId = pd.getString(FixHeader.HEADER_SSH_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId);
        String pdServIp = ssh.getServerIp();
        String pdPort = pd.getString(FixHeader.HEADER_CLIENT_PORT);
        String pdAddress = String.format("%s:%s", pdServIp, pdPort);
        return pdAddress;
    }
    
}
