package com.zzstack.paas.underlying.metasvr.autocheck.probe;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.metasvr.alarm.AlarmType;
import com.zzstack.paas.underlying.metasvr.autodeploy.DeployerFactory;
import com.zzstack.paas.underlying.metasvr.autodeploy.ServiceDeployer;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.SequenceId;
import com.zzstack.paas.underlying.metasvr.exception.SeqException;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.bean.SVarObject;

import io.vertx.core.json.JsonObject;
import redis.clients.jedis.JedisCluster;

public class BaseProber {
    
    protected static final Logger logger = LoggerFactory.getLogger(BaseProber.class);
    
    protected String alarmApplicationCode;
    
    private String servInstID;
    private String servType;
    
    private static final String HEADER_APPLICATION_CODE = "applicationCode";
    private static final String HEADER_ALARM_CODE       = "alarmCode";
    private static final String HEADER_VARIABLE         = "variable";
    private static final String HEADER_ABSTRACT_MSG     = "abstractMsg";
    private static final String HEADER_MSG              = "msg";
    private static final String HEADER_CODE             = "code";
    private static final String HEADER_MESSAGE          = "message";
    
    protected static int MAX_RETRY = 3;
    
    private static Map<String, String> HEADERS = null;
    
    static {
        HEADERS = new HashMap<String, String>();
        HEADERS.put("Content-Type", "application/json");
    }
    
    public BaseProber(String servInstID, String servType) {
        this.servInstID = servInstID;
        this.servType = servType;
    }
    
    public BaseProber(String servInstID, String servType, String alarmApplicationCode) {
        this.servInstID = servInstID;
        this.servType = servType;
        this.alarmApplicationCode = alarmApplicationCode;
    }
    
    public JsonObject loadMetaData() {
        JsonObject retJson = new JsonObject();
        if (!MetaDataDao.loadServiceTopo(retJson, servInstID)) {
            return null;
        }
        
        String servRootCmptName = MetaSvrGlobalRes.get().getCmptMeta().getServRootCmpt(servType);
        if (servRootCmptName == null) {
            logger.error("servRootCmpt not found:{}", servType);
        }
          
        JsonObject topoJson = retJson.getJsonObject(FixHeader.HEADER_RET_INFO);
        JsonObject servJson = topoJson.getJsonObject(servRootCmptName);
        
        return servJson;
    }
    
    /**
     * 
     * @param applicationCode 应用编码
     * @param alarmCode       告警编码
     * @param variable        变量     非必填
     * @param abstractMsg     摘要
     * @param msg             详情
     */
    public boolean notifyAlarmCenter(String applicationCode, String alarmCode, String variable, String abstractMsg, String msg) {
        if (!SysConfig.get().isAlarmNotifyEnabled())
            return true;
        
        String url = String.format("%s/%s", SysConfig.get().getAlarmNotifyUrl(), FixDefs.ADD_ALARM_EVENT_URI);
        
        JSONObject reqData = new JSONObject();
        reqData.put(HEADER_APPLICATION_CODE, applicationCode);
        reqData.put(HEADER_ALARM_CODE,       alarmCode);
        reqData.put(HEADER_VARIABLE,         variable);
        reqData.put(HEADER_ABSTRACT_MSG,     abstractMsg);
        reqData.put(HEADER_MSG,              msg);
        
        SVarObject httpResult = new SVarObject();
        try {
            if (!HttpCommonTools.postData(url, HEADERS, reqData.toString(), httpResult))
                return false;
            
            JSONObject jsonResult = JSONObject.parseObject(httpResult.getVal());
            logger.info(jsonResult.toString());
            
            String code = jsonResult.getString(HEADER_CODE);
            String message = jsonResult.getString(HEADER_MESSAGE);
            if (!code.equals("200")) {
                logger.error("send alarm event fail: {}", message);
                return false;
            }
        
        } catch (IOException e) {
            logger.error("send alarm event caught exception: {}", e.getMessage(), e);
            return false;
        }
        
        return true;
    }

    protected void generateAlarm(JedisCluster jedisClient, String key, String servInstId, String servType, String instId, String cmptName, AlarmType alarm) throws SeqException {
        ResultBean result = new ResultBean();
        MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_ALARM, result, "");
        
