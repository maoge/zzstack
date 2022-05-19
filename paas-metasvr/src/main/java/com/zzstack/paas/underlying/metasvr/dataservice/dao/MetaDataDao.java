package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.LongMargin;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.alarm.AlarmType;
import com.zzstack.paas.underlying.metasvr.bean.AccountBean;
import com.zzstack.paas.underlying.metasvr.bean.AccountSessionBean;
import com.zzstack.paas.underlying.metasvr.bean.PaasCmptVer;
import com.zzstack.paas.underlying.metasvr.bean.PaasDeployFile;
import com.zzstack.paas.underlying.metasvr.bean.PaasDeployHost;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasInstance;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaAttr;
import com.zzstack.paas.underlying.metasvr.bean.PaasMetaCmpt;
import com.zzstack.paas.underlying.metasvr.bean.PaasPos;
import com.zzstack.paas.underlying.metasvr.bean.PaasServer;
import com.zzstack.paas.underlying.metasvr.bean.PaasService;
import com.zzstack.paas.underlying.metasvr.bean.PaasSsh;
import com.zzstack.paas.underlying.metasvr.bean.PaasTopology;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.sql.MetaDataSql;
import com.zzstack.paas.underlying.metasvr.eventbus.EventBean;
import com.zzstack.paas.underlying.metasvr.eventbus.EventBusMsg;
import com.zzstack.paas.underlying.metasvr.eventbus.EventType;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.CacheKeyUtils;
import com.zzstack.paas.underlying.metasvr.utils.PasswdUtils;
import com.zzstack.paas.underlying.metasvr.utils.SmsWebConsoleConnector;
import com.zzstack.paas.underlying.metasvr.utils.StringUtils;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.UUIDUtils;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import redis.clients.jedis.JedisCluster;

import com.zzstack.paas.underlying.metasvr.utils.DateFmtUtil;

public class MetaDataDao {

    private static Logger logger = LoggerFactory.getLogger(MetaDataDao.class);
    
    private static final int NOTIFY_RETRY_CNT = 3;
    
    private static String METADB_NAME = null;
    
    static {
        METADB_NAME = SysConfig.get().getMetaDBYamlName();
    }
    
