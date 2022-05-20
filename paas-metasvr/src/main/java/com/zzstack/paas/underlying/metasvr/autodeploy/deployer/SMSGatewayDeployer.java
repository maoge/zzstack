package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.SmsGatewayDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonObject;

public class SMSGatewayDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        
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
        
        // update deploy flag and local cache
        return DeployUtils.postProc(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
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
