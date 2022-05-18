package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.VoltDBDeployUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class VoltDBDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
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
        String version = topoResult.getVersion();
        
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
        
        // update deploy flag and local cache
        DeployUtils.postProc(servInstID, FixDefs.STR_FALSE, logKey, magicKey, result);
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
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
