package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.RocketMqDeployUtils;
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
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RocketMqDeployer implements ServiceDeployer {

    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)){
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        String version = serv.getVersion();
        
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());
        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }

        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(deployFlag)) {
            if (!RocketMqDeployUtils.deployFakeService(servJson, logKey, servInstID, result, magicKey)) {
                return false;
            }

            String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        //部署namesrv服务
        JsonObject nameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray nameSrvArr = nameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);
        
        for(int i = 0; i <nameSrvArr.size();i++){
            JsonObject nameSrv = nameSrvArr.getJsonObject(i);
            if(!RocketMqDeployUtils.deployNamesrv(nameSrv, version, logKey, magicKey, result)){
                DeployLog.pubFailLog(logKey, "rocketmq namesrv deploy failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }

        //部署broker服务
        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray vbrokerArr = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);
        String namesrvAddrs = RocketMqDeployUtils.getNameSrvAddrs(nameSrvArr);
        
        for (int i = 0; i < vbrokerArr.size(); i++) {
            JsonObject vbroker = vbrokerArr.getJsonObject(i);
            // String vbrokerInstId = vbroker.getString(FixHeader.HEADER_INST_ID);
            JsonArray brokerArr = vbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            for (int j = 0; j< brokerArr.size(); j++) {
                JsonObject broker = brokerArr.getJsonObject(j);
                String brokerId = String.format("%d", j);
                if (!RocketMqDeployUtils.deployBroker(broker, servInstID, namesrvAddrs, brokerId,
                        version, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker start failed ......");
                    DeployLog.pubFailLog(logKey, result.getRetInfo());
                    return false;
                }
            }
        }

        //部署collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (collectd != null && !collectd.isEmpty()) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd deploy failed ......");
                    return false;
                }
            }
        }

        // 部署rocketmq-console
        String singleNameSrv = RocketMqDeployUtils.getSingleNameSrvAddrs(nameSrvArr);
        if (servJson.containsKey(FixHeader.HEADER_ROCKETMQ_CONSOLE)) {
            JsonObject console = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_CONSOLE);
            if (console != null && !console.isEmpty()) {
                if (!RocketMqDeployUtils.deployConsole(console, servInstID, singleNameSrv, version, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "rocketmq-console deploy failed ......");
                    DeployLog.pubFailLog(logKey, result.getRetInfo());
                    return false;
                }
            }
        }

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
    public boolean undeployService(String servInstID, boolean force, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)){
            return false;
        }
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        if (!force && DeployUtils.isServiceNotDeployed(serv, logKey, result)){
            return false;
        }

        //卸载伪部署
        if (CONSTS.DEPLOY_FLAG_PSEUDO.equals(serv.getPseudoDeployFlag())) { //服务是伪部署的话
            if (!RocketMqDeployUtils.undeployFakeService(servJson, logKey, servInstID, result, magicKey)) {
                return false;
            }

            String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
            return true;
        }

        JsonObject rocketMqNameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray rocketMqNameSrv = rocketMqNameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);

        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray rocketMqVBroker = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);

        //卸载broker服务
        for (int i = 0; i < rocketMqVBroker.size(); i++) {
            JsonObject jsonRocketbroker = rocketMqVBroker.getJsonObject(i);
            JsonArray jsonBrokerArray = jsonRocketbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            for (int j = 0; j < jsonBrokerArray.size(); j++) {
                JsonObject jsonRokectBroker = jsonBrokerArray.getJsonObject(j);
                if (!RocketMqDeployUtils.undeployBroker(jsonRokectBroker, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "rocketmq broker undeploy failed ......");
                    return false;
                }
            }
        }

        //卸载namesrv服务
        for (int i = 0; i <rocketMqNameSrv.size();i++) {
            JsonObject jsonRokectMq = rocketMqNameSrv.getJsonObject(i);
            if (!RocketMqDeployUtils.undeployNamesrv(jsonRokectMq, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey,"rocketmq namesrv undeploy failed ......");
                return false;
            }
        }

        //卸载collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd undeploy failed ......");
                    return false;
                }
            }
        }
        
        // 卸载rockmetmq-console服务
        if (servJson.containsKey(FixHeader.HEADER_ROCKETMQ_CONSOLE)) {
            JsonObject console = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_CONSOLE);
            if (!console.isEmpty()) {
                if (!RocketMqDeployUtils.undeployConsole(console, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "rocketmq-console undeploy failed ......");
                    return false;
                }
            }
        }

        // 3. update t_meta_service is_deployed flag
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

        JsonObject nameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray nameSrvArr = nameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);
        
        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray vbrokerArr = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);
        
        String namesrvAddrs = RocketMqDeployUtils.getNameSrvAddrs(nameSrvArr);
        boolean deployResult = false;
        
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_ROCKETMQ_NAMESRV:
            JsonObject nameSrv = DeployUtils.getSpecifiedItem(nameSrvArr, instID);
            deployResult = RocketMqDeployUtils.deployNamesrv(nameSrv, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ROCKETMQ_BROKER:
            JsonObject broker = DeployUtils.getSpecifiedRocketMQBroker(vbrokerArr, instID);
            String vbrokerInstId = DeployUtils.getSpecifiedVBrokerId(vbrokerArr, instID);
            JsonArray brokerArr = DeployUtils.getSpecifiedBrokerArr(vbrokerArr, instID);
            String brokerId = String.format("%d", brokerArr.size() - 1);
            deployResult = RocketMqDeployUtils.deployBroker(broker, vbrokerInstId, namesrvAddrs, brokerId, version, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            deployResult = CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ROCKETMQ_CONSOLE:
            JsonObject console = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_CONSOLE);
            deployResult = RocketMqDeployUtils.deployConsole(console, servInstID, namesrvAddrs, version, logKey, magicKey, result);
            break;
        default:
            break;
        }

        if (deployResult) {
            String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("service inst_id:%s, deploy failed ......", servInstID);
            DeployLog.pubFailLog(logKey, info);
            DeployLog.pubFailLog(logKey, result.getRetInfo());
        }
        return true;
    }

    @Override
    public boolean undeployInstance(String servInstID, String instID, String logKey, String magicKey, ResultBean result) {
        JsonObject retJson = new JsonObject();
        if (!DeployUtils.getServiceTopo(retJson, servInstID, logKey, result)) {
            return false;
        }
        PaasInstance inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstID);
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(inst.getCmptId());

        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        JsonObject nameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray nameSrvArr = nameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);

        JsonObject vbrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray vbrokerArr = vbrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);

        boolean undeployResult = false;
        switch (cmpt.getCmptName()) {
        case FixHeader.HEADER_ROCKETMQ_NAMESRV:
            JsonObject nameSrv = DeployUtils.getSpecifiedItem(nameSrvArr, instID);
            undeployResult = RocketMqDeployUtils.undeployNamesrv(nameSrv, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ROCKETMQ_BROKER:
            JsonObject broker = DeployUtils.getSpecifiedRocketMQBroker(vbrokerArr, instID);
            undeployResult = RocketMqDeployUtils.undeployBroker(broker, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            undeployResult = CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result);
            break;
        case FixHeader.HEADER_ROCKETMQ_CONSOLE:
            JsonObject console = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_CONSOLE);
            undeployResult = RocketMqDeployUtils.undeployConsole(console, logKey, magicKey, result);
            break;
        default:
            break;
        }

        if (undeployResult) {
            String info = String.format("service inst_id: %s, undeploy sucess ......", servInstID);
            DeployLog.pubSuccessLog(logKey, info);
        } else {
            String info = String.format("service inst_id: %s, undeploy fail ......", servInstID);
            DeployLog.pubFailLog(logKey, info);
        }
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
