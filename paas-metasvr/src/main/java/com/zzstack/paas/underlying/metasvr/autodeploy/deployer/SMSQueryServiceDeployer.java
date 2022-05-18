package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SmsQueryServiceDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SMSQueryServiceDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        
        JsonObject servJson = topoResult.getServJson();
        JsonObject ngxContainer = servJson.getJsonObject(FixHeader.HEADER_NGX_CONTAINER);
        JsonObject smsQueryContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_QUERY_CONTAINER);
        String servList = SmsQueryServiceDeployUtils.getSmsQueryServList(FixHeader.HEADER_SMS_QUERY, smsQueryContainer);
        
        String ngxContainerId = ngxContainer.getString(FixHeader.HEADER_INST_ID);
        String smsQueryContainerId = smsQueryContainer.getString(FixHeader.HEADER_INST_ID);
        
        JsonArray ngxArr = ngxContainer.getJsonArray(FixHeader.HEADER_NGX);
        JsonArray smsQueryArr = smsQueryContainer.getJsonArray(FixHeader.HEADER_SMS_QUERY);
        
        if (!SmsQueryServiceDeployUtils.deploySmsQueryArr(servInstID, smsQueryContainerId, smsQueryArr, logKey, magicKey, result)) {
            return false;
        }
        
        if (!SmsQueryServiceDeployUtils.deployNgxArr(servInstID, ngxContainerId, ngxArr, servList, logKey, magicKey, result)) {
            return false;
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
        JsonObject ngxContainer = servJson.getJsonObject(FixHeader.HEADER_NGX_CONTAINER);
        JsonObject smsQueryContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_QUERY_CONTAINER);
        
        String ngxContainerId = ngxContainer.getString(FixHeader.HEADER_INST_ID);
        String smsQueryContainerId = smsQueryContainer.getString(FixHeader.HEADER_INST_ID);
        
        JsonArray ngxArr = ngxContainer.getJsonArray(FixHeader.HEADER_NGX);
        JsonArray smsQueryArr = smsQueryContainer.getJsonArray(FixHeader.HEADER_SMS_QUERY);
        
        if (!SmsQueryServiceDeployUtils.undeployNgxArr(servInstID, ngxContainerId, ngxArr, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsQueryServiceDeployUtils.undeploySmsQueryArr(servInstID, smsQueryContainerId, smsQueryArr, logKey, magicKey, result)) {
            return false;
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
        
        JsonObject ngxContainer = servJson.getJsonObject(FixHeader.HEADER_NGX_CONTAINER);
        JsonObject smsQueryContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_QUERY_CONTAINER);
        
        JsonArray ngxArr = ngxContainer.getJsonArray(FixHeader.HEADER_NGX);
        JsonArray smsQueryArr = smsQueryContainer.getJsonArray(FixHeader.HEADER_SMS_QUERY);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_NGX:
            String servList = SmsQueryServiceDeployUtils.getSmsQueryServList(servInstID);
            JsonObject ngx = DeployUtils.getSpecifiedItem(ngxArr, instID);
            deployResult = SmsQueryServiceDeployUtils.deployNgxNode(ngx, servList, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            JsonObject smsQry = DeployUtils.getSpecifiedItem(smsQueryArr, instID);
            deployResult = SmsQueryServiceDeployUtils.deploySmsQueryNode(smsQry, version, logKey, magicKey, result);
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
        String version = topoResult.getVersion();
        
        JsonObject ngxContainer = servJson.getJsonObject(FixHeader.HEADER_NGX_CONTAINER);
        JsonObject smsQueryContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_QUERY_CONTAINER);
        
        JsonArray ngxArr = ngxContainer.getJsonArray(FixHeader.HEADER_NGX);
        JsonArray smsQueryArr = smsQueryContainer.getJsonArray(FixHeader.HEADER_SMS_QUERY);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_NGX:
            JsonObject ngx = DeployUtils.getSpecifiedItem(ngxArr, instID);
            undeployResult = SmsQueryServiceDeployUtils.undeployNgxNode(ngx, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            JsonObject smsQry = DeployUtils.getSpecifiedItem(smsQueryArr, instID);
            undeployResult = SmsQueryServiceDeployUtils.undeploySmsQueryNode(smsQry, version, logKey, magicKey, result);
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
        return SmsQueryServiceDeployUtils.maintainInstance(servInstID, instID, servType, op, logKey, magicKey, result);
    }

    @Override
    public boolean updateInstanceForBatch(String servInstID, String instID, String servType, boolean loadDeployFile,
            boolean rmDeployFile, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return SmsQueryServiceDeployUtils.updateInstanceForBatch(servInstID, instID, servType, loadDeployFile, rmDeployFile,
                logKey, magicKey, result);
    }

    @Override
    public boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey,
            ResultBean result) {
        return SmsQueryServiceDeployUtils.checkInstanceStatus(servInstID, instID, servType, magicKey, result);
    }

}
