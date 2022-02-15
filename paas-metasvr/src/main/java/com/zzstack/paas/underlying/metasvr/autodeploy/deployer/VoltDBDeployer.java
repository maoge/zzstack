package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.VoltDBDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class VoltDBDeployer implements ServiceDeployer {

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
        
        JsonObject voltdbContainer = servJson.getJsonObject(FixHeader.HEADER_VOLTDB_CONTAINER);
        JsonArray voltdbServerArr = voltdbContainer.getJsonArray(FixHeader.HEADER_VOLTDB_SERVER);
        
        String hosts = VoltDBDeployUtils.getVoltDBHosts(voltdbServerArr, logKey, result);
        String userName = serv.getUser();
        String userPasswd = serv.getPassword();
        
        // 1. deploy voltdb server
        for (int i = 0; i < voltdbServerArr.size(); ++i) {
            JsonObject voltdb = voltdbServerArr.getJsonObject(i);
            if (!VoltDBDeployUtils.deployVoltDBServer(voltdb, version, hosts, userName, userPasswd, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "voltdb-server deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 2. 创建dual表用于连接池validationQuery
        // create table dual (id varchar(48) not null primary key);
        // insert into dual values('abc');
        if (!VoltDBDeployUtils.createValidationTable(voltdbServerArr.getJsonObject(0), version, logKey, magicKey, result)) {
            DeployLog.pubFailLog(logKey, "voltdb create validation table failed ......");
            return false;
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
        String version = serv.getVersion();
        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        
        JsonObject voltdbContainer = servJson.getJsonObject(FixHeader.HEADER_VOLTDB_CONTAINER);
        JsonArray voltdbServerArr = voltdbContainer.getJsonArray(FixHeader.HEADER_VOLTDB_SERVER);
        
        // 1. stop and remove
        for (int i = 0; i < voltdbServerArr.size(); ++i) {
            JsonObject voltdb = voltdbServerArr.getJsonObject(i);
            if (!VoltDBDeployUtils.undeployVoltDBServer(voltdb, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pulsar undeploy failed ......");
                return false;
            }
        }
        
        // 2. update t_meta_instance is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }
        
        // 3. update t_meta_service is_deployed flag
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }
        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
        
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        String info = String.format("voltdb 社区版本不支持动态扩缩容");
        DeployLog.pubLog(logKey, info);
        
        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        String info = String.format("voltdb 社区版本不支持动态扩缩容");
        DeployLog.pubLog(logKey, info);
        
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
