package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.bean.*;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.sql.RedisStatisticSql;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class RedisStatisticDao {

    private static final Logger logger = LoggerFactory.getLogger(RedisStatisticDao.class);

    private static final String TD_NAME;

    static {
        TD_NAME = SysConfig.get().getTDYamlName();
    }

    public static void getRedisServNodes(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        Vector<PaasTopology> containers = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, containers);

        JsonArray arr = new JsonArray();
        String containerId = null;

        for (PaasTopology topo : containers) {
            String toeId = topo.getToe(servInstId);
            if (toeId == null || toeId.isEmpty()) {
                continue;
            }

            PaasInstance containerInst = cmptMeta.getInstance(toeId);
            PaasMetaCmpt cmpt = cmptMeta.getCmptById(containerInst.getCmptId());
            if (cmpt.getCmptName().equals(FixHeader.HEADER_REDIS_NODE_CONTAINER)) {
                containerId = toeId;
                break;
            }
        }

        Vector<PaasTopology> redisNodes = new Vector<PaasTopology>();
        if (containerId != null) {
            cmptMeta.getInstRelations(containerId, redisNodes);
        }

        for (PaasTopology topo : redisNodes) {
            String redisInstId = topo.getToe(containerId);
            String sshId = cmptMeta.getInstAttr(redisInstId, 116).getAttrValue();     // 116, 'SSH_ID'
            String port = cmptMeta.getInstAttr(redisInstId, 101).getAttrValue();      // 101, 'PORT'

            PaasSsh ssh = cmptMeta.getSshById(sshId);
            String ip = ssh.getServerIp();

            String addr = String.format("%s:%s", ip, port);

            JsonObject item = new JsonObject();
            item.put(FixHeader.HEADER_REDIS_NODE, addr);
            item.put(FixHeader.HEADER_INST_ID, redisInstId);

            arr.add(item);
        }

        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        retval.put(FixHeader.HEADER_RET_INFO, arr);
    }

    public static void getRedisInstanceInfo(JsonObject retval, String instId, Long startTimestamp, Long endTimestamp) {
        StringBuilder sql = new StringBuilder(RedisStatisticSql.SQL_QUERY_REDIS_INSTANCE_INFO);
        sql.append(" WHERE ");
        if (StringUtils.isNotBlank(instId)) {
            sql.append("INSTANT_ID=").append("'").append(instId).append("' AND ");
        }
        if (startTimestamp != null) {
            sql.append(FixHeader.HEADER_TS + ">=").append(startTimestamp).append(" AND ");
        }
        if (endTimestamp != null) {
            sql.append(FixHeader.HEADER_TS + "<=").append(endTimestamp).append(" AND ");
        }
        String sqlstr = sql.toString();
        if (sqlstr.endsWith("AND ")) {
            sqlstr = sqlstr.substring(0, sqlstr.length() - 4);
        }
        try {
            SqlBean sqlBean = new SqlBean(sqlstr);
            CRUD c = new CRUD(TD_NAME);
            c.putSqlBean(sqlBean);
            List<JsonObject> passRedisInfoList = new ArrayList<>();
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList != null && resultList.size() > 0) {
                resultList.forEach(result -> {
                    PassRedisInfo passRedisInfo = PassRedisInfo.convert(result);
                    passRedisInfoList.add(passRedisInfo.toJson());
                });
            }
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, passRedisInfoList);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getRedisServiceInfo(JsonObject retval, String[] instId, Long startTimestamp, Long endTimestamp) {
        StringBuilder sql = new StringBuilder(RedisStatisticSql.SQL_QUERY_REDIS_INSTANCE_INFO);
        sql.append(" WHERE ");
        if (instId == null || instId.length != 2) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_NOT_TWO_NUM_CLUSTER_ID);
            return;
        }
        String clusterA = instId[0];
        String clusterB = instId[1];
        sql.append("(INSTANT_ID='").append(clusterA).append("' OR INSTANT_ID='").append(clusterB).append("') AND ");
        if (startTimestamp != null) {
            sql.append(FixHeader.HEADER_TS + ">=").append(startTimestamp).append(" AND ");
        }
        if (endTimestamp != null) {
            sql.append(FixHeader.HEADER_TS + "<=").append(endTimestamp).append(" AND ");
        }
        String sqlstr = sql.toString();
        if (sqlstr.endsWith("AND ")) {
            sqlstr = sqlstr.substring(0, sqlstr.length() - 4);
        }
        try {
            JsonObject jsonObject = new JsonObject();
            SqlBean sqlBean = new SqlBean(sqlstr);
            CRUD c = new CRUD(TD_NAME);
            c.putSqlBean(sqlBean);
            Map<String, List<JsonObject>> clusterMap = new HashMap<>();
            clusterMap.put(clusterA, new ArrayList<>());
            clusterMap.put(clusterB, new ArrayList<>());
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList != null && resultList.size() > 0) {
                resultList.forEach(result -> {
                    PassRedisInfo passRedisInfo = PassRedisInfo.convert(result);
                    String instantId = passRedisInfo.getInstantId();
                    List<JsonObject> passRedisInfoList = clusterMap.get(instantId);
                    passRedisInfoList.add(passRedisInfo.toJson());
                });
            }
            jsonObject.put(clusterA, clusterMap.get(clusterA));
            jsonObject.put(clusterB, clusterMap.get(clusterB));
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, jsonObject);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void saveRedisInfo(JsonObject retval, List<PassRedisInfo> passRedisInfoList) {
        try {
            if (passRedisInfoList == null || passRedisInfoList.size() < 1) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            CRUD c = new CRUD(TD_NAME);
            passRedisInfoList.forEach(passRedisInfo -> {
                SqlBean sqlBean = new SqlBean(RedisStatisticSql.SQL_ADD_REDIS_INFO);
                Long ts = passRedisInfo.getTs();
                String role = passRedisInfo.getRole();
                String instId = passRedisInfo.getInstantId();
                Integer connectedClients = passRedisInfo.getConnectedClients();
                Long usedMemory = passRedisInfo.getUsedMemory();
                Long maxMemory = passRedisInfo.getMaxMemory();
                Integer instantaneousOpsPerSec = passRedisInfo.getInstantaneousOpsPerSec();
                Double instantaneousInputKbps = passRedisInfo.getInstantaneousInputKbps();
                Double instantaneousOutputKbps = passRedisInfo.getInstantaneousOutputKbps();
                Integer syncFull = passRedisInfo.getSyncFull();
                Long expiredKeys = passRedisInfo.getExpiredKeys();
                Long evictedKeys = passRedisInfo.getEvictedKeys();
                Long keyspaceHits = passRedisInfo.getKeyspaceHits();
                Long keyspaceMisses = passRedisInfo.getKeyspaceMisses();
                Double usedCpuSys = passRedisInfo.getUsedCpuSys();
                Double usedCpuUser = passRedisInfo.getUsedCpuUser();
                String tableName = String.format("redis_%s", instId.replace("-", "_"));
                sqlBean.addParams(new Object[]{
                        tableName, instId, new Timestamp(ts), instId, role, connectedClients, usedMemory, maxMemory,
                        instantaneousOpsPerSec, instantaneousInputKbps, instantaneousOutputKbps, syncFull, expiredKeys,
                        evictedKeys, keyspaceHits, keyspaceMisses, usedCpuSys, usedCpuUser
                });
                c.putSqlBean(sqlBean);
            });
            boolean res = c.batchUpdate();
            if (!res) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }


    public static void main(String[] args) {
        JsonObject retval = new JsonObject();
        List<PassRedisInfo> passRedisInfoList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            PassRedisInfo passRedisInfo = new PassRedisInfo();
            passRedisInfo.setInstantId(i + "-000000000-" + i);
            passRedisInfoList.add(passRedisInfo);
        }
        RedisStatisticDao.saveRedisInfo(retval, passRedisInfoList);
        System.out.println(retval.toString());
    }

}
