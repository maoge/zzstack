package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.TiDBDeployerUtils;
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

public class TiDBDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
        // 部署pd-server服务
        JsonObject pdContainer = servJson.getJsonObject(FixHeader.HEADER_PD_SERVER_CONTAINER);
        JsonArray pdArr = pdContainer.getJsonArray(FixHeader.HEADER_PD_SERVER);
        String pdLongAddrList = TiDBDeployerUtils.getPDLongAddress(pdArr);
        for (int i = 0; i < pdArr.size(); i++) {
            JsonObject jsonPdServer = pdArr.getJsonObject(i);
            if (!TiDBDeployerUtils.deployPdServer(jsonPdServer, version, pdLongAddrList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pd-server deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }

        // 部署tikv-server服务
        JsonObject tikvServerContainer = servJson.getJsonObject(FixHeader.HEADER_TIKV_SERVER_CONTAINER);
        JsonArray tikvServer = tikvServerContainer.getJsonArray(FixHeader.HEADER_TIKV_SERVER);
        String pdShortAddrList = TiDBDeployerUtils.getPDShortAddress(pdArr);
        for (int i = 0; i < tikvServer.size(); i++) {
            JsonObject jsonTikvServer = tikvServer.getJsonObject(i);
            if (!TiDBDeployerUtils.deployTikvServer(jsonTikvServer, version, pdShortAddrList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "tikv-server deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }

        // 部署tidb-server服务
        JsonObject tidbServerContainer = servJson.getJsonObject(FixHeader.HEADER_TIDB_SERVER_CONTAINER);
        JsonArray tidbServer = tidbServerContainer.getJsonArray(FixHeader.HEADER_TIDB_SERVER);
        for (int i = 0; i < tidbServer.size(); i++) {
            JsonObject jsonTidbServer = tidbServer.getJsonObject(i);
            if (!TiDBDeployerUtils.deployTidbServer(jsonTidbServer, version, pdShortAddrList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "tidb-server deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // tidb服务部署完成修改root密码,默认密码为空串
        DeployUtils.resetDBPwd(tidbServer.getJsonObject(0), logKey, result);
        
        // 部署dashboard-proxy
        JsonObject dashboardProxy = servJson.getJsonObject(FixHeader.HEADER_DASHBOARD_PROXY);
        if (dashboardProxy != null && !dashboardProxy.isEmpty()) {
            String pdAddress = TiDBDeployerUtils.getFirstPDAddress(pdArr);
            if (!TiDBDeployerUtils.deployDashboardProxy(dashboardProxy, version, pdAddress, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "dashboard proxy deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
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
        String version = topoResult.getVersion();
        
        // 卸载dashboard-proxy
        JsonObject dashboardProxy = servJson.getJsonObject(FixHeader.HEADER_DASHBOARD_PROXY);
        if (dashboardProxy != null && !dashboardProxy.isEmpty()) {
            if (!TiDBDeployerUtils.undeployDashboardProxy(dashboardProxy, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "dashboard-proxy undeploy failed ......");
                return false;
            }
        }

        // 卸载tidb-server服务
        JsonObject tidbServerContainer = servJson.getJsonObject(FixHeader.HEADER_TIDB_SERVER_CONTAINER);
        JsonArray tidbServer = tidbServerContainer.getJsonArray(FixHeader.HEADER_TIDB_SERVER);
        for (int i = 0; i < tidbServer.size(); i++) {
            JsonObject jsonTidbServer = tidbServer.getJsonObject(i);
            if (!TiDBDeployerUtils.undeployTidbServer(jsonTidbServer, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "tidb-server undeploy failed ......");
                return false;
            }
        }

        JsonObject pdServerContainer = servJson.getJsonObject(FixHeader.HEADER_PD_SERVER_CONTAINER);
        JsonArray pdServer = pdServerContainer.getJsonArray(FixHeader.HEADER_PD_SERVER);

        // 卸载tikv-server服务
        JsonObject tikvServerContainer = servJson.getJsonObject(FixHeader.HEADER_TIKV_SERVER_CONTAINER);
        JsonArray tikvServer = tikvServerContainer.getJsonArray(FixHeader.HEADER_TIKV_SERVER);
        JsonObject jsonPdCtl = pdServer.getJsonObject(0);
        for (int i = 0; i < tikvServer.size(); i++) {
            JsonObject jsonTikvServer = tikvServer.getJsonObject(i);
            if (!TiDBDeployerUtils.undeployTikvServer(jsonTikvServer, jsonPdCtl, version, true, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "tikv-server undeploy failed ......");
                return false;
            }
        }

        // 卸载pd-server服务
        for (int i = 0; i < pdServer.size(); i++) {
            JsonObject jsonPdServer = pdServer.getJsonObject(i);
            if (!TiDBDeployerUtils.undeployPdServer(jsonPdServer, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pd-server undeploy failed ......");
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
        
        JsonObject pdContainer = servJson.getJsonObject(FixHeader.HEADER_PD_SERVER_CONTAINER);
        JsonObject tikvContainer = servJson.getJsonObject(FixHeader.HEADER_TIKV_SERVER_CONTAINER);
        JsonObject tidbContainer = servJson.getJsonObject(FixHeader.HEADER_TIDB_SERVER_CONTAINER);
        JsonObject dashboardProxy = servJson.getJsonObject(FixHeader.HEADER_DASHBOARD_PROXY);
        
        JsonArray pdArr = pdContainer.getJsonArray(FixHeader.HEADER_PD_SERVER);
        JsonArray tikvArr = tikvContainer.getJsonArray(FixHeader.HEADER_TIKV_SERVER);
        JsonArray tidbArr = tidbContainer.getJsonArray(FixHeader.HEADER_TIDB_SERVER);
        
        String pdShortAddrList = TiDBDeployerUtils.getPDShortAddress(pdArr);
        String pdLongAddrList = TiDBDeployerUtils.getPDLongAddress(pdArr);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        
        boolean deployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_TIDB_SERVER:
            JsonObject tidbItem = DeployUtils.getSpecifiedItem(tidbArr, instID);
            deployResult = TiDBDeployerUtils.deployTidbServer(tidbItem, version, pdShortAddrList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_TIKV_SERVER:
            JsonObject tikvItem = DeployUtils.getSpecifiedItem(tikvArr, instID);
            deployResult = TiDBDeployerUtils.deployTikvServer(tikvItem, version, pdShortAddrList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PD_SERVER:
            JsonObject pdItem = DeployUtils.getSpecifiedItem(pdArr, instID);
            deployResult = TiDBDeployerUtils.deployPdServer(pdItem, version, pdLongAddrList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_DASHBOARD_PROXY:
            String pdAddress = TiDBDeployerUtils.getFirstPDAddress(pdArr);
            deployResult = TiDBDeployerUtils.deployDashboardProxy(dashboardProxy, version, pdAddress, logKey, magicKey, result);
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
        String version = topoResult.getVersion();
        
        JsonObject pdContainer = servJson.getJsonObject(FixHeader.HEADER_PD_SERVER_CONTAINER);
        JsonObject tikvContainer = servJson.getJsonObject(FixHeader.HEADER_TIKV_SERVER_CONTAINER);
        JsonObject tidbContainer = servJson.getJsonObject(FixHeader.HEADER_TIDB_SERVER_CONTAINER);
        JsonObject dashboardProxy = servJson.getJsonObject(FixHeader.HEADER_DASHBOARD_PROXY);
        
        JsonArray pdArr = pdContainer.getJsonArray(FixHeader.HEADER_PD_SERVER);
        JsonArray tikvArr = tikvContainer.getJsonArray(FixHeader.HEADER_TIKV_SERVER);
        JsonArray tidbArr = tidbContainer.getJsonArray(FixHeader.HEADER_TIDB_SERVER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        
        boolean undeployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_TIDB_SERVER:
            JsonObject tidbItem = DeployUtils.getSpecifiedItem(tidbArr, instID);
            undeployResult = TiDBDeployerUtils.undeployTidbServer(tidbItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_TIKV_SERVER:
            JsonObject tikvItem = DeployUtils.getSpecifiedItem(tikvArr, instID);
            JsonObject jsonPdCtl = pdArr.getJsonObject(0);
            undeployResult = TiDBDeployerUtils.undeployTikvServer(tikvItem, jsonPdCtl, version, false, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PD_SERVER:
            JsonObject pdItem = DeployUtils.getSpecifiedItem(pdArr, instID);
            undeployResult = TiDBDeployerUtils.undeployPdServer(pdItem, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_DASHBOARD_PROXY:
            undeployResult = TiDBDeployerUtils.undeployDashboardProxy(dashboardProxy, version, logKey, magicKey, result);
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
