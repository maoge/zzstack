package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.RedisDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
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

public class RedisMSDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)){
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }

        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        
        String version = serv.getVersion();

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

        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);

        return true;
    }


    @Override
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)){
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)){
            return false;
        }


        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);


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

        // 3. update zz_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);

        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        StringBuilder metaCmptName = new StringBuilder();
        if (!DeployUtils.getInstCmptName(instID, metaCmptName, logKey, result)) {
            return false;
        }
        String cmptName = metaCmptName.toString();

        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);
        
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        if (cmptName.equals(FixDefs.CMPT_REDIS_NODE)) {
            JsonObject selfJson = RedisDeployUtils.getSelfRedisNode(redisNodeArr, instID);
            boolean isMasterNode = RedisDeployUtils.isMasterNode(selfJson);
            
            if (RedisDeployUtils.checkMasterNode(redisNodeArr, result)) {
                DeployLog.pubErrorLog(logKey, FixDefs.ERR_EXIST_MULTI_MASTER_NODE);
                return false;
            }
            
            // 主节点直接部署，启动start脚本
            // 从节点，先启动，再执行slaveof挂载到主节点上
            if (!RedisDeployUtils.deploySingleRedisNode(selfJson, redisNodeArr, isMasterNode, version, logKey, magicKey, result)) {
                return false;
            }
        } else if (cmptName.equals(FixDefs.CMPT_COLLECTD)) {
            // 部署collectd服务
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
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
        JsonObject nodeContainer = servJson.getJsonObject(FixHeader.HEADER_REDIS_NODE_CONTAINER);
        JsonArray redisNodeArr = nodeContainer.getJsonArray(FixHeader.HEADER_REDIS_NODE);

        if (cmptName.equals(FixDefs.CMPT_REDIS_NODE)) {
            JsonObject selfJson = RedisDeployUtils.getSelfRedisNode(redisNodeArr, instID);
            boolean isMasterNode = RedisDeployUtils.isMasterNode(selfJson);

            if (isMasterNode) //不允许卸载主节点
                return false;

            if (!RedisDeployUtils.undeploySingleRedisNode(selfJson, true, logKey, magicKey, result))
                return false;
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
