package com.zzstack.paas.underlying.metasvr.eventbus;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.threadpool.WorkerPool;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class SysEventProcessor implements Runnable {
    
    private static Logger logger = LoggerFactory.getLogger(SysEventProcessor.class);
    
    private static final int EVENT_EXPIRE_TTL = SysConfig.get().getEventbusExpireTtl();

    String msg;

    public SysEventProcessor(String msg) {
        this.msg = msg;
    }

    @Override
    public void run() {
        JsonObject jsonObj = new JsonObject(msg);
        if (jsonObj.isEmpty()) {
            return;
        }

        int eventCode = jsonObj.getInteger(FixHeader.HEADER_EVENT_CODE);
        EventType eventType = EventType.get(eventCode);

        String metaServId = jsonObj.getString(FixHeader.HEADER_META_SERV_ID);
        long eventTs = jsonObj.getLong(FixHeader.HEADER_EVENT_TS);
        String msgBody = jsonObj.getString(FixHeader.HEADER_MSG_BODY);

        if ((System.currentTimeMillis() - eventTs) > EVENT_EXPIRE_TTL) {
            logger.info("event msg expired:{}", msg);
            return;
        }

        if (MetaSvrGlobalRes.get().getMetaServId().equals(metaServId)) {
            return;
        }

        switch (eventType) {
        case EVENT_SYNC_SESSION:
            break;
        
        case EVENT_ADD_SERVICE:
            EventProc.procAddService(msgBody);
            break;
        
        case EVENT_DEL_SERVICE:
            EventProc.procDelService(msgBody);
            break;
        
        case EVENT_MOD_SERVICE:
            EventProc.procModService(msgBody);
            break;

        case EVENT_UPD_SERVICE_DEPLOY:
            EventProc.procUpdServiceDeploy(msgBody);
            break;
        
        case EVENT_ADD_INSTANCE:
            EventProc.procAddInstance(msgBody);
            break;
        
        case EVENT_DEL_INSTANCE:
            EventProc.procDelInstance(msgBody);
            break;
        
        case EVENT_UPD_INST_POS:
            EventProc.procUpdInstPos(msgBody);
            break;
        
        case EVENT_UPD_INST_DEPLOY:
            EventProc.procUpdInstDeploy(msgBody);
            break;
            
        case EVENT_ADD_INST_ATTR:
            EventProc.procAddInstAttr(msgBody);
            break;
        
        case EVENT_DEL_INST_ATTR:
            EventProc.procDelInstAttr(msgBody);
            break;
        
        case EVENT_ADD_TOPO:
            EventProc.addTopo(msgBody);
            break;
        
        case EVENT_DEL_TOPO:
            EventProc.delTopo(msgBody);
            break;
        
        case EVENT_MOD_TOPO:
            EventProc.modTopo(msgBody);
            break;
        
        case EVENT_ADD_SERVER:
            EventProc.procAddServer(msgBody);
            break;
        
        case EVENT_DEL_SERVER:
            EventProc.procDelServer(msgBody);
            break;
        
        case EVENT_ADD_SSH:
            EventProc.addSSH(msgBody);
            break;
        
        case EVENT_MOD_SSH:
            EventProc.modSSH(msgBody);
            break;
        
        case EVENT_DEL_SSH:
            EventProc.delSSH(msgBody);
            break;
        
        case EVENT_ADD_SESSON:
            EventProc.addSession(msgBody);
            break;
        
        case EVENT_REMOVE_SESSON:
            EventProc.removeSession(msgBody);
            break;
        
        case EVENT_AJUST_QUEUE_WEIGHT:
            EventProc.adjustQueueWeight(msgBody);
            break;
        
        case EVENT_SWITCH_DB_TYPE:
            EventProc.switchDBType(msgBody);
            break;
        
        case EVENT_ADD_CMPT_VER:
            EventProc.addCmptVerion(msgBody);
            break;
        
        case EVENT_DEL_CMPT_VER:
            EventProc.delCmptVerion(msgBody);
            break;
        
        case EVENT_MOD_ACC_PASSWD:
            EventProc.modPasswd(msgBody);
            break;
        
        case EVENT_UPD_INST_PRE_EMBEDDED:
            EventProc.procUpdInstPreEmbadded(msgBody);
            break;
        
        case EVENT_RELOAD_METADATA:
            EventProc.procReloadMetaData(msgBody);
            break;
        
        default:
            break;
        }
    }

    @SuppressWarnings("unused")
    private void generalNotify(EventType type, String servId, long eventTs, String msgBody, Set<String> clients) {
        EventBean ev = new EventBean();
        ev.setEvType(type);
        ev.setMetaServID(servId);
        ev.setEventTs(eventTs);
        ev.setMsgBody(msgBody);
        String msg = ev.asJsonString();

        for (String addr : clients) {
            String arr[] = addr.split(":");
            String ip = arr[0];
            int port = Integer.valueOf(arr[1]);

            EventNotifier notifier = new EventNotifier(ip, port, msg);
            WorkerPool.get().execute(notifier);
        }
    }

}
