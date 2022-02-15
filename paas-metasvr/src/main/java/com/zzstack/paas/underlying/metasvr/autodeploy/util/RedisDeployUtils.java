package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import java.util.ArrayList;

import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasRedisCluster;
import com.zzstack.paas.underlying.metasvr.bean.PaasRedisNode;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.StringUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RedisDeployUtils {
    
    public static boolean getClusterNodes(JsonArray redisNodeArr, StringBuilder nodes4cluster, StringBuilder nodes4proxy,
            String logKey, ResultBean retval) {

        int size = redisNodeArr.size();

        for (int i = 0; i < size; ++i) {
            JsonObject redisJson = redisNodeArr.getJsonObject(i);

            // String instId = redisJson.getString(FixHeader.HEADER_INST_ID);
            String sshId = redisJson.getString(FixHeader.HEADER_SSH_ID);
            String port = redisJson.getString(FixHeader.HEADER_PORT);

            PaasSsh ssh = MetaSvrGlobalRes.get().getCmptMeta().getSshById(sshId);
            String servIp = ssh.getServerIp();

            String node = "";
            node += servIp;
            node += ":";
            node += port;

            if (nodes4cluster.length() > 0)
                nodes4cluster.append(" ");
            nodes4cluster.append(node);

            if (nodes4proxy.length() > 0)
                nodes4proxy.append(",");
            nodes4proxy.append(node);
        }

        return true;
    }

    public static boolean deployRedisNode(JsonObject redisJson, boolean init, boolean expand, boolean typeCluster,
            String node4cluster, String version, String logKey, String magicKey, ResultBean result) {
        
        // {
        //     "INST_ID": "302a5f2f-b8dd-ffb5-878e-4a86ea0c99e0", 
        //     "MAX_CONN": "1000", 
        //     "MAX_MEMORY": "2", 
        //     "PORT": "8101", 
        //     "POS": { }, 
        //     "SSH_ID": "24b06d9d-624e-4e69-8e9d-ac957754b8ee"
        // }
        
        String  maxConn    = redisJson.getString(FixHeader.HEADER_MAX_CONN);
        String  sMaxMem    = redisJson.getString(FixHeader.HEADER_MAX_MEMORY);  // unit: GB
        String  port       = redisJson.getString(FixHeader.HEADER_PORT);
        
        String  instId     = redisJson.getString(FixHeader.HEADER_INST_ID);
        String  sshId      = redisJson.getString(FixHeader.HEADER_SSH_ID);
        long    lMaxMem    = Long.valueOf(sMaxMem) * CONSTS.UNIT_G;
        
        String  maxMem     = String.valueOf(lMaxMem);
        
        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        
        {
            String info = String.format("start deploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "redis-server", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("redis-server.服务端口 is in use");
            return false;
        }
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.CACHE_REDIS_SERVER_FILE_ID, logKey, result);
        String srcFileName   = deployFile.getFileName();
        
        // service.VERSION > deploy_file.VERSION
        if (version == null || version.isEmpty())
            version  = deployFile.getVersion();
        
        // CACHE_REDIS_SERVER_FILE_ID -> 'redis-6.0.tar.gz'
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.CACHE_REDIS_SERVER_FILE_ID,
                FixDefs.CACHE_REDIS_ROOT, version, logKey, result)) {
            return false;
        }
        
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);
        
        String newName = "redis_" + port;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify redis configure files ......");
        
        if (!DeployUtils.cd(ssh2, newName + "/etc", logKey, result)) { ssh2.close(); return false; }
        
        String newConf = "redis_" + port + ".conf";
        if (!DeployUtils.mv(ssh2, newConf, FixDefs.REDIS_CONF, logKey, result)) { ssh2.close(); return false; }
        
        String pidFile = "redis_" + port + ".pid";
        String aofFile = "appendonly_" + port + ".aof";
        String confFile = "nodes_" + port + ".conf";
        // bind %SERV_IP%
        // port %SERV_PORT%
        // pidfile ./data/%PID_FILE%
        // requirepass %PASSWORD%
        // maxclients %MAX_CONN%
        // maxmemory %MAX_MEMORY%
        // appendfilename %APPENDONLY_FILENAME%
        // cluster-config-file %REDIS_CONF_FILENAME%
        // cluster-enabled %CLUSTER_ENABLED%
        String clusterEnabledFlag = typeCluster ? "yes" : "no";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_IP, servIp, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_PORT, port, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PID_FILE, pidFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_CONN, maxConn, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_MEMORY, maxMem, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APPENDONLY_FILENAME, aofFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.REDIS_CLUSTER_CONF_FILENAME, confFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_ENABLED, clusterEnabledFlag, newConf, logKey, result)) { ssh2.close(); return false; }
        
        // if needs auth
        // new node added to redis cluster need set auth setting before join.
        if (expand) {
            DeployLog.pubLog(logKey, "set requirepass and masterauth ......");
            String requirepass = "requirepass " + FixDefs.ZZSOFT_REDIS_PASSWD;
            String masterauth  = "masterauth " + FixDefs.ZZSOFT_REDIS_PASSWD;
            if (!DeployUtils.addLine(ssh2, requirepass, newConf, logKey, result)) { ssh2.close(); return false; }
            if (!DeployUtils.addLine(ssh2, masterauth, newConf, logKey, result)) { ssh2.close(); return false; }
        }
        
        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        
        String startShell = String.format("./bin/redis-server ./etc/%s", newConf);
        if (!DeployUtils.createShell(ssh2, FixDefs.START_SHELL, startShell, logKey, result)) { ssh2.close(); return false; }
        
        
        String stopShell = String.format("./bin/redis-cli -h %s -p %s -a %s -c shutdown", servIp, port, FixDefs.ZZSOFT_REDIS_PASSWD);
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_SHELL, stopShell, logKey, result)) { ssh2.close(); return false; }
        
        
        String stopNoAuthShell = String.format("./bin/redis-cli -h %s -p %s -c shutdown", servIp, port);
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_NOAUTH_SHELL, stopNoAuthShell, logKey, result)) { ssh2.close(); return false; }
        
        // start
        DeployLog.pubLog(logKey, "start redis-server ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortUp(ssh2, "redis-server", instId, servIp, port, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        // init redis cluster when the last node deploy ok
        if (init) {
            String initCmd = String.format("./bin/redis-cli --cluster create %s --cluster-replicas %d", node4cluster, FixDefs.REDIS_CLUSTER_REPLICAS);
            DeployLog.pubLog(logKey, "init redis cluster ......");
            
            if (!DeployUtils.initRedisCluster(ssh2, initCmd, logKey, result)) { ssh2.close(); return false; }
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deployProxyNode(JsonObject proxyJson, String nodes4proxy, String logKey, String magicKey, ResultBean result) {
        // "REDIS_PROXY": [
        //     {
        //         "INST_ID": "955518f4-775b-bdf2-57ea-564cb197bb01", 
        //         "MAX_CONN": "10000", 
        //         "NODE_CONN_POOL_SIZE": "10", 
        //         "PORT": "8000", 
        //         "POS": { }, 
        //         "SSH_ID": "24b06d9d-624e-4e69-8e9d-ac957754b8ee"
        //     }
        // ]
        
        String instId         = proxyJson.getString(FixHeader.HEADER_INST_ID);
        String maxConn        = proxyJson.getString(FixHeader.HEADER_MAX_CONN);
        String connPoolSize   = proxyJson.getString(FixHeader.HEADER_NODE_CONN_POOL_SIZE);
        String proxyThreads   = proxyJson.getString(FixHeader.HEADER_PROXY_THREADS);
        String port           = proxyJson.getString(FixHeader.HEADER_PORT);
        String sshId          = proxyJson.getString(FixHeader.HEADER_SSH_ID);
        
        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        
        String  servIp        = ssh.getServerIp();
        String  sshName       = ssh.getSshName();
        String  sshPwd        = ssh.getSshPwd();
        int     sshPort       = ssh.getSshPort();
        
        {
            String info = String.format("start deploy redis-proxy, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "redis-proxy", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("redis-proxy.服务端口 is in use");
            return false;
        }
        
        // CACHE_REDIS_PROXY_FILE_ID -> 'redis-cluster-proxy-1.0.tar.gz'
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.CACHE_REDIS_PROXY_FILE_ID, FixDefs.CACHE_REDIS_ROOT,
                "", logKey, result)) {
            ssh2.close();
            return false;
        }

        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.CACHE_REDIS_PROXY_FILE_ID, logKey, result);
        String srcFileName   = deployFile.getFileName();
        String version       = deployFile.getVersion();
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);
        
        String new_name = String.format("%s%s", FixDefs.CACHE_REDIS_PROXY_PREFIX, port);
        if (!DeployUtils.rm(ssh2, new_name, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, new_name, oldName, logKey, result)) { ssh2.close(); return false; }
        
        DeployLog.pubLog(logKey, "modify redis_proxy configure file ......");
        String new_conf = String.format("proxy_%s.conf", port);
        if (!DeployUtils.cd(ssh2, new_name + "/etc", logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.mv(ssh2, new_conf, FixDefs.PROXY_CONF, logKey, result)) { ssh2.close(); return false; }
        
        // cluster %CLUSTER_NODES%
        // port %SERV_PORT%
        // bind %SERV_IP%
        // connections-pool-size %CONN_POOL_SIZE%
        // pidfile ./data/%PID_FILE%
        // logfile ./log/%LOG_FILE%
        // maxclients %MAX_CONN%
        // threads %PROXY_THREADS%
        // auth %PASSWORD%
        String pid_file = String.format("proxy_%s.pid", port);
        String log_file = String.format("proxy_%s.log", port);
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NODES, nodes4proxy, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_PORT, port, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_IP, servIp, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CONN_POOL_SIZE, connPoolSize, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PID_FILE, pid_file, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LOG_FILE, log_file, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_CONN, maxConn, new_conf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PROXY_THREADS, proxyThreads, new_conf, logKey, result)) { ssh2.close(); return false; }
        // if (!DeployUtils.sed(ssh2, FixDefs.CONF_PASSWORD, ZZSOFT_REDIS_PASSWD, new_conf, log_key)) return false;
        
        // create start and stop shell
        DeployLog.pubLog(logKey, "create proxy start and stop shell ......");
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        
        //String startShell = String.format("./bin/redis-cluster-proxy -c ./etc/%s", new_conf);
        //if (!DeployUtils.createShell(ssh2, FixDefs.START_SHELL, startShell, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, FixDefs.START_SHELL, logKey, result)) { ssh2.close(); return false; }
        
        //String stopShell = String.format("PID=\\`cat ./data/%s | sed -n '1p'\\`\\nkill -9 \\${PID}", pid_file);
        //if (!DeployUtils.createShell(ssh2, FixDefs.STOP_SHELL, stopShell, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PORT, port, FixDefs.STOP_SHELL, logKey, result)) { ssh2.close(); return false; }
        
        // start
        DeployLog.pubLog(logKey, "start redis cluster proxy ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortUp(ssh2, "redis-proxy", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeployProxyNode(JsonObject proxyJson, boolean force, String logKey, String magicKey, ResultBean result) {
        String instId         = proxyJson.getString(FixHeader.HEADER_INST_ID);
        String port           = proxyJson.getString(FixHeader.HEADER_PORT);
        String sshId          = proxyJson.getString(FixHeader.HEADER_SSH_ID);
        
        PaasSsh ssh           = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;

        String servIp         = ssh.getServerIp();
        String sshName        = ssh.getSshName();
        String sshPwd         = ssh.getSshPwd();
        int    sshPort        = ssh.getSshPort();
        
        String infoStart = String.format("start undeploy redis-proxy, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
        DeployLog.pubLog(logKey, infoStart);

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String proxy_dir = String.format("%s%s", FixDefs.CACHE_REDIS_PROXY_PREFIX, port);
        String root_dir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, proxy_dir);
        if (!DeployUtils.cd(ssh2, root_dir, logKey, result)) { ssh2.close(); return false; }
        
        // stop
        DeployLog.pubLog(logKey, "stop redis-proxy ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!force && !DeployUtils.checkPortDown(ssh2, "redis-server", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, proxy_dir, logKey, result)) { ssh2.close(); return false; }
        
        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        String infoSuccess = String.format("undeploy redis-proxy success, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
        DeployLog.pubSuccessLog(logKey, infoSuccess);
        
        ssh2.close();
        return true;
    }
    
    public static boolean undeployRedisNode(JsonObject redisJson, boolean shrink, String logKey, String magicKey, ResultBean result) {
        String  port       = redisJson.getString(FixHeader.HEADER_PORT);
        String  instId     = redisJson.getString(FixHeader.HEADER_INST_ID);
        String  sshId      = redisJson.getString(FixHeader.HEADER_SSH_ID);

        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;

        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        
        {
            String info = String.format("start undeploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String redisDir = String.format("redis_%s", port);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, redisDir);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }
        
        if (shrink) {
            ResultBean clusterNode = new ResultBean();
            if (!getRedisClusterNode(servIp, port, sshName, sshPwd, sshPort, logKey, clusterNode)) { ssh2.close(); return false; }
            PaasRedisCluster cluster = new PaasRedisCluster();
            cluster.parse(clusterNode.getRetInfo());
            
            // if self node is master role, must undeploy relate slave node.
            PaasRedisNode self = cluster.getSelfInfo(servIp, port/*, selfId, role, slotRange*/);
            if (self == null) {
                DeployLog.pubLog(logKey, "cluster getSelfInfo no data return ......");
                ssh2.close();
                return false;
            }
            
            String selfId = self.getNodeId();
            String slotRange = self.getSlotRange();
            int role = self.getRedisRole();
            
            {
                String info = String.format("shrink, id:%s, role:%d", selfId, role);
                DeployLog.pubLog(logKey, info);
            }
            
            if (role == FixDefs.REDIS_ROLE_MASTER) {
                // remove MASTER ROLE Redis Node
                ArrayList<PaasRedisNode> slaves = new ArrayList<PaasRedisNode>();
                cluster.getSlaves(selfId, slaves);

                // remove slaves from cluster
                for (PaasRedisNode slaveNode : slaves) {
                    String info = String.format("remove sub-slave node:{%s:%s %s} from cluster",
                                slaveNode.getIp(), slaveNode.getPort(), slaveNode.getNodeId());
                    DeployLog.pubLog(logKey, info);

                    if (!removeFromCluster(ssh2, slaveNode.getIp(), slaveNode.getPort(), slaveNode.getNodeId(), logKey, result)) {
                        DeployLog.pubFailLog(logKey, "remove slave node NOK ......");
                        ssh2.close();
                        return false;
                    }
                    DeployLog.pubLog(logKey, "remove slave node OK ......");
                }

                // migrate master slot to other masters
                if (!slotRange.isEmpty()) {
                    ArrayList<PaasRedisNode> masters = new ArrayList<PaasRedisNode>();
                    cluster.getSlotedMasters(masters);
                    
                    int avgMoveSlotCnt = self.getNodeSlotCount() / (masters.size() - 1);
                    
                    if (masters.size() <= FixDefs.REDIS_CLUSTER_MIN_MASTER_NODES) {
                        String info = String.format("slot <<%s>> not null, need to migrate slot by tools first", slotRange);
                        DeployLog.pubLog(logKey, info);
                        
                        ssh2.close();
                        return true;
                    }
                    
                    String info = String.format("slot <<%s>> not null, need to migrate slot by tools first", slotRange);
                    DeployLog.pubLog(logKey, info);

                    for (int idx = 0; idx < masters.size(); ++idx) {
                        PaasRedisNode node = masters.get(idx);
                        String desNodeId = node.getNodeId();
                        if (desNodeId.equals(selfId)) continue;

                        // redis cluster slot resharding
                        String cmd = String.format("./bin/redis-cli --cluster reshard %s:%s --cluster-from %s --cluster-to %s --cluster-slots %d -c --no-auth-warning --cluster-yes",
                                    node.getIp(), node.getPort(), selfId, desNodeId, avgMoveSlotCnt);
                        if (!DeployUtils.migrateRedisClusterSlot(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
                    }
                }

                // remove master from cluster
                String removeInfo = String.format("remove master node:{%s:%s %s} from cluster", servIp, port, selfId);
                DeployLog.pubLog(logKey, removeInfo);
                
                if (!removeFromCluster(ssh2, servIp, port, selfId, logKey, result)) {
                    DeployLog.pubLog(logKey, "remove master node NOK ......");
                    ssh2.close();
                    return false;
                }
                DeployLog.pubLog(logKey, "remove master node from cluster OK ......");
            } else {
                // remove SLAVE ROLE Redis Node
                if (!removeFromCluster(ssh2, servIp, port, selfId, logKey, result)) {
                    DeployLog.pubFailLog(logKey, "remove slave node from cluster NOK ......");
                    ssh2.close();
                    return false;
                }
                DeployLog.pubLog(logKey, "remove slave node from cluster OK ......");
            }
            
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop redis-server ......");
        String shell = String.format("./%s", FixDefs.STOP_NOAUTH_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, shell, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.checkPortDown(ssh2, "redis-server", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, redisDir, logKey, result)) { ssh2.close(); return false; }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }
        
        ssh2.close();
        return true;
    }
    
    public static boolean deploySingleRedisNode(JsonObject redisJson, JsonArray nodeArray, boolean isMasterNode,
            String version, String logKey, String magicKey, ResultBean result) {
        
        // {
        //     "INST_ID": "302a5f2f-b8dd-ffb5-878e-4a86ea0c99e0", 
        //     "MAX_CONN": "1000", 
        //     "MAX_MEMORY": "2", 
        //     "PORT": "8101", 
        //     "POS": { }, 
        //     "SSH_ID": "24b06d9d-624e-4e69-8e9d-ac957754b8ee",
        //     "NODE_TYPE: "1"
        // }
        
        String  maxConn    = redisJson.getString(FixHeader.HEADER_MAX_CONN);
        String  sMaxMem    = redisJson.getString(FixHeader.HEADER_MAX_MEMORY);  // unit: GB
        String  port       = redisJson.getString(FixHeader.HEADER_PORT);
        
        String  instId     = redisJson.getString(FixHeader.HEADER_INST_ID);
        String  ssh_id     = redisJson.getString(FixHeader.HEADER_SSH_ID);
        long    lMaxMem    = Long.valueOf(sMaxMem) * CONSTS.UNIT_G;
        
        String  maxMem     = String.valueOf(lMaxMem);
        
        PaasSsh ssh        = DeployUtils.getSshById(ssh_id, logKey, result);
        if (ssh == null) return false;
        
        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();
        
        {
            String info = String.format("start deploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "redis-server", instId, servIp, port, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("redis-server.服务端口 is in use");
            return false;
        }
        
        // CACHE_REDIS_SERVER_FILE_ID -> 'redis-6.0.tar.gz'
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.CACHE_REDIS_SERVER_FILE_ID,
                FixDefs.CACHE_REDIS_ROOT, version, logKey, result)) {
            return false;
        }
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.CACHE_REDIS_SERVER_FILE_ID, logKey, result);
        String srcFileName   = deployFile.getFileName();
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }
        int idx = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, idx);
        
        String newName = "redis_" + port;
        if (!DeployUtils.rm(ssh2, newName, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.mv(ssh2, newName, oldName, logKey, result)) { ssh2.close(); return false; }
        DeployLog.pubLog(logKey, "modify redis configure files ......");
        
        if (!DeployUtils.cd(ssh2, newName + "/etc", logKey, result)) { ssh2.close(); return false; }
        
        String newConf = "redis_" + port + ".conf";
        if (!DeployUtils.mv(ssh2, newConf, FixDefs.REDIS_CONF, logKey, result)) { ssh2.close(); return false; }
        
        String pidFile = "redis_" + port + ".pid";
        String aofFile = "appendonly_" + port + ".aof";
        String confFile = "nodes_" + port + ".conf";
        // bind %SERV_IP%
        // port %SERV_PORT%
        // pidfile ./data/%PID_FILE%
        // requirepass %PASSWORD%
        // maxclients %MAX_CONN%
        // maxmemory %MAX_MEMORY%
        // appendfilename %APPENDONLY_FILENAME%
        // cluster-config-file %REDIS_CONF_FILENAME%
        // cluster-enabled %CLUSTER_ENABLED%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_IP, servIp, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SERV_PORT, port, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PID_FILE, pidFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_CONN, maxConn, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_MEMORY, maxMem, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_APPENDONLY_FILENAME, aofFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.REDIS_CLUSTER_CONF_FILENAME, confFile, newConf, logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_ENABLED, "no", newConf,logKey, result)) { ssh2.close(); return false; }
        
        // create start and stop shell
        DeployLog.pubLog(logKey, "create start and stop shell ......");
        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        
        String startShell = String.format("./bin/redis-server ./etc/%s", newConf);
        if (!DeployUtils.createShell(ssh2, FixDefs.START_SHELL, startShell, logKey, result)) { ssh2.close(); return false; }
        
        
        String stopShell = String.format("./bin/redis-cli -h %s -p %s -a %s -c shutdown", servIp, port, FixDefs.ZZSOFT_REDIS_PASSWD);
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_SHELL, stopShell, logKey, result)) { ssh2.close(); return false; }
        
        
        String stopNoAuthShell = String.format("./bin/redis-cli -h %s -p %s -c shutdown", servIp, port);
        if (!DeployUtils.createShell(ssh2, FixDefs.STOP_NOAUTH_SHELL, stopNoAuthShell, logKey, result)) { ssh2.close(); return false; }
        
        // start
        DeployLog.pubLog(logKey, "start redis-server ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) { ssh2.close(); return false; }
        
        if (!DeployUtils.checkPortUp(ssh2, "redis-server", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        // 从节点执行slaveof挂载到主下面
        if (!isMasterNode) {
            // SLAVEOF host port
            JsonObject masterNode = getMasterRedisNode(nodeArray);
            if (masterNode == null) {
                String errInfo = String.format("%s, master node not exists: %s", FixDefs.ERR_METADATA_NOT_FOUND, nodeArray.toString());
                DeployLog.pubFailLog(logKey, errInfo);
                
                ssh2.close();
                return false;
            }
            String masterPort = masterNode.getString(FixHeader.HEADER_PORT);
            String masterSshId = redisJson.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh masterSsh = DeployUtils.getSshById(masterSshId, logKey, result);
            if (masterSsh == null) {
                String errInfo = String.format("%s, ssh_id: %s", FixDefs.ERR_METADATA_NOT_FOUND, masterSshId);
                DeployLog.pubFailLog(logKey, errInfo);
                        
                ssh2.close();
                return false;
            }
            
            String masterHost = masterSsh.getServerIp();
            String cmdSlaveOf = String.format("./bin/redis-cli -h %s -p %s slaveof %s %s", servIp, port, masterHost, masterPort);
            // SSHExecutor ssh2, String cmd, String logKey, ResultBean result
            if (!DeployUtils.redisSlaveof(ssh2, cmdSlaveOf, logKey, result)) { ssh2.close(); return false; }
        }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }


    public static boolean undeploySingleRedisNode(JsonObject redisJson, boolean shrink, String logKey, String magicKey, ResultBean result) {
        String  port       = redisJson.getString(FixHeader.HEADER_PORT);
        String  instId     = redisJson.getString(FixHeader.HEADER_INST_ID);
        String  sshId      = redisJson.getString(FixHeader.HEADER_SSH_ID);

        PaasSsh ssh        = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;

        String  servIp     = ssh.getServerIp();
        String  sshName    = ssh.getSshName();
        String  sshPwd     = ssh.getSshPwd();
        int     sshPort    = ssh.getSshPort();

        {
            String info = String.format("start undeploy redis-server, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, port);
            DeployLog.pubLog(logKey, info);
        }

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;

        String redisDir = String.format("redis_%s", port);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, redisDir);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }

        // stop
        DeployLog.pubLog(logKey, "stop redis-server ......");
        String shell = String.format("./%s", FixDefs.STOP_NOAUTH_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, shell, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.checkPortDown(ssh2, "redis-server", instId, servIp, port, logKey, result)) { ssh2.close(); return false; }

        if (!DeployUtils.cd(ssh2, "..", logKey, result)) { ssh2.close(); return false; }
        if (!DeployUtils.rm(ssh2, redisDir, logKey, result)) { ssh2.close(); return false; }

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }


    private static boolean removeFromCluster(SSHExecutor ssh2, String ip, String redisPort, String selfId,
            String logKey, ResultBean result) {
        String cmd = String.format("./bin/redis-cli --cluster del-node %s:%s %s -c --no-auth-warning", ip, redisPort, selfId);  // // -a passwd
        return DeployUtils.removeFromRedisCluster(ssh2, cmd, logKey, result);
    }
    
    public static boolean getRedisClusterNode(String servIp, String port, String sshName, String sshPwd, int sshPort,
            String logKey, ResultBean clusterNode) {

        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, clusterNode))
            return false;

        String redisDir = String.format("redis_%s", port);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, redisDir);

        if (!DeployUtils.cd(ssh2, rootDir, logKey, clusterNode))
            return false;

        String cmd = String.format("./bin/redis-cli -h %s -p %s -c --no-auth-warning cluster nodes", servIp, port);

        boolean res = DeployUtils.getRedisClusterNode(ssh2, cmd, logKey, clusterNode);
        ssh2.close();
        
        return res;
    }
    
    public static boolean joinAsMasterNode(String ip, String redisPort, String sshUser, String sshPasswd,
            int sshPort, PaasRedisCluster cluster, String logKey, ResultBean result) {
        
        SSHExecutor ssh2 = new SSHExecutor(sshUser, sshPasswd, ip, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String redisDir = String.format("redis_%s", redisPort);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, redisDir);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }
        
        String joinMasterAddr = cluster.getOneMasterAddr();
        if (!StringUtils.isNull(joinMasterAddr)) { ssh2.close(); return false; }
        
        ArrayList<PaasRedisNode> masters = new ArrayList<PaasRedisNode>();
        cluster.getSlotedMasters(masters);
        // int masterCnt = masters.size();
        // int slotAvg = FixDefs.REDIS_CLUSTER_TTL_SLOT / (masterCnt + 1);  // old + 1 new
        // int moveSlotCnt = (FixDefs.REDIS_CLUSTER_TTL_SLOT / masterCnt) - slotAvg;
        
        // do join
        String cmdJoin = String.format("./bin/redis-cli --cluster add-node %s:%s %s -c --no-auth-warning", ip, redisPort, joinMasterAddr);
        DeployLog.pubLog(logKey, cmdJoin);
        if (!DeployUtils.joinRedisCluster(ssh2, cmdJoin, logKey, result)) {
            DeployLog.pubFailLog(logKey, "join as master node NOK ......");
            ssh2.close();
            return false;
        } else {
            DeployLog.pubSuccessLog(logKey, "join as master node OK ......");
        }
        
        // get self node id
        /*String cmdCheck = String.format("./bin/redis-cli -h %s -p %s -c --no-auth-warning cluster nodes | grep myself", ip, redisPort);
        String selfId = DeployUtils.getRedisNodeId(ssh2, cmdCheck, logKey, result);
        if (StringUtils.isNull(selfId)) { ssh2.close(); return false; }
        
        // do migrate slot from exist master nodes to newer
        
        for (int idx = 0; idx < masters.size(); ++idx) {
            ZZRedisNode& node = masters.at(idx);
            std::string& src_node_id = node.node_id;
            
            memset(cmd, 0, 2048);
            snprintf(cmd, 2047, "./bin/redis-cli --cluster reshard %s:%s --cluster-from %s --cluster-to %s --cluster-slots %d -c -a %s "\
                            "--no-auth-warning --cluster-yes",
                        node.ip.c_str(), node.port.c_str(), src_node_id.c_str(), self_id.c_str(), move_slot_cnt, ZZSOFT_REDIS_PASSWD);
            // if (!ssh2.reshard_redis_cluster(cmd, log_key)) return false;
        }*/
        
        ssh2.close();
        return true;
    }
    
    public static boolean joinAsSlaveNode(String ip, String redisPort, String sshUser, String sshPasswd,
            int sshPort, PaasRedisCluster cluster, String logKey, ResultBean result) {
        
        SSHExecutor ssh2 = new SSHExecutor(sshUser, sshPasswd, ip, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String redisDir = String.format("redis_%s", redisPort);
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.CACHE_REDIS_ROOT, redisDir);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) { ssh2.close(); return false; }
        
        String joinMasterAddr = cluster.getOneMasterAddr();
        if (!StringUtils.isNull(joinMasterAddr)) { ssh2.close(); return false; }
        
        String aloneMasterId = cluster.getAloneMaster();
        
        // do join
        // redis-cli --cluster add-node 127.0.0.1:6386 127.0.0.1:6385 --cluster-slave --cluster-master-id 46f0b68b3f605b3369d3843a89a2b4a164ed21e8
        String cmd = String.format("./bin/redis-cli --cluster add-node %s:%s %s -c --cluster-slave --no-auth-warning --cluster-master-id %s",
                        ip, redisPort, joinMasterAddr, aloneMasterId);
        DeployLog.pubLog(logKey, cmd);
        
        if (!DeployUtils.joinRedisCluster(ssh2, cmd, logKey, result)) {
            DeployLog.pubFailLog(logKey, "join as slave node NOK ......");
            ssh2.close();
            return false;
        }
        
        ssh2.close();
        DeployLog.pubSuccessLog(logKey, "join as slave node OK ......");
        return true;
    }
    
    public static JsonObject getSelfRedisNode(JsonArray redisNodeArr, String instID) {
        int size = redisNodeArr.size();
        JsonObject redisNode = null;
        
        for (int i = 0; i < size; ++i) {
            JsonObject currNode = redisNodeArr.getJsonObject(i);
            String currID = currNode.getString(FixHeader.HEADER_INST_ID);
            if (currID.equals(instID)) {
                redisNode = currNode;
                break;
            }
        }
        
        return redisNode;
    }
    
    public static boolean isMasterNode(JsonObject redisJson){
        if (!redisJson.containsKey(FixDefs.ATTR_NODE_TYPE)) {
            return false;
        }

        String strNodeType = redisJson.getString(FixDefs.ATTR_NODE_TYPE);
        if(StringUtils.isNull(strNodeType)){
            return false;
        }
        return FixDefs.TYPE_REDIS_MASTER_NODE.equals(strNodeType);
    }
    
    public static JsonObject getMasterRedisNode(JsonArray redisNodeArr) {
        int size = redisNodeArr.size();
        JsonObject redisNode = null;
        
        for (int i = 0; i < size; ++i) {
            JsonObject currNode = redisNodeArr.getJsonObject(i);
            String strNodeType = currNode.getString(FixDefs.ATTR_NODE_TYPE);
            if(!StringUtils.isNull(strNodeType)){
                if(FixDefs.TYPE_REDIS_MASTER_NODE.equals(strNodeType)) {
                    redisNode = currNode;
                    break;
                }
            }
        }
        
        return redisNode;
    }
    
    public static boolean isExistMultiMasterNode(JsonArray redisNodeArr){
        int size = redisNodeArr.size();
        int iFlag = 0;

        for (int i = 0; i < size; i++) {

            JsonObject redisJson = redisNodeArr.getJsonObject(i);
            String strNodeType = redisJson.getString(FixDefs.ATTR_NODE_TYPE);

            if(!StringUtils.isNull(strNodeType)){
                if(FixDefs.TYPE_REDIS_MASTER_NODE.equals(strNodeType)){
                    iFlag++;
                }
            }
        }

        return iFlag > 1;
    }

    //伪部署
    public static boolean deployFakeService(JsonArray redisNodeArr, ResultBean result, String servInstID, String magicKey) {
        for (int i = 0; i < redisNodeArr.size(); i++) {
            JsonObject jsonRedisNode = redisNodeArr.getJsonObject(i);
            String instId = jsonRedisNode.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) {
                return false;
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

    public static boolean undeployFakeService(JsonArray redisNodeArr, ResultBean result, String servInstID, String magicKey) {

        for (int i = 0; i < redisNodeArr.size(); i++) {
            JsonObject jsonRedisNode = redisNodeArr.getJsonObject(i);
            String instId = jsonRedisNode.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
                return false;
            }
        }

        // 3. update zz_service is_deployed flag
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


    public static boolean deployFakeClusterService(JsonObject servJson, ResultBean result, String servInstID, String magicKey) {
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);

        // 1. deploy redis nodes
        int redisNodeCnt = redisNodeArr.size();
        for (int idx = 0; idx < redisNodeCnt; ++idx) {
            JsonObject redisJson = redisNodeArr.getJsonObject(idx);
            String redisNodeID = redisJson.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(redisNodeID, FixDefs.STR_TRUE, result, magicKey)) {
                return false;
            }
        }

        // 2. deploy proxy
        int redisProxyCnt = proxyArr.size();
        for (int idx = 0; idx < redisProxyCnt; ++idx) {
            JsonObject proxyJson = proxyArr.getJsonObject(idx);
            String proxyID = proxyJson.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(proxyID, FixDefs.STR_TRUE, result, magicKey)) {
                return false;
            }
        }

        // 3. update t_service.is_deployed and local cache
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey))
            return false;
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey))
            return false;

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PSEUDO, magicKey)) {
            return false;
        }
        return true;
    }

    public static boolean unDeployFakeClusterService(JsonObject servJson, ResultBean result, String servInstID, String magicKey) {
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);

        // 1. deploy redis nodes
        int redisNodeCnt = redisNodeArr.size();
        for (int idx = 0; idx < redisNodeCnt; ++idx) {
            JsonObject redisJson = redisNodeArr.getJsonObject(idx);
            String redisNodeID = redisJson.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(redisNodeID, FixDefs.STR_FALSE, result, magicKey)) {
                return false;
            }
        }

        // 2. deploy proxy
        int redisProxyCnt = proxyArr.size();
        for (int idx = 0; idx < redisProxyCnt; ++idx) {
            JsonObject proxyJson = proxyArr.getJsonObject(idx);
            String proxyID = proxyJson.getString(FixHeader.HEADER_INST_ID);
            if (!MetaDataDao.updateInstanceDeployFlag(proxyID, FixDefs.STR_FALSE, result, magicKey)) {
                return false;
            }
        }

        // 3. update t_service.is_deployed and local cache
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey))
            return false;
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey))
            return false;

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PHYSICAL, magicKey)) {
            return false;
        }
        return true;
    }

}
