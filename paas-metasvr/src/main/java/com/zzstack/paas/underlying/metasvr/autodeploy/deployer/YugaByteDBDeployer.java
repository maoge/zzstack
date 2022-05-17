package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.YugaByteDBDeployerUtils;
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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class YugaByteDBDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) return false;

        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());
        
        String version = serv.getVersion();

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) return false;
        
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        String masterList = YugaByteDBDeployerUtils.getYbMasterList(ybMasterArr, magicKey, result);
        
        // 1. deploy yb-master
        for (int i = 0; i < ybMasterArr.size(); ++i) {
            JsonObject ybMaster = ybMasterArr.getJsonObject(i);
            if (!YugaByteDBDeployerUtils.deployMaster(ybMaster, version, masterList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "yb-master deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        // 2. deploy yb-tserver
        for (int i = 0; i < ybTServerArr.size(); ++i) {
            JsonObject ybTServer = ybTServerArr.getJsonObject(i);
            if (!YugaByteDBDeployerUtils.deployTServer(ybTServer, version, masterList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "yb-tserver deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 3. update t_meta_service.is_deployed and local cache
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
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        
        // 1. undeploy yb-master
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        for (int i = 0; i < ybMasterArr.size(); ++i) {
            JsonObject ybMaster = ybMasterArr.getJsonObject(i);
            if (!YugaByteDBDeployerUtils.undeployMaster(ybMaster, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "yb-master undeploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 2. undeploy yb-tserver
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        for (int i = 0; i < ybTServerArr.size(); ++i) {
            JsonObject ybTServer = ybTServerArr.getJsonObject(i);
            if (!YugaByteDBDeployerUtils.undeployTServer(ybTServer, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "yb-tserver undeploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 3. update t_meta_service is_deployed flag
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
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance servInst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt servCmpt = cmptMeta.getCmptById(servInst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(servCmpt.getCmptName());
        
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        String masterList = YugaByteDBDeployerUtils.getYbMasterList(ybMasterArr, magicKey, result);
        
        boolean deployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_YB_MASTER:
            JsonObject ybMasterItem = DeployUtils.getSpecifiedItem(ybMasterArr, instID);
            deployResult = YugaByteDBDeployerUtils.deployMaster(ybMasterItem, version, masterList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_YB_TSERVER:
            JsonObject ybTServerItem = DeployUtils.getSpecifiedItem(ybTServerArr, instID);
            deployResult = YugaByteDBDeployerUtils.deployTServer(ybTServerItem, version, masterList, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        if (deployResult) {
            String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("service inst_id:%s, deploy failed ......", servInstID);
            DeployLog.pubFailLog(logKey, info);
            DeployLog.pubFailLog(logKey, result.getRetInfo());
        }
        return deployResult;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance servInst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(servInst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        
        boolean undeployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_YB_MASTER:
            JsonObject ybMasterItem = DeployUtils.getSpecifiedItem(ybMasterArr, instID);
            undeployResult = YugaByteDBDeployerUtils.undeployMaster(ybMasterItem, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_YB_TSERVER:
            JsonObject ybTServerItem = DeployUtils.getSpecifiedItem(ybTServerArr, instID);
            undeployResult = YugaByteDBDeployerUtils.undeployTServer(ybTServerItem, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        if (undeployResult) {
            String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("service inst_id: %s, undeploy fail ......", servInstID);
            DeployLog.pubFailLog(logKey, info);
        }
        return undeployResult;
    }

    @Override
    public boolean maintainInstance(String servInstID, String instID, String servType, InstanceOperationEnum op,
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return true;
    }

    @Override
    public boolean updateInstanceForBatch(String servInstID, String instID, String servType, boolean loadDeployFile,
            boolean rmDeployFile, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return true;
    }

    @Override
    public boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey,
            ResultBean result) {
        return true;
    }

}