        boolean exist = jedisClient.exists(key);
        if (!exist) {
            long alarmId = SequenceId.get().getNextId(FixDefs.SEQ_ALARM);
            
            logger.error("proc down, SERV_INST_ID:{}, INST_ID:{}, CMPT_NAME:{}", servInstId, instId, cmptName);
            jedisClient.set(key, String.valueOf(alarmId));
            jedisClient.pexpire(key, SysConfig.get().getAlarmTimeWindow());
            
            MetaDataDao.insertAlarm(alarmId, servInstId, servType, instId, cmptName, alarm.getCode(), System.currentTimeMillis());
            
            if (SysConfig.get().isAlarmNotifyEnabled()) {
                CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
                String sshId = cmptMeta.getInstAttr(instId, 116).getAttrValue();  // 116 -> 'SSH_ID'
                String ip = cmptMeta.getSshById(sshId).getServerIp();
                String alarmCode = AlarmType.ALARM_APP_PROC_DOWN.getInfo();
                String msg = String.format("servInstId: %s, instId: %s, cmptName: %s, ip: %s", servInstId, instId, cmptName, ip);
                notifyAlarmCenter(alarmApplicationCode, alarmCode, instId, alarmCode, msg);
            }
        }
        
        if (alarm == AlarmType.ALARM_APP_PROC_DOWN) {
            doRecover(servInstId, servType, instId, cmptName);
        }
    }
    
    protected void doRecover(String servInstId, String servType, String instId, String cmptName) {
        // 先尝试恢复异常的进程，异常进程不能恢复的情况下再通过拉起预埋的配置来达到异常隔离
        if (recoverCashedProc(servInstId, servType, instId, cmptName)) {
            return;
        }
        
        if (recoverBackupProc(servInstId, servType, instId, cmptName)) {
            logger.info("servType: {}, cmptName: {}, instId: {} 异常恢复失败，尝试拉起备份进程成功", servType, cmptName, instId);
        } else {
            logger.info("servType: {}, cmptName: {}, instId: {} 异常恢复失败，尝试拉起备份进程失败", servType, cmptName, instId);
        }
        
    }
    
    private boolean recoverCashedProc(String servInstId, String servType, String instId, String cmptName) {
        boolean startResult = false;
        String logKey = "";
        String magicKey = "";
        ResultBean result = new ResultBean();
        InstanceOperationEnum op = InstanceOperationEnum.INSTANCE_OPERATION_START;
        
        ServiceDeployer serviceDeployer = DeployerFactory.getDeployer(servType, logKey);
        startResult = serviceDeployer.maintainInstance(servInstID, instId, servType, op, false, logKey, magicKey, result);
        
        return startResult;
    }
    
    private boolean recoverBackupProc(String servInstId, String servType, String instId, String cmptName) {
        if (cmptName.equals(FixHeader.HEADER_SMS_SERVER)) {
            // smsserver 运维需求不需要拉起备份，因要手工配套修改防火墙路由配置
            return false;
        }
        
        logger.info("servType: {}, cmptName: {}, instId: {} 异常恢复失败，尝试拉起备份进程", servType, cmptName, instId);
        
        // 首先查找对应的预埋配置
        String preEmbeddedInstId = findPreEmbeddedInst(servInstId, servType, instId, cmptName);
        // 拉起预埋配置对应的实例, 预埋配置已经提前做了包安装以便缩短恢复需要的时间
        if (StringUtils.isEmpty(preEmbeddedInstId)) {
            logger.error("servType: {}, cmptName: {}, instId: {} 异常恢复失败，找不到预埋配置", servType, cmptName, instId);
            return false;
        }
        
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance preEmbaddedInst = cmptMeta.getInstance(preEmbeddedInstId);
        if (!preEmbaddedInst.isDeployed()) {
            logger.error("servType: {}, cmptName: {}, instId: {} 异常恢复失败，预埋实例未部署 {}", servType, cmptName, instId, preEmbeddedInstId);
            return false;
        }
        
        boolean recoverResult = recoverCashedProc(servInstId, servType, preEmbeddedInstId, cmptName);
        if (recoverResult) {
            // 预埋实例拉起成功，修改PRE_EMBEDDED属性为S_FALSE，视为与正常实例一样
            ResultBean result = new ResultBean();
            String magicKey = "";
            recoverResult = MetaDataDao.updateInstancePreEmbadded(preEmbeddedInstId, FixDefs.S_FALSE, result, magicKey);
            
            // 将老的置成预埋状态
            ResultBean result1 = new ResultBean();
            MetaDataDao.updateInstancePreEmbadded(instId, FixDefs.S_TRUE, result1, magicKey);
            MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_PRE_EMBADDED, result, magicKey);
        }
        
        return recoverResult;
    }
    
    private String findPreEmbeddedInst(String servInstId, String servType, String instId, String cmptName) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Collection<PaasTopology> topoList = cmptMeta.getSameLevelInstList(servInstId, instId);
        if (topoList == null || topoList.isEmpty())
            return null;
        
        String embaddedInstId = null;
        switch(cmptName) {
        case FixHeader.HEADER_SMS_PROCESS:
            embaddedInstId = getPreEmbaddedSmsProcess(topoList, instId);
            break;
        case FixHeader.HEADER_SMS_CLIENT:
            embaddedInstId = getPreEmbaddedSmsClient(topoList, instId);
            break;
        case FixHeader.HEADER_SMS_BATSAVE:
            embaddedInstId = getPreEmbaddedSmsBatSave(topoList, instId);
            break;
        case FixHeader.HEADER_SMS_STATS:
            embaddedInstId = getPreEmbaddedSmsStats(topoList, instId);
            break;
        case FixHeader.HEADER_SMS_QUERY:
            embaddedInstId = getPreEmbaddedSmsQuery(topoList, instId);
            break;
        default:
            break;
        }
        
        return embaddedInstId;
    }

    protected void clearAlarm(JedisCluster jedisClient, String key, String instId) {
        if (jedisClient.exists(key)) {
            String s = jedisClient.get(key);
            if (s != null) {
                long alarmId = Long.valueOf(s);
                ResultBean result = new ResultBean();
                MetaDataDao.updateAlarmStateByAlarmId(alarmId, System.currentTimeMillis(), FixDefs.SYS_USER, FixDefs.ALARM_DEALED, result);
                jedisClient.del(key);
            }
        }
        
        ResultBean result = new ResultBean();
        MetaDataDao.updateInstanceDeployFlag(instId, FixDefs.STR_DEPLOYED, result, "");
    }
    
    private String getPreEmbaddedSmsProcess(Collection<PaasTopology> topoList, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        String processor = cmptMeta.getInstAttr(instId, 205).getAttrValue();  // 205 -> 'PROCESSOR'
        String dbInstId = cmptMeta.getInstAttr(instId, 213).getAttrValue();   // 213 -> 'DB_INST_ID'
        PaasInstance orignalInst = cmptMeta.getInstance(instId);
        
        for (PaasTopology topo : topoList) {
            String id = topo.getInstId2();
            if (id.equals(instId))
                continue;
            
            PaasInstance preEmbaddedInst = cmptMeta.getInstance(id);
            if (preEmbaddedInst.getCmptId() != orignalInst.getCmptId())
                continue;
            
            PaasInstAttr preEmbaddedAttr = cmptMeta.getInstAttr(id, 320);             // 320 -> 'PRE_EMBEDDED'
            if (preEmbaddedAttr == null)
                continue;
            String preEmbadded = preEmbaddedAttr.getAttrValue();
            if (preEmbadded == null || preEmbadded.equals(FixDefs.S_FALSE))
                continue;
            
            if (cmptMeta.getInstance(id).getStatus().equals(FixDefs.STR_DEPLOYED)) {
                continue;
            }
            
            String embaddedProcessor = cmptMeta.getInstAttr(id, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            String embaddedDbInstId = cmptMeta.getInstAttr(id, 213).getAttrValue();   // 213 -> 'DB_INST_ID'
            if (processor.equals(embaddedProcessor) && dbInstId.equals(embaddedDbInstId)) {
                return id;
            }
        }
        
        return null;
    }
    
    private String getPreEmbaddedSmsClient(Collection<PaasTopology> topoList, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        String processor = cmptMeta.getInstAttr(instId, 205).getAttrValue();  // 205 -> 'PROCESSOR'
        PaasInstance orignalInst = cmptMeta.getInstance(instId);
        
        for (PaasTopology topo : topoList) {
            String id = topo.getInstId2();
            if (id.equals(instId))
                continue;
            
            PaasInstance preEmbaddedInst = cmptMeta.getInstance(id);
            if (preEmbaddedInst.getCmptId() != orignalInst.getCmptId())
                continue;
            
            PaasInstAttr preEmbaddedAttr = cmptMeta.getInstAttr(id, 320);             // 320 -> 'PRE_EMBEDDED'
            if (preEmbaddedAttr == null)
                continue;
            String preEmbadded = preEmbaddedAttr.getAttrValue();
            if (preEmbadded == null || preEmbadded.equals(FixDefs.S_FALSE))
                continue;
            
            if (cmptMeta.getInstance(id).getStatus().equals(FixDefs.STR_DEPLOYED)) {
                continue;
            }
            
            String embaddedProcessor = cmptMeta.getInstAttr(id, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            if (processor.equals(embaddedProcessor)) {
                return id;
            }
        }
        
        return null;
    }
    
    private String getPreEmbaddedSmsBatSave(Collection<PaasTopology> topoList, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        String processor = cmptMeta.getInstAttr(instId, 205).getAttrValue();  // 205 -> 'PROCESSOR'
        String dbInstId = cmptMeta.getInstAttr(instId, 213).getAttrValue();   // 213 -> 'DB_INST_ID'
        PaasInstance orignalInst = cmptMeta.getInstance(instId);
        
        for (PaasTopology topo : topoList) {
            String id = topo.getInstId2();
            if (id.equals(instId))
                continue;
            
            PaasInstance preEmbaddedInst = cmptMeta.getInstance(id);
            if (preEmbaddedInst.getCmptId() != orignalInst.getCmptId())
                continue;
            
            PaasInstAttr preEmbaddedAttr = cmptMeta.getInstAttr(id, 320);             // 320 -> 'PRE_EMBEDDED'
            if (preEmbaddedAttr == null)
                continue;
            String preEmbadded = preEmbaddedAttr.getAttrValue();
            if (preEmbadded == null || preEmbadded.equals(FixDefs.S_FALSE))
                continue;
            
            if (cmptMeta.getInstance(id).getStatus().equals(FixDefs.STR_DEPLOYED)) {
                continue;
            }
            
            String embaddedProcessor = cmptMeta.getInstAttr(id, 205).getAttrValue();  // 205 -> 'PROCESSOR'
            String embaddedDbInstId = cmptMeta.getInstAttr(id, 213).getAttrValue();   // 213 -> 'DB_INST_ID'
            if (processor.equals(embaddedProcessor) && dbInstId.equals(embaddedDbInstId)) {
                return id;
            }
        }
        
        return null;
    }
    
    private String getPreEmbaddedSmsStats(Collection<PaasTopology> topoList, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance orignalInst = cmptMeta.getInstance(instId);
        
        for (PaasTopology topo : topoList) {
            String id = topo.getInstId2();
            if (id.equals(instId))
                continue;
            
            PaasInstance preEmbaddedInst = cmptMeta.getInstance(id);
            if (preEmbaddedInst.getCmptId() != orignalInst.getCmptId())
                continue;
            
            PaasInstAttr preEmbaddedAttr = cmptMeta.getInstAttr(id, 320);             // 320 -> 'PRE_EMBEDDED'
            if (preEmbaddedAttr == null)
                continue;
            String preEmbadded = preEmbaddedAttr.getAttrValue();
            if (preEmbadded == null || preEmbadded.equals(FixDefs.S_FALSE))
                continue;
            
            if (cmptMeta.getInstance(id).getStatus().equals(FixDefs.STR_DEPLOYED)) {
                continue;
            }
            
            return id;
        }
        
        return null;
    }
    
    private String getPreEmbaddedSmsQuery(Collection<PaasTopology> topoList, String instId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasInstance orignalInst = cmptMeta.getInstance(instId);
        
        for (PaasTopology topo : topoList) {
            String id = topo.getInstId2();
            if (id.equals(instId))
                continue;
            
            PaasInstance preEmbaddedInst = cmptMeta.getInstance(id);
            if (preEmbaddedInst.getCmptId() != orignalInst.getCmptId())
                continue;
            
            PaasInstAttr preEmbaddedAttr = cmptMeta.getInstAttr(id, 320);             // 320 -> 'PRE_EMBEDDED'
            if (preEmbaddedAttr == null)
                continue;
            String preEmbadded = preEmbaddedAttr.getAttrValue();
            if (preEmbadded == null || preEmbadded.equals(FixDefs.S_FALSE))
                continue;
            
            if (cmptMeta.getInstance(id).getStatus().equals(FixDefs.STR_DEPLOYED)) {
                continue;
            }
            
            return id;
        }
        
        return null;
    }

}
