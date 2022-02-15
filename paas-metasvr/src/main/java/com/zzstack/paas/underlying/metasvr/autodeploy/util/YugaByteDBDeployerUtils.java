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

public class YugaByteDBDeployerUtils {
    
    public static boolean deployMaster(JsonObject ybMaster, String version, String masterList,
            String logKey, String magicKey, ResultBean result) {
        
        String instId = ybMaster.getString(FixHeader.HEADER_INST_ID);
        String sshId = ybMaster.getString(FixHeader.HEADER_SSH_ID);
        
        String rpcBindPort = ybMaster.getString(FixHeader.HEADER_RPC_BIND_PORT);
        String webServPort = ybMaster.getString(FixHeader.HEADER_WEBSERVER_PORT);
        String durableWalWrite = ybMaster.getString(FixHeader.HEADER_DURABLE_WAL_WRITE);
        String enableLoadBalancing = ybMaster.getString(FixHeader.HEADER_ENABLE_LOAD_BALANCING);
        String maxClockSkewUsec = ybMaster.getString(FixHeader.HEADER_MAX_CLOCK_SKEW_USEC);
        String replicFactor = ybMaster.getString(FixHeader.HEADER_REPLICATION_FACTOR);
        String ybNumShardsPerTServer = ybMaster.getString(FixHeader.HEADER_YB_NUM_SHARDS_PER_TSERVER);
        String ysqlNumShardsPerTServer = ybMaster.getString(FixHeader.HEADER_YSQL_NUM_SHARDS_PER_TSERVER);
        String placementCloud = ybMaster.getString(FixHeader.HEADER_PLACEMENT_CLOUD);
        String placementZone = ybMaster.getString(FixHeader.HEADER_PLACEMENT_ZONE);
        String placementRegion = ybMaster.getString(FixHeader.HEADER_PLACEMENT_REGION);
        String cdcWalRetentionTimeSecs = ybMaster.getString(FixHeader.HEADER_CDC_WAL_RETENTION_TIME_SECS);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy yb-master: %s:%s, instId:%s", servIp, rpcBindPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-master", instId, servIp, rpcBindPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-master.RPC_BIND_PORT is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-master", instId, servIp, webServPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-master.WEBSERVER_PORT is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_YUGABYTEDB_FILE_ID, FixDefs.DB_YUGABYTEDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_YUGABYTEDB_FILE_ID, version, logKey, result);
        String newName = "yb-master_" + rpcBindPort;
        if (!DeployUtils.rmOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }
        if (!DeployUtils.mvOnFailClose(ssh2, newName, oldName, logKey, result)) {
            return false;
        }
        if (!DeployUtils.cdOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }
        
        String currPath = DeployUtils.pwd(ssh2, logKey, result);
        
        //  etc/yb-master.conf
        //  %YB_MASTER_ADDR% 替换为master1:bind_port1,master2:bind_port2,master3:bind_port3
        //  %FS_DATA_DIRS% 替换为masterList
        //  %RPC_BIND_ADDR% 替换为masterIp:rpc_bind_port
        //  %SERV_BROADCAST_ADDR% 替换为masterIp
        //  %WEBSERV_INTERFACE% 替换为masterIp
        //  %WEBSERV_PORT% 替换为$WEBSERV_PORT
        //  %DURABLE_WAL_WRITE% 替换为$DURABLE_WAL_WRITE
        //  %ENABLE_LOAD_BALANCING% 替换为$ENABLE_LOAD_BALANCING
        //  %MAX_CLOCK_SKEW_USEC% 替换为$MAX_CLOCK_SKEW_USEC
        //  %YB_NUM_SHARDS_PER_TSERVER% 替换为$YB_NUM_SHARDS_PER_TSERVER
        //  %YSQL_NUM_SHARDS_PER_TSERVER% 替换为$YSQL_NUM_SHARDS_PER_TSERVER
        //  %PLACEMENT_CLOUD% 替换为$PLACEMENT_CLOUD
        //  %PLACEMENT_ZONE% 替换为$PLACEMENT_ZONE
        //  %PLACEMENT_REGION% 替换为$PLACEMENT_REGION
        //  %CDC_WAL_RETENTION_TIME_SECS% 替换为$CDC_WAL_RETENTION_TIME_SECS
        DeployLog.pubLog(logKey, "modify etc/yb-master.conf params ......");
        String ybMasterConf = "./etc/yb-master.conf";
        String fsDataDirs = String.format("%s/data", currPath).replaceAll("/", "\\\\/");
        String rpcBindAddr = String.format("%s:%s", servIp, rpcBindPort);
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YB_MASTER_ADDR, masterList, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_FS_DATA_DIRS, fsDataDirs, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_RPC_BIND_ADDR, rpcBindAddr, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_SERV_BROADCAST_ADDR, servIp, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_WEBSERV_INTERFACE, servIp, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_WEBSERVER_PORT, webServPort, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_DURABLE_WAL_WRITE, durableWalWrite, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ENABLE_LOAD_BALANCING, enableLoadBalancing, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_MAX_CLOCK_SKEW_USEC, maxClockSkewUsec, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_REPLICATION_FACTOR, replicFactor, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YB_NUM_SHARDS_PER_TSERVER, ybNumShardsPerTServer, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YSQL_NUM_SHARDS_PER_TSERVER, ysqlNumShardsPerTServer, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_CLOUD, placementCloud, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_ZONE, placementZone, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_REGION, placementRegion, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_CDC_WAL_RETENTION_TIME_SECS, cdcWalRetentionTimeSecs, ybMasterConf, logKey, result)) {
            return false;
        }
        
        // 修改启停脚本
        if (!DeployUtils.mvOnFailClose(ssh2, FixDefs.START_SHELL, "master_start.sh", logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mvOnFailClose(ssh2, FixDefs.STOP_SHELL, "master_stop.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "tserver_start.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "tserver_stop.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "./etc/yb-tserver.conf", logKey, result)) {
            return false;
        }
        
        // 先执行符号路径替换
        DeployLog.pubLog(logKey, "replacing symbol link ......");
        String preInstallCmd = "bin/post_install.sh";
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, preInstallCmd, logKey, result)) {
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start yb-master ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) {
            return false;
        }

        if (!DeployUtils.checkPortUpOnFailClose(ssh2, "yb-master", instId, servIp, rpcBindPort, logKey, result)) {
            return false;
        }

        // update t_meta_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "deploy yb-master success ......");
        ssh2.close();
        
        return true;
    }
    
    public static boolean undeployMaster(JsonObject ybMaster, String logKey, String magicKey, ResultBean result) {
        String instId = ybMaster.getString(FixHeader.HEADER_INST_ID);
        String sshId = ybMaster.getString(FixHeader.HEADER_SSH_ID);
        String rpcBindPort = ybMaster.getString(FixHeader.HEADER_RPC_BIND_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy yb-master, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, rpcBindPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = "yb-master_" + rpcBindPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_YUGABYTEDB_ROOT, newName);
        if (!DeployUtils.cdOnFailClose(ssh2, rootDir, logKey, result)) {
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop yb-master ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) {
            return false;
        }
        
        if (!DeployUtils.cdOnFailClose(ssh2, "..", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }

        if (!DeployUtils.checkPortDownOnFailClose(ssh2, "yb-master", instId, servIp, rpcBindPort, logKey, result)) {
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
    
    public static boolean deployTServer(JsonObject ybTServer, String version, String masterList,
            String logKey, String magicKey, ResultBean result) {
        
        String instId = ybTServer.getString(FixHeader.HEADER_INST_ID);
        String sshId = ybTServer.getString(FixHeader.HEADER_SSH_ID);
        
        String maxClockSkewUsec = ybTServer.getString(FixHeader.HEADER_MAX_CLOCK_SKEW_USEC);
        String rpcBindPort = ybTServer.getString(FixHeader.HEADER_RPC_BIND_PORT);
        String webServPort = ybTServer.getString(FixHeader.HEADER_WEBSERVER_PORT);
        String durableWalWrite = ybTServer.getString(FixHeader.HEADER_DURABLE_WAL_WRITE);
        String ybNumShardsPerTServer = ybTServer.getString(FixHeader.HEADER_YB_NUM_SHARDS_PER_TSERVER);
        String ysqlNumShardsPerTServer = ybTServer.getString(FixHeader.HEADER_YSQL_NUM_SHARDS_PER_TSERVER);
        String placementCloud = ybTServer.getString(FixHeader.HEADER_PLACEMENT_CLOUD);
        String placementZone = ybTServer.getString(FixHeader.HEADER_PLACEMENT_ZONE);
        String placementRegion = ybTServer.getString(FixHeader.HEADER_PLACEMENT_REGION);
        String pgProxyBindPort = ybTServer.getString(FixHeader.HEADER_PGSQL_PROXY_BIND_PORT);
        String pgProxyWebServPort = ybTServer.getString(FixHeader.HEADER_PGSQL_PROXY_WEBSERVER_PORT);
        String cqlProxyBindPort = ybTServer.getString(FixHeader.HEADER_CQL_PROXY_BIND_PORT);
        String cqlProxyWebservPort = ybTServer.getString(FixHeader.HEADER_CQL_PROXY_WEBSERVER_PORT);
        String ysqlMaxConnections = ybTServer.getString(FixHeader.HEADER_YSQL_MAX_CONNECTIONS);
        String rocksdbCompactFlushRateLimitBytesPerSec = ybTServer.getString(FixHeader.HEADER_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC);
        String rocksdbUniversalCompactionMinMergeWidth = ybTServer.getString(FixHeader.HEADER_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH);
        String rocksdbUniversalCompactionSizeRatio = ybTServer.getString(FixHeader.HEADER_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO);
        String timestampHistoryRetentionIntervalSec = ybTServer.getString(FixHeader.HEADER_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC);
        String remoteBootstrapRateLimitBytesPerSec = ybTServer.getString(FixHeader.HEADER_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy yb-tserver: %s:%s, instId:%s", servIp, rpcBindPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, rpcBindPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.RPC_BIND_PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, webServPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.WEBSERV_PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, pgProxyBindPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.PGSQL_PROXY_BIND_PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, pgProxyWebServPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.PGSQL_PROXY_WEBSERVER_PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, cqlProxyBindPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.CQL_PROXY_BIND_PORT is in use");
            return false;
        }
        if (DeployUtils.checkPortUpPredeployOnClose(ssh2, "yb-tserver", instId, servIp, cqlProxyWebservPort, logKey, result)) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("yb-tserver.CQL_PROXY_WEBSERVER_PORT is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_YUGABYTEDB_FILE_ID, FixDefs.DB_YUGABYTEDB_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_YUGABYTEDB_FILE_ID, version, logKey, result);
        String newName = "yb-tserver_" + rpcBindPort;
        if (!DeployUtils.rmOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }
        if (!DeployUtils.mvOnFailClose(ssh2, newName, oldName, logKey, result)) {
            return false;
        }
        if (!DeployUtils.cdOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }
        
        String currPath = DeployUtils.pwd(ssh2, logKey, result);
        
        //  etc/yb-tserver.conf
        //  %MASTER_ADDRS% 替换为master1:bind_port1,master2:bind_port2,master3:bind_port3
        //  %FS_DATA_DIRS% 替换为masterList
        //  %MAX_CLOCK_SKEW_USEC% 替换为$MAX_CLOCK_SKEW_USEC
        //  %RPC_BIND_ADDR% 替换为servIp:rpcBindPort
        //  %SERV_BROADCAST_ADDR% 替换为servIp
        //  %WEBSERV_INTERFACE% 替换为servIp
        //  %WEBSERV_PORT% 替换为$WEBSERV_PORT
        //  %DURABLE_WAL_WRITE% 替换为$DURABLE_WAL_WRITE
        //  %YB_NUM_SHARDS_PER_TSERVER% 替换为$YB_NUM_SHARDS_PER_TSERVER
        //  %YSQL_NUM_SHARDS_PER_TSERVER% 替换为$YSQL_NUM_SHARDS_PER_TSERVER
        //  %PLACEMENT_CLOUD% 替换为$PLACEMENT_CLOUD
        //  %PLACEMENT_ZONE% 替换为$PLACEMENT_ZONE
        //  %PLACEMENT_REGION% 替换为$PLACEMENT_REGION
        //  %PGSQL_PROXY_BIND_ADDR% 替换为$servIp:$PGSQL_PROXY_BIND_PORT
        //  %PGSQL_PROXY_WEBSERVER_PORT% 替换为$PGSQL_PROXY_WEBSERVER_PORT
        //  %YSQL_MAX_CONNECTIONS% 替换为$YSQL_MAX_CONNECTIONS
        //  %CQL_PROXY_BIND_ADDR% 替换为servIp:$CQL_PROXY_BIND_PORT
        //  %CQL_PROXY_WEBSERVER_PORT% 替换为$CQL_PROXY_WEBSERVER_PORT
        //  %ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC% 替换为$ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC
        //  %ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH% 替换为$ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH
        //  %ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO% 替换为$ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO
        //  %TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC% 替换为$TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC
        //  %REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC% 替换为$REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC
        DeployLog.pubLog(logKey, "modify etc/yb-tserver.conf params ......");
        String ybMasterConf = "./etc/yb-tserver.conf";
        String fsDataDirs = String.format("%s/data", currPath).replaceAll("/", "\\\\/");
        String rpcBindAddr = String.format("%s:%s", servIp, rpcBindPort);
        String pgProxyBindAddr = String.format("%s:%s", servIp, pgProxyBindPort);
        String cqlProxyBindAddr = String.format("%s:%s", servIp, cqlProxyBindPort);
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_MASTER_ADDRS, masterList, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_FS_DATA_DIRS, fsDataDirs, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_MAX_CLOCK_SKEW_USEC, maxClockSkewUsec, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_RPC_BIND_ADDR, rpcBindAddr, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_SERV_BROADCAST_ADDR, servIp, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_WEBSERV_INTERFACE, servIp, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_WEBSERVER_PORT, webServPort, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_DURABLE_WAL_WRITE, durableWalWrite, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YB_NUM_SHARDS_PER_TSERVER, ybNumShardsPerTServer, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YSQL_NUM_SHARDS_PER_TSERVER, ysqlNumShardsPerTServer, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_CLOUD, placementCloud, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_ZONE, placementZone, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PLACEMENT_REGION, placementRegion, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PGSQL_PROXY_BIND_ADDR, pgProxyBindAddr, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PGSQL_PROXY_WEBSERVER_PORT, pgProxyWebServPort, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_PGSQL_PROXY_WEBSERVER_PORT, pgProxyWebServPort, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_YSQL_MAX_CONNECTIONS, ysqlMaxConnections, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_CQL_PROXY_BIND_ADDR, cqlProxyBindAddr, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_CQL_PROXY_WEBSERVER_PORT, cqlProxyWebservPort, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ROCKSDB_COMPACT_FLUSH_RATE_LIMIT_BYTES_PER_SEC, rocksdbCompactFlushRateLimitBytesPerSec, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ROCKSDB_UNIVERSAL_COMPACTION_MIN_MERGE_WIDTH, rocksdbUniversalCompactionMinMergeWidth, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_ROCKSDB_UNIVERSAL_COMPACTION_SIZE_RATIO, rocksdbUniversalCompactionSizeRatio, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_TIMESTAMP_HISTORY_RETENTION_INTERVAL_SEC, timestampHistoryRetentionIntervalSec, ybMasterConf, logKey, result)) {
            return false;
        }
        if (!DeployUtils.sedOnFailClose(ssh2, FixDefs.CONF_REMOTE_BOOTSTRAP_RATE_LIMIT_BYTES_PER_SEC, remoteBootstrapRateLimitBytesPerSec, ybMasterConf, logKey, result)) {
            return false;
        }
        
        // 修改启停脚本
        if (!DeployUtils.mvOnFailClose(ssh2, FixDefs.START_SHELL, "tserver_start.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.mvOnFailClose(ssh2, FixDefs.STOP_SHELL, "tserver_stop.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "master_start.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "master_stop.sh", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, "./etc/yb-master.conf", logKey, result)) {
            return false;
        }
        
        // 先执行符号路径替换
        DeployLog.pubLog(logKey, "replacing symbol link ......");
        String preInstallCmd = "bin/post_install.sh";
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, preInstallCmd, logKey, result)) {
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start yb-tserver ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) {
            return false;
        }

        if (!DeployUtils.checkPortUpOnFailClose(ssh2, "yb-tserver", instId, servIp, rpcBindPort, logKey, result)) {
            return false;
        }

        // update t_meta_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_TRUE, result, magicKey)) return false;

        DeployLog.pubLog(logKey, "deploy yb-tserver success ......");
        ssh2.close();
        
        return true;
    }
    
    public static boolean undeployTServer(JsonObject ybTServer, String logKey, String magicKey, ResultBean result) {
        String instId = ybTServer.getString(FixHeader.HEADER_INST_ID);
        String sshId = ybTServer.getString(FixHeader.HEADER_SSH_ID);
        String rpcBindPort = ybTServer.getString(FixHeader.HEADER_RPC_BIND_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy yb-tserver, inst_id:%s, serv_ip:%s, port:%s", instId, servIp, rpcBindPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        String newName = "yb-tserver_" + rpcBindPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_YUGABYTEDB_ROOT, newName);
        if (!DeployUtils.cdOnFailClose(ssh2, rootDir, logKey, result)) {
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop yb-tserver ......");
        String cmd = String.format("./%s", FixDefs.STOP_SHELL);
        if (!DeployUtils.execSimpleCmdOnFailClose(ssh2, cmd, logKey, result)) {
            return false;
        }
        
        if (!DeployUtils.cdOnFailClose(ssh2, "..", logKey, result)) {
            return false;
        }
        if (!DeployUtils.rmOnFailClose(ssh2, newName, logKey, result)) {
            return false;
        }

        if (!DeployUtils.checkPortDownOnFailClose(ssh2, "yb-tserver", instId, servIp, rpcBindPort, logKey, result)) {
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
    
    public static String getYbMasterList(JsonArray ybMasterArr, String logKey, ResultBean result) {
        StringBuilder masterList = new StringBuilder("");
        for (int i = 0; i < ybMasterArr.size(); ++i) {
            if (masterList.length() > 0)
                masterList.append(",");
            
            JsonObject ybMaster = ybMasterArr.getJsonObject(i);
            String sshId = ybMaster.getString(FixHeader.HEADER_SSH_ID);
            PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
            if (ssh == null)
                continue;
            String servIp = ssh.getServerIp();
            String rpcBindPort = ybMaster.getString(FixHeader.HEADER_RPC_BIND_PORT);
            String address = String.format("%s:%s", servIp, rpcBindPort);
            masterList.append(address);
        }
        
        return masterList.toString();
    }

}
