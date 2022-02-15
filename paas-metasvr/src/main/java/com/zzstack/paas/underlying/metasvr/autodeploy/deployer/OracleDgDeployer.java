package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.OracleDbDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OracleDgDeployer implements ServiceDeployer {
    
    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
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


        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_TRUE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PSEUDO, magicKey)) {
            return false;
        }
        String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);

        return true;
    }

    @Override
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
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

        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }

        if (!MetaDataDao.modServicePseudoFlag(result, servInstID, CONSTS.DEPLOY_FLAG_PHYSICAL, magicKey)) {
            return false;
        }

        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);

        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {

        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
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


        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);

        //更新部署标记位
        for (int j = 0; j < dgContainer.size(); j++) {
            JsonArray orclInst = dgContainer.getJsonObject(j).getJsonArray(FixHeader.HEADER_ORCL_INSTANCE);
            for (int i = 0; i < orclInst.size(); i++) {
                JsonObject jsonOrclInst = orclInst.getJsonObject(i);
                if (instID.equals(jsonOrclInst.getString(FixHeader.HEADER_INST_ID))) {
                    if (!OracleDbDeployerUtils.deployOrclInst(jsonOrclInst, logKey, magicKey, result)) {
                        DeployLog.pubFailLog(logKey, "oracle instance start failed ......");
                        return false;
                    }
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


        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        if (DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        JsonArray dgContainer = servJson.getJsonArray(FixHeader.HEADER_DG_CONTAINER);

        //卸载服务
        for (int j = 0; j < dgContainer.size(); j++) {
            JsonArray orclInst = dgContainer.getJsonObject(j).getJsonArray(FixHeader.HEADER_ORCL_INSTANCE);
            for (int i = 0; i < orclInst.size(); i++) {
                JsonObject jsonOrclInst = orclInst.getJsonObject(i);
                if (instID.equals(jsonOrclInst.getString(FixHeader.HEADER_INST_ID))) {
                    if (!OracleDbDeployerUtils.unDeployOrclInst(jsonOrclInst, logKey, magicKey, result)) {
                        DeployLog.pubFailLog(logKey, "oracle instance undeploy failed ......");
                        return false;
                    }
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

        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);

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