    public static boolean loadAccount(Map<String, AccountBean> accountMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_ACCOUNT);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                AccountBean account = AccountBean.convert(item);
                accountMap.put(account.getAccName(), account);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }
    
    public static AccountSessionBean getSessionFromRedis(String accName) {
        String key = CacheKeyUtils.getRedisSessionKey(accName);
        JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
        String sessionData = jedisClient.get(key);
        if (sessionData == null || sessionData.isEmpty())
            return null;
        
        return AccountSessionBean.fromJson(sessionData);
    }
    
    public static void putSessionToRedis(AccountSessionBean accSession) {
        String strJson = accSession.toJson();
        String key = CacheKeyUtils.getRedisSessionKey(accSession.getAccName());
        JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
        jedisClient.set(key, strJson);
    }
    
    public static boolean login(String user, String passwd, ResultBean result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        AccountBean account = meta.getAccount(user);
        if (account == null) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_ACCOUNT_NOT_EXISTS);
            return false;
        }
        
        String encrypt = PasswdUtils.generatePasswd(user, passwd);
        if (!encrypt.equals(account.getPasswd())) {
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_PWD_INCORRECT);
            return false;
        }
        
        AccountSessionBean session = meta.getAccSession(user);
        if (session == null) {
            session = getSessionFromRedis(user);
            
            if (session == null) {
                String magicKey = UUIDUtils.genUUID();
                session = new AccountSessionBean(user, magicKey);
                meta.addAccSession(session, false);
            } else {
                // redis session已经过期, 重新生成
                if (!session.isSessionValid()) {
                    String oldKey = session.getMagicKey();
                    meta.removeTtlSession(user, oldKey, false);
                    
                    String magicKey = UUIDUtils.genUUID();
                    session = new AccountSessionBean(user, magicKey);
                    meta.addAccSession(session, false);
                } else {
                    meta.addAccSession(session, true);
                }
            }
            
            result.setRetCode(CONSTS.REVOKE_OK);
            result.setRetInfo(session.getMagicKey());
            return true;
        }
        
        // 本地session已经过期, 重新生成
        if (!session.isSessionValid()) {
            String oldKey = session.getMagicKey();
            meta.removeTtlSession(user, oldKey, false);
            
            String magicKey = UUIDUtils.genUUID();
            session = new AccountSessionBean(user, magicKey);
            meta.addAccSession(session, false);
        }
        
        result.setRetCode(CONSTS.REVOKE_OK);
        result.setRetInfo(session.getMagicKey());
        return true;
    }

    public static void modPasswd(String magicKey, String passwd, JsonObject result) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        AccountSessionBean accSession = meta.getSessionByMagicKey(magicKey);
        if (accSession == null || !accSession.isSessionValid()) {
            result.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            result.put(FixHeader.HEADER_RET_INFO, "登陆认证过期或未登陆，请重新登陆再改密!");
            return;
        }
        
        String accName = meta.getAccNameByMagicKey(magicKey);
        try {
            String encrypt = PasswdUtils.generatePasswd(accName, passwd);
            
            SqlBean sqlBean = new SqlBean(MetaDataSql.UPD_ACC_PASSWD);
            sqlBean.addParams(new Object[] { encrypt, accName });
            
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            
            if (c.executeUpdate()) {
                meta.modPasswd(accName, encrypt);
                
                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_ACC_NAME, accName);
                msgBody.put(FixHeader.HEADER_PASSWORD, encrypt);
                
                EventBean ev = new EventBean(EventType.EVENT_MOD_ACC_PASSWD, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);
            } else {
                logger.error("exec update:{} fail, accName:{}, passwd:{}", MetaDataSql.UPD_ACC_PASSWD, accName, passwd);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static boolean getServiceById(String instId, PaasService service) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_SERVICE_BY_ID);
            sqlBean.addParams(new Object[] { instId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            Map<String, Object> mapper = c.queryForMap();
            PaasService.convert(mapper, service);

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaAttr(Map<Integer, PaasMetaAttr> metaAttrIdMap,
            Map<String, PaasMetaAttr> metaAttrNameMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_ATTR);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasMetaAttr attr = PaasMetaAttr.convert(item);

                metaAttrIdMap.put(attr.getAttrId(), attr);
                metaAttrNameMap.put(attr.getAttrName(), attr);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaCmpt(Map<Integer, PaasMetaCmpt> metaCmptIdMap,
            Map<String, PaasMetaCmpt> metaCmptNameMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_CMPT);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasMetaCmpt cmpt = PaasMetaCmpt.convert(item);

                metaCmptIdMap.put(cmpt.getCmptId(), cmpt);
                metaCmptNameMap.put(cmpt.getCmptName(), cmpt);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaCmptAttr(Multimap<Integer, Integer> metaCmptAttrMMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_CMPT_ATTR);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                Integer cmptId = (Integer) item.get(FixHeader.HEADER_CMPT_ID);
                Integer attrId = (Integer) item.get(FixHeader.HEADER_ATTR_ID);

                metaCmptAttrMMap.put(cmptId, attrId);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaInst(Map<String, PaasInstance> metaInstMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_INST);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasInstance inst = PaasInstance.convert(item);
                metaInstMap.put(inst.getInstId(), inst);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaInstAttr(Multimap<String, PaasInstAttr> metaInstAttrMMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_INST_ATTR);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasInstAttr instAttr = PaasInstAttr.convert(item);
                metaInstAttrMMap.put(instAttr.getInstId(), instAttr);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaService(Map<String, PaasService> metaServiceMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_SERVICE);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasService service = PaasService.convert(item);
                metaServiceMap.put(service.getInstId(), service);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaTopo(Multimap<String, PaasTopology> metaTopoMMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_TOPO);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasTopology topo = PaasTopology.convert(item);
                metaTopoMMap.put(topo.getInstId1(), topo);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadDeployHost(Map<Integer, PaasDeployHost> metaDeployHostMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_DEP_HOST);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasDeployHost deployHost = PaasDeployHost.convert(item);
                metaDeployHostMap.put(deployHost.getHostId(), deployHost);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadDeployFile(Map<Integer, PaasDeployFile> metaDeployFileMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_DEP_FILE);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasDeployFile deployFile = PaasDeployFile.convert(item);
                metaDeployFileMap.put(deployFile.getFileId(), deployFile);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaServer(Map<String, PaasServer> metaServerMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_SERVER);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasServer server = PaasServer.convert(item);
                metaServerMap.put(server.getServerIp(), server);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    public static boolean loadMetaSsh(Multimap<String, PaasSsh> metaSshMMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_SSH);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;

            for (HashMap<String, Object> item : resultList) {
                PaasSsh ssh = PaasSsh.convert(item);
                metaSshMMap.put(ssh.getServerIp(), ssh);
            }

            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }
    
    public static boolean loadMetaCmptVersion(Map<String,  PaasCmptVer> metaCmptVerMap) {
        boolean ret = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SEL_META_CMPT_VER);
            
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null)
                return true;
            
            String lastServType = "";
            PaasCmptVer lastCmptVer = null;
            for (HashMap<String, Object> item : resultList) {
                String servType = (String) item.get(FixHeader.HEADER_SERV_TYPE);
                String version = (String) item.get(FixHeader.HEADER_VERSION);
                
                if (!lastServType.equals(servType)) {
                    lastServType = servType;
                    lastCmptVer = new PaasCmptVer(servType);
                    lastCmptVer.addVersion(version);
                    metaCmptVerMap.put(servType, lastCmptVer);
                } else {
                    lastCmptVer.addVersion(version);
                }
            }
            
            ret = true;
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
        }
        return ret;
    }

    public static void getServiceCnt(JsonObject retval, String servName, String servClazz, String servType) {
        StringBuilder sql = new StringBuilder(MetaDataSql.SQL_COUNT_SERVICE_LIST);
        if (servName != null && !servName.isEmpty()) {
            sql.append(" AND SERV_NAME=").append("'").append(servName).append("' ");
        }
        if (servClazz != null && !servClazz.isEmpty()) {
            sql.append(" AND SERV_CLAZZ=").append("'").append(servClazz).append("' ");
        }
        if (servType != null && !servType.isEmpty()) {
            sql.append(" AND SERV_TYPE=").append("'").append(servType).append("' ");
        }

        try {
            SqlBean sqlBean = new SqlBean(sql.toString());

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            int val = c.queryForCount();
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, val);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }
    
    public static void getServTypeVerCount(JsonObject retval, String servType) {
        StringBuilder sql = new StringBuilder(MetaDataSql.SQL_COUNT_SERV_TYPE_VER_LIST);
        if (servType != null && !servType.isEmpty()) {
            sql.append(" AND SERV_TYPE=").append("'").append(servType).append("' ");
        }
        
        try {
            SqlBean sqlBean = new SqlBean(sql.toString());

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            int val = c.queryForCount();
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, val);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getServiceList(JsonObject retval, int pageSize, int pageNum, String servInstID, String servName, String servClazz,
            String servType) {

        StringBuilder sqlWhere = new StringBuilder("");
        if (servInstID != null && !servInstID.isEmpty()) {
            sqlWhere.append(" AND INST_ID=").append("'").append(servInstID).append("' ");
        }
        if (servName != null && !servName.isEmpty()) {
            sqlWhere.append(" AND SERV_NAME=").append("'").append(servName).append("' ");
        }
        if (servClazz != null && !servClazz.isEmpty()) {
            sqlWhere.append(" AND SERV_CLAZZ=").append("'").append(servClazz).append("' ");
        }
        if (servType != null && !servType.isEmpty()) {
            sqlWhere.append(" AND SERV_TYPE=").append("'").append(servType).append("' ");
        }

        int start = pageSize * (pageNum - 1);
        String sql = String.format(MetaDataSql.SQL_SEL_SERVICE_LIST, sqlWhere.toString(), start, pageSize);
        try {
            SqlBean sqlBean = new SqlBean(sql);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }

            JsonArray svcArr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    item.put(entry.getKey(), entry.getValue());
                }
                svcArr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, svcArr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }

    }
    
    public static void getServTypeVerListByPage(JsonObject retval, int pageSize, int pageNum, String servType) {
        StringBuilder sqlWhere = new StringBuilder("");
        if (servType != null && !servType.isEmpty()) {
            sqlWhere.append(" AND SERV_TYPE=").append("'").append(servType).append("' ");
        }

        int start = pageSize * (pageNum - 1);
        String sql = String.format(MetaDataSql.SQL_SEL_SERV_TYPE_VER_LIST, sqlWhere.toString(), start, pageSize);
        try {
            SqlBean sqlBean = new SqlBean(sql);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }

            JsonArray servTypeVerArr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    item.put(entry.getKey(), entry.getValue());
                }
                servTypeVerArr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, servTypeVerArr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }

    }
    
    public static void getClickHouseDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        boolean ret = false;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance instance = cmptMeta.getInstance(toeId);
            if (instance == null)
                continue;
            
            int cmptId = instance.getCmptId();
            // 102 -> 'GRAFANA'
            if (cmptId == 102) {
                String httpPort = cmptMeta.getInstAttr(toeId, 122).getAttrValue();    // 'HTTP_PORT'),
                String sshId = cmptMeta.getInstAttr(toeId, 116).getAttrValue();       // 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String serverIp = ssh.getServerIp();
                String url = String.format("http://%s:%s", serverIp, httpPort);
                
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, url);
                
                ret = true;
                
                break;
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }
    
    public static void getVoltDBDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        boolean ret = false;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance voltdbContainer = cmptMeta.getInstance(toeId);
            if (voltdbContainer == null)
                continue;
            
            int cmptId = voltdbContainer.getCmptId();
            // 291 -> 'VOLTDB_CONTAINER'
            if (cmptId == 291) {
                Vector<PaasTopology> subRelations = new Vector<PaasTopology>();
                cmptMeta.getInstRelations(voltdbContainer.getInstId(), subRelations);
                if (subRelations.isEmpty())
                    continue;
                
                for (PaasTopology voltdbRelation : subRelations) {
                    String voltdbInstId = voltdbRelation.getToe(voltdbContainer.getInstId());
                    PaasInstance voltdb = cmptMeta.getInstance(voltdbInstId);
                    if (voltdbInstId != null && voltdb.isDeployed()) {
                        String httpPort = cmptMeta.getInstAttr(voltdbInstId, 256).getAttrValue();    // 'VOLT_WEB_PORT'
                        String sshId = cmptMeta.getInstAttr(voltdbInstId, 116).getAttrValue();       // 'SSH_ID'
                        PaasSsh ssh = cmptMeta.getSshById(sshId);
                        String serverIp = ssh.getServerIp();
                        String url = String.format("http://%s:%s", serverIp, httpPort);
                        
                        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                        retval.put(FixHeader.HEADER_RET_INFO, url);
                        
                        ret = true;
                        
                        break;
                    }
                }
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }
    
    public static void getRocketMQDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        boolean ret = false;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance instance = cmptMeta.getInstance(toeId);
            if (instance == null)
                continue;
            
            int cmptId = instance.getCmptId();
            // 246 -> 'ROCKETMQ_CONSOLE'
            if (cmptId == 246) {
                String consolePort = cmptMeta.getInstAttr(toeId, 249).getAttrValue();    // 'CONSOLE_PORT'
                String sshId = cmptMeta.getInstAttr(toeId, 116).getAttrValue();          // 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String serverIp = ssh.getServerIp();
                String url = String.format("http://%s:%s", serverIp, consolePort);
                
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, url);
                
                ret = true;
                
                break;
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }
    
    public static void getTiDBDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        boolean ret = false;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance instance = cmptMeta.getInstance(toeId);
            if (instance == null)
                continue;
            
            int cmptId = instance.getCmptId();
            // 'DASHBOARD_PROXY'
            if (cmptId == 217) {
                String dashboardPort = cmptMeta.getInstAttr(toeId, 230).getAttrValue();  // 'DASHBOARD_PORT'
                String sshId = cmptMeta.getInstAttr(toeId, 116).getAttrValue();          // 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String serverIp = ssh.getServerIp();
                String url = String.format("http://%s:%s/dashboard", serverIp, dashboardPort);
                
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, url);
                
                ret = true;
                
                break;
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }

    public static void getPulsarDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        boolean ret = false;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance instance = cmptMeta.getInstance(toeId);
            if (instance == null)
                continue;
            
            int cmptId = instance.getCmptId();
            // 263 -> 'PULSAR_MANAGER'
            if (cmptId == 263) {
                String httpPort = cmptMeta.getInstAttr(toeId, 270).getAttrValue();  // 'PULSAR_MGR_PORT'
                String sshId = cmptMeta.getInstAttr(toeId, 116).getAttrValue();     // 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String serverIp = ssh.getServerIp();
                String url = String.format("http://%s:%s/ui/index.html", serverIp, httpPort);
                
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, url);
                
                ret = true;
                
                break;
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }
    
    public static void getYugaByteDashboardAddr(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, relations);
        if (relations.isEmpty()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
            return;
        }
        
        String ybMasterContainerID = null;
        for (PaasTopology relation : relations) {
            String toeId = relation.getToe(servInstId);
            PaasInstance instance = cmptMeta.getInstance(toeId);
            if (instance == null)
                continue;
            
            int cmptId = instance.getCmptId();
            // 301 -> 'YB_MASTER_CONTAINER'
            if (cmptId == 301) {
                ybMasterContainerID = toeId;
                break;
            }
        }
        
        boolean ret = false;
        if (ybMasterContainerID != null) {
            Vector<PaasTopology> subRelations = new Vector<PaasTopology>();
            cmptMeta.getInstRelations(ybMasterContainerID, subRelations);
            for (PaasTopology relation : subRelations) {
                String toeId = relation.getToe(ybMasterContainerID);
                PaasInstance instance = cmptMeta.getInstance(toeId);
                if (instance == null)
                    continue;
                
                String webServPort = cmptMeta.getInstAttr(toeId, 275).getAttrValue();  // 'WEBSERVER_PORT'
                String sshId = cmptMeta.getInstAttr(toeId, 116).getAttrValue();        // 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String serverIp = ssh.getServerIp();
                
                String url = String.format("http://%s:%s", serverIp, webServPort);
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, url);
                
                ret = true;
                break;
            }
        }
        
        if (!ret) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, "");
        }
    }

    public static void addService(JsonObject retval, String instId, String servName, String servClazz, String servType,
            String version, String isProduct, String user, String password, String magicKey) {

        try {
            if (MetaSvrGlobalRes.get().getCmptMeta().isServiceNameExists(servName)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SERVICE_NAME_EXISTS);
                return;
            }
            
            long ts = System.currentTimeMillis();

            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_ADD_SERVICE);
            sqlBean.addParams(new Object[] { instId, servName, servClazz, servType, version, CONSTS.STR_FALSE, isProduct,
                    ts, user, password, CONSTS.DEPLOY_FLAG_PHYSICAL });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            boolean res = c.executeUpdate();
            if (!res) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);

            // add to local
            boolean is_deployed = false;
            boolean is_product = isProduct.equals(CONSTS.STR_TRUE);
            PaasService service = new PaasService(instId, servName, servClazz, servType, version, is_deployed, is_product, ts,
                    user, password, CONSTS.DEPLOY_FLAG_PHYSICAL);
            MetaSvrGlobalRes.get().getCmptMeta().addService(service);

            JsonObject msgBody = service.toJsonObject();
            EventBean ev = new EventBean(EventType.EVENT_ADD_SERVICE, msgBody.toString(), magicKey);
            EventBusMsg.publishEvent(ev);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void delService(JsonObject retval, String instId, String magicKey) {
        PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(instId);
        if (serv == null) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
            return;
        } else {
            logger.info("is_deployed:{}, is_product:{}", serv.isDeployed(), serv.isProduct());

            if (serv.isDeployed()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_NO_DEL_DEPLOYED_SERV);
                return;
            }
        }

        try {
            JsonArray childArr = new JsonArray();
            getChildNodeExcludingServRoot(instId, childArr);

            Vector<EventBean> events = new Vector<EventBean>();
            if (enumDelService("", instId, childArr, retval, events, magicKey)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);

                for (EventBean ev : events) {
                    EventBusMsg.publishEvent(ev);

                    JsonObject obj = new JsonObject(ev.getMsgBody());
                    String id = obj.getString(FixHeader.HEADER_INST_ID);
                    switch (ev.getEvType()) {
                    case EVENT_DEL_SERVICE:
                        MetaSvrGlobalRes.get().getCmptMeta().delService(instId);
                        break;
                    case EVENT_DEL_INSTANCE:
                        MetaSvrGlobalRes.get().getCmptMeta().delInstance(id);
                        break;
                    case EVENT_DEL_INST_ATTR:
                        MetaSvrGlobalRes.get().getCmptMeta().delInstAttr(id);
                        break;
                    case EVENT_DEL_TOPO:
                        {
                            String supId = obj.getString(FixHeader.HEADER_PARENT_ID);
                            logger.info("EVENT_DEL_TOPO, parent_id:{}, inst_id:{}", supId, id);
                            MetaSvrGlobalRes.get().getCmptMeta().delTopo(supId, id);
                        }
                        break;

                    default:
                        break;
                    }
                }
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static boolean modService(JsonObject retval, String instId, String servName, String version, String isProduct, String magicKey) {
        boolean bResult = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_SERVICE);
            sqlBean.addParams(new Object[] { servName, version, isProduct, instId });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
				bResult = true;
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

            if (bResult) {
                // reload local
                MetaSvrGlobalRes.get().getCmptMeta().reloadService(instId);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);

                EventBean ev = new EventBean(EventType.EVENT_MOD_SERVICE, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);
			}

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
		
		return bResult;
    }

    public static boolean modServiceVersion(JsonObject retval, String instId, String version, String magicKey) {
        boolean bResult = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_SERVICE_VERSION);
            sqlBean.addParams(new Object[] { version, instId });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                bResult = true;
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

            if (bResult) {
                // reload local
                MetaSvrGlobalRes.get().getCmptMeta().reloadService(instId);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);

                EventBean ev = new EventBean(EventType.EVENT_MOD_SERVICE, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
        
        return bResult;
    }

    public static boolean modServicePseudoFlag(ResultBean retval, String instId, String pseudoFlag, String magicKey) {
        // SQL_MOD_SERVICE_PSEUDO_DEPLOY_FLAG = "UPDATE t_meta_service SET PSEUDO_DEPLOY_FLAG = ? WHERE INST_ID = ?";
        boolean bResult = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_SERVICE_PSEUDO_DEPLOY_FLAG);
            sqlBean.addParams(new Object[] { pseudoFlag, instId });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                retval.setRetCode(CONSTS.REVOKE_OK);
                bResult = true;
            } else {
                retval.setRetCode(CONSTS.REVOKE_NOK);
                retval.setRetInfo(CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

            // reload local
			if (bResult) {
                MetaSvrGlobalRes.get().getCmptMeta().reloadService(instId);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);

                EventBean ev = new EventBean(EventType.EVENT_MOD_SERVICE, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.setRetCode(CONSTS.REVOKE_NOK);
            retval.setRetInfo(CONSTS.ERR_DB);

        }
        return bResult;
    }

    public static void getServerCnt(JsonObject retval, String servIp, String servName) {
        StringBuilder sqlWhere = new StringBuilder("");
        if (servIp != null && !servIp.isEmpty()) {
            sqlWhere.append(" AND SERVER_IP=").append("'").append(servIp).append("' ");
        }
        if (servName != null && !servName.isEmpty()) {
            sqlWhere.append(" AND SERVER_NAME=").append("'").append(servName).append("' ");
        }

        try {
            String sql = String.format(MetaDataSql.SQL_COUNT_SERVER_LIST, sqlWhere.toString());

            SqlBean sqlBean = new SqlBean(sql);
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            int val = c.queryForCount();
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, val);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getServerList(JsonObject retval, int pageSize, int pageNum, String servIp, String servName) {
        int start = pageSize * (pageNum - 1);
        StringBuilder sqlWhere = new StringBuilder("");
        if (servIp != null && !servIp.isEmpty()) {
            sqlWhere.append(" AND SERVER_IP=").append("'").append(servIp).append("' ");
        }
        if (servName != null && !servName.isEmpty()) {
            sqlWhere.append(" AND SERVER_NAME=").append("'").append(servName).append("' ");
        }

        String sql = String.format(MetaDataSql.SQL_SEL_SERVER_LIST, sqlWhere.toString(), start, pageSize);

        try {
            SqlBean sqlBean = new SqlBean(sql);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            // JsonArray svrArr = c.queryForJSONArray();
            // if (svrArr == null) {
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }

            JsonArray svrArr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    item.put(entry.getKey(), entry.getValue());
                }
                svrArr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, svrArr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void addServer(JsonObject retval, String servIp, String servName, String magicKey) {
        try {
            if (MetaSvrGlobalRes.get().getCmptMeta().isServerIpExists(servIp)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SERVER_IP_EXISTS);
                return;
            }
            
            long ts = System.currentTimeMillis();

            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_ADD_SERVER);
            sqlBean.addParams(new Object[] { servIp, servName, ts });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                PaasServer server = new PaasServer(servIp, servName);

                // add local cache
                MetaSvrGlobalRes.get().getCmptMeta().addServer(server);

                // broadcast event to cluster
                JsonObject msgBody = server.toJsonObject();
                
                EventBean ev = new EventBean(EventType.EVENT_ADD_SERVER, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void delServer(JsonObject retval, String servIp, String magicKey) {
        if (!MetaSvrGlobalRes.get().getCmptMeta().isServerNull(servIp)) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SERVER_NOT_NULL);
            return;
        }

        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_DEL_SERVER);
            sqlBean.addParams(new Object[] { servIp });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                // delete local cache
                MetaSvrGlobalRes.get().getCmptMeta().delServer(servIp);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_SERVER_IP, servIp);

                EventBean ev = new EventBean(EventType.EVENT_DEL_SERVER, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getServiceById(JsonObject retval, String instId) {
        try {
            PaasService serv = MetaSvrGlobalRes.get().getCmptMeta().getService(instId);
            if (serv != null) {
                JsonObject node = new JsonObject();
                node.put("INST_ID", serv.getInstId());
                node.put("SERV_NAME", serv.getServName());
                node.put("SERV_CLAZZ", serv.getServClass());
                node.put("SERV_TYPE", serv.getServType());
                node.put("IS_DEPLOYED", serv.isDeployed());
                node.put("IS_PRODUCT", serv.isProduct());
                node.put("CREATE_TIME", serv.getCreateTime());
                node.put("USER", serv.getUser());
                node.put("PASSWORD", serv.getPassword());

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, node);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }

    public static void getSshListByIp(JsonObject retval, String servIp, int pageSize, int pageNum) {
        try {
            int start = pageSize * (pageNum - 1);
            String sql = String.format(MetaDataSql.SQL_SSH_LIST_BY_IP, start, pageSize);

            SqlBean sqlBean = new SqlBean(sql);
            sqlBean.addParams(new Object[] { servIp });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            // JsonArray svrArr = c.queryForJSONArray();
            // if (svrArr == null) {
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }

            JsonArray svrArr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    item.put(entry.getKey(), entry.getValue());
                }
                svrArr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, svrArr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getSshCntByIp(JsonObject retval, String servIp) {
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_SSH_CNT_BY_IP);
            sqlBean.addParams(new Object[] { servIp });
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            int val = c.queryForCount();
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, val);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void addSsh(JsonObject retval, String sshName, String sshPwd, int sshPort, String servClazz,
            String servIp, String magicKey) {

        try {
            String[] servClazzList = servClazz.split(",");
            Vector<EventBean> events = new Vector<EventBean>();
            
            for (String servClazzItem : servClazzList) {
                if (MetaSvrGlobalRes.get().getCmptMeta().isSshExists(sshName, servIp, servClazzItem)) {
                    String info = String.format("%s ssh exists: %s, %s", servIp, sshName, servClazzItem);
                    
                    retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                    retval.put(FixHeader.HEADER_RET_INFO, info);
                    return;
                }
            }

            for (String serv : servClazzList) {
                String id = UUIDUtils.genUUID();

                SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_ADD_SSH);
                sqlBean.addParams(new Object[] { sshName, sshPwd, sshPort, serv, servIp, id });

                CRUD c = new CRUD(METADB_NAME);
                c.putSqlBean(sqlBean);

                if (c.executeUpdate()) {
                    PaasSsh ssh = new PaasSsh(id, sshName, sshPwd, sshPort, serv, servIp);
                    MetaSvrGlobalRes.get().getCmptMeta().addSsh(ssh);

                    // broadcast event to cluster
                    JsonObject msgBody = ssh.toJsonObject();
                    EventBean ev = new EventBean(EventType.EVENT_ADD_SSH, msgBody.toString(), magicKey);
                    events.add(ev);
                } else {
                    retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                    retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                    logger.error("{}", CONSTS.ERR_DB);
                    break;
                }
            }

            for (EventBean ev : events) {
                EventBusMsg.publishEvent(ev);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void modSsh(JsonObject retval, String sshName, String sshPwd, int sshPort, String sshId,
            String serverIp, String magicKey) {

        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_SSH);
            sqlBean.addParams(new Object[] { sshName, sshPwd, sshPort, sshId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            if (c.executeUpdate()) {
                // update local cache
                MetaSvrGlobalRes.get().getCmptMeta().modSsh(serverIp, sshId, sshName, sshPwd, sshPort);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_SERVER_IP, serverIp);
                msgBody.put(FixHeader.HEADER_SSH_ID, sshId);
                msgBody.put(FixHeader.HEADER_SSH_NAME, sshName);
                msgBody.put(FixHeader.HEADER_SSH_PWD, sshPwd);
                msgBody.put(FixHeader.HEADER_SSH_PORT, sshPort);

                EventBean ev = new EventBean(EventType.EVENT_MOD_SSH, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void delSsh(JsonObject retval, String sshId, String serverIp, String magicKey) {
        try {
            if (MetaSvrGlobalRes.get().getCmptMeta().isSshUsing(sshId)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SSH_IS_USING);
                return;
            }
            
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_DEL_SSH);
            sqlBean.addParams(new Object[] { sshId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            if (c.executeUpdate()) {
                // remove local cache
                MetaSvrGlobalRes.get().getCmptMeta().delSsh(serverIp, sshId);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_SERVER_IP, serverIp);
                msgBody.put(FixHeader.HEADER_SSH_ID, sshId);

                EventBean ev = new EventBean(EventType.EVENT_DEL_SSH, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                logger.error("{}", CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getUserByServClazzFromDB(JsonObject retval, String servClazz) {
        try {
            String oldServerIp = new String("");

            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_GET_USER_BY_SERVCLAZZ);
            sqlBean.addParams(new Object[] { servClazz });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            JsonArray sshs = new JsonArray();
            JsonObject user = new JsonObject();
            JsonArray res = new JsonArray();

            for (HashMap<String, Object> item : resultList) {
                String serverIp = (String) item.get(FixHeader.HEADER_SERVER_IP);
                String sshName = (String) item.get(FixHeader.HEADER_SSH_NAME);
                String sshId = (String) item.get(FixHeader.HEADER_SSH_ID);
                if (!oldServerIp.equals(serverIp)) {
                    if (!user.isEmpty()) {
                        user.put("SSH_LIST", sshs);
                        res.add(user);
                    }
                    user = new JsonObject();
                    user.put(FixHeader.HEADER_SERVER_IP, serverIp);
                    sshs = new JsonArray();
                }

                JsonObject ssh = new JsonObject();
                ssh.put(FixHeader.HEADER_SSH_NAME, sshName);
                ssh.put(FixHeader.HEADER_SSH_ID, sshId);
                sshs.add(ssh);

                oldServerIp = serverIp;
            }

            user.put("SSH_LIST", sshs);
            res.add(user);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, res.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }
    
    public static void getUserByServClazzFromCache(JsonObject retval, String servClazz) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            JsonArray res = meta.getSurpportSSHList(servClazz);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, res.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }
    
    public static void getServListFromCache(JsonObject retval, String servType) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            JsonArray res = meta.getServListFromCache(servType);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, res.toString());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void saveServTopoSkeleton(JsonObject retval, JsonObject topJson, String servType, String magicKey) {
        try {
            String servRootName = MetaSvrGlobalRes.get().getCmptMeta().getServRootCmpt(servType);

            JsonObject subJson = topJson.getJsonObject(servRootName);
            String servInstId = subJson.getString(FixHeader.HEADER_INST_ID);
            Vector<EventBean> events = new Vector<EventBean>();

            if (MetaSvrGlobalRes.get().getCmptMeta().getInstance(servInstId) != null) {
                // save pos after move
                enumSavePos(servRootName, subJson, retval, events, magicKey);
            } else {
                // init containers
                enumSaveSkeleton(topJson, retval, events, magicKey);
            }

            for (EventBean ev : events) {
                EventBusMsg.publishEvent(ev);
            }
            events.clear();

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    
    private static boolean enumSavePos(String cmptName, JsonObject json, JsonObject retval, Vector<EventBean> events, String magicKey) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();
        PaasMetaCmpt rootCmpt = cmptMeta.getCmptByName(cmptName);
        if (rootCmpt == null)
            return true;

        if (json.containsKey(FixHeader.HEADER_INST_ID) && json.containsKey(FixHeader.HEADER_POS)) {
            JsonObject jsonPos = json.getJsonObject(FixHeader.HEADER_POS);
            String instId = json.getString(FixHeader.HEADER_INST_ID);

            if (!jsonPos.isEmpty()) {
                PaasPos pos = new PaasPos();
                getPos(jsonPos, pos);

                SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPDATE_POS);
                sqlBean.addParams(new Object[] { pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(), pos.getRow(),
                        pos.getCol(), instId });

                CRUD c = new CRUD(METADB_NAME);
                c.putSqlBean(sqlBean);

                if (c.executeUpdate()) {
                    PaasInstance instance = new PaasInstance(instId, rootCmpt.getCmptId(), false, CONSTS.STR_FALSE, pos.getX(), pos.getY(), pos.getWidth(),
                            pos.getHeight(), pos.getRow(), pos.getCol());

                    // add to local cache
                    MetaSvrGlobalRes.get().getCmptMeta().updInstPos(instance);

                    // broadcast event to cluster
                    JsonObject msgBody = instance.toJsonObject();
                    EventBean ev = new EventBean(EventType.EVENT_UPD_INST_POS, msgBody.toString(), magicKey);
                    events.add(ev);
                }
            }
        }

        String instId = json.getString(FixHeader.HEADER_INST_ID);
        Iterator<Entry<String, Object>> it = json.iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            String subCmptName = entry.getKey();
            Object obj = entry.getValue();

            if (obj instanceof JsonObject) {
                JsonObject subJson = (JsonObject) obj;
                
                String subInstId = subJson.getString(FixHeader.HEADER_INST_ID);
                if (subInstId == null)
                    return true;
                
                // 新增支持在已部署服务下扩充新的组件
                PaasInstance metaInst = cmptMeta.getInstance(subInstId);
                if (metaInst == null) {
                    PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptByName(subCmptName);
                    
                    // 1. add instance
                    addInstance(subInstId, cmpt, subJson, retval, events, magicKey);
                    
                    // ignore container instance attributes
                    addCmptAttr(subInstId, cmpt, subJson, retval, events, magicKey);
                    
                    // 2. add topology relations
                    addRelation(instId, subInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                } else {
                    if (!enumSavePos(subCmptName, subJson, retval, events, magicKey)) {
                        return false;
                    }
                }
            } else if (obj instanceof JsonArray) {
                JsonArray subs = (JsonArray) obj;
                int len = subs.size();
                for (int i = 0; i < len; ++i) {
                    JsonObject node = subs.getJsonObject(i);
                    if (!enumSavePos(subCmptName, node, retval, events, magicKey)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean enumSaveSkeleton(Object json, JsonObject retval, Vector<EventBean> events, String magicKey) {
        if (json instanceof JsonObject) {
            JsonObject obj = (JsonObject) json;
            if (obj.isEmpty())
                return true;
            
            Iterator<Entry<String, Object>> it = obj.iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                String key = entry.getKey();
                Object value = entry.getValue();
                
                PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptByName(key);
                if (cmpt == null)
                    continue;
                
                if (cmpt.getNodeJsonType().equals(CONSTS.SCHEMA_OBJECT)) {
                    JsonObject subNode = (JsonObject) value;
                    if (subNode.isEmpty())
                        continue;
                    
                    if (!addInstanceWithAttrRelation(subNode, cmpt, retval, events, magicKey))
                        return false;

                    if (!enumSaveSkeleton(value, retval, events, magicKey))
                        return false;
                } else if (cmpt.getNodeJsonType().equals(CONSTS.SCHEMA_ARRAY)) {
                    JsonArray subNodeArr = (JsonArray) value;
                    if (subNodeArr.isEmpty())
                        continue;
                    
                    int size = subNodeArr.size();
                    for (int i = 0; i < size; ++i) {
                        JsonObject subNode = subNodeArr.getJsonObject(i);
                        if (subNode.isEmpty())
                            continue;
                        
                        if (!addInstanceWithAttrRelation(subNode, cmpt, retval, events, magicKey))
                            return false;
                        
                        if (!enumSaveSkeleton(subNode, retval, events, magicKey))
                            return false;
                    }
                }
            }
        }
        
        return true;
    }

    public static void saveServiceNode(String parentId, int opType, JsonObject nodeJson, JsonObject retval, String magicKey) {
        try {
            Vector<EventBean> events = new Vector<EventBean>();
            if (enumSaveServiceNode(parentId, opType, nodeJson, retval, events, magicKey)) {
                for (EventBean ev : events) {
                    EventBusMsg.publishEvent(ev);
                }
                events.clear();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void delServNode(String parentId, String instId, JsonObject retval, String magicKey) {
        try {
            Vector<EventBean> events = new Vector<EventBean>();
            if (delInstance(parentId, instId, retval, events, magicKey)) {
                for (EventBean ev : events) {
                    EventBusMsg.publishEvent(ev);

                    JsonObject obj = new JsonObject(ev.getMsgBody());
                    String id = obj.getString(FixHeader.HEADER_INST_ID);
                    switch (ev.getEvType()) {
                    case EVENT_DEL_INSTANCE:
                        MetaSvrGlobalRes.get().getCmptMeta().delInstance(id);
                        break;
                    case EVENT_DEL_INST_ATTR:
                        MetaSvrGlobalRes.get().getCmptMeta().delInstAttr(id);
                        break;
                    case EVENT_DEL_TOPO: {
                        String supId = obj.getString(FixHeader.HEADER_PARENT_ID);
                        MetaSvrGlobalRes.get().getCmptMeta().delTopo(supId, id);
                    }
                        break;
                    default:
                        break;
                    }
                }

                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                retval.put(FixHeader.HEADER_RET_INFO, "");
            } else {
                logger.error("del_instance nok, parent_id:{}, inst_id:{}", parentId, instId);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getMetaDataNodeByInstId(String instId, JsonObject retval) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            
            PaasInstance instance = meta.getInstance(instId);
            if (instance == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INSTANCE_NOT_FOUND);
                return;
            }

            PaasMetaCmpt cmpt = meta.getCmptById(instance.getCmptId());
            if (cmpt == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_CMPT_NOT_FOUND);
                return;
            }

            JsonObject metaJson = new JsonObject();
            JsonArray attrArr = new JsonArray();

            Collection<PaasInstAttr> attrs = meta.getInstAttrs(instId);
            for (PaasInstAttr attr : attrs) {
                JsonObject node = new JsonObject();
                node.put(FixHeader.HEADER_ATTR_NAME, attr.getAttrName());
                node.put(FixHeader.HEADER_ATTR_VALUE, attr.getAttrValue());
                
                if (attr.getAttrName().equals(FixHeader.HEADER_SSH_ID)) {
                    String sshID = attr.getAttrValue();
                    PaasSsh ssh = meta.getSshById(sshID);
                    
                    JsonObject tmp = new JsonObject();
                    tmp.put(FixHeader.HEADER_ATTR_NAME, FixHeader.HEADER_IP);
                    tmp.put(FixHeader.HEADER_ATTR_NAME_CN, FixHeader.HEADER_IP);
                    tmp.put(FixHeader.HEADER_ATTR_VALUE, ssh.getServerIp());
                    attrArr.add(tmp);
                }

                PaasMetaAttr metaAttr = meta.getAttr(attr.getAttrId());
                if (metaAttr != null) {
                    node.put(FixHeader.HEADER_ATTR_NAME_CN, metaAttr.getAttrNameCn());
                }

                attrArr.add(node);
            }
            metaJson.put(cmpt.getCmptNameCn(), attrArr);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, metaJson);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }
    
    public static void getSmsABQueueWeightInfo(String servInstId, JsonObject retval) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            
            PaasInstance instance = meta.getInstance(servInstId);
            if (instance == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INSTANCE_NOT_FOUND);
                return;
            }

            PaasMetaCmpt cmpt = meta.getCmptById(instance.getCmptId());
            if (cmpt == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_CMPT_NOT_FOUND);
                return;
            }

            PaasInstAttr attr = null;
            Collection<PaasTopology> topos = meta.getInstRelations(servInstId);
            boolean find = false;
            // 从 sms-server | sms-process | sms-client | sms-batsave | sms-statistics 中任意一个去取REDIS_CLUSTER_QUEUE属性
            for (PaasTopology topo : topos) {
                String containerId = topo.getToe(servInstId);
                if (containerId == null || containerId.isEmpty())
                    continue;
                
                Collection<PaasTopology> subTopos = meta.getInstRelations(containerId);
                for (PaasTopology subTopo : subTopos) {
                    String instId = subTopo.getToe(containerId);
                    attr = meta.getInstAttr(instId, 203);  // 203 -> 'REDIS_CLUSTER_QUEUE'
                    if (attr != null) {
                        find = true;
                        break;
                    }
                }
                
                if (find)
                    break;
            }
            
            if (attr == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_REDIS_QUEUE_NOT_INITIALIZED);
                return;
            }
            
            // 取到REDIS_CLUSTER_QUEUE后,对应取下面每个集群的权重信息
            String redisQueueServId = attr.getAttrValue();
            PaasInstance queueServInst = meta.getInstance(redisQueueServId);
            if (queueServInst == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_REDIS_QUEUE_NOT_INITIALIZED);
            } else {
                Collection<PaasTopology> abQueueClusterTopos = meta.getInstRelations(redisQueueServId);
                if (abQueueClusterTopos == null || abQueueClusterTopos.isEmpty()) {
                    retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                    retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_REDIS_QUEUE_NOT_INITIALIZED);
                } else {
                    JsonObject retInfo = new JsonObject();
                    for (PaasTopology abTopo : abQueueClusterTopos) {
                        String queueServId = abTopo.getToe(redisQueueServId);
                        
                        PaasInstAttr containerNameAttr = meta.getInstAttr(queueServId, 142); // 142, 'SERV_CONTAINER_NAME'
                        Collection<PaasInstAttr> queueContainerAttrs = meta.getInstAttrs(queueServId);
                        JsonObject item = new JsonObject();
                        for (PaasInstAttr containerAttr : queueContainerAttrs) {
                            item.put(containerAttr.getAttrName(), containerAttr.getAttrValue());
                        }
                        retInfo.put(containerNameAttr.getAttrValue(), item);
                    }
                
                    retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
                    retval.put(FixHeader.HEADER_RET_INFO, retInfo);
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }
    
    public static void adjustSmsABQueueWeightInfo(String servInstID, String queueServInstID, JsonObject topoJson, JsonObject retval, String magicKey) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasInstance servInst = meta.getInstance(queueServInstID);
            if (servInst == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INSTANCE_NOT_FOUND);
                return;
            }
            
            Collection<PaasTopology> topos = meta.getInstRelations(queueServInstID);
            if (topos == null || topos.isEmpty()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_REDIS_QUEUE_NOT_INITIALIZED);
                return;
            }
            
            // "RedisClusterA" | "RedisClusterB"
            JsonObject clusterA = topoJson.getJsonObject(CONSTS.REDIS_CLUSTER_A);
            JsonObject clusterB = topoJson.getJsonObject(CONSTS.REDIS_CLUSTER_B);
            if (clusterA == null || clusterB == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_REDIS_A_OR_B_MISSING);
                return;
            }
            
            String instAId = clusterA.getString(FixHeader.HEADER_INST_ID);
            String instBId = clusterB.getString(FixHeader.HEADER_INST_ID);
            
            String weightA = clusterA.getString(FixHeader.HEADER_WEIGHT);
            String weightB = clusterB.getString(FixHeader.HEADER_WEIGHT);
            
            // update database
            CRUD c = new CRUD(METADB_NAME);
            
            SqlBean sqlBean1 = new SqlBean(MetaDataSql.SQL_MOD_ATTR);
            sqlBean1.addParams(new Object[] { weightA, instAId, 141 });  // 141 -> 'WEIGHT'
            
            SqlBean sqlBean2 = new SqlBean(MetaDataSql.SQL_MOD_ATTR);
            sqlBean2.addParams(new Object[] { weightB, instBId, 141 });  // 141 -> 'WEIGHT
            
            c.putSqlBean(sqlBean1);
            c.putSqlBean(sqlBean2);
            c.batchUpdate();
            
            // update local cache
            meta.adjustSmsABQueueWeightInfo(instAId, weightA, instBId, weightB);
            
            // broadcast event bus to synchronize local cache
            JsonObject msgBody = new JsonObject();
            msgBody.put(FixHeader.HEADER_INST_ID_A, instAId);
            msgBody.put(FixHeader.HEADER_WEIGHT_A, weightA);
            msgBody.put(FixHeader.HEADER_INST_ID_B, instBId);
            msgBody.put(FixHeader.HEADER_WEIGHT_B, weightB);

            EventBean ev = new EventBean(EventType.EVENT_AJUST_QUEUE_WEIGHT, msgBody.toString(), magicKey);
            EventBusMsg.publishEvent(ev);
            
            // 依次遍历每个sms服务下的每个模块的web console接口, 指定调整权重操作
            JsonObject servTopoRetVal = new JsonObject();
            if (!loadServiceTopo(servTopoRetVal, servInstID)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_LOAD_SERV_TOPO_FAIL);
                return;
            }
            JsonObject servTopo = servTopoRetVal.getJsonObject(FixHeader.HEADER_RET_INFO);
            
            String cmd = CONSTS.CMD_ADJUST_REDIS_WEIGHT;
            String msg = String.format("%s:%s,%s:%s", CONSTS.REDIS_CLUSTER_A, weightA, CONSTS.REDIS_CLUSTER_B, weightB);
            String[] data = new String[] { cmd, msg };
            
            if (enumSmsTopoToSendData(servTopo, data)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_AJUST_REDIS_WEIGHT_PARTAIL_OK);
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }
    
    public static void switchSmsDBType(String servInstID, String dbServInstID, String dbType, String dbName,
            JsonObject retval, String magicKey) {

        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasInstance smsServInst = meta.getInstance(servInstID);
            if (smsServInst == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SMS_INSTANCE_NOT_FOUND);
                return;
            }
            
            Collection<PaasTopology> smsTopos = meta.getInstRelations(servInstID);
            if (smsTopos == null || smsTopos.isEmpty()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SMS_SERV_NOT_INITIALIZED);
                return;
            }
            
            PaasInstance dbServInst = meta.getInstance(dbServInstID);
            if (dbServInst == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB_INSTANCE_NOT_FOUND);
                return;
            }
            
            Collection<PaasTopology> dbServTopos = meta.getInstRelations(dbServInstID);
            if (dbServTopos == null || dbServTopos.isEmpty()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB_SERV_NOT_INITIALIZED);
                return;
            }
            
            // find the specified dg container
            String dgContainerID = null;
            for (PaasTopology topo : dbServTopos) {
                String id = topo.getToe(dbServInstID);
                PaasInstAttr dgNameAttr = meta.getInstAttr(id, 136);  // 136 -> 'DG_NAME'
                if (dgNameAttr != null && dgNameAttr.getAttrValue().equals(dbName)) {
                    dgContainerID = id;
                    break;
                }
            }
            
            if (dgContainerID == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_SPECIFIED_DG_NOT_FOUND);
                return;
            }
            
            // update database
            CRUD c = new CRUD(METADB_NAME);
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_ATTR);
            sqlBean.addParams(new Object[] { dbType, dgContainerID, 225 });  // 225, 'ACTIVE_DB_TYPE'
            c.putSqlBean(sqlBean);
            if (!c.executeUpdate()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            
            // update local cache
            meta.switchSmsDBType(dgContainerID, dbType);
            
            // broadcast event bus to synchronize local cache
            JsonObject msgBody = new JsonObject();
            msgBody.put(FixHeader.HEADER_INST_ID, dgContainerID);
            msgBody.put(FixHeader.HEADER_ACTIVE_DB_TYPE, dbType);
            
            EventBean ev = new EventBean(EventType.EVENT_SWITCH_DB_TYPE, msgBody.toString(), magicKey);
            EventBusMsg.publishEvent(ev);
            
            String cmd = CONSTS.CMD_SWITCH_DB_TYPE;
            String[] data = new String[] { cmd, dbType, dbName };
            
            // 依次遍历每个sms服务下的每个模块的web console接口, 指定调整权重操作
            JsonObject servTopoRetVal = new JsonObject();
            if (!loadServiceTopo(servTopoRetVal, servInstID)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_LOAD_SERV_TOPO_FAIL);
                return;
            }
            JsonObject servTopo = servTopoRetVal.getJsonObject(FixHeader.HEADER_RET_INFO);
            if (enumSmsTopoToSendData(servTopo, data)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            } else {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_AJUST_REDIS_WEIGHT_PARTAIL_OK);
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }
    
    private static boolean enumSmsTopoToSendData(JsonObject servTopo, String[] data) {
        JsonObject smsServContainer = servTopo.getJsonObject(FixHeader.HEADER_SMS_GATEWAY_SERV_CONTAINER);
        
        JsonObject serverContainer = smsServContainer.getJsonObject(FixHeader.HEADER_SMS_SERVER_CONTAINER);
        JsonObject processContainer = smsServContainer.getJsonObject(FixHeader.HEADER_SMS_PROCESS_CONTAINER);
        JsonObject clientContainer = smsServContainer.getJsonObject(FixHeader.HEADER_SMS_CLIENT_CONTAINER);
        JsonObject batSaveContainer = smsServContainer.getJsonObject(FixHeader.HEADER_SMS_BATSAVE_CONTAINER);
        JsonObject statsContainer = smsServContainer.getJsonObject(FixHeader.HEADER_SMS_STATS_CONTAINER);
        
        JsonArray smsServerArr = serverContainer.getJsonArray(FixHeader.HEADER_SMS_SERVER);
        JsonArray smsProcessArr = processContainer.getJsonArray(FixHeader.HEADER_SMS_PROCESS);
        JsonArray smsClientArr = clientContainer.getJsonArray(FixHeader.HEADER_SMS_CLIENT);
        JsonArray batSaveArr = batSaveContainer.getJsonArray(FixHeader.HEADER_SMS_BATSAVE);
        JsonArray statsArr = statsContainer.getJsonArray(FixHeader.HEADER_SMS_STATS);

        boolean dispachSmsServerOk = dispatchSmsEvent(FixHeader.HEADER_SMS_SERVER, smsServerArr, data);
        boolean dispachSmsProcessOk = dispatchSmsEvent(FixHeader.HEADER_SMS_PROCESS, smsProcessArr, data);
        boolean dispachSmsClientOk = dispatchSmsEvent(FixHeader.HEADER_SMS_CLIENT, smsClientArr, data);
        boolean dispachSmsBatSaveOk = dispatchSmsEvent(FixHeader.HEADER_SMS_BATSAVE, batSaveArr, data);
        boolean dispachSmsStatsOk = dispatchSmsEvent(FixHeader.HEADER_SMS_STATS, statsArr, data);

        return dispachSmsServerOk && dispachSmsProcessOk && dispachSmsClientOk && dispachSmsBatSaveOk
                && dispachSmsStatsOk;
    }
    
    private static boolean dispatchSmsEvent(String smsCmpt, JsonArray instArr, String[] data) {
        if (instArr == null || instArr.isEmpty())
            return true;
        
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        
        boolean allOk = true;
        int size = instArr.size();
        for (int i = 0; i < size; ++i) {
            JsonObject item = instArr.getJsonObject(i);
            String instID = item.getString(FixHeader.HEADER_INST_ID);
            String ip = item.getString(FixHeader.HEADER_IP);
            String webConsolePort = item.getString(FixHeader.HEADER_WEB_CONSOLE_PORT);
            String processor = item.getString(FixHeader.HEADER_PROCESSOR);
            String passwd = CONSTS.SMS_CONSOLE_PASSWD;
            int retry = 0;
            boolean notfyOk = false;

            int portOffset = processor == null ? 0 : Integer.valueOf(processor);
            int realPort = portOffset + Integer.valueOf(webConsolePort);
            
            PaasInstance inst = meta.getInstance(instID);
            if (inst == null || !inst.isDeployed())
                continue;
            
            do {
                if (notfifySmsInstanceEvent(ip, realPort, passwd, data)) {
                    notfyOk = true;
                    break;
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        
                    }
                }
            } while (++retry < NOTIFY_RETRY_CNT);
            
            if (!notfyOk) {
                allOk = false;
            }
        }
        
        return allOk;
    }

    private static boolean notfifySmsInstanceEvent(String ip, int port, String passwd, String[] data) {
        boolean ret = false;
        try {
            SmsWebConsoleConnector connector = new SmsWebConsoleConnector(ip, port, passwd);
            connector.connect();
            connector.sendData(data);
            connector.close();

            ret = true;
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (String s : data) {
                if (sb.length() > 0)
                    sb.append(" ");
                
                sb.append(s);
            }
            logger.error("notify sms instance:{}:{}, event:\"{}\" fail, {}", ip, port, sb.toString(), e.getMessage(), e);
        }

        return ret;
    }
    
    public static void getMetaDataTreeByInstId(String instId, JsonObject retval) {
        try {
            JsonArray arr = new JsonArray();
            getChildNode(instId, arr);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, arr);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INTERNAL);
        }
    }

    public static boolean updateInstanceDeployFlag(String instId, String deployFlag, ResultBean result, String magicKey) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_INST_DEPLOY);
            sqlBean.addParams(new Object[] { deployFlag, instId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                // add to local
                MetaSvrGlobalRes.get().getCmptMeta().updInstDeploy(instId, deployFlag);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);
                msgBody.put(FixHeader.HEADER_IS_DEPLOYED, deployFlag);

                EventBean ev = new EventBean(EventType.EVENT_UPD_INST_DEPLOY, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                res = true;
            } else {
                logger.error(CONSTS.ERR_DB);
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_DB);
        }

        return res;
    }
    
    public static boolean updateInstancePreEmbadded(String instId, String preEmbadded, ResultBean result, String magicKey) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_INSTANCE_PRE_EMBADDED);
            sqlBean.addParams(new Object[] { preEmbadded, instId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                // update local cache
                MetaSvrGlobalRes.get().getCmptMeta().updInstPreEmbadded(instId, preEmbadded);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);
                msgBody.put(FixHeader.HEADER_PRE_EMBADDED, preEmbadded);

                EventBean ev = new EventBean(EventType.EVENT_UPD_INST_PRE_EMBEDDED, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                res = true;
            } else {
                logger.error(CONSTS.ERR_DB);
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_DB);
        }

        return res;
    }

    public static boolean updateServiceDeployFlag(String instId, String deployFlag, ResultBean result, String magicKey) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_SERV_DEPLOY);
            sqlBean.addParams(new Object[]{deployFlag, instId});

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            if (c.executeUpdate()) {
                // add to local
                MetaSvrGlobalRes.get().getCmptMeta().updServDeploy(instId, deployFlag);

                // broadcast event to cluster
                JsonObject msgBody = new JsonObject();
                msgBody.put(FixHeader.HEADER_INST_ID, instId);
                msgBody.put(FixHeader.HEADER_IS_DEPLOYED, deployFlag);

                EventBean ev = new EventBean(EventType.EVENT_UPD_SERVICE_DEPLOY, msgBody.toString(), magicKey);
                EventBusMsg.publishEvent(ev);

                res = true;
            } else {
                logger.error(CONSTS.ERR_DB);
                result.setRetCode(CONSTS.REVOKE_NOK);
                result.setRetInfo(CONSTS.ERR_DB);
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_DB);
        }

        return res;
    }

    private static boolean enumSaveServiceNode(String parentId, int opType, JsonObject nodeJson, JsonObject retval,
            Vector<EventBean> events, String magicKey) {
        
        Iterator<Entry<String, Object>> it = nodeJson.iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            String cmptName = entry.getKey();
            Object subJson = entry.getValue();

            PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptByName(cmptName);
            if (cmpt == null)
                continue;

            if (!(subJson instanceof JsonArray)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_JSONNODE_ILLEGAL);
                return false;
            }

            JsonArray subs = (JsonArray) subJson;
            int size = subs.size();
            for (int i = 0; i < size; ++i) {
                JsonObject node = subs.getJsonObject(i);
                String instId = node.getString(FixHeader.HEADER_INST_ID);

                if (instId.equals("") || node.isEmpty()) {
                    logger.error("enumSaveServiceNode missing INST_ID");
                    continue;
                }
                
                // 防止前端未保存实例的情况下，第一次保存opType传了2
                if (MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId) == null) {
                    opType = CONSTS.OP_TYPE_ADD;
                }

                if (opType == CONSTS.OP_TYPE_ADD) {
                    // 1. add instance
                    addInstance(instId, cmpt, node, retval, events, magicKey);

                    // 2. add component attribute
                    addCmptAttr(instId, cmpt, node, retval, events, magicKey);

                    // 3. add relation
                    addRelation(parentId, instId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                } else if (opType == CONSTS.OP_TYPE_MOD) {
                    // mod component attribute
                    modCmptAttr(instId, cmpt, node, retval, events, magicKey);
                } else {
                    retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                    retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_OP_TYPE);
                    return false;
                }
            }
        }

        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        return true;
    }

    public static boolean addInstanceWithAttrRelation(JsonObject json, PaasMetaCmpt cmpt, JsonObject retval,
            Vector<EventBean> events, String magicKey) {

        String instId = json.getString(FixHeader.HEADER_INST_ID);
        // json {}
        if (StringUtils.isNull(instId)) {
            return true;
        }

        // 1. add instance
        addInstance(instId, cmpt, json, retval, events, magicKey);

        // 2. add component attributes
        addCmptAttr(instId, cmpt, json, retval, events, magicKey);

        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        boolean isContainer = cmpt.haveSubComponent();
        if (isContainer) {
            // 支持容器包含服务的容器
            Set<Integer> subCmptIDSet = cmpt.getSubCmptId();
            for (int subCmptID : subCmptIDSet) {
                PaasMetaCmpt subCmpt = meta.getCmptById(subCmptID);
                boolean isSubCmptServRoot = meta.isServRootCmpt(subCmpt.getServType(), subCmpt.getCmptName());
                if (isSubCmptServRoot) {
                    // add container to service container relation
                    String subServInstId = json.getString(FixHeader.HEADER_SERV_INST_ID);
                    boolean isSubServInstIdNull = subServInstId == null || subServInstId.isEmpty();
                    if (!isSubServInstIdNull) {
                        if (!meta.isTopoRelationExists(instId, subServInstId)) {
                            delRelation(instId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                            addRelation(instId, subServInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                        }
                    }
                }
            }
        }

        // 3. add topology relations
        Iterator<Entry<String, Object>> it = json.iterator();
        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            String key = entry.getKey();
            Object value = entry.getValue();

            PaasMetaCmpt subCmpt = meta.getCmptByName(key);
            boolean isSubComponent = subCmpt != null;

            if (isContainer && isSubComponent/* && value instanceof JsonObject*/) {
                String subCmptNodeJsonType = subCmpt.getNodeJsonType();
                if (subCmptNodeJsonType.equals(CONSTS.SCHEMA_OBJECT)) {
                    JsonObject node = (JsonObject) value;
                    if (node.isEmpty())
                        continue;
                    
                    String subInstId = node.getString(FixHeader.HEADER_INST_ID);
                    addRelation(instId, subInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                } else if (subCmptNodeJsonType.equals(CONSTS.SCHEMA_ARRAY)) {
                    JsonArray nodeArr = (JsonArray) value;
                    if (nodeArr.isEmpty())
                        continue;
                    
                    int size = nodeArr.size();
                    for (int i = 0; i < size; ++i) {
                        JsonObject subCmptItem = nodeArr.getJsonObject(i);
                        String subInstId = subCmptItem.getString(FixHeader.HEADER_INST_ID);
                        addRelation(instId, subInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                    }
                }
            }
        }

        return true;
    }

    public static void addInstance(String instId, PaasMetaCmpt cmpt, JsonObject node, JsonObject retval,
            Vector<EventBean> events, String magicKey) {

        int cmptId = cmpt.getCmptId();
        boolean deployed = cmpt.isNeedDeploy() ? false : true; // container not need to deploy, service instance need
                                                               // deploy

        PaasPos pos = new PaasPos();
        if (node.containsKey(FixHeader.HEADER_POS)) {
            JsonObject jsonPos = node.getJsonObject(FixHeader.HEADER_POS);
            getPos(jsonPos, pos);
        }

        // 1. add instance
        String isDeployed = deployed ? CONSTS.STR_TRUE : CONSTS.STR_FALSE;

        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_INSTANCE);
        sqlBean.addParams(new Object[] { instId, cmptId, isDeployed, pos.getX(), pos.getY(), pos.getWidth(),
                pos.getHeight(), pos.getRow(), pos.getCol() });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        c.executeUpdate();

        PaasInstance instance = new PaasInstance(instId, cmptId, deployed, isDeployed, pos.getX(), pos.getY(), pos.getWidth(),
                pos.getHeight(), pos.getRow(), pos.getCol());

        // add to local cache
        MetaSvrGlobalRes.get().getCmptMeta().addInstance(instance);

        // broadcast event to cluster
        JsonObject msgBody = instance.toJsonObject();
        EventBean ev = new EventBean(EventType.EVENT_ADD_INSTANCE, msgBody.toString(), magicKey);
        events.add(ev);
    }

    public static void addCmptAttr(String instId, PaasMetaCmpt cmpt, JsonObject node, JsonObject retval,
            Vector<EventBean> events, String magicKey) {

        Vector<PaasMetaAttr> attrs = MetaSvrGlobalRes.get().getCmptMeta().getCmptAttrs(cmpt.getCmptId());
        if (attrs != null) {
            for (PaasMetaAttr attr : attrs) {
                int attrId = attr.getAttrId();
                String attrName = attr.getAttrName();
                String attrVal = node.containsKey(attrName) ? node.getString(attrName) : "";

                SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_INSTANCE_ATTR);
                sqlBean.addParams(new Object[] { instId, attrId, attrName, attrVal });
                CRUD c = new CRUD(METADB_NAME);
                c.putSqlBean(sqlBean);
                if (!c.executeUpdate()) {
                    String info = String.format("save component attribute fail, instId:%s, attrId:%s, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal);
                    logger.error(info);
                }

                // add to local cache
                PaasInstAttr instAttr = new PaasInstAttr(instId, attrId, attrName, attrVal);
                MetaSvrGlobalRes.get().getCmptMeta().addInstAttr(instAttr);

                // broadcast event to cluster
                JsonObject msgBody = instAttr.toJsonObject();
                EventBean ev = new EventBean(EventType.EVENT_ADD_INST_ATTR, msgBody.toString(), magicKey);
                events.add(ev);
            }
        }
    }

    private static void addRelation(String parentId, String instId, int topoType, JsonObject retval, Vector<EventBean> events, String magicKey) {
        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_TOPOLOGY);
        sqlBean.addParams(new Object[] { parentId, instId, topoType });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        c.executeUpdate();

        // add to local cache
        PaasTopology topo = new PaasTopology(parentId, instId, topoType);
        MetaSvrGlobalRes.get().getCmptMeta().addTopo(topo);

        // broadcast event to cluster
        JsonObject msgBody = topo.toJsonObject();
        EventBean ev = new EventBean(EventType.EVENT_ADD_TOPO, msgBody.toString(), magicKey);
        events.add(ev);
    }
    
    private static void modRelation(String parentId, String instId, int topoType, JsonObject retval, Vector<EventBean> events, String magicKey) {
        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_TOPOLOGY);
        sqlBean.addParams(new Object[] { instId, parentId });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        c.executeUpdate();

        // update local cache
        PaasTopology topo = new PaasTopology(parentId, instId, topoType);
        MetaSvrGlobalRes.get().getCmptMeta().modTopo(topo);

        // broadcast event to cluster
        JsonObject msgBody = topo.toJsonObject();
        EventBean ev = new EventBean(EventType.EVENT_MOD_TOPO, msgBody.toString(), magicKey);
        events.add(ev);
    }
    
    private static void delRelation(String parentId, int topoType, JsonObject retval, Vector<EventBean> events, String magicKey) {
        SqlBean sqlBean1 = new SqlBean(MetaDataSql.SQL_DEL_ALL_SUB_TOPOLOGY);
        sqlBean1.addParams(new Object[] { parentId, parentId });
        CRUD c1 = new CRUD(METADB_NAME);
        c1.putSqlBean(sqlBean1);
        c1.executeUpdate();
        
        // update local cache
        MetaSvrGlobalRes.get().getCmptMeta().delAllSubTopo(parentId);
        
        // broadcast event to cluster
        JsonObject msgBody = new JsonObject();
        msgBody.put(FixHeader.HEADER_PARENT_ID, parentId);
        msgBody.put(FixHeader.HEADER_INST_ID, "");
        msgBody.put(FixHeader.HEADER_TOPO_TYPE, topoType);
        
        EventBean ev = new EventBean(EventType.EVENT_DEL_TOPO, msgBody.toString(), magicKey);
        events.add(ev);
    }
    
    /*private static void delRelation(String parentId, String instId, int topoType, JsonObject retval, Vector<EventBean> events) {
        SqlBean sqlBean1 = new SqlBean(MetaDataSql.SQL_DEL_TOPOLOGY);
        sqlBean1.addParams(new Object[] { parentId, instId, instId });
        CRUD c1 = new CRUD(METADB_NAME);
        c1.putSqlBean(sqlBean1);
        c1.executeUpdate();
        
        // update local cache
        MetaSvrGlobalRes.get().getCmptMeta().delTopo(parentId, instId);
        
        // broadcast event to cluster
        JsonObject msgBody = new JsonObject();
        msgBody.put(FixHeader.HEADER_PARENT_ID, parentId);
        msgBody.put(FixHeader.HEADER_INST_ID, instId);
        msgBody.put(FixHeader.HEADER_TOPO_TYPE, topoType);
        
        EventBean ev = new EventBean(EventType.EVENT_DEL_TOPO, msgBody.toString());
        events.add(ev);
    }*/

    private static void modCmptAttr(String instId, PaasMetaCmpt cmpt, JsonObject node, JsonObject retval,
            Vector<EventBean> events, String magicKey) {

        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        Vector<PaasMetaAttr> attrs = meta.getCmptAttrs(cmpt.getCmptId());
        if (attrs == null)
            return;

        for (PaasMetaAttr attr : attrs) {
            int attrId = attr.getAttrId();
            String attrName = attr.getAttrName();
            if (attrName.equals(FixHeader.HEADER_INST_ID))
                continue;

            String attrVal = node.containsKey(attrName) ? node.getString(attrName) : "";

            if (attrName.equals(FixHeader.HEADER_SERV_INST_ID)) {
                // add container to service container relation
                String subServInstId = attrVal;
                if (meta.isTopoRelationExists(instId)) {
                    modRelation(instId, subServInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                } else {
                    addRelation(instId, subServInstId, CONSTS.TOPO_TYPE_CONTAIN, retval, events, magicKey);
                }
            }
            
            // 元数据模型有可能新增组件属性,如果都是按update方式则不能动态扩展
            // attrId
            boolean isAttrExists = meta.isInstAttrExists(instId, attrId);
            PaasInstAttr instAttr = null;
            // SQL_MOD_ATTR
            if (isAttrExists) {
                // 已经存在做更新动作
                SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_ATTR);
                sqlBean.addParams(new Object[] { attrVal, instId, attrId });
                CRUD c = new CRUD(METADB_NAME);
                c.putSqlBean(sqlBean);
                if (!c.executeUpdate()) {
                    String info = String.format("save component attribute fail, instId:%s, attrId:%s, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal);
                    logger.error(info);
                }
                
                // refresh to local cache
                instAttr = new PaasInstAttr(instId, attrId, attrName, attrVal);
                MetaSvrGlobalRes.get().getCmptMeta().updInstAttr(instAttr);
            } else {
                // 没有对应的属性,说明组件新增了属性,此时对应做insert操作
                SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_INSTANCE_ATTR);
                sqlBean.addParams(new Object[] { instId, attrId, attrName, attrVal });
                CRUD c = new CRUD(METADB_NAME);
                c.putSqlBean(sqlBean);
                if (!c.executeUpdate()) {
                    String info = String.format("save component attribute fail, instId:%s, attrId:%s, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal);
                    logger.error(info);
                }
                
                // refresh to local cache
                instAttr = new PaasInstAttr(instId, attrId, attrName, attrVal);
                MetaSvrGlobalRes.get().getCmptMeta().addInstAttr(instAttr);
            }

            // broadcast event to cluster
            JsonObject msgBody = instAttr.toJsonObject();
            EventBean ev = new EventBean(EventType.EVENT_ADD_INST_ATTR, msgBody.toString(), magicKey);
            events.add(ev);
        }
    }
    
    public static boolean modInstanceAttr(String instId, int attrId, String attrName, String attrVal, String magicKey) {
        boolean res = false;
        
        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_MOD_ATTR);
        sqlBean.addParams(new Object[] { attrVal, instId, attrId });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        res = c.executeUpdate();
        if (!res) {
            String info = String.format("save component attribute fail, instId:%s, attrId:%s, attrName:%s, attrVal:%s", instId, attrId, attrName, attrVal);
            logger.error(info);
        } else {
            PaasInstAttr instAttr = new PaasInstAttr(instId, attrId, attrName, attrVal);
            MetaSvrGlobalRes.get().getCmptMeta().updInstAttr(instAttr);
            
            // broadcast event to cluster
            JsonObject msgBody = instAttr.toJsonObject();
            EventBean ev = new EventBean(EventType.EVENT_ADD_INST_ATTR, msgBody.toString(), magicKey);
            EventBusMsg.publishEvent(ev);
        }
        
        return res;
    }

    private static void getPos(JsonObject jsonPos, PaasPos pos) {
        pos.setX(jsonPos.getInteger(FixHeader.HEADER_X));
        pos.setY(jsonPos.getInteger(FixHeader.HEADER_Y));
        
        if (jsonPos.containsKey("row"))
            pos.setRow(jsonPos.getInteger("row"));
        if (jsonPos.containsKey("col"))
            pos.setCol(jsonPos.getInteger("col"));
        if (jsonPos.containsKey("width"))
            pos.setWidth(jsonPos.getInteger("width"));
        if (jsonPos.containsKey("height"))
            pos.setHeight(jsonPos.getInteger("height"));
    }

    public static boolean loadServiceTopo(JsonObject retval, String instId) {
        try {
            PaasInstance instance = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
            if (instance == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.SERVICE_NOT_INIT);
                retval.put(FixHeader.HEADER_RET_INFO, "");
                return false;
            }

            int cmptId = instance.getCmptId();
            PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(cmptId);
            if (cmpt == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
                return false;
            }

            JsonObject topoJson = new JsonObject();
            JsonObject attrJson = new JsonObject();
            JsonArray deployFlagArr = new JsonArray();

            if (!loadInstanceAttribute(instId, attrJson, deployFlagArr)) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_METADATA_NOT_FOUND);
                return false;
            }

            topoJson.put(FixHeader.HEADER_SERV_CLAZZ, cmpt.getServClazz());
            topoJson.put(FixHeader.HEADER_SERV_TYPE, cmpt.getServType());
            topoJson.put(cmpt.getCmptName(), attrJson);
            topoJson.put(FixHeader.HEADER_DEPLOY_FLAG, deployFlagArr);

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, topoJson);

        } catch (Exception e) {
            logger.error("loadInstanceAttribute instId:{}, error:{}", instId, e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_LOAD_SERV_TOPO_FAIL);
            return false;
        }

        return true;
    }
    
    public static boolean loadInstanceMeta(JsonObject retval, String instId) {
        try {
            CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
            PaasInstance instance = meta.getInstance(instId);
            if (instance == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.SERVICE_NOT_INIT);
                retval.put(FixHeader.HEADER_RET_INFO, "");
                return false;
            }

            JsonObject attrJson = new JsonObject();
            Collection<PaasInstAttr> attrs = meta.getInstAttrs(instId);
            for (PaasInstAttr attr : attrs) {
                attrJson.put(attr.getAttrName(), attr.getAttrValue());
                
                if (attr.getAttrName().equals(FixHeader.HEADER_SSH_ID)) {
                    String sshId = attr.getAttrValue();
                    PaasSsh ssh = MetaSvrGlobalRes.get().getCmptMeta().getSshById(sshId);
                    if (ssh != null) {
                        attrJson.put(FixHeader.HEADER_IP, ssh.getServerIp());
                    }
                }
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, attrJson);

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            return false;
        }

        return true;
    }

    private static boolean loadInstanceAttribute(String instId, JsonObject attrJson, JsonArray deployFlagArr) {

        PaasInstance instance = MetaSvrGlobalRes.get().getCmptMeta().getInstance(instId);
        if (instance == null)
            return false;

        int cmptId = instance.getCmptId();
        PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(cmptId);
        if (cmpt == null)
            return false;

        JsonObject deploy_json = new JsonObject();
        // deploy_json.put(instId, instance.isDeployed() ? CONSTS.STR_TRUE : CONSTS.STR_FALSE);
        deploy_json.put(instId, instance.getStatus());
        deployFlagArr.add(deploy_json);

        Collection<PaasInstAttr> attrs = MetaSvrGlobalRes.get().getCmptMeta().getInstAttrs(instId);
        for (PaasInstAttr attr : attrs) {
            attrJson.put(attr.getAttrName(), attr.getAttrValue());
            
            if (attr.getAttrName().equals(FixHeader.HEADER_SSH_ID)) {
                String sshId = attr.getAttrValue();
                PaasSsh ssh = MetaSvrGlobalRes.get().getCmptMeta().getSshById(sshId);
                if (ssh != null) {
                    attrJson.put(FixHeader.HEADER_IP, ssh.getServerIp());
                }
            }
        }

        // instance POS
        JsonObject pos_json = new JsonObject();
        if (!instance.isDefaultPos()) {
            pos_json.put(FixHeader.HEADER_X, instance.getPosX());
            pos_json.put(FixHeader.HEADER_Y, instance.getPosY());

            if (instance.getWidth() != CONSTS.POS_DEFAULT_VALUE
                    && instance.getHeight() != CONSTS.POS_DEFAULT_VALUE) {
                pos_json.put("width", instance.getWidth());
                pos_json.put("height", instance.getHeight());
            }
            if (instance.getRow() != CONSTS.POS_DEFAULT_VALUE
                    && instance.getCol() != CONSTS.POS_DEFAULT_VALUE) {
                pos_json.put("row", instance.getRow());
                pos_json.put("col", instance.getCol());
            }
        }
        attrJson.put(FixHeader.HEADER_POS, pos_json);

        // sub components, add sub node skeleton in order to avoid no sub instance.
        Set<Integer> subCmptIds = cmpt.getSubCmptId();
        if (subCmptIds.isEmpty())
            return true;
        for (Integer sub_cmpt_id : subCmptIds) {
            PaasMetaCmpt sub_cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(sub_cmpt_id.intValue());
            if (sub_cmpt == null)
                return false;

            String sub_cmpt_name = sub_cmpt.getCmptName();
            String sub_node_type = sub_cmpt.getNodeJsonType();
            if (sub_node_type.equals(CONSTS.SCHEMA_ARRAY)) {
                attrJson.put(sub_cmpt_name, new JsonArray());
            } else {
                attrJson.put(sub_cmpt_name, new JsonObject());
            }
        }

        Collection<PaasTopology> relations = MetaSvrGlobalRes.get().getCmptMeta().getInstRelations(instId);
        for (PaasTopology topo : relations) {
            String toe_id = topo.getToe(instId);

            PaasInstance sub_inst = MetaSvrGlobalRes.get().getCmptMeta().getInstance(toe_id);
            if (sub_inst == null)
                return false;

            PaasMetaCmpt sub_cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(sub_inst.getCmptId());
            if (sub_cmpt == null)
                return false;

            String sub_cmpt_name = sub_cmpt.getCmptName();
            String sub_node_type = sub_cmpt.getNodeJsonType();
            if (sub_node_type.equals(CONSTS.SCHEMA_ARRAY)) {
                JsonArray sub_cmpt_json = attrJson.getJsonArray(sub_cmpt_name);
                JsonObject tmp_json = new JsonObject();
                if (loadInstanceAttribute(toe_id, tmp_json, deployFlagArr)) {
                    sub_cmpt_json.add(tmp_json);
                } else {
                    return false;
                }
            } else {
                JsonObject sub_cmpt_json = attrJson.getJsonObject(sub_cmpt_name);
                if (!loadInstanceAttribute(toe_id, sub_cmpt_json, deployFlagArr)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void getChildNode(String instId, JsonArray arr) {
        Collection<PaasTopology> topos = MetaSvrGlobalRes.get().getCmptMeta().getInstRelations(instId);
        if (topos == null)
            return;

        for (PaasTopology topo : topos) {
            JsonObject node = new JsonObject();

            JsonArray child_nodes = new JsonArray();
            getChildNode(topo.getInstId2(), child_nodes);

            node.put("inst_id", topo.getInstId2());

            PaasInstance instance = MetaSvrGlobalRes.get().getCmptMeta().getInstance(topo.getInstId2());
            if (instance != null) {
                PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(instance.getCmptId());
                node.put("text", cmpt.getCmptNameCn() + " (" + instance.getInstId() + ")");
            } else {
                node.put("text", topo.getInstId2());
            }

            if (!child_nodes.isEmpty())
                node.put("nodes", child_nodes);
            arr.add(node);
        }
    }
    
    private static void getChildNodeExcludingServRoot(String instId, JsonArray arr) {
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        Collection<PaasTopology> topos = meta.getInstRelations(instId);
        if (topos == null)
            return;

        for (PaasTopology topo : topos) {
            JsonObject node = new JsonObject();
            String toeId = topo.getToe(instId);
            if (toeId == null || toeId.isEmpty())
                continue;
            
            PaasInstance toeInst = meta.getInstance(toeId);
            if (toeInst == null)
                continue;
            PaasMetaCmpt toeCmpt = meta.getCmptById(toeInst.getCmptId());
            if (meta.isServRootCmpt(toeCmpt.getServType(), toeCmpt.getCmptName())) {
                continue;
            }

            JsonArray child_nodes = new JsonArray();
            getChildNodeExcludingServRoot(toeId, child_nodes);

            node.put("inst_id", toeId);

            PaasInstance instance = meta.getInstance(toeId);
            if (instance != null) {
                PaasMetaCmpt cmpt = MetaSvrGlobalRes.get().getCmptMeta().getCmptById(instance.getCmptId());
                node.put("text", cmpt.getCmptNameCn() + " (" + instance.getInstId() + ")");
            } else {
                node.put("text", toeId);
            }

            if (!child_nodes.isEmpty())
                node.put("nodes", child_nodes);
            arr.add(node);
        }
    }

    private static boolean enumDelService(String parentId, String instId, JsonArray subNodes, JsonObject retval,
            Vector<EventBean> events, String magicKey) {
        
        CmptMeta meta = MetaSvrGlobalRes.get().getCmptMeta();
        if (!subNodes.isEmpty()) {
            int len = subNodes.size();
            
            for (int i = 0; i < len; ++i) {
                JsonObject node = subNodes.getJsonObject(i);
                
                String subInstId = node.getString("inst_id");
                JsonArray subSubNodes = node.getJsonArray("nodes");
                
                if (StringUtils.isNull(subInstId))
                    continue;
                
                if (meta.isInstServRootCmpt(subInstId))
                    continue;
                
                if (subSubNodes != null && !subSubNodes.isEmpty()) {
                    if (!enumDelService(instId, subInstId, subSubNodes, retval, events, magicKey))
                        return false;
                }
            }
        }

        if (!delInstance(parentId, instId, retval, events, magicKey))
            return false;

        if (parentId.equals("")) {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_DEL_SERVICE);
            sqlBean.addParams(new Object[] { instId });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            c.executeUpdate();

            // broadcast event to cluster
            JsonObject msgBody = new JsonObject();
            msgBody.put(FixHeader.HEADER_INST_ID, instId);

            EventBean ev = new EventBean(EventType.EVENT_DEL_SERVICE, msgBody.toString(), magicKey);
            events.add(ev);
        }

        return true;
    }

    private static boolean delInstance(String parentId, String instId, JsonObject retval, Vector<EventBean> events, String magicKey) {
        CmptMeta cmpt = MetaSvrGlobalRes.get().getCmptMeta();

        // remove subnodes first
        Vector<PaasTopology> relations = new Vector<PaasTopology>();
        cmpt.getInstRelations(instId, relations);

        for (PaasTopology topo : relations) {
            String subInstId = topo.getToe(instId);
            
            if (StringUtils.isNull(subInstId))
                continue;
            
            if (cmpt.isInstServRootCmpt(subInstId))
                continue;
            
            if (!delInstance(instId, subInstId, retval, events, magicKey)) {
                logger.info("delInstance fail, parent_id:{}, inst_id:{}", instId, subInstId);
                return false;
            }
        }

        // 1.1 remove relations
        SqlBean sqlBean1 = new SqlBean(MetaDataSql.SQL_DEL_TOPOLOGY);
        sqlBean1.addParams(new Object[] { parentId, instId, instId });
        CRUD c1 = new CRUD(METADB_NAME);
        c1.putSqlBean(sqlBean1);
        c1.executeUpdate();

        // 1.2 broadcast event to cluster
        JsonObject jsonDelTopo = new JsonObject();
        jsonDelTopo.put(FixHeader.HEADER_INST_ID, instId);
        jsonDelTopo.put(FixHeader.HEADER_PARENT_ID, parentId);

        EventBean evDelTopo = new EventBean(EventType.EVENT_DEL_TOPO, jsonDelTopo.toString(), magicKey);
        events.add(evDelTopo);

        // 2.1 delete instance attribute
        SqlBean sqlBean2 = new SqlBean(MetaDataSql.SQL_DEL_INSTANCE_ATTR);
        sqlBean2.addParams(new Object[] { instId });
        CRUD c2 = new CRUD(METADB_NAME);
        c2.putSqlBean(sqlBean2);
        c2.executeUpdate();

        // 2.2 broadcast event to cluster
        JsonObject jsonDelInstAttr = new JsonObject();
        jsonDelInstAttr.put(FixHeader.HEADER_INST_ID, instId);

        EventBean evDelInstAttr = new EventBean(EventType.EVENT_DEL_INST_ATTR, jsonDelInstAttr.toString(), magicKey);
        events.add(evDelInstAttr);

        // 3.1 delete instance
        SqlBean sqlBean3 = new SqlBean(MetaDataSql.SQL_DEL_INSTANCE);
        sqlBean3.addParams(new Object[] { instId });
        CRUD c3 = new CRUD(METADB_NAME);
        c3.putSqlBean(sqlBean3);
        c3.executeUpdate();

        // 3.2 broadcast event to cluster
        JsonObject jsonDelInst = new JsonObject();
        jsonDelInst.put(FixHeader.HEADER_INST_ID, instId);

        EventBean evDelInst = new EventBean(EventType.EVENT_DEL_INSTANCE, jsonDelInstAttr.toString(), magicKey);
        events.add(evDelInst);

        return true;

    }
    
    public static void getServTypeVerList(JsonObject retval) {
        MetaSvrGlobalRes.get().getCmptMeta().getServTypeVerList(retval);
    }
    
    public static void getServTypeList(JsonObject retval) {
        MetaSvrGlobalRes.get().getCmptMeta().getServTypeList(retval);
    }
    
    public static void addCmptVersion(String servType, String version, JsonObject retval, String magicKey) {
        CmptMeta cmpt = MetaSvrGlobalRes.get().getCmptMeta();
        
        Set<String> servTypeSet = cmpt.getServTypeListFromLocalCache();
        if (!servTypeSet.contains(servType)) {
            String info = String.format("传入SERV_TYPE:%s, 不在支持列表内 ......", servType);
            logger.error(info);
            
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, info);
            return;
        }
        
        // 1. update database
        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_CMPT_VER);
        sqlBean.addParams(new Object[] { servType, version });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        if (!c.executeUpdate()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DUMPLICATE_KEY_OR_DB_EXCEPTION);
            return;
        }
        
        // 2. update local cache
        cmpt.addCmptVersion(servType, version);
        
        // 3. broadcast by event bus
        JsonObject msgBody = new JsonObject();
        msgBody.put(FixHeader.HEADER_SERV_TYPE, servType);
        msgBody.put(FixHeader.HEADER_VERSION, version);
        
        EventBean ev = new EventBean(EventType.EVENT_ADD_CMPT_VER, msgBody.toString(), magicKey);
        EventBusMsg.publishEvent(ev);
        
        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
    }
    
    public static void delCmptVersion(String servType, String version, JsonObject retval, String magicKey) {
        CmptMeta cmpt = MetaSvrGlobalRes.get().getCmptMeta();
        
        if (cmpt.getCmptVerCnt(servType) <= 1) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_CANNOT_DEL_SERVICE_VERSION);
            return;
        }
        
        // 1. update database
        SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_DEL_CMPT_VER);
        sqlBean.addParams(new Object[] { servType, version });
        CRUD c = new CRUD(METADB_NAME);
        c.putSqlBean(sqlBean);
        if (!c.executeUpdate()) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            return;
        }
        
        // 2. update local cache
        cmpt.delCmptVersion(servType, version);
        
        // 3. broadcast by event bus
        JsonObject msgBody = new JsonObject();
        msgBody.put(FixHeader.HEADER_SERV_TYPE, servType);
        msgBody.put(FixHeader.HEADER_VERSION, version);
        
        EventBean ev = new EventBean(EventType.EVENT_DEL_CMPT_VER, msgBody.toString(), magicKey);
        EventBusMsg.publishEvent(ev);
        
        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
    }

    public static LongMargin getNextSeqMargin(String seqName, int step) {
        LongMargin ret = null;
        
        try {
            CRUD c = new CRUD();
            
            SqlBean sqlBean1 = new SqlBean(MetaDataSql.SQL_NEXT_SEQ_LOCK);
            sqlBean1.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean1);
            
            String sql2 = String.format(MetaDataSql.SQL_NEXT_SEQ_UPDATE, step);
            SqlBean sqlBean2 = new SqlBean(sql2);
            sqlBean2.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean2);
    
            ResultBean result = new ResultBean();
            ret = c.getNextSeqMargin(step, result);
            if (result.getRetCode() == CONSTS.REVOKE_NOK)
                return null;
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return ret;
    }

    public static boolean insertAlarm(long alarmId, String servInstId, String servType, String instId, String cmptName, int alarmType, long alarmTime) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_INS_ALARM);
            sqlBean.addParams(new Object[] { alarmId, servInstId, servType, instId, cmptName, alarmType, alarmTime });
            
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return res;
    }
    
    public static boolean updateAlarmStateByAlarmId(long alarmId, long dealTime, String dealUser, String dealFlag, ResultBean result) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_ALARM_STATE_BY_ALARMID);
            sqlBean.addParams(new Object[] { dealTime, dealUser, dealFlag, alarmId });
            
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            
            result.setRetCode(CONSTS.REVOKE_NOK);
            result.setRetInfo(CONSTS.ERR_DB);
        }
        
        return res;
    }
    
    public static boolean clearAlarmCache(String instId, long alarmType) {
        String key = String.format("alarm-%s-%d", instId, alarmType);
        JedisCluster jedisClient = MetaSvrGlobalRes.get().getRedisClient();
        return jedisClient.del(key) > 0;
    }
    
    public static boolean updateAlarmStateByInstId(String servInstId, String instId, int alarmType, long dealTime, String dealUser, String dealFlag) {
        boolean res = false;
        try {
            SqlBean sqlBean = new SqlBean(MetaDataSql.SQL_UPD_ALARM_STATE_BY_INSTID);
            sqlBean.addParams(new Object[] { dealTime, dealUser, servInstId, dealFlag, instId, alarmType });
            
            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return res;
    }
    
    public static void getAlarmCount(JsonObject retval, String dealFlag) {
        StringBuilder sql = new StringBuilder(MetaDataSql.SQL_SEL_ALARM_CNT);
        
        switch (dealFlag) {
        case FixDefs.ALARM_UNDEALED:
        case FixDefs.ALARM_DEALED:
            sql.append(" AND IS_DEALED = ").append("'").append(dealFlag).append("' ");
            break;
        default:
            break;
        }

        try {
            SqlBean sqlBean = new SqlBean(sql.toString());

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            int val = c.queryForCount();
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, val);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getAlarmList(JsonObject retval, int pageSize, int pageNum, String servInstID, String instID, String dealFlag) {
        StringBuilder sqlWhere = new StringBuilder("");
        switch (dealFlag) {
        case FixDefs.ALARM_UNDEALED:
        case FixDefs.ALARM_DEALED:
            sqlWhere.append(" AND IS_DEALED = ").append("'").append(dealFlag).append("' ");
            break;
        default:
            break;
        }

        if (servInstID != null && !servInstID.isEmpty()) {
            sqlWhere.append(" AND a.SERV_INST_ID=").append("'").append(servInstID).append("' ");
        }

        if (instID != null && !instID.isEmpty()) {
            sqlWhere.append(" AND a.INST_ID=").append("'").append(instID).append("' ");
        }

        int start = pageSize * (pageNum - 1);
        String sql = String.format(MetaDataSql.SQL_SEL_ALARM_LIST, sqlWhere.toString(), start, pageSize);
        try {
            SqlBean sqlBean = new SqlBean(sql);

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList == null) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            
            JsonArray alarmArr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key.equals(FixHeader.HEADER_ALARM_TYPE)) {
                        int alarmType = (int) value;
                        item.put(FixHeader.HEADER_ALARM_INFO, AlarmType.get(alarmType).getInfo());
                        item.put(FixHeader.HEADER_ALARM_TYPE, alarmType);
                        continue;
                    }
                    
                    if (key.equals(FixHeader.HEADER_ALARM_TIME)) {
                        if (value == null) {
                            item.put(FixHeader.HEADER_ALARM_TIME, "");
                        } else {
                            item.put(FixHeader.HEADER_ALARM_TIME, DateFmtUtil.format((long) value));
                        }
                        continue;
                    }
                    
                    if (key.equals(FixHeader.HEADER_DEAL_TIME)) {
                        if (value == null) {
                            item.put(FixHeader.HEADER_DEAL_TIME, "");
                        } else {
                            item.put(FixHeader.HEADER_DEAL_TIME, DateFmtUtil.format((long) value));
                        }
                        continue;
                    }
                    
                    if (key.equals(FixHeader.HEADER_DEAL_ACC_NAME)) {
                        if (value == null) {
                            item.put(FixHeader.HEADER_DEAL_ACC_NAME, "");
                        } else {
                            item.put(FixHeader.HEADER_DEAL_ACC_NAME, value);
                        }
                        continue;
                    }
                    
                    item.put(key, value);
                }
                
                alarmArr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, alarmArr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }
    
    public static boolean reloadMetaData(String type, String magicKey) {
        try {
            MetaSvrGlobalRes.get().getCmptMeta().reloadMetaData(type);
            
            JsonObject reloadJson = new JsonObject();
            reloadJson.put(FixHeader.HEADER_RELOAD_TYPE, type);
            
            EventBean ev = new EventBean(EventType.EVENT_RELOAD_METADATA, reloadJson.toString(), magicKey);
            EventBusMsg.publishEvent(ev);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return true;
    }

}
