package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.MinioDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
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

public class MinioDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
        JsonObject minioContainer = servJson.getJsonObject(FixHeader.HEADER_MINIO_CONTAINER);
        JsonArray minioArr = minioContainer.getJsonArray(FixHeader.HEADER_MINIO);
        // 部署minio服务
        String endpoints = MinioDeployerUtils.getEndpoints(minioArr);
        // endpoints += "2>./log/stderr.log 1>./log/stdout.log &";
        endpoints = endpoints.replaceAll("/", "\\\\/");//.replaceAll("\n", "\\\\\n");
        
        for (int i = 0; i < minioArr.size(); i++) {
            JsonObject minioNode = minioArr.getJsonObject(i);
            if (!MinioDeployerUtils.deployMinioNode(minioNode, endpoints, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "minio node deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // update deploy flag and local cache
        DeployUtils.postProc(servInstID, FixDefs.STR_TRUE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
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
        
        // update deploy flag and local cache
        DeployUtils.postProc(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, false, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;
        
        JsonObject minioContainer = servJson.getJsonObject(FixHeader.HEADER_MINIO_CONTAINER);
        JsonArray minioArr = minioContainer.getJsonArray(FixHeader.HEADER_MINIO);
        String endpoints = MinioDeployerUtils.getEndpoints(minioArr);
        switch (instCmpt.getCmptName()) {
        case FixDefs.CMPT_MINIO:
            JsonObject minioNode = DeployUtils.getSelfNode(minioArr, instID);
            deployResult = MinioDeployerUtils.deployMinioNode(minioNode, endpoints, version, logKey, magicKey, result);
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
