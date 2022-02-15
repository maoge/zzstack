package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SmsGatewayDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class SMSGatewayDeployer implements ServiceDeployer {

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
        
        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(deployFlag)) {
            DeployUtils.updateSerivceDeployFlag(servInstID, FixDefs.STR_TRUE, logKey, magicKey, result);
            return true;
        }
        
        JsonObject serverContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_SERVER_CONTAINER);
        JsonObject serverExtContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_SERVER_EXT_CONTAINER);
        JsonObject processContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_PROCESS_CONTAINER);
        JsonObject clientContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_CLIENT_CONTAINER);
        JsonObject batsaveContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_BATSAVE_CONTAINER);
        JsonObject statsContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_STATS_CONTAINER);
        
		if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_SERVER, servInstID, serverContainer, logKey, magicKey, result)) {
			return false;
		}
		
		if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_SERVER_EXT, servInstID, serverExtContainer, logKey, magicKey, result)) {
            return false;
        }
        
        if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_PROCESS, servInstID, processContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_CLIENT, servInstID, clientContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_BATSAVE, servInstID, batsaveContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.deploySmsInstanceArr(FixHeader.HEADER_SMS_STATS, servInstID, statsContainer, logKey, magicKey, result)) {
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
        
        JsonObject serverContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_SERVER_CONTAINER);
        JsonObject serverExtContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_SERVER_EXT_CONTAINER);
        JsonObject processContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_PROCESS_CONTAINER);
        JsonObject clientContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_CLIENT_CONTAINER);
        JsonObject batsaveContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_BATSAVE_CONTAINER);
        JsonObject statsContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_STATS_CONTAINER);
        
        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_SERVER, serverContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_SERVER_EXT, serverExtContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_PROCESS, processContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_CLIENT, clientContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_BATSAVE, batsaveContainer, logKey, magicKey, result)) {
            return false;
        }

        if (!SmsGatewayDeployUtils.undeploySmsInstanceArr(FixHeader.HEADER_SMS_STATS, statsContainer, logKey, magicKey, result)) {
            return false;
        }
        
        DeployUtils.updateSerivceDeployFlag(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        return SmsGatewayDeployUtils.deploySmsGatewayInstance(servInstID, instID, logKey, magicKey, result);
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        return SmsGatewayDeployUtils.undeploySmsGatewayInstance(instID, logKey, magicKey, result);
    }

    @Override
    public boolean maintainInstance(String servInstID, String instID, String servType, InstanceOperationEnum op,
            boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return SmsGatewayDeployUtils.maintainInstance(servInstID, instID, servType, op, isOperateByHandle, logKey, magicKey, result);
    }

    @Override
    public boolean updateInstanceForBatch(String servInstID, String instID, String servType, boolean loadDeployFile,
            boolean rmDeployFile, boolean isOperateByHandle, String logKey, String magicKey, ResultBean result) {
        return SmsGatewayDeployUtils.updateInstanceForBatch(servInstID, instID, servType, loadDeployFile, rmDeployFile,
                isOperateByHandle, logKey, magicKey, result);
    }

    @Override
    public boolean checkInstanceStatus(String servInstID, String instID, String servType, String magicKey, ResultBean result) {
        return SmsGatewayDeployUtils.checkInstanceStatus(servInstID, instID, servType, magicKey, result);
    }

}
