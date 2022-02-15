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

/**
 * 
 * Redis Cluster自动部署实现
 * 
 */
public class RedisClusterDeployer implements ServiceDeployer {
    
    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) return false;

        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());
        
        String version = serv.getVersion();

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) return false;

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

        //部署collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd start failed ......");
                    return false;
                }
            }
        }

        // 3. update t_service.is_deployed and local cache
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) return false;
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) return false;

        String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
        
        return true;
    }


    @Override
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) return false;
        
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());
        
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)) return false;

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
        //卸载collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd undeploy failed ......");
                    return false;
                }
            }
        }
        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) return false;
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) return false;

        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        StringBuilder metaCmptName = new StringBuilder();
        if (!DeployUtils.getInstCmptName(instID, metaCmptName, logKey, result)) return false;
        String cmptName = metaCmptName.toString();

        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) return false;
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getServType();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        // JsonObject collectdJson = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);
        JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);
        
        StringBuilder node4cluster = new StringBuilder();
        StringBuilder nodes4proxy = new StringBuilder();
        if (!RedisDeployUtils.getClusterNodes(redisNodeArr, node4cluster, nodes4proxy, logKey, result)) return false;

        if (cmptName.equals(FixDefs.CMPT_REDIS_PROXY)) {
            int proxySize = proxyArr.size();
            for (int idx = 0; idx < proxySize; ++idx) {
                JsonObject proxyJson = proxyArr.getJsonObject(idx);
                String curr_id = proxyJson.getString(FixHeader.HEADER_INST_ID);
                if (!curr_id.equals(instID)) continue;

                if (!RedisDeployUtils.deployProxyNode(proxyJson, nodes4proxy.toString(), logKey, magicKey, result))
                    return false;
            }
        } else if (cmptName.equals(FixDefs.CMPT_REDIS_NODE)) {
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
        } else if (cmptName.equals(FixDefs.CMPT_COLLECTD)) {
            String collectdId = collectd.getString(FixHeader.HEADER_INST_ID);
            
            if (collectdId.equals(instID)) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd start failed ......");
                    return false;
                }
            }
        }

        String successLog = String.format("instance inst_id: %s, deploy sucess ......", instID);
        DeployLog.pubSuccessLog(logKey, successLog);
        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        
        StringBuilder metaCmptName = new StringBuilder();
        if (!DeployUtils.getInstCmptName(instID, metaCmptName, logKey, result)) return false;
        String cmptName = metaCmptName.toString();

        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) return false;

        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        // JsonObject collectdJson = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonObject proxyContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_PROXY_CONTAINER);

        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        JsonArray proxyArr = proxyContainer.getJsonArray(FixHeader.HEADER_REDIS_PROXY);

        if (cmptName.equals(FixDefs.CMPT_REDIS_PROXY)) {
            int proxySize = proxyArr.size();
            for (int idx = 0; idx < proxySize; ++idx) {
                JsonObject proxyJson = proxyArr.getJsonObject(idx);
                String curr_id = proxyJson.getString(FixHeader.HEADER_INST_ID);
                if (!curr_id.equals(instID)) continue;

                if (!RedisDeployUtils.undeployProxyNode(proxyJson, false, logKey, magicKey, result)) return false;
                // TODO notify clients ......
            }
        } else if (cmptName.equals(FixDefs.CMPT_REDIS_NODE)) {
            int nodeSize = redisNodeArr.size();
            for (int i = 0; i < nodeSize; ++i) {
                JsonObject redisJson = redisNodeArr.getJsonObject(i);
                String currId = redisJson.getString(FixHeader.HEADER_INST_ID);
                if (!currId.equals(instID)) continue;

                if (!RedisDeployUtils.undeployRedisNode(redisJson, true, logKey, magicKey, result)) return false;
            }
        }
        //卸载collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd undeploy failed ......");
                    return false;
                }
            }
        }
        
        return true;
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
