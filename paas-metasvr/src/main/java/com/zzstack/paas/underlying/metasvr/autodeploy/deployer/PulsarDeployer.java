package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.PulsarDeployerUtils;
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

public class PulsarDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        // 部署zookeeper服务
        String zkAddrList = DeployUtils.getZKAddress(zkArr);
        for (int i = 0; i < zkArr.size(); i++) {
            JsonObject zk = zkArr.getJsonObject(i);
            if (!DeployUtils.deployZKNode(zk, (i + 1), version, zkAddrList, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "zookeeper deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署bookkeeper服务
        JsonObject bookieContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BOOKKEEPER_CONTAINER);
        JsonArray bookieArr = bookieContainer.getJsonArray(FixHeader.HEADER_PULSAR_BOOKKEEPER);
        String zkShortAddress = DeployUtils.getZKShortAddress(zkArr);
        for (int i = 0; i < bookieArr.size(); i++) {
            JsonObject bookie = bookieArr.getJsonObject(i);
            boolean initMeta = i == 0;
            if (!PulsarDeployerUtils.deployBookie(bookie, version, zkShortAddress, logKey, magicKey, initMeta, result)) {
                DeployLog.pubFailLog(logKey, "bookkeeper deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署pulsar broker服务
        JsonObject pulsarContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BROKER_CONTAINER);
        JsonArray pulsarArr = pulsarContainer.getJsonArray(FixHeader.HEADER_PULSAR_BROKER);
        String pulsarClusterName = pulsarContainer.getString(FixHeader.HEADER_INST_ID);
        String brokerAddrList = PulsarDeployerUtils.getPulsarBrokerList(pulsarArr, null);
        for (int i = 0; i < pulsarArr.size(); i++) {
            JsonObject pulsar = pulsarArr.getJsonObject(i);
            boolean initMeta = i == 0;
            if (!PulsarDeployerUtils.deployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, zkShortAddress,
                    logKey, magicKey, initMeta, result)) {
                DeployLog.pubFailLog(logKey, "pulsar deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署pulsar-manager
        JsonObject pulsarManager = servJson.getJsonObject(FixHeader.HEADER_PULSAR_MANAGER);
        if (pulsarManager != null && !pulsarManager.isEmpty()) {
            String bookies = PulsarDeployerUtils.getPulsarBookieListForPrometheus(bookieArr);
            
            if (!PulsarDeployerUtils.deployPulsarManager(pulsarManager, bookies, version,
                    logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pulsar-manager deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署prometheus
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        if (prometheus != null && !prometheus.isEmpty()) {
            String brokers = PulsarDeployerUtils.getPulsarBrokerListForPrometheus(pulsarArr);
            String bookies = PulsarDeployerUtils.getPulsarBookieListForPrometheus(bookieArr);
            String zks = PulsarDeployerUtils.getPulsarZKListForPrometheus(zkArr);
            
            if (!DeployUtils.deployPrometheus(prometheus, pulsarClusterName, brokers, bookies, zks, version,
                    logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "prometheus deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 部署grafana
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        if (grafana != null && !grafana.isEmpty()) {
            if (!DeployUtils.deployGrafana(grafana, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "grafana deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }
        
        // 3. update t_meta_service.is_deployed and local cache
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
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        
        JsonObject bookieContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BOOKKEEPER_CONTAINER);
        JsonArray bookieArr = bookieContainer.getJsonArray(FixHeader.HEADER_PULSAR_BOOKKEEPER);
        
        JsonObject pulsarContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BROKER_CONTAINER);
        JsonArray pulsarArr = pulsarContainer.getJsonArray(FixHeader.HEADER_PULSAR_BROKER);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        // 卸载grafana
        if (grafana != null && !grafana.isEmpty()) {
            if (!DeployUtils.undeployGrafana(grafana, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "grafana undeploy failed ......");
                return false;
            }
        }
        
        // 卸载prometheus
        if (prometheus != null && !prometheus.isEmpty()) {
            if (!DeployUtils.undeployPrometheus(prometheus, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pulsar undeploy failed ......");
                return false;
            }
        }
        
        // 卸载pulsar-manager
        JsonObject pulsarManager = servJson.getJsonObject(FixHeader.HEADER_PULSAR_MANAGER);
        if (pulsarManager != null && !pulsarManager.isEmpty()) {
            if (!PulsarDeployerUtils.undeployPulsarManager(pulsarManager, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "prometheus deploy failed ......");
                return false;
            }
        }
        
        // 卸载pulsar broker
        for (int i = 0; i < pulsarArr.size(); i++) {
            JsonObject pulsar = pulsarArr.getJsonObject(i);
            if (!PulsarDeployerUtils.undeployPulsar(pulsar, null, null, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "pulsar undeploy failed ......");
                return false;
            }
        }
        
        // 卸载bookkeeper
        for (int i = 0; i < bookieArr.size(); i++) {
            JsonObject bookie = bookieArr.getJsonObject(i);
            if (!PulsarDeployerUtils.undeployBookie(bookie, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "bookkeeper undeploy failed ......");
                return false;
            }
        }
        
        // 卸载zookkeeper
        for (int i = 0; i < zkArr.size(); i++) {
            JsonObject zk = zkArr.getJsonObject(i);
            if (!DeployUtils.undeployZookeeper(zk, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "zookeeper undeploy failed ......");
                return false;
            }
        }
        
        // 4. update t_meta_service is_deployed flag
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
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        
        JsonObject bookieContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BOOKKEEPER_CONTAINER);
        JsonArray bookieArr = bookieContainer.getJsonArray(FixHeader.HEADER_PULSAR_BOOKKEEPER);
        
        JsonObject pulsarContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BROKER_CONTAINER);
        JsonArray pulsarArr = pulsarContainer.getJsonArray(FixHeader.HEADER_PULSAR_BROKER);
        String pulsarClusterName = pulsarContainer.getString(FixHeader.HEADER_INST_ID);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        JsonObject pulsarManager = servJson.getJsonObject(FixHeader.HEADER_PULSAR_MANAGER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        String zkShortAddress = DeployUtils.getZKShortAddress(zkArr);
        
        String bookies = PulsarDeployerUtils.getPulsarBookieListForPrometheus(bookieArr);
        
        boolean deployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_ZOOKEEPER:
            JsonObject zk = DeployUtils.getSpecifiedItem(zkArr, instID);
            String zkAddrList = DeployUtils.getZKAddress(zkArr);
            deployResult = DeployUtils.deployZKNode(zk, zkArr.size(), version, zkAddrList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PULSAR_BOOKKEEPER:
            JsonObject bookie = DeployUtils.getSpecifiedItem(bookieArr, instID);
            deployResult = PulsarDeployerUtils.deployBookie(bookie, version, zkShortAddress, logKey, magicKey, false, result);
            break;
        case FixHeader.HEADER_PULSAR_BROKER:
            JsonObject pulsar = DeployUtils.getSpecifiedItem(pulsarArr, instID);
            String brokerAddrList = PulsarDeployerUtils.getPulsarBrokerList(pulsarArr, null);
            deployResult = PulsarDeployerUtils.deployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, zkShortAddress,
                    logKey, magicKey, false, result);
            break;
        case FixHeader.HEADER_PULSAR_MANAGER:
            deployResult = PulsarDeployerUtils.deployPulsarManager(pulsarManager, bookies, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PROMETHEUS:
            String brokers = PulsarDeployerUtils.getPulsarBrokerListForPrometheus(pulsarArr);
            String zks = PulsarDeployerUtils.getPulsarZKListForPrometheus(bookieArr);
            deployResult = DeployUtils.deployPrometheus(prometheus, pulsarClusterName, brokers, bookies, zks,
                    version, logKey, magicKey, result);
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
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        
        JsonObject bookieContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BOOKKEEPER_CONTAINER);
        JsonArray bookieArr = bookieContainer.getJsonArray(FixHeader.HEADER_PULSAR_BOOKKEEPER);
        
        JsonObject pulsarContainer = servJson.getJsonObject(FixHeader.HEADER_PULSAR_BROKER_CONTAINER);
        JsonArray pulsarArr = pulsarContainer.getJsonArray(FixHeader.HEADER_PULSAR_BROKER);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        JsonObject pulsarManager = servJson.getJsonObject(FixHeader.HEADER_PULSAR_MANAGER);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        
        boolean undeployResult = false;
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_ZOOKEEPER:
            JsonObject zk = DeployUtils.getSpecifiedItem(zkArr, instID);
            undeployResult = DeployUtils.undeployZookeeper(zk, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PULSAR_BOOKKEEPER:
            JsonObject bookie = DeployUtils.getSpecifiedItem(bookieArr, instID);
            undeployResult = PulsarDeployerUtils.undeployBookie(bookie, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PULSAR_BROKER:
            JsonObject pulsar = DeployUtils.getSpecifiedItem(pulsarArr, instID);
            String pulsarClusterName = pulsarContainer.getString(FixHeader.HEADER_INST_ID);
            String brokerAddrList = PulsarDeployerUtils.getPulsarBrokerList(pulsarArr, instID);
            undeployResult = PulsarDeployerUtils.undeployPulsar(pulsar, pulsarClusterName, brokerAddrList, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PULSAR_MANAGER:
            undeployResult = PulsarDeployerUtils.undeployPulsarManager(pulsarManager, version, logKey, magicKey, result);
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
