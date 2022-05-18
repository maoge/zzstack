package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.RedisDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasRedisCluster;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.StringUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RedisClusterDeployer implements ServiceDeployer {
    
    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();

        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(deployFlag)) {
            if (!RedisDeployUtils.deployFakeClusterService(servJson, result, servInstID, magicKey)) {
                DeployLog.pubFailLog(logKey, "redis deploy failed ......");
                return false;
            }

            String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        // JsonObject collectdJson = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);

        StringBuilder node4cluster = new StringBuilder("");
        StringBuilder nodes4proxy = new StringBuilder("");
        if (!RedisDeployUtils.getClusterNodes(redisNodeArr, node4cluster, nodes4proxy, logKey, result)) return false;
        
        // 1. deploy redis nodes
        int redisNodeCnt = redisNodeArr.size();
        for (int idx = 0; idx < redisNodeCnt; ++idx) {
            boolean init = idx == (redisNodeCnt - 1);
            JsonObject redisJson = redisNodeArr.getJsonObject(idx);
            if (!RedisDeployUtils.deployRedisNode(redisJson, init, false, true, node4cluster.toString(), version,
                    logKey, magicKey, result))
                return false;
        }

        // 2. deploy proxy
        int redisProxyCnt = proxyArr.size();
        for (int idx = 0; idx < redisProxyCnt; ++idx) {
            JsonObject proxyJson = proxyArr.getJsonObject(idx);
            if (!RedisDeployUtils.deployProxyNode(proxyJson, nodes4proxy.toString(), logKey, magicKey, result))
                return false;
        }

        // 部署collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd start failed ......");
                    return false;
                }
            }
        }

        // update deploy flag and local cache
        DeployUtils.postProc(servInstID, FixDefs.STR_TRUE, logKey, magicKey, result);
        return true;
    }


    @Override
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);

        //卸载伪部署
        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(serv.getPseudoDeployFlag())) { //服务是伪部署的话
            if (!RedisDeployUtils.unDeployFakeClusterService(servJson, result, servInstID, magicKey)) {
                return false;
            }

            String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        // JsonObject collectdJson = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);
        
        // 1. undeploy proxy
        int proxySize = proxyArr.size();
        for (int idx = 0; idx < proxySize; ++idx) {
            JsonObject proxyJson = proxyArr.getJsonObject(idx);
            if (!RedisDeployUtils.undeployProxyNode(proxyJson, force, logKey, magicKey, result)) {
                if (!force) return false;
            }
        }
        
        // 2. undeploy redis nodes
        int redisNodeSize = redisNodeArr.size();
        for (int idx = 0; idx < redisNodeSize; ++idx) {
            JsonObject redisJson = redisNodeArr.getJsonObject(idx);
            
            if (!RedisDeployUtils.undeployRedisNode(redisJson, false, logKey, magicKey, result)) {
                if (!force) return false;
            }
        }
        // 卸载collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd undeploy failed ......");
                    return false;
                }
            }
        }
        
        // update deploy flag and local cache
        DeployUtils.postProc(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();

        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);
        
        StringBuilder node4cluster = new StringBuilder();
        StringBuilder nodes4proxy = new StringBuilder();
        if (!RedisDeployUtils.getClusterNodes(redisNodeArr, node4cluster, nodes4proxy, logKey, result)) return false;
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;

        switch (instCmpt.getCmptName()) {
        case FixDefs.CMPT_REDIS_PROXY:
            JsonObject proxyJson = DeployUtils.getSpecifiedItem(proxyArr, instID);
            deployResult = RedisDeployUtils.deployProxyNode(proxyJson, nodes4proxy.toString(), logKey, magicKey, result);
            break;
        case FixDefs.CMPT_REDIS_NODE:
            String joinIp = "", joinPort = "", sshUser = "", sshPasswd = "";
            int sshPort = 22;
            int nodeSize = redisNodeArr.size();
            for (int i = 0; i < nodeSize; ++i) {
                JsonObject redis_json = redisNodeArr.getJsonObject(i);
                String curr_id = redis_json.getString(FixHeader.HEADER_INST_ID);
                if (!curr_id.equals(instID)) {
                    if (joinPort.length() == 0) {
                        // get some one node in cluster in order to connect and fetch cluster info.
                        // fetch_ip_and_port(join_ip, join_port, ssh_user, ssh_passwd, ssh_port, redis_json, logKey, result);
                        String sshID = redis_json.getString(FixHeader.HEADER_SSH_ID);
                        PaasSsh ssh = DeployUtils.getSshById(sshID, logKey, result);
                        if (ssh != null) {
                            joinIp    = ssh.getServerIp();
                            sshUser   = ssh.getSshName();
                            sshPasswd = ssh.getSshPwd();
                            sshPort   = ssh.getSshPort();
                            joinPort  = redis_json.getString(FixHeader.HEADER_PORT);
                        }
                    }
                    continue;
                }
                
                if (!RedisDeployUtils.deployRedisNode(redis_json, false, true, true, "", version, logKey, magicKey,
                        result))
                    return false;

                // get deployed cluster nodes info
                ResultBean clusterInfo = new ResultBean();
                if (!RedisDeployUtils.getRedisClusterNode(joinIp, joinPort, sshUser, sshPasswd, sshPort, logKey, clusterInfo)) return false;
                PaasRedisCluster cluster = new PaasRedisCluster();
                cluster.parse(clusterInfo.getRetInfo());
                // get self node info.
                String selfSshID = redis_json.getString(FixHeader.HEADER_SSH_ID);
                PaasSsh selfSsh = DeployUtils.getSshById(selfSshID, logKey, result);
                if (selfSsh != null) {
                    joinIp    = selfSsh.getServerIp();
                    sshUser   = selfSsh.getSshName();
                    sshPasswd = selfSsh.getSshPwd();
                    sshPort   = selfSsh.getSshPort();
                    joinPort  = redis_json.getString(FixHeader.HEADER_PORT);
                }

                // first check whether REDIS_CLUSTER_REPLICAS > 0 and have mater node with no slave
                // if REDIS_CLUSTER_REPLICAS == 0 new node is joined with master role,
                // or else need to check cluster status to jutify which role.

                // get deployed cluster nodes info
                
                // join cluster and reshard
                if (FixDefs.REDIS_CLUSTER_REPLICAS > 0) {
                    String alone_master_id = cluster.getAloneMaster();
                    if (StringUtils.isNull(alone_master_id)) {
                        // join as master node
                        if (!RedisDeployUtils.joinAsMasterNode(joinIp, joinPort, sshUser, sshPasswd, sshPort, cluster, logKey, result)) return false;
                    } else {
                        // join as slave node
                        if (!RedisDeployUtils.joinAsSlaveNode(joinIp, joinPort, sshUser, sshPasswd, sshPort, cluster, logKey, result)) return false;
                    }
                }
            }
            break;
        case FixDefs.CMPT_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            deployResult = CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result);
            break;
        default:
            break;
        }

        DeployUtils.postDeployLog(deployResult, servInstID, logKey, "deploy");
        return deployResult;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;

        switch (instCmpt.getCmptName()) {
        case FixDefs.CMPT_REDIS_PROXY:
            JsonObject proxyJson = DeployUtils.getSpecifiedItem(proxyArr, instID);
            undeployResult = RedisDeployUtils.undeployProxyNode(proxyJson, false, logKey, magicKey, result);
            break;
        case FixDefs.CMPT_REDIS_NODE:
            JsonObject redisJson = DeployUtils.getSpecifiedItem(redisNodeArr, instID);
            undeployResult = RedisDeployUtils.undeployRedisNode(redisJson, true, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            undeployResult = CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        DeployUtils.postDeployLog(undeployResult, servInstID, logKey, "undeploy");
        return undeployResult;
    }

    @Override
    public boolean maintainInstance(String servInstID, String instID, String servType, InstanceOperationEnum op,
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return false;
    }

    @Override
    public boolean updateInstanceForBatch(String servInstID, String instID, String servType, boolean loadDeployFile,
            boolean rmDeployFile, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return false;
    }

    @Override
    public boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey,
            ResultBean result) {
        return false;
    }

}
