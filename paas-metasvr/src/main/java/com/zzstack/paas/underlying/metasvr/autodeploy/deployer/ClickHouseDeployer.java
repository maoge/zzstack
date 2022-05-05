package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.ClickHouseDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * clickhouse自动部署
 */
public class ClickHouseDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }
        
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
        
        // 部署clickhouse服务
        JsonObject replicasContainer = servJson.getJsonObject(FixHeader.HEADER_CLICKHOUSE_REPLICAS_CONTAINER);
        JsonArray replicasArr = replicasContainer.getJsonArray(FixHeader.HEADER_CLICKHOUSE_REPLICAS);
        String replicaCluster = ClickHouseDeployUtils.getRelicaCluster(replicasArr);
        String zkCluster = ClickHouseDeployUtils.getZkCluster(zkArr);
        
        replicaCluster = replicaCluster.replaceAll("/", "\\\\/").replaceAll("\n", "\\\\\n");
        zkCluster = zkCluster.replaceAll("/", "\\\\/").replaceAll("\n", "\\\\\n");
        
        for (int i = 0; i < replicasArr.size(); ++i) {
            JsonObject replicas = replicasArr.getJsonObject(i);
            String replicasID = replicas.getString(FixHeader.HEADER_INST_ID);
            JsonArray clickHouseArr = replicas.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);
                if (!ClickHouseDeployUtils.deployClickHouseServer(clickhouse, version, replicasID, replicaCluster, zkCluster, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "clickhouse-server deploy failed ......");
                    return false;
                }
            }
        }
        
        // 部署监控组件
        // CLICKHOUSE_EXPORTER -> PROMETHEUS -> GRAFANA
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        if (prometheus != null && !prometheus.isEmpty()) {
            String exporters = ClickHouseDeployUtils.getExporterList(replicasArr);
            if (!ClickHouseDeployUtils.deployPrometheus(prometheus, servInstID, exporters, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "prometheus deploy failed ......");
                return false;
            }
        }
        
        if (grafana != null && !grafana.isEmpty()) {
            if (!DeployUtils.deployGrafana(grafana, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "grafana deploy failed ......");
                return false;
            }
        }
        
        // update t_meta_service.is_deployed and local cache
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
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();

        PaasInstance inst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceNotDeployed(serv, logKey, result)) {
            return false;
        }
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        
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
                DeployLog.pubFailLog(logKey, "prometheus undeploy failed ......");
                return false;
            }
        }
        
        // 卸载clickhouse服务
        JsonObject replicasContainer = servJson.getJsonObject(FixHeader.HEADER_CLICKHOUSE_REPLICAS_CONTAINER);
        JsonArray replicasArr = replicasContainer.getJsonArray(FixHeader.HEADER_CLICKHOUSE_REPLICAS);
        for (int i = 0; i < replicasArr.size(); ++i) {
            JsonObject replicas = replicasArr.getJsonObject(i);
            JsonArray clickHouseArr = replicas.getJsonArray(FixHeader.HEADER_CLICKHOUSE_SERVER);
            for (int j = 0; j < clickHouseArr.size(); ++j) {
                JsonObject clickhouse = clickHouseArr.getJsonObject(j);
                if (!ClickHouseDeployUtils.undeployClickHouseServer(clickhouse, version, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "clickhouse-server undeploy failed ......");
                    return false;
                }
            }
        }
        
        // 卸载zookeeper服务
        for (int i = 0; i < zkArr.size(); i++) {
            JsonObject zk = zkArr.getJsonObject(i);
            if (!DeployUtils.undeployZookeeper(zk, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "zookeeper undeploy failed ......");
                return false;
            }
        }
        
        // 4. update t_meta_service is_deployed flag
        if (!MetaDataDao.updateInstanceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }
        if (!MetaDataDao.updateServiceDeployFlag(servInstID, FixDefs.STR_FALSE, result, magicKey)) {
            return false;
        }
        String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
        
        return true;
    }

    @Override
    public boolean deployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        // 新增clickhouse-server节点, 需要拉起新增节点, 再更新原clickhouse节点的配置<shard></shard>部分的配置
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }

        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasService serv = cmptMeta.getService(servInstID);
        String version = serv.getVersion();
        
        PaasInstance servInst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt servCmpt = cmptMeta.getCmptById(servInst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(servCmpt.getCmptName());
        
        JsonObject zkContainer = servJson.getJsonObject(FixHeader.HEADER_ZOOKEEPER_CONTAINER);
        JsonArray zkArr = zkContainer.getJsonArray(FixHeader.HEADER_ZOOKEEPER);
        
        JsonObject replicasContainer = servJson.getJsonObject(FixHeader.HEADER_CLICKHOUSE_REPLICAS_CONTAINER);
        JsonArray replicasArr = replicasContainer.getJsonArray(FixHeader.HEADER_CLICKHOUSE_REPLICAS);
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_ZOOKEEPER:
            JsonObject zk = DeployUtils.getSpecifiedItem(zkArr, instID);
            String zkAddrList = DeployUtils.getZKAddress(zkArr);
            deployResult = DeployUtils.deployZKNode(zk, zkArr.size(), version, zkAddrList, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_CLICKHOUSE_SERVER:
            JsonObject clickhouse = DeployUtils.getSpecifiedClickHouseItem(replicasArr, instID);
            String replicasID = DeployUtils.getSpecifiedClickHouseParentID(replicasArr, instID);

            String replicaCluster = ClickHouseDeployUtils.getRelicaCluster(replicasArr);
            String zkCluster = ClickHouseDeployUtils.getZkCluster(zkArr);

            replicaCluster = replicaCluster.replaceAll("/", "\\\\/").replaceAll("\n", "\\\\\n");
            zkCluster = zkCluster.replaceAll("/", "\\\\/").replaceAll("\n", "\\\\\n");
            
            deployResult = ClickHouseDeployUtils.deployClickHouseServer(clickhouse, version, replicasID, replicaCluster, zkCluster, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_PROMETHEUS:
            String exporters = ClickHouseDeployUtils.getExporterList(replicasArr);
            deployResult = ClickHouseDeployUtils.deployPrometheus(prometheus, servInstID, exporters, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_GRAFANA:
            deployResult = DeployUtils.deployGrafana(grafana, version, logKey, magicKey, result);
            break;
        default:
            break;
        }
        
        if (deployResult) {
            String info = String.format("new instance inst_id:%s, deploy sucess ......", instID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("new instance inst_id:%s, deploy failed ......", instID);
            DeployLog.pubFailLog(logKey, info);
        }
        return deployResult;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey,
            ResultBean result) {
        
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        PaasService serv = cmptMeta.getService(servInstID);
        PaasInstance servInst = cmptMeta.getInstance(servInstID);
        PaasMetaCmpt cmpt = cmptMeta.getCmptById(servInst.getCmptId());
        
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        String version = serv.getVersion();
        
        JsonObject prometheus = servJson.getJsonObject(FixHeader.HEADER_PROMETHEUS);
        JsonObject grafana = servJson.getJsonObject(FixHeader.HEADER_GRAFANA);
        
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_ZOOKEEPER:
        case FixHeader.HEADER_CLICKHOUSE_SERVER:
            // 缩容复杂，非特殊情况不做缩容
            String info = String.format("service inst_id:%s, undeploy not support ......", servInstID);
            DeployLog.pubFailLog(logKey, info);
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
        
        if (undeployResult) {
            String info = String.format("instance inst_id:%s, undeploy sucess ......", instID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("instance inst_id:%s, undeploy failed ......", instID);
            DeployLog.pubFailLog(logKey, info);
        }
        
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
