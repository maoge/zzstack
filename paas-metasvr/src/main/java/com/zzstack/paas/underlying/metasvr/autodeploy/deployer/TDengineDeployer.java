package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.TDengineDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.global.DeployLog;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TDengineDeployer implements ServiceDeployer {
    @Override
    public boolean deployService(String servInstID, String deployFlag, String logKey, String magicKey, ResultBean result) {
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
        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject tdArbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        //部署arbitrator服务
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String arbitratorAddr = servIp +
                ":" + tdArbitrator.getString(FixHeader.HEADER_PORT);
        if (!TDengineDeployerUtils.deployArbitrator(tdArbitrator, version, logKey, magicKey, result)) {
            DeployLog.pubFailLog(logKey, "arbitrator start failed ......");
            DeployLog.pubFailLog(logKey, result.getRetInfo());
            return false;
        }


        //部署dnode服务
        JsonObject dNodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dNode = dNodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        String firstNodeIp = "";
        for (int i = 0; i < dNode.size(); i++) {
            JsonObject jsondNode = dNode.getJsonObject(i);
            boolean bIsFirst = false;
            if (i == 0) {
                String sshdNodeId = dNode.getJsonObject(0).getString(FixHeader.HEADER_SSH_ID);
                PaasSsh dNodessh = DeployUtils.getSshById(sshdNodeId, logKey, result);
                if (dNodessh == null) return false;
                String dNodeservIp = dNodessh.getServerIp();
                firstNodeIp = dNodeservIp + ":" + dNode.getJsonObject(0).getString(FixHeader.HEADER_PORT);
                bIsFirst = true;
            }
            if (!TDengineDeployerUtils.deployDnode(jsondNode, arbitratorAddr, firstNodeIp, bIsFirst, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "dnode start failed ......");
                DeployLog.pubFailLog(logKey, result.getRetInfo());
                return false;
            }
        }

        //部署collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd start failed ......");
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

        //卸载dNode
        JsonObject dNodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dNode = dNodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        for (int i = 0; i < dNode.size(); i++) {
            JsonObject jsondNode = dNode.getJsonObject(i);
            if (!TDengineDeployerUtils.undeployDnode(jsondNode, logKey, result, false, magicKey)) {
                DeployLog.pubFailLog(logKey, "dnode undeploy failed ......");
                return false;
            }
        }

        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject tdArbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        //卸载arbitrator
        if (!TDengineDeployerUtils.undeployArbitrator(tdArbitrator, logKey, magicKey, result)) {
            DeployLog.pubFailLog(logKey, "arbitrator undeploy failed ......");
            return false;
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

        // 3. update zz_service is_deployed flag
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

        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }

        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject tdArbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);

        //部署arbitrator服务
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String arbitratorAddr = servIp +
                ":" + tdArbitrator.getString(FixHeader.HEADER_PORT);
        if (instID.equals(tdArbitrator.getString(FixHeader.HEADER_INST_ID))) {
            if (!TDengineDeployerUtils.deployArbitrator(tdArbitrator, version, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "arbitrator start failed ......");
                return false;
            }
        }
        PaasInstance arbitrarorInst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(tdArbitrator.getString(FixHeader.HEADER_INST_ID));
        if (!DeployUtils.isInstanceDeployed(arbitrarorInst, logKey, result)) {
            DeployLog.pubFailLog(logKey, "arbitrator not start,please priority start arbitrator inst......");
            return false;
        }

        //部署dnode服务
        JsonObject dNodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dNode = dNodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        String firstNodeIp = "";
        for (int i = 0; i < dNode.size(); i++) {
            JsonObject jsondNode = dNode.getJsonObject(i);
            if (instID.equals(jsondNode.getString(FixHeader.HEADER_INST_ID))) {
                boolean bIsFirst = false;
                if (i == 0) {
                    bIsFirst = true;
                }
                String sshdNodeId = dNode.getJsonObject(0).getString(FixHeader.HEADER_SSH_ID);
                PaasSsh dNodessh = DeployUtils.getSshById(sshdNodeId, logKey, result);
                if (dNodessh == null) return false;
                String dNodeservIp = dNodessh.getServerIp();
                firstNodeIp = dNodeservIp + ":" + dNode.getJsonObject(0).getString(FixHeader.HEADER_PORT);
                if (!TDengineDeployerUtils.deployDnode(jsondNode, arbitratorAddr, firstNodeIp, bIsFirst, version, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "dnode start failed ......");
                    return false;
                }
            }
        }

        //部署collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd start failed ......");
                    return false;
                }
            }
        }

        String info = String.format("service inst_id:%s, deploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
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
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(servInstID);
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(cmpt.getCmptName());

        if (DeployUtils.isServiceDeployed(serv, logKey, result)) {
            return false;
        }

        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject tdArbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);

        //卸载dnode服务
        JsonObject dNodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dNode = dNodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        if (dNode.size() <= 3) {
            DeployLog.pubFailLog(logKey, "less than three dnodes,can not undeploy ......");
            return false;
        }
        for (int i = 0; i < dNode.size(); i++) {
            JsonObject jsondNode = dNode.getJsonObject(i);
            if (instID.equals(jsondNode.getString(FixHeader.HEADER_INST_ID))) {
                if (!TDengineDeployerUtils.undeployDnode(jsondNode, logKey, result, true, magicKey)) {
                    DeployLog.pubFailLog(logKey, "dnode undeploy failed ......");
                    return false;
                }
            }
        }

        //卸载arbitrator服务
        if (instID.equals(tdArbitrator.getString(FixHeader.HEADER_INST_ID))) {
            if (!TDengineDeployerUtils.undeployArbitrator(tdArbitrator, logKey, magicKey, result)) {
                DeployLog.pubFailLog(logKey, "arbitrator undeploy failed ......");
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

        String info = String.format("service inst_id:%s, undeploy sucess ......", servInstID);
        DeployLog.pubSuccessLog(logKey, info);
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
