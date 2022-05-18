package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.OracleDbDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
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

public class OracleDgDeployer implements ServiceDeployer {
    
    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);

        //更新部署标记位
        for (int j = 0; j < dgContainer.size(); j++) {
            JsonArray orclInst = dgContainer.getJsonObject(j).getJsonArray(FixHeader.HEADER_ORCL_INSTANCE);
            for (int i = 0; i < orclInst.size(); i++) {
                JsonObject jsonOrclInst = orclInst.getJsonObject(i);
                if (!OracleDbDeployerUtils.deployOrclInst(jsonOrclInst, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "oracle instance start failed ......");
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
        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);
        
        //卸载服务
        for (int j = 0; j < dgContainer.size(); j++) {
            JsonArray orclInst = dgContainer.getJsonObject(j).getJsonArray(FixHeader.HEADER_ORCL_INSTANCE);
            for (int i = 0; i < orclInst.size(); i++) {
                JsonObject jsonOrclInst = orclInst.getJsonObject(i);
                if (!OracleDbDeployerUtils.unDeployOrclInst(jsonOrclInst, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "oracle instance undeploy failed ......");
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

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PHYSICAL, magicKey)) {
            return false;
        }
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
        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = true;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (collectd != null && !collectd.isEmpty()) {
                deployResult = CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result);
            }
            break;
        case FixHeader.HEADER_ORCL_INSTANCE:
            JsonObject orclInst = DeployUtils.getSpecifiedOrclInst(dgContainer, instID);
            deployResult = OracleDbDeployerUtils.deployOrclInst(orclInst, logKey, magicKey, result);
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
        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = true;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (collectd != null && !collectd.isEmpty()) {
                undeployResult = CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result);
            }
            break;
        case FixHeader.HEADER_ORCL_INSTANCE:
            JsonObject orclInst = DeployUtils.getSpecifiedOrclInst(dgContainer, instID);
            undeployResult = OracleDbDeployerUtils.unDeployOrclInst(orclInst, logKey, magicKey, result);
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
