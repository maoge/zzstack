package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.ApiSixDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.YugaByteDBDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
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

public class ApiSixDeployer implements ServiceDeployer {

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
        
        JsonObject etcdContainer = servJson.getJsonObject(FixHeader.HEADER_ETCD_CONTAINER);
        String etcdContainerInstId = etcdContainer.getString(FixHeader.HEADER_INST_ID);
        JsonArray etcdNodeArr = etcdContainer.getJsonArray(FixHeader.HEADER_ETCD);
        
        JsonObject apiSixNodeContainer = servJson.getJsonObject(FixHeader.HEADER_APISIX_CONTAINER);
        JsonArray apiSixNodeArr = apiSixNodeContainer.getJsonArray(FixHeader.HEADER_APISIX_SERVER);
        String apiSixInstantId = apiSixNodeContainer.getString(FixHeader.HEADER_INST_ID);
        
        if (!DeployUtils.etcdCheckBeforeDeploy(serv, etcdNodeArr, apiSixNodeArr, logKey)) {
            return false;
        }
        
        // 1、先部署etcd
        String etcdLongAddr = DeployUtils.getEtcdLongAddr(etcdNodeArr);
        String etcdShortAddr = DeployUtils.getEtcdShortAddr(etcdNodeArr);
        String etcdFullAddr = DeployUtils.getEtcdFullAddr(etcdNodeArr);
        
        for (int i = 0; i < etcdNodeArr.size(); ++i) {
            JsonObject jsonEtcdNode = etcdNodeArr.getJsonObject(i);
            if (!DeployUtils.deployEtcdNode(jsonEtcdNode, etcdFullAddr, etcdContainerInstId, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 2、再部署apisix
        for (int i = 0; i < apiSixNodeArr.size(); ++i) {
            JsonObject jsonApiSixNode = apiSixNodeArr.getJsonObject(i);
            if (!ApiSixDeployUtils.deployApiSixNode(jsonApiSixNode, apiSixInstantId, etcdLongAddr,
                    etcdShortAddr, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }

        // 部署prometheus
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        if (prometheus != null) {
            String apisixMetricList = ApiSixDeployUtils.getApisixMetricList(apiSixNodeArr);
            if (!DeployUtils.deployPrometheus(prometheus, servInstID, apisixMetricList, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "prometheus deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署监控组件
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        if (grafana != null) {
            if (!DeployUtils.deployGrafana(grafana, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "grafana deploy failed ......");
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
        String version = topoResult.getVersion();
        
        // 1. undeploy grafana and prometheus
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        if (grafana != null) {
            if (!DeployUtils.undeployGrafana(grafana, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "grafana undeploy failed ......");
                return false;
            }
        }
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        if (prometheus != null) {
            if (!DeployUtils.undeployPrometheus(prometheus, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "prometheus undeploy failed ......");
                return false;
            }
        }
        
        // 2. undeploy apisix nodes
        JsonObject apiSixNodeContainer = servJson.getJsonObject(FixHeader.HEADER_APISIX_CONTAINER);
        JsonArray apiSixNodeArr = apiSixNodeContainer.getJsonArray(FixHeader.HEADER_APISIX_SERVER);
        for (int i = 0; i < apiSixNodeArr.size(); i++) {
            JsonObject jsonApiSixNode = apiSixNodeArr.getJsonObject(i);
            if (!ApiSixDeployUtils.undeployApiSixNode(jsonApiSixNode, version, logKey, magicKey, result)) {
                String info = String.format("service inst_id: %s, undeploy fail ......", servInstID);
                DeployLog.pubFailLog(logKey, info);
            }
        }
        
        // 3. undeploy etcd nodes
        JsonObject etcdContainer = servJson.getJsonObject(FixHeader.HEADER_ETCD_CONTAINER);
        JsonArray etcdNodeArr = etcdContainer.getJsonArray(FixHeader.HEADER_ETCD);
        int etcdNodeSize = etcdNodeArr.size();
        for (int idx = 0; idx < etcdNodeSize; ++idx) {
            JsonObject jsonEtcdNode = etcdNodeArr.getJsonObject(idx);
            if (!DeployUtils.undeployEtcdNode(jsonEtcdNode, logKey, magicKey, result)) {
                if (!force) {
                    return false;
                }
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
        
        JsonObject apiSixNodeContainer = servJson.getJsonObject(FixHeader.HEADER_APISIX_CONTAINER);
        JsonArray apiSixNodeArr = apiSixNodeContainer.getJsonArray(FixHeader.HEADER_APISIX_SERVER);
        
        JsonObject etcdContainer = servJson.getJsonObject(FixHeader.HEADER_ETCD_CONTAINER);
        String etcdContainerInstId = etcdContainer.getString(FixHeader.HEADER_INST_ID);
        JsonArray etcdNodeArr = etcdContainer.getJsonArray(FixHeader.HEADER_ETCD);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        String etcdLongAddr = DeployUtils.getEtcdLongAddr(etcdNodeArr);
        String etcdShortAddr = DeployUtils.getEtcdShortAddr(etcdNodeArr);
        String etcdFullAddr = DeployUtils.getEtcdFullAddr(etcdNodeArr);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_APISIX_SERVER:
            JsonObject apiSixNode = DeployUtils.getSpecifiedItem(apiSixNodeArr, instID);
            deployResult = ApiSixDeployUtils.deployApiSixNode(apiSixNode, servInstID, etcdLongAddr, etcdShortAddr, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ETCD:
            JsonObject etcdNode = DeployUtils.getSpecifiedItem(etcdNodeArr, instID);
            deployResult = DeployUtils.deployEtcdNode(etcdNode, etcdFullAddr, etcdContainerInstId, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PROMETHEUS:
            String apisixMetricList = ApiSixDeployUtils.getApisixMetricList(apiSixNodeArr);
            deployResult = DeployUtils.deployPrometheus(prometheus, servInstID, apisixMetricList, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_GRAFANA:
            deployResult = DeployUtils.deployGrafana(grafana, version, logKey, magicKey, result);
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
        
        JsonObject apiSixNodeContainer = servJson.getJsonObject(FixHeader.HEADER_APISIX_CONTAINER);
        JsonArray apiSixNodeArr = apiSixNodeContainer.getJsonArray(FixHeader.HEADER_APISIX_SERVER);
        
        JsonObject etcdContainer = servJson.getJsonObject(FixHeader.HEADER_ETCD_CONTAINER);
        JsonArray etcdNodeArr = etcdContainer.getJsonArray(FixHeader.HEADER_ETCD);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_APISIX_SERVER:
            JsonObject apiSixNode = DeployUtils.getSpecifiedItem(apiSixNodeArr, instID);
            undeployResult = ApiSixDeployUtils.undeployApiSixNode(apiSixNode, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ETCD:
            JsonObject etcdNode = DeployUtils.getSpecifiedItem(etcdNodeArr, instID);
            undeployResult = DeployUtils.undeployEtcdNode(etcdNode, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PROMETHEUS:
            undeployResult = DeployUtils.undeployPrometheus(prometheus, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_GRAFANA:
            undeployResult = DeployUtils.undeployGrafana(grafana, version, logKey, magicKey, result);
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
