package com.zzstack.paas.underlying.metasvr.eventbus;

import com.zzstack.paas.underlying.metasvr.bean.AccountSessionBean;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasServer;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.AccOperLogDao;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class EventProc {
    
    public static void procWriteOperLog(EventBean evBean) {
        EventType eventType = evBean.getEvType();
        if (!eventType.isNeedWriteOperLog()) {
            return;
        }
        
        String magicKey = evBean.getMagicKey();
        String msgBody = evBean.getMsgBody();
        long eventTS = evBean.getEventTs();
        
        String accName = MetaSvrGlobalRes.get().getCmptMeta().getAccNameByMagicKey(magicKey);
        if (accName == null)
            accName = "";  // 防护一手，尽管可能性很低
        
        String opType = eventType.name();
        
        AccOperLogDao.addOpLog(accName, opType, msgBody, eventTS);
    }
    
    public static void procAddService(String msg) {
        PaasService service = PaasService.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addService(service);
    }
    
    public static void procDelService(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        MetaSvrGlobalRes.get().getCmptMeta().delService(instId);
    }
    
    public static void procModService(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        MetaSvrGlobalRes.get().getCmptMeta().reloadService(instId);
    }
    
    public static void procUpdServiceDeploy(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        String deployFlag = json.getString(FixHeader.HEADER_IS_DEPLOYED);
        MetaSvrGlobalRes.get().getCmptMeta().updServDeploy(instId, deployFlag);
    }
    
    public static void procAddInstance(String msg) {
        PaasInstance instance = PaasInstance.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addInstance(instance);
    }
    
    public static void procDelInstance(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        MetaSvrGlobalRes.get().getCmptMeta().delInstance(instId);
    }
    
    public static void procUpdInstPos(String msg) {
        PaasInstance instance = PaasInstance.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().updInstPos(instance);
    }
    
    public static void procUpdInstDeploy(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        String deployFlag = json.getString(FixHeader.HEADER_IS_DEPLOYED);
        MetaSvrGlobalRes.get().getCmptMeta().updInstDeploy(instId, deployFlag);
    }
    
    public static void procUpdInstPreEmbadded(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        String proEmbadded = json.getString(FixHeader.HEADER_PRE_EMBADDED);
        MetaSvrGlobalRes.get().getCmptMeta().updInstPreEmbadded(instId, proEmbadded);
    }
    
    public static void procReloadMetaData(String msg) {
        JsonObject json = new JsonObject(msg);
        String type = json.getString(FixHeader.HEADER_RELOAD_TYPE);
        MetaSvrGlobalRes.get().getCmptMeta().reloadMetaData(type);
    }
    
    public static void procAddInstAttr(String msg) {
        PaasInstAttr instAttr = PaasInstAttr.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addInstAttr(instAttr);
    }
    
    public static void procDelInstAttr(String msg) {
        JsonObject json = new JsonObject(msg);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        MetaSvrGlobalRes.get().getCmptMeta().delInstAttr(instId);
    }
    
    public static void addTopo(String msg) {
        PaasTopology topo = PaasTopology.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addTopo(topo);
    }
    
    public static void delTopo(String msg) {
        JsonObject json = new JsonObject(msg);
        String parentId = json.getString(FixHeader.HEADER_PARENT_ID);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        
        if (instId == null || instId.isEmpty())
            MetaSvrGlobalRes.get().getCmptMeta().delTopo(parentId);
        else
            MetaSvrGlobalRes.get().getCmptMeta().delTopo(parentId, instId);
    }
    
    public static void modTopo(String msg) {
        PaasTopology topo = PaasTopology.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().modTopo(topo);
    }
    
    public static void procAddServer(String msg) {
        PaasServer server = PaasServer.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addServer(server);
    }
    
    public static void procDelServer(String msg) {
        JsonObject json = new JsonObject(msg);
        String serverIp = json.getString(FixHeader.HEADER_SERVER_IP);
        MetaSvrGlobalRes.get().getCmptMeta().delServer(serverIp);
    }
    
    public static void addSSH(String msg) {
        PaasSsh ssh = PaasSsh.fromJson(msg);
        MetaSvrGlobalRes.get().getCmptMeta().addSsh(ssh);
    }
    
    public static void modSSH(String msg) {
        JsonObject json = new JsonObject(msg);
        String serverIp = json.getString(FixHeader.HEADER_SERVER_IP);
        String sshId = json.getString(FixHeader.HEADER_SSH_ID);
        String sshName = json.getString(FixHeader.HEADER_SSH_NAME);
        String sshPwd = json.getString(FixHeader.HEADER_SSH_PWD);
        int sshPort = json.getInteger(FixHeader.HEADER_SSH_PORT);
        MetaSvrGlobalRes.get().getCmptMeta().modSsh(serverIp, sshId, sshName, sshPwd, sshPort);
    }
    
    public static void delSSH(String msg) {
        JsonObject json = new JsonObject(msg);
        String serverIp = json.getString(FixHeader.HEADER_SERVER_IP);
        String sshId = json.getString(FixHeader.HEADER_SSH_ID);
        MetaSvrGlobalRes.get().getCmptMeta().delSsh(serverIp, sshId);
    }
    
    public static void addSession(String msg) {
        JsonObject json = new JsonObject(msg);
        String accName = json.getString(FixHeader.HEADER_ACC_NAME);
        String magicKey = json.getString(FixHeader.HEADER_MAGIC_KEY);
        long sessionTimeOut = json.getLong(FixHeader.HEADER_SESSION_TIMEOUT);
        
        AccountSessionBean session = new AccountSessionBean(accName, magicKey, sessionTimeOut);
        MetaSvrGlobalRes.get().getCmptMeta().addAccSession(session, true);
    }
    
    public static void removeSession(String msg) {
        JsonObject json = new JsonObject(msg);
        String accName = json.getString(FixHeader.HEADER_ACC_NAME);
        String magicKey = json.getString(FixHeader.HEADER_MAGIC_KEY);
        
        MetaSvrGlobalRes.get().getCmptMeta().removeTtlSession(accName, magicKey, true);
    }
    
    public static void adjustQueueWeight(String msg) {
        JsonObject json = new JsonObject(msg);
        String instAId = json.getString(FixHeader.HEADER_INST_ID_A);
        String instBId = json.getString(FixHeader.HEADER_INST_ID_B);
        
        String weightA = json.getString(FixHeader.HEADER_WEIGHT_A);
        String weightB = json.getString(FixHeader.HEADER_WEIGHT_B);
        
        MetaSvrGlobalRes.get().getCmptMeta().adjustSmsABQueueWeightInfo(instAId, weightA, instBId, weightB);
    }
    
    public static void switchDBType(String msg) {
        JsonObject json = new JsonObject(msg);
        String dgContainerID = json.getString(FixHeader.HEADER_INST_ID);
        String dbType = json.getString(FixHeader.HEADER_ACTIVE_DB_TYPE);
        
        MetaSvrGlobalRes.get().getCmptMeta().switchSmsDBType(dgContainerID, dbType);
    }
    
    public static void addCmptVerion(String msg) {
        JsonObject json = new JsonObject(msg);
        String servType = json.getString(FixHeader.HEADER_SERV_TYPE);
        String version = json.getString(FixHeader.HEADER_VERSION);
        
        MetaSvrGlobalRes.get().getCmptMeta().addCmptVersion(servType, version);
    }
    
    public static void delCmptVerion(String msg) {
        JsonObject json = new JsonObject(msg);
        String servType = json.getString(FixHeader.HEADER_SERV_TYPE);
        String version = json.getString(FixHeader.HEADER_VERSION);
        
        MetaSvrGlobalRes.get().getCmptMeta().delCmptVersion(servType, version);
    }
    
    public static void modPasswd(String msg) {
        JsonObject json = new JsonObject(msg);
        String accName = json.getString(FixHeader.HEADER_ACC_NAME);
        String passwd = json.getString(FixHeader.HEADER_PASSWORD);
        MetaSvrGlobalRes.get().getCmptMeta().modPasswd(accName, passwd);
    }

}
