package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.MinioDeployerUtils;
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

public class MinioDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }
        
        JsonObject minioContainer = servJson.getJsonObject(FixHeader.HEADER_MINIO_CONTAINER);
        JsonArray minioArr = minioContainer.getJsonArray(FixHeader.HEADER_MINIO);
        // 部署minio服务
        String endpoints = MinioDeployerUtils.getEndpoints(minioArr);
        for (int i = 0; i < minioArr.size(); i++) {
            JsonObject minioNode = minioArr.getJsonObject(i);
            if (!MinioDeployerUtils.deployMinioNode(minioNode, endpoints, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "minio node deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // update t_meta_service.is_deployed and local cache
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
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        
        JsonObject minioContainer = servJson.getJsonObject(FixHeader.HEADER_MINIO_CONTAINER);
        JsonArray minioArr = minioContainer.getJsonArray(FixHeader.HEADER_MINIO);
        for (int i = 0; i < minioArr.size(); i++) {
            JsonObject minioNode = minioArr.getJsonObject(i);
            if (!MinioDeployerUtils.undeployMinioNode(minioNode, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "minio node undeploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // update t_meta_service is_deployed flag
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
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        StringBuilder metaCmptName = new StringBuilder();
        if (!DeployUtils.getInstCmptName(instID, metaCmptName, logKey, result)) return false;
        String cmptName = metaCmptName.toString();
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        
        JsonObject minioContainer = servJson.getJsonObject(FixHeader.HEADER_MINIO_CONTAINER);
        JsonArray minioArr = minioContainer.getJsonArray(FixHeader.HEADER_MINIO);
        String endpoints = MinioDeployerUtils.getEndpoints(minioArr);
        if (cmptName.equals(FixDefs.CMPT_MINIO)) {
            JsonObject minioNode = DeployUtils.getSelfNode(minioArr, instID);
            if (!MinioDeployerUtils.deployMinioNode(minioNode, endpoints, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "minio node deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        String successLog = String.format("instance inst_id: %s, deploy sucess ......", instID);
        DeployLog.pubSuccessLog(logKey, successLog);
        
        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        result.setRetCode(CONSTS.REVOKE_NOK);
        result.setRetInfo(CONSTS.ERR_UNSURPORT_OPERATION);
        DeployLog.pubFailLog(logKey, CONSTS.ERR_UNSURPORT_OPERATION);
        
        return false;
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
