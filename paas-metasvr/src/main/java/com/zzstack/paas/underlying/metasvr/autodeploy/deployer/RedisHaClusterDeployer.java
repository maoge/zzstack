package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class RedisHaClusterDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
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
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
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
        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
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
