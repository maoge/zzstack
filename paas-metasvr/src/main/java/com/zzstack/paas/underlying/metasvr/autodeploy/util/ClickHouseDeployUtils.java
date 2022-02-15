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

public class ClickHouseDeployUtils {
    
    public static String getRelicaCluster(JsonArray replicasArr) {
        // marshell format:
        //    <shard>
        //        <weight>100</weight>
        //        <internal_replication>false</internal_replication>
        //
        //        <replica>
        //            <host>172.20.0.247</host>
        //            <port>9000</port>
        //            <user>default</user>
        //            <password>abcd.1234</password>
        //        </replica>
        //    </shard>
        //
        //    <shard>
        //        <weight>100</weight>
        //        <internal_replication>false</internal_replication>
        //
        //        <replica>
        //            <host>172.20.0.248</host>
        //            <port>9000</port>
        //            <user>default</user>
        //            <password>abcd.1234</password>
        //        </replica>
        //    </shard>
        
        StringBuilder shards = new StringBuilder();
        for (int i = 0; i < replicasArr.size(); ++i) {
            StringBuilder replicas = new StringBuilder();
            replicas.append("            <shard>").append(CONSTS.LINE_END);
            
            JsonObject replicasNode = replicasArr.getJsonObject(i);
            JsonArray clickHouseArr = replicasNode.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            
            String replicaType = replicasNode.getString(FixHeader.HEADER_INTERNAL_REPLICATION);
            String weight       = String.format("                <weight>%d</weight>", FixDefs.CLICKHOUSE_DEFAULT_REPLICA_WEIGHT);
            String interReplica = String.format("                <internal_replication>%s</internal_replication>", replicaType);
            
            replicas.append(weight).append(CONSTS.LINE_END);
            replicas.append(interReplica).append(CONSTS.LINE_END);
            
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);

                String sshID = clickhouse.getString(FixHeader.HEADER_SSH_ID);
                String tcpPort = clickhouse.getString(FixHeader.HEADER_TCP_PORT);
                
                PaasSsh ssh = DeployUtils.getSshById(sshID, null, null);
                if (ssh == null)
                    continue;
                
                String host   = String.format("                    <host>%s</host>", ssh.getServerIp());
                String port   = String.format("                    <port>%s</port>", tcpPort);
                String user   = String.format("                    <user>%s</user>", FixDefs.CLICKHOUSE_DEFAULT_USER);
                String passwd = String.format("                    <password>%s</password>", FixDefs.CLICKHOUSE_DEFAULT_PASSWD);
                
                replicas.append("                <replica>").append(CONSTS.LINE_END);
                replicas.append(host).append(CONSTS.LINE_END);
                replicas.append(port).append(CONSTS.LINE_END);
                replicas.append(user).append(CONSTS.LINE_END);
                replicas.append(passwd).append(CONSTS.LINE_END);
                replicas.append("                </replica>").append(CONSTS.LINE_END);
            }

