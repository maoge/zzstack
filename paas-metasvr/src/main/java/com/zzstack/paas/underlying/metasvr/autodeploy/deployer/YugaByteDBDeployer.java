package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.YugaByteDBDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
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
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
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
        
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
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
        JsonObject ybMasterContainer = servJson.getJsonObject(FixHeader.HEADER_YB_MASTER_CONTAINER);
        JsonObject ybTServerContainer = servJson.getJsonObject(FixHeader.HEADER_YB_TSERVER_CONTAINER);
        
        JsonArray ybMasterArr = ybMasterContainer.getJsonArray(FixHeader.HEADER_YB_MASTER);
        JsonArray ybTServerArr = ybTServerContainer.getJsonArray(FixHeader.HEADER_YB_TSERVER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
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
        
        DeployUtils.postDeployLog(undeployResult, servInstID, logKey, "undeploy");
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
