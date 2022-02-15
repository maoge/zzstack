package com.zzstack.paas.underlying.metasvr.autocheck.probe;

import com.zzstack.paas.underlying.metasvr.alarm.AlarmType;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import redis.clients.jedis.JedisCluster;

public class SmsQueryServiceProber extends BaseProber implements CmptProber {

    public SmsQueryServiceProber(String servInstID, String servType) {
        super(servInstID, servType, "sms_query_service");
    }

    @Override
    public boolean doCheck(final String servInstID, final String servType) {
        JsonObject servJson = loadMetaData();
        if (servJson == null) {
            logger.error("loadMetaData fail, servInstID:{}, servType:{} ......", servInstID, servType);
            return false;
        }
        
        JsonObject smsQueryContainer = servJson.getJsonObject(FixHeader.HEADER_SMS_QUERY_CONTAINER);
        checkInstanceArr(FixHeader.HEADER_SMS_QUERY, servInstID, smsQueryContainer, servType);
        
        return true;
    }

    private void checkInstanceArr(String header, String servInstId, JsonObject container, String servType) {
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
            String port = item.getString(FixHeader.HEADER_VERTX_PORT);
            
            AlarmType alarm = AlarmType.ALARM_APP_PROC_DOWN;
            String key = String.format("alarm-%s-%d", instId, alarm.getCode());
            JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
            
            try {
                // 进程异常
                if (!doCheck(cmptName, instId, ip, port)) {
                    // 在告警窗口内不重复生成
                    generateAlarm(jedisClient, key, servInstId, servType, instId, cmptName, alarm);
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

    private boolean doCheck(String cmptName, String instId, String ip, String port) {
        boolean ret = false;
        int retry = 0, failCnt = 0;
        String url = String.format("http://%s:%s/%s", ip, port, FixDefs.HEALTH_CHECK_URI);
        SVarObject sVar = new SVarObject();
        
        while (retry < MAX_RETRY) {
            retry++;
            
            try {
                if (HttpCommonTools.getData(url, null, sVar)) {
                    ret = true;
                    break;
                }
            } catch (Exception e) {
                failCnt++;
            }
        }
        
        return (!ret && failCnt > 0) ? false : true;
    }

}
