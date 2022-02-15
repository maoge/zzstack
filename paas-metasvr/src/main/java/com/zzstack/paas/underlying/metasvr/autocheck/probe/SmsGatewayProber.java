package com.zzstack.paas.underlying.metasvr.autocheck.probe;

import java.io.IOException;

import com.zzstack.paas.underlying.metasvr.alarm.AlarmType;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.SmsWebConsoleConnector;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import redis.clients.jedis.JedisCluster;

public class SmsGatewayProber extends BaseProber implements CmptProber {

    private static final String[] CMD_PING = new String[]{"PING"};

    public SmsGatewayProber(String servInstID, String servType) {
        super(servInstID, servType, "sms_gateway");
    }

    @Override
    public boolean doCheck(final String servInstID, final String servType) {
        JsonObject servJson = loadMetaData();
        if (servJson == null) {
            logger.error("loadMetaData fail, servInstID:{}, servType:{} ......", servInstID, servType);
            return false;
        }
        
        JsonObject serverContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_SERVER_CONTAINER);
        JsonObject processContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_PROCESS_CONTAINER);
        JsonObject clientContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_CLIENT_CONTAINER);
        JsonObject batsaveContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_BATSAVE_CONTAINER);
        JsonObject statsContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_STATS_CONTAINER);
        
        checkSmsInstanceArr(FixHeader.HEADER_SMS_SERVER, servInstID, serverContainer, servType);
        checkSmsInstanceArr(FixHeader.HEADER_SMS_PROCESS, servInstID, processContainer, servType);
        checkSmsInstanceArr(FixHeader.HEADER_SMS_CLIENT, servInstID, clientContainer, servType);
        checkSmsInstanceArr(FixHeader.HEADER_SMS_BATSAVE, servInstID, batsaveContainer, servType);
        checkSmsInstanceArr(FixHeader.HEADER_SMS_STATS, servInstID, statsContainer, servType);
        
        return true;
    }

    private void checkSmsInstanceArr(String header, String servInstId, JsonObject container, String servType) {
        JsonArray smsInstanceArr = container.getJsonArray(header);
        if (smsInstanceArr == null || smsInstanceArr.isEmpty())
            return;
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        
        int size = smsInstanceArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject item = smsInstanceArr.getJsonObject(i);
            String instId = item.getString(FixHeader.HEADER_INST_ID);
            PaasInstance instance = cmptMeta.getInstance(instId);
            // 未部署的实例不用检测
            if (!instance.isDeployed())
                continue;
            
            // 停机维护的实例不检测
            if (instance.getStatus().equals(FixDefs.STR_WARN))
                continue;
            
            // 预埋的不检测
            if (instance.getStatus().equals(FixDefs.STR_PRE_EMBADDED))
                continue;
            
            int cmptId = instance.getCmptId();
            PaasMetaCmpt cmpt = cmptMeta.getCmptById(cmptId);
            String cmptName = cmpt.getCmptName();
            
            String ip = item.getString(FixHeader.HEADER_IP);
            String webConsolePort = item.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
            String processor = item.getString(FixHeader.HEADER_PROCESSOR);
            String passwd = CONSTS.SMS_CONSOLE_PASSWD;
            
            int portOffset = processor == null ? 0 : Integer.valueOf(processor);
            int realPort = portOffset + Integer.valueOf(webConsolePort);
            
            String key = String.format("alarm-%s-%d", instId, AlarmType.ALARM_APP_PROC_DOWN.getCode());
            JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
            
            try {
                // 进程异常
                if (!pingSmsGateway(cmptName, instId, ip, realPort, passwd, CMD_PING)) {
                    // 在告警窗口内不重复生成
                    generateAlarm(jedisClient, key, servInstId, servType, instId, cmptName, AlarmType.ALARM_APP_PROC_DOWN);
                } else {
                    // 如果已经恢复则复位
                    if (instance.getStatus().equals(CONSTS.STR_ALARM)) {
                        logger.error("proc recoverd, SERV_INST_ID:{}, INST_ID:{}, CMPT_NAME:{}", servInstId, instId, cmptName);
                        clearAlarm(jedisClient, key, instId);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private boolean pingSmsGateway(String cmptName, String instId, String ip, int port, String passwd, String[] data) {
        boolean ret = false;
        boolean isConnected = false;
        int retry = 0, failCnt = 0;
        
        while (retry < MAX_RETRY) {
            retry++;
            SmsWebConsoleConnector connector = new SmsWebConsoleConnector(ip, port, passwd);
            try {
                connector.connect();
                isConnected = true;
                
                if (connector.sendData(data)) {
                    ret = true;
                    break;
                }
    
                // 底层除了smsserver有PING命令其它都暂时没有
                // if (connector.getResponse().startsWith(FixDefs.SMS_GATEWAY_CMD_PING_RESP)) {
                //     ret = true;
                //     break;
                // }
            } catch (Exception e) {
                failCnt++;
            } finally {
                try {
                    if (isConnected) {
                        connector.close();
                    }
                } catch (IOException e) {
                    ;
                }
            }
        }
        
        return (!ret && failCnt > 0) ? false : true;
    }

}
