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
import com.zzstack.paas.underlying.utils.StringTools;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PulsarDeployerUtils {
    
    public static String getZKAddress(JsonArray zkArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        StringBuilder buff = new StringBuilder();
        int size = zkArr.size();
        for (int idx = 0; idx < size; ++idx) {
            JsonObject item = zkArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String clientPort1 = item.getString(FixHeader.HEADER_ZK_CLIENT_PORT1);
            String clientPort2 = item.getString(FixHeader.HEADER_ZK_CLIENT_PORT2);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            // server.1=host1:2888:3888
            // server.2=host2:2888:3888
            // server.3=host3:2888:3888
            String line = String.format("server.%d=%s:%s:%s", (idx + 1), servIP, clientPort1, clientPort2);
            if (idx > 0)
                buff.append("\n");
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getZKShortAddress(JsonArray zkArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        StringBuilder buff = new StringBuilder();
        int size = zkArr.size();
        for (int idx = 0; idx < size; ++idx) {
            JsonObject item = zkArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String clientPort = item.getString(FixHeader.HEADER_CLIENT_PORT);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, clientPort);
            if (idx > 0)
                buff.append(",");
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getPulsarBrokerList(JsonArray pulsarArr, String instID) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        StringBuilder buff = new StringBuilder();
        for (int idx = 0; idx < pulsarArr.size(); ++idx) {
            JsonObject item = pulsarArr.getJsonObject(idx);
            String id = item.getString(FixHeader.HEADER_INST_ID);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String brokerPort = item.getString(FixHeader.HEADER_BROKER_PORT);
            
            if (instID != null && id.equals(instID)) {
                continue;
            }
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, brokerPort);
            if (idx == 0) {
                buff.append("pulsar://");
            } else {
                buff.append(",");
            }
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getPulsarBrokerListForPrometheus(JsonArray pulsarArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        StringBuilder buff = new StringBuilder();
        for (int idx = 0; idx < pulsarArr.size(); ++idx) {
            JsonObject item = pulsarArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String webPort = item.getString(FixHeader.HEADER_WEB_PORT);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, webPort);
            if (idx > 0) {
                buff.append(CONSTS.METASVR_ADDR_SPLIT);
            }
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getPulsarBookieListForPrometheus(JsonArray bookieArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        StringBuilder buff = new StringBuilder();
        for (int idx = 0; idx < bookieArr.size(); ++idx) {
            JsonObject item = bookieArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String httpServPort = item.getString(FixHeader.HEADER_HTTP_SERVER_PORT);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, httpServPort);
            if (idx > 0) {
                buff.append(CONSTS.METASVR_ADDR_SPLIT);
            }
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static String getPulsarZKListForPrometheus(JsonArray zkArr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        StringBuilder buff = new StringBuilder();
        for (int idx = 0; idx < zkArr.size(); ++idx) {
            JsonObject item = zkArr.getJsonObject(idx);
            String sshID = item.getString(FixHeader.HEADER_SSH_ID);
            String adminPort = item.getString(FixHeader.HEADER_ADMIN_PORT);
            
            PaasSsh ssh = meta.getSshById(sshID);
            String servIP = ssh.getServerIp();
            
            String line = String.format("%s:%s", servIP, adminPort);
            if (idx > 0) {
                buff.append(CONSTS.METASVR_ADDR_SPLIT);
            }
            
            buff.append(line);
        }
        
        return buff.toString();
    }
    
    public static boolean deployBookie(JsonObject bookie, String version, String zkAddrList, String logKey, String magicKey, boolean initMeta, ResultBean result) {
        String instId = bookie.getString(FixHeader.HEADER_INST_ID);
        String sshId = bookie.getString(FixHeader.HEADER_SSH_ID);
        String bookiePort = bookie.getString(FixHeader.HEADER_BOOKIE_PORT);
        String httpServerPort = bookie.getString(FixHeader.HEADER_HTTP_SERVER_PORT);
        String grpcPort = bookie.getString(FixHeader.HEADER_GRPC_PORT);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy bookkeeper: %s:%s, instId:%s", servIp, bookiePort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "bookkeeper", instId, servIp, bookiePort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("bookkeeper.bookiePort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "bookkeeper", instId, servIp, httpServerPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("bookkeeper.httpServerPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "bookkeeper", instId, servIp, grpcPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("bookkeeper.grpcPort is in use");
            return false;
        }

        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.BOOKIE_FILE_ID, FixDefs.MQ_PULSAR_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.BOOKIE_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + bookiePort;
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
        
        // bookieId=%BOOKIE_ID%
        // bookiePort=%BOOKIE_PORT%
        // advertisedAddress=%ADVERTISED_ADDRESS%
        // httpServerPort=%HTTP_SERVER_PORT%
        // journalDirectories=%JOURNAL_DIRS%
        // ledgerDirectories=%LEDGER_DIRS%
        // metadataServiceUri=%META_DATA_SERVICE_URI%
        // zkServers=%ZK_SERVERS%
        // storageserver.grpc.port=%GRPC_PORT%
        
        String journalDir = String.format("data/bk-txn");
        journalDir = journalDir.replaceAll("/", "\\\\/");
        
        String ledgerDir = String.format("data/bk-data");
        ledgerDir = ledgerDir.replaceAll("/", "\\\\/");
        
        // String metaDataServiceUri = String.format("zk+hierarchical://%s/ledgers", zkAddrList);
        // metaDataServiceUri = metaDataServiceUri.replaceAll("/", "\\\\/");
        
        DeployLog.pubLog(logKey, "modify bookie configures ......");
        
        String configFile = "./conf/bk_server.conf";
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BOOKIE_ID, instId, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BOOKIE_PORT, bookiePort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADVERTISED_ADDRESS, servIp, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HTTP_SERVER_PORT, httpServerPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_JOURNAL_DIRS, journalDir, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LEDGER_DIRS, ledgerDir, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        /*if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_DATA_SERVICE_URI, metaDataServiceUri, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }*/
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ZK_SERVERS, zkAddrList, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_GRPC_PORT, grpcPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop.sh %BOOKIE_PORT%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BOOKIE_PORT, bookiePort, FixDefs.STOP_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // init bookie metadata
        if (initMeta) {
            String initMetaCmd = String.format("bin/bookkeeper shell metaformat");
            DeployLog.pubLog(logKey, initMetaCmd);
            if (!DeployUtils.execSimpleCmd(ssh2, initMetaCmd, logKey, result)) {
                ssh2.close();
                return false;
            }
        }
        
        // start
        DeployLog.pubLog(logKey, "start bookkeeper ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }

        if (!DeployUtils.checkPortUp(ssh2, "bookkeeper", instId, servIp, bookiePort, logKey, result)) {
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
    
    public static boolean deployPulsar(JsonObject pulsar, String pulsarClusterName, String brokerAddrList,
            String version, String zkAddrList, String logKey, String magicKey, boolean initMeta, ResultBean result) {
        
        String instId = pulsar.getString(FixHeader.HEADER_INST_ID);
        String sshId = pulsar.getString(FixHeader.HEADER_SSH_ID);
        String brokerPort = pulsar.getString(FixHeader.HEADER_BROKER_PORT);
        String webPort = pulsar.getString(FixHeader.HEADER_WEB_PORT);

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy pulsar: %s:%s, instId:%s", servIp, brokerPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pulsar", instId, servIp, brokerPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("bookkeeper.brokerPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pulsar", instId, servIp, webPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("bookkeeper.webPort is in use");
            return false;
        }

        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.PULSAR_FILE_ID, FixDefs.MQ_PULSAR_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.PULSAR_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + brokerPort;
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
        
        // zookeeperServers=%ZK_SERVERS%
        // configurationStoreServers=%CONF_STORE_SERVERS%
        // brokerServicePort=%BROKER_PORT%
        // webServicePort=%WEB_PORT%
        // bindAddress=%BROKER_ADDRESS%
        // advertisedAddress=%ADVERTISED_ADDRESS%
        // clusterName=%CLUSTER_NAME%
        // bookkeeperMetadataServiceUri=%BOOKIE_META_URI%
        
        DeployLog.pubLog(logKey, "modify pulsar configures ......");
        String configFile = "./conf/broker.conf";
        
        String metaDataServiceUri = String.format("zk+hierarchical://%s/ledgers", zkAddrList);
        metaDataServiceUri = metaDataServiceUri.replaceAll("/", "\\\\/").replaceAll(",", ";");
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ZK_SERVERS, zkAddrList, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        // 多集群部署时管理多个pulsar集群元数据的zookeeper集群地址，单集群部署时可以和zookeeperServers设置一样
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_STORE_SERVERS, zkAddrList, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_PORT, brokerPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_WEB_PORT, webPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BROKER_ADDRESS, servIp, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_ADVERTISED_ADDRESS, servIp, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NAME, pulsarClusterName, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_META_DATA_SERVICE_URI, metaDataServiceUri, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start pulsar ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // initialize cluster metadata
        if (initMeta) {
            DeployLog.pubLog(logKey, "initialize cluster metadata ......");
            String initMetaCmd = String.format("./bin/pulsar initialize-cluster-metadata "
                    + "--cluster %s --zookeeper %s "
                    + "--configuration-store %s "
                    + "--web-service-url http://%s:%s "
                    + "--broker-service-url pulsar://%s:%s", 
                    pulsarClusterName, zkAddrList, zkAddrList, servIp, webPort, servIp, brokerPort);
            DeployLog.pubLog(logKey, initMetaCmd);
            if (!DeployUtils.execSimpleCmd(ssh2, initMetaCmd, logKey, result)) {
                ssh2.close();
                return false;
            }
        } else {
            DeployLog.pubLog(logKey, "update broker-url ......");
            String updateUrlCmd = String.format("./bin/pulsar-admin --admin-url http://%s:%s "
                    + "clusters update %s "
                    + "--url http://%s:%s "
                    + "--broker-url %s",
                    servIp, webPort, pulsarClusterName, servIp, webPort, brokerAddrList);
            DeployLog.pubLog(logKey, updateUrlCmd);
            if (!DeployUtils.execSimpleCmd(ssh2, updateUrlCmd, logKey, result)) {
                ssh2.close();
                return false;
            }
        }

        if (!DeployUtils.checkPortUp(ssh2, "pulsar", instId, servIp, brokerPort, logKey, result)) {
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
    
    public static boolean deployPulsarManager(JsonObject pulsarManager, String bookies, String version,
            String logKey, String magicKey, ResultBean result) {
        
        String instId = pulsarManager.getString(FixHeader.HEADER_INST_ID);
        String sshId = pulsarManager.getString(FixHeader.HEADER_SSH_ID);
        String pulsarMgrPort = pulsarManager.getString(FixHeader.HEADER_PULSAR_MGR_PORT);
        String herddbPort = pulsarManager.getString(FixHeader.HEADER_HERDDB_PORT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy pulsar-manager: %s:%s, instId:%s", servIp, pulsarMgrPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pulsar-manager", instId, servIp, pulsarMgrPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("pulsar-manager.pulsarMgrPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "pulsar-manager", instId, servIp, herddbPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("pulsar-manager.herddbPort is in use");
            return false;
        }

        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.PULSAR_MANAGER_FILE_ID, FixDefs.MQ_PULSAR_ROOT, version, logKey, result))
            return false;
        
        //修改文件名
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PULSAR_MANAGER_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + pulsarMgrPort;
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
        
        // %PULSAR_MGR_PORT%
        // %HERDDB_PORT%
        // %BOOKIE_LIST%
        
        DeployLog.pubLog(logKey, "modify pulsar-manager configures ......");
        String configFile = "./application.properties";
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_MGR_PORT, pulsarMgrPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HERDDB_PORT, herddbPort, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_BOOKIE_LIST, bookies, configFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start pulsar-manager ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "pulsar-manager", instId, servIp, pulsarMgrPort, logKey, result)) {
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
    
    public static boolean undeployPulsarManager(JsonObject pulsarManager, String version, 
            String logKey, String magicKey, ResultBean result) {
        
        String instId = pulsarManager.getString(FixHeader.HEADER_INST_ID);
        String sshId = pulsarManager.getString(FixHeader.HEADER_SSH_ID);
        String pulsarMgrPort = pulsarManager.getString(FixHeader.HEADER_PULSAR_MGR_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy pulsar-manager, inst_id:%s, serv_ip:%s, pulsar_mgr_port:%s", instId, servIp, pulsarMgrPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PULSAR_MANAGER_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();
        
        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + pulsarMgrPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.MQ_PULSAR_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop pulsar-manager ......");
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

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }
    
    public static boolean deployPrometheus(JsonObject prometheus, String pulsarClusterName, String brokers,
            String bookies, String zks, String version, String logKey, String magicKey, ResultBean result) {

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
        
        // scp prometheus_pulsar.yml
        if (!DeployUtils.fetchFile(ssh2, FixDefs.PROMETHEUS_PULSAR_YML_FILE_ID, logKey, result)) {
            DeployLog.pubFailLog(logKey, "scp prometheus_pulsar.yml fail ......");
            return false;
        }
        
        // cluster: %CLUSTER_NAME%
        // %PULSAR_BROKERS%
        // %PULSAR_BOOKIES%
        // %PULSAR_ZOOKEEPERS%
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NAME, pulsarClusterName, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_BROKERS, brokers, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_BOOKIES, bookies, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PULSAR_ZOOKEEPERS, zks, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, FixDefs.PROMETHEUS_YML, FixDefs.PROMETHEUS_PULSAR_YML, logKey, result)) {
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
    
    public static boolean undeployPrometheus(JsonObject prometheus, String version, String logKey, String magicKey,
            ResultBean result) {

        String instId = prometheus.getString(FixHeader.HEADER_INST_ID);
        String sshId = prometheus.getString(FixHeader.HEADER_SSH_ID);
        String prometheusPort = prometheus.getString(FixHeader.HEADER_PROMETHEUS_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy prometheus, inst_id:%s, serv_ip:%s, prometheus_port:%s", instId, servIp, prometheusPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
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
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.COMMON_TOOLS_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop prometheus ......");
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

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }
    
    public static boolean undeployPulsar(JsonObject pulsar, String pulsarClusterName, String brokerAddrList,
            String version, String logKey, String magicKey, ResultBean result) {
        String instId = pulsar.getString(FixHeader.HEADER_INST_ID);
        String sshId = pulsar.getString(FixHeader.HEADER_SSH_ID);
        String brokerPort = pulsar.getString(FixHeader.HEADER_BROKER_PORT);
        String webPort = pulsar.getString(FixHeader.HEADER_WEB_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy pulsar, inst_id:%s, serv_ip:%s, broker_port:%s", instId, servIp, brokerPort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.PULSAR_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + brokerPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.MQ_PULSAR_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // 卸载pulsar broker时要从broker列表把即将卸载的broker节点剔除
        if (StringTools.notNullAndEmpty(pulsarClusterName) && StringTools.notNullAndEmpty(brokerAddrList)) {
            DeployLog.pubLog(logKey, "update broker-url ......");
            String updateUrlCmd = String.format("./bin/pulsar-admin --admin-url http://%s:%s "
                    + "clusters update %s "
                    + "--url http://%s:%s "
                    + "--broker-url %s",
                    servIp, webPort, pulsarClusterName, servIp, webPort, brokerAddrList);
            if (!DeployUtils.execSimpleCmd(ssh2, updateUrlCmd, logKey, result)) {
                ssh2.close();
                return false;
            }
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop pulsar broker ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortDown(ssh2, "pulsar", instId, servIp, brokerPort, logKey, result)) {
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

        // update instance deploy flag
        if (!MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_FALSE, result, magicKey)) {
            ssh2.close();
            return false;
        }

        ssh2.close();
        return true;
    }
    
    public static boolean undeployBookie(JsonObject bookie, String version, String logKey, String magicKey, ResultBean result) {
        String instId = bookie.getString(FixHeader.HEADER_INST_ID);
        String sshId = bookie.getString(FixHeader.HEADER_SSH_ID);
        String bookiePort = bookie.getString(FixHeader.HEADER_BOOKIE_PORT);
        
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        {
            String info = String.format("start undeploy bookkeeper, inst_id:%s, serv_ip:%s, bookie_port:%s", instId, servIp, bookiePort);
            DeployLog.pubLog(logKey, info);
        }
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.BOOKIE_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + bookiePort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.MQ_PULSAR_ROOT, newName);
        
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop bookkeeper ......");
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
        
        if (!DeployUtils.checkPortDown(ssh2, "bookkeeper", instId, servIp, bookiePort, logKey, result)) {
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
