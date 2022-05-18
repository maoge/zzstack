package com.zzstack.paas.underlying.metasvr.autodeploy.deployer;

import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.CollectdDeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.DeployUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.TDengineDeployerUtils;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.TopoResult;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
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
        TopoResult topoResult = DeployUtils.LoadServTopo(servInstID, logKey, true, result);
        if (!topoResult.isOk()) {
            return false;
        }
        JsonObject servJson = topoResult.getServJson();
        String version = topoResult.getVersion();
        
        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject tdArbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        // 部署arbitrator服务
        String sshId = tdArbitrator.getString(FixHeader.HEADER_SSH_ID);
        PaasSsh ssh = DeployUtils.getSshById(sshId, logKey, result);
        if (ssh == null) return false;
        String servIp = ssh.getServerIp();
        String arbitratorAddr = servIp + ":" + tdArbitrator.getString(FixHeader.HEADER_PORT);
        if (!TDengineDeployerUtils.deployArbitrator(tdArbitrator, version, logKey, magicKey, result)) {
            DeployLog.pubFailLog(logKey, "arbitrator start failed ......");
            DeployLog.pubFailLog(logKey, result.getRetInfo());
            return false;
        }

        // 部署dnode服务
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

        // 部署collectd服务
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

        // 卸载dnode
        JsonObject dnodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dnodeArr = dnodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        for (int i = 0; i < dnodeArr.size(); i++) {
            JsonObject dnode = dnodeArr.getJsonObject(i);
            if (!TDengineDeployerUtils.undeployDnode(dnode, logKey, result, false, magicKey)) {
                DeployLog.pubFailLog(logKey, "dnode undeploy failed ......");
                return false;
            }
        }

        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject arbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        // 卸载arbitrator
        if (!TDengineDeployerUtils.undeployArbitrator(arbitrator, logKey, magicKey, result)) {
            DeployLog.pubFailLog(logKey, "arbitrator undeploy failed ......");
            return false;
        }

        // 卸载collectd服务
        if (servJson.containsKey(FixHeader.HEADER_COLLECTD)) {
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            if (!collectd.isEmpty()) {
                if (!CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result)) {
                    DeployLog.pubFailLog(logKey, "collectd undeploy failed ......");
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

        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject arbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        
        JsonObject dnodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dnodeArr = dnodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean deployResult = true;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_TD_ARBITRATOR:
            deployResult = TDengineDeployerUtils.deployArbitrator(arbitrator, version, logKey, magicKey, result);
            break;
            
        case FixHeader.HEADER_TD_DNODE:
            JsonObject dnode = DeployUtils.getSpecifiedItem(dnodeArr, instID);
            String arbitratorAddr = TDengineDeployerUtils.getArbitratorAddr(arbitrator);
            String firstNode = TDengineDeployerUtils.getFirstNode(dnodeArr);
            deployResult = TDengineDeployerUtils.deployDnode(dnode, arbitratorAddr, firstNode, false, version, logKey, magicKey, result);
            break;
            
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            deployResult = CollectdDeployUtils.deployCollectd(collectd, servInstID, logKey, magicKey, result);
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
        JsonObject arbitratorContainer = servJson.getJsonObject(FixHeader.HEADER_ARBITRATOR_CONTAINER);
        JsonObject arbitrator = arbitratorContainer.getJsonObject(FixHeader.HEADER_TD_ARBITRATOR);
        
        JsonObject dnodeContainer = servJson.getJsonObject(FixHeader.HEADER_DNODE_CONTAINER);
        JsonArray dnodeArr = dnodeContainer.getJsonArray(FixHeader.HEADER_TD_DNODE);
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance inst = cmptMeta.getInstance(instID);
        PaasMetaCmpt instCmpt = cmptMeta.getCmptById(inst.getCmptId());
        boolean undeployResult = false;
        
        switch (instCmpt.getCmptName()) {
        case FixHeader.HEADER_TD_ARBITRATOR:
            undeployResult = TDengineDeployerUtils.undeployArbitrator(arbitrator, logKey, magicKey, result);
            break;
        
        case FixHeader.HEADER_TD_DNODE:
            if (dnodeArr.size() <= 3) {
                DeployLog.pubFailLog(logKey, "less than 3 dnodes, can not undeploy ......");
                return false;
            }
            JsonObject dnode = DeployUtils.getSpecifiedItem(dnodeArr, instID);
            undeployResult = TDengineDeployerUtils.undeployDnode(dnode, logKey, result, true, magicKey);
            break;
        
        case FixHeader.HEADER_COLLECTD:
            JsonObject collectd = servJson.getJsonObject(FixHeader.HEADER_COLLECTD);
            undeployResult = CollectdDeployUtils.undeployCollectd(collectd, logKey, magicKey, result);
            break;
            
        default:
            break;
        }

        DeployUtils.postDeployLog(undeployResult, servInstID, logKey, "undeploy");
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
