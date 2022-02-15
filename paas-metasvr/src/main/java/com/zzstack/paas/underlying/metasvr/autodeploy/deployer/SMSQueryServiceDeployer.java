package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SmsQueryServiceDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SMSQueryServiceDeployer implements ServiceDeployer {

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
        
        DeployUtils.updateSerivceDeployFlag(servInstID, FixDefs.STR_TRUE, logKey, magicKey, result);
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
        
        DeployUtils.updateSerivceDeployFlag(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance instance = cmptMeta.getInstance(instID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(instance.getCmptId());
        boolean res = false;
        
        JsonObject instItem = new JsonObject();
        MetaDataDao.loadInstanceMeta(instItem, instID);
        String version = DeployUtils.getServiceVersion(servInstID, instID);
        
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_NGX:
            String servList = SmsQueryServiceDeployUtils.getSmsQueryServList(servInstID);
            res = SmsQueryServiceDeployUtils.deployNgxNode(instItem, servList, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            res = SmsQueryServiceDeployUtils.deploySmsQueryNode(instItem, version, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        return res;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance instance = cmptMeta.getInstance(instID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(instance.getCmptId());
        boolean res = false;
        
        JsonObject instItem = new JsonObject();
        MetaDataDao.loadInstanceMeta(instItem, instID);
        String version = DeployUtils.getServiceVersion(servInstID, instID);
        
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_NGX:
            res = SmsQueryServiceDeployUtils.undeployNgxNode(instItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            res = SmsQueryServiceDeployUtils.undeploySmsQueryNode(instItem, version, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        return res;
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