            replicas.append("            </shard>").append(CONSTS.LINE_END);
            shards.append(replicas.toString());
        }

        return shards.toString();
    }
    
    public static String getExporterList(JsonArray replicasArr) {
        StringBuilder sb = new StringBuilder("");
        for (int i = 0; i < replicasArr.size(); ++i) {
            JsonObject replicasNode = replicasArr.getJsonObject(i);
            JsonArray clickHouseArr = replicasNode.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);

                String sshID = clickhouse.getString(FixHeader.HEADER_SSH_ID);
                String port = clickhouse.getString(FixHeader.HEADER_EXPORTER_PORT);
                
                PaasSsh ssh = DeployUtils.getSshById(sshID, null, null);
                if (ssh == null)
                    continue;
                
                String exporter = String.format("%s:%s", ssh.getServerIp(), port);
                
                if (sb.length() > 0) {
                    sb.append(",");
                }
                
                sb.append(exporter);
            }
        }
        
        return sb.toString();
    }
    
    public static String getZkCluster(JsonArray zkArr) {
        //    <node>
        //        <host>172.20.0.41</host>
        //        <port>24003</port>
        //    </node>
        //    <node>
        //        <host>172.20.0.42</host>
        //        <port>24003</port>
        //    </node>
        //    <node>
        //        <host>172.20.0.43</host>
        //        <port>24003</port>
        //    </node>

        StringBuilder zkCluster = new StringBuilder();
        for (int i = 0; i < zkArr.size(); ++i) {
            JsonObject zk = zkArr.getJsonObject(i);

            String sshID = zk.getString(FixHeader.HEADER_SSH_ID);
            String clientPort = zk.getString(FixHeader.HEADER_CLIENT_PORT);

            PaasSsh ssh = DeployUtils.getSshById(sshID, null, null);
            if (ssh == null)
                continue;

            String host = String.format("                <host>%s</host>", ssh.getServerIp());
            String port = String.format("                <port>%s</port>", clientPort);

            StringBuilder zkNode = new StringBuilder();
            zkNode.append("            <node>").append(CONSTS.LINE_END);
            zkNode.append(host).append(CONSTS.LINE_END);
            zkNode.append(port).append(CONSTS.LINE_END);
            zkNode.append("            </node>").append(CONSTS.LINE_END);
            
            if (i < zkArr.size() - 1)
                zkCluster.append(zkNode.toString()).append(CONSTS.LINE_END);
            else
                zkCluster.append(zkNode.toString());
        }

        return zkCluster.toString();
    }
    
    public static boolean deployClickHouseServer(JsonObject clickhouse, String version, String parentID,
            String replicaCluster, String zkCluster, String logKey, String magicKey, ResultBean result) {
        
        String instId = clickhouse.getString(FixHeader.HEADER_INST_ID);
        String sshId = clickhouse.getString(FixHeader.HEADER_SSH_ID);
        String httpPort = clickhouse.getString(FixHeader.HEADER_HTTP_PORT);
        String tcpPort = clickhouse.getString(FixHeader.HEADER_TCP_PORT);
        String mysqlPort = clickhouse.getString(FixHeader.HEADER_MYSQL_PORT);
        String exporterPort = clickhouse.getString(FixHeader.HEADER_EXPORTER_PORT);
        String interServerHttpPort = clickhouse.getString(FixHeader.HEADER_INTERSERVER_HTTP_PORT);
        String maxConnections = clickhouse.getString(FixHeader.HEADER_MAX_CONNECTIONS);
        String maxConcurrentQueries = clickhouse.getString(FixHeader.HEADER_MAX_CONCURRENT_QUERIES);
        String maxServerMemoryUsage = clickhouse.getString(FixHeader.HEADER_MAX_SERVER_MEMORY_USAGE);
        String maxMemoryUsage = clickhouse.getString(FixHeader.HEADER_MAX_MEMORY_USAGE);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("deploy clickhouse-server: %s:%s, instId:%s", servIp, tcpPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "clickhouse-server", instId, servIp, httpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("clickhouse-server.httpPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "clickhouse-server", instId, servIp, tcpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("clickhouse-server.tcpPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "clickhouse-server", instId, servIp, mysqlPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("clickhouse-server.mysqlPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "clickhouse-server", instId, servIp, exporterPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("clickhouse-server.exporterPort is in use");
            return false;
        }
        
        if (DeployUtils.checkPortUpPredeploy(ssh2, "clickhouse-server", instId, servIp, interServerHttpPort, logKey, result)) {
            ssh2.close();
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo("clickhouse-server.interServerHttpPort is in use");
            return false;
        }
        
        //获取文件,解压文件
        if (!DeployUtils.fetchAndExtractTGZDeployFile(ssh2, FixDefs.DB_CLICKHOUSE_FILE_ID, FixDefs.DB_CLICKHOUSE_ROOT, version, logKey, result))
            return false;
        
        String oldName = DeployUtils.getVersionedFileName(FixDefs.DB_CLICKHOUSE_FILE_ID, version, logKey, result);
        String newName = oldName + "_" + tcpPort;
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
        
        // etc/config.xml
        // <http_port>%HTTP_PORT%</http_port>
        // <tcp_port>%TCP_PORT%</tcp_port>
        // <mysql_port>%MYSQL_PORT%</mysql_port>
        // <interserver_http_port>%INTERSERVER_HTTP_PORT%</interserver_http_port>
        // <listen_host>%LISTEN_HOST%</listen_host>
        // <max_connections>%MAX_CONNECTIONS%</max_connections>
        // <max_concurrent_queries>%MAX_CONCURRENT_QUERIES%</max_concurrent_queries>
        // <max_server_memory_usage>%MAX_SERVER_MEMORY_USAGE%</max_server_memory_usage>
        // %CLICKHOUSE_SHARDS%
        // %ZK_NODES%
        // <shard>%SHARD_ID%</shard>
        // <replica>%REPLICA_ID%</replica>
        String confFile = String.format("etc/config.xml");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_HTTP_PORT, httpPort, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_TCP_PORT, tcpPort, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MYSQL_PORT, mysqlPort, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_INTERSERVER_HTTP_PORT, interServerHttpPort, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_LISTEN_HOST, servIp, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_CONNECTIONS, maxConnections, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_CONCURRENT_QUERIES, maxConcurrentQueries, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_SERVER_MEMORY_USAGE, maxServerMemoryUsage, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.appendMultiLine(ssh2, FixDefs.CONF_CLICKHOUSE_SHARDS, replicaCluster, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.appendMultiLine(ssh2, FixDefs.CONF_ZK_NODES, zkCluster, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SHARD_ID, parentID, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_REPLICA_ID, instId, confFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // etc/user.xml
        // <max_memory_usage>%MAX_MEMORY_USAGE%</max_memory_usage>
        // <password>%PASSWORD%</password>
        String userFile = String.format("etc/users.xml");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_MAX_MEMORY_USAGE, maxMemoryUsage, userFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_PASSWORD, FixDefs.CLICKHOUSE_DEFAULT_PASSWD, userFile, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start.sh
        // export SCRAPE_URI=%SCRAPE_URI%
        // export TELEMETRY_ADDRESS=%TELEMETRY_ADDRESS%
        // export CLICKHOUSE_USER=%CLICKHOUSE_USER%
        // export CLICKHOUSE_PASSWORD=%CLICKHOUSE_PASSWORD%
        String scrapeUri = String.format("http://%s:%s/", servIp, httpPort).replaceAll("/", "\\\\/");
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_SCRAPE_URI, scrapeUri, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        String telemetryAddr = String.format("%s:%s", servIp, exporterPort);
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_TELEMETRY_ADDRESS, telemetryAddr, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLICKHOUSE_USER, FixDefs.CLICKHOUSE_DEFAULT_USER, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLICKHOUSE_PASSWORD, FixDefs.CLICKHOUSE_DEFAULT_PASSWD, FixDefs.START_SHELL, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // start
        DeployLog.pubLog(logKey, "start clickhouse ......");
        String cmd = String.format("./%s", FixDefs.START_SHELL);
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortUp(ssh2, "clickhouse-server", instId, servIp, tcpPort, logKey, result)) {
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
    
    public static boolean undeployClickHouseServer(JsonObject clickhouse, String version, String logKey, String magicKey,
            ResultBean result) {

        String instId = clickhouse.getString(FixHeader.HEADER_INST_ID);
        String sshId = clickhouse.getString(FixHeader.HEADER_SSH_ID);
        String tcpPort = clickhouse.getString(FixHeader.HEADER_TCP_PORT);
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;

        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String sshName = ssh.getSshName();
        String sshPwd = ssh.getSshPwd();
        int sshPort = ssh.getSshPort();
        
        {
            String info = String.format("start undeploy clickhouse-server: %s:%s, instId:%s", servIp, tcpPort, instId);
            DeployLog.pubLog(logKey, info);
        }
        
        if (DeployUtils.isInstanceNotDeployed(inst, logKey, result)) return true;
        
        SSHExecutor ssh2 = new SSHExecutor(sshName, sshPwd, servIp, sshPort);
        if (!DeployUtils.initSsh2(ssh2, logKey, result)) return false;
        
        PaasDeployFile deployFile = DeployUtils.getDeployFile(FixDefs.DB_CLICKHOUSE_FILE_ID, logKey, result);
        String srcFileName = deployFile.getFileName();

        if (version == null || version.isEmpty()) {
            version = deployFile.getVersion();
        }
        if (srcFileName.indexOf(CONSTS.REG_VERSION) != -1 && version != null && !version.isEmpty()) {
            srcFileName = srcFileName.replaceFirst(CONSTS.REG_VERSION, version);
        }

        int i = srcFileName.indexOf(CONSTS.TAR_GZ_SURFIX);
        String oldName = srcFileName.substring(0, i);

        String newName = oldName + "_" + tcpPort;
        String rootDir = String.format("%s/%s/%s", FixDefs.PAAS_ROOT, FixDefs.DB_CLICKHOUSE_ROOT, newName);
        if (!DeployUtils.cd(ssh2, rootDir, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        // stop
        DeployLog.pubLog(logKey, "stop clickhouse-server ......");
        String cmd = "./stop.sh";
        if (!DeployUtils.execSimpleCmd(ssh2, cmd, logKey, result)) {
            ssh2.close();
            return false;
        }
        
        if (!DeployUtils.checkPortDown(ssh2, "clickhouse-server", instId, servIp, tcpPort, logKey, result)) {
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
    
    public static boolean deployPrometheus(JsonObject prometheus, String clusterName, String exporters,
            String version, String logKey, String magicKey, ResultBean result) {

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
        
        // scp prometheus_clickhouse.yml
        if (!DeployUtils.fetchFile(ssh2, FixDefs.PROMETHEUS_CLICKHOUSE_YML_FILE_ID, logKey, result)) {
            DeployLog.pubFailLog(logKey, "scp prometheus_clickhouse.yml fail ......");
            return false;
        }
        
        // cluster: %CLUSTER_NAME%
        // targets: [%CLICKHOUSE_EXPORTER_LIST%]
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLUSTER_NAME, clusterName, FixDefs.PROMETHEUS_CLICKHOUSE_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.sed(ssh2, FixDefs.CONF_CLICKHOUSE_EXPORTER_LIST, exporters, FixDefs.PROMETHEUS_CLICKHOUSE_YML, logKey, result)) {
            ssh2.close();
            return false;
        }
        if (!DeployUtils.mv(ssh2, FixDefs.PROMETHEUS_YML, FixDefs.PROMETHEUS_CLICKHOUSE_YML, logKey, result)) {
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

}
