package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.RedisDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RedisMSDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();

        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);

        if (!RedisDeployUtils.checkMasterNode(redisNodeArr, result)) { // 判断主节点是否已存在
            DeployLog.pubErrorLog(logKey, result.getRetInfo());
            return false;
        }

        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(deployFlag)) {
            if (!RedisDeployUtils.deployFakeService(redisNodeArr, result, servInstID, magicKey)) {
                DeployLog.pubFailLog(logKey, "redis deploy failed ......");
                return false;
            }

            String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        //主从节点部署判断
        for (int i = 0; i <redisNodeArr.size();i++) {
            JsonObject jsonRedisNode = redisNodeArr.getJsonObject(i);
            if (FixDefs.TYPE_REDIS_MASTER_NODE.equals(jsonRedisNode.getString(FixDefs.ATTR_NODE_TYPE))) {
                if (!RedisDeployUtils.deploySingleRedisNode(jsonRedisNode, redisNodeArr, true, version, logKey, magicKey, result)) {
                    return false;
                }
            } else {
                if (!RedisDeployUtils.deploySingleRedisNode(jsonRedisNode, redisNodeArr, false, version, logKey, magicKey, result)) {
                    return false;
                }
            }
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

        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);

        //卸载伪部署
        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(serv.getPseudoDeployFlag())) { //服务是伪部署的话
            if (!RedisDeployUtils.undeployFakeService(redisNodeArr, result, servInstID, magicKey)) {
                return false;
            }

            String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        // 2. undeploy redis nodes
        int redisNodeSize = redisNodeArr.size();
        for (int idx = 0; idx < redisNodeSize; ++idx) {
            JsonObject redisJson = redisNodeArr.getJsonObject(idx);
            if (!RedisDeployUtils.undeploySingleRedisNode(redisJson, false, logKey, magicKey, result)) {
                if (!force){
                    return false;
                }
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
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;

        switch (instCmpt.getCmptName()) {
        case FixDefs.CMPT_REDIS_NODE:
            JsonObject selfJson = RedisDeployUtils.getSelfRedisNode(redisNodeArr, instID);
            boolean isMasterNode = RedisDeployUtils.isMasterNode(selfJson);
            
            if (RedisDeployUtils.checkMasterNode(redisNodeArr, result)) {
                DeployLog.pubErrorLog(logKey, FixDefs.ERR_EXIST_MULTI_MASTER_NODE);
                return false;
            }
            
            // 主节点直接部署，启动start脚本
            // 从节点，先启动，再执行slaveof挂载到主节点上
            deployResult = RedisDeployUtils.deploySingleRedisNode(selfJson, redisNodeArr, isMasterNode, version, logKey, magicKey, result);
            break;
        case FixDefs.CMPT_COLLECTD:
            // 部署collectd服务
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
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        
        JsonObject servJson = topoResult.getServJson();
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;

        switch (instCmpt.getCmptName()) {
        case FixDefs.CMPT_REDIS_NODE:
            JsonObject selfJson = RedisDeployUtils.getSelfRedisNode(redisNodeArr, instID);

            if (RedisDeployUtils.isMasterNode(selfJson)) //不允许卸载主节点
                return false;

            undeployResult = RedisDeployUtils.undeploySingleRedisNode(selfJson, true, logKey, magicKey, result);
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
