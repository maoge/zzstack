package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.bean.*;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.sql.RocketMqStatisticSql;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

public class RocketMqStatisticDao {

    private static final Logger logger = LoggerFactory.getLogger(RocketMqStatisticDao.class);

    private static final String TD_NAME;
    private static final String ROCKETMQ_TABLE_FIX = "rocketmq_";

    static {
        TD_NAME = SysConfig.get().getTDYamlName();
    }


    private static List<HashMap<String, Object>> getRocketMqTables() {
        CRUD c = new CRUD(TD_NAME);
        SqlBean sqlBean = new SqlBean(RocketMqStatisticSql.SQL_SHOW_TABLES);
        c.putSqlBean(sqlBean);
        List<HashMap<String, Object>> rocketmqTables = null;
        try {
            rocketmqTables = c.queryForList();
        } catch (Exception e) {
            logger.error("fetch tables name error ......");
            rocketmqTables = new ArrayList<>();
        }

        return rocketmqTables;
    }

    public static void getTopicNameByInstId(JsonObject retval, String strInstId) {

        if (strInstId == null) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INSTANCE_NOT_FOUND);
            return;
        }

        List<HashMap<String, Object>> rocketmqTables = getRocketMqTables();
        Set<String> topicNames = new HashSet<String>();
        for (int i = 0; i < rocketmqTables.size(); i++) {
            String tbName = (String) rocketmqTables.get(i).get("TBNAME");
            String[] split = tbName.split(ROCKETMQ_TABLE_FIX);
            String instId = split[1].substring(0, split[1].length() - 1).replaceAll("_","-");
            if (strInstId.equals(instId)) {
                String strTopicName = split[2].substring(0, split[2].length() - 1);
                topicNames.add(strTopicName);
            }

        }

        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        retval.put(FixHeader.HEADER_RET_INFO, topicNames);

    }

    public static void getConsumeGroupByTopicName(JsonObject retval, String strTopicName) {

        if (strTopicName == null) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_TOPIC_NAME);
            return;
        }
        List<HashMap<String, Object>> rocketmqTables = getRocketMqTables();
        Set<String> consumeGroups = new HashSet<String>();
        for (int i = 0; i < rocketmqTables.size(); i++) {
            String tbName = (String) rocketmqTables.get(i).get("TBNAME");
            String[] split = tbName.split(ROCKETMQ_TABLE_FIX);
            String topicName = split[2].substring(0, split[2].length() - 1);
            if (strTopicName.equals(topicName)) {
                String strConsumeGroup = split[3];
                consumeGroups.add(strConsumeGroup);
            }

        }

        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        retval.put(FixHeader.HEADER_RET_INFO, consumeGroups);

    }

    public static void saveRocketMqInfo(JsonObject retval, List<PassRocketMqInfo> passRocketMqInfoList) {
        try {
            if (passRocketMqInfoList == null || passRocketMqInfoList.size() < 1) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            CRUD c = new CRUD(TD_NAME);
            passRocketMqInfoList.forEach(passRocketMqInfo -> {
                SqlBean sqlBean = new SqlBean(RocketMqStatisticSql.SQL_ADD_ROCKETMQ_INFO);
                Long ts = passRocketMqInfo.getTs();
                String instId = passRocketMqInfo.getInstantId();
                String topicName = passRocketMqInfo.getTopicName();
                String consumeGroup = passRocketMqInfo.getConsumeGroup();
                Long diffTotal = passRocketMqInfo.getDiffTotal();
                Long consumeTotal = passRocketMqInfo.getConsumeTotal();
                Long produceTotal = passRocketMqInfo.getProduceTotal();
                Double produceTps = passRocketMqInfo.getProduceTps();
                Double consumeTps = passRocketMqInfo.getConsumeTps();
                String tableName = String.format("rocketmq_%s_rocketmq_%s_rocketmq_%s", instId.replace("-", "_"), topicName, consumeGroup);
                sqlBean.addParams(new Object[]{
                        tableName, instId.replace("-", "_"), topicName, consumeGroup, new Timestamp(ts), instId, topicName, consumeGroup, diffTotal, produceTotal,
                        produceTps, consumeTotal, consumeTps
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


    public static void getRocketMqServiceInfo(JsonObject retval, String instId, String topicName,
                                              String consumeGroup, Long startTimestamp, Long endTimestamp) {
        StringBuilder sql = new StringBuilder(RocketMqStatisticSql.SQL_QUERY_ROCKETMQ_INFO);
        sql.append(" WHERE 1 = 1");
        if (instId == null) {
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_INSTANCE_NOT_FOUND);
            return;
        }

        if (instId != null && !"".equals(instId)) {
            sql.append(" AND ").append(FixHeader.HEADER_INST_ID  + "=").append("'" + instId + "'" );
        }

        if (topicName != null && !"".equals(topicName)) {
            sql.append(" AND ").append(FixHeader.HEADER_TOPIC_NAME + "=").append("'" + topicName + "'");
        }

        if (consumeGroup != null && !"".equals(consumeGroup)) {
            sql.append(" AND ").append(FixHeader.HEADER_CONSUME_GROUP + "=").append("'" + consumeGroup + "'");
        }

        if (startTimestamp != null) {
            sql.append(" AND ").append(FixHeader.HEADER_TS + ">=").append(startTimestamp);
        }
        if (endTimestamp != null) {
            sql.append(" AND ").append(FixHeader.HEADER_TS + "<=").append(endTimestamp);
        }

        try {
            SqlBean sqlBean = new SqlBean(sql.toString());
            CRUD c = new CRUD(TD_NAME);
            c.putSqlBean(sqlBean);

            List<HashMap<String, Object>> resultList = c.queryForList();

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, resultList);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void getRocketMQServBrokers(JsonObject retval, String servInstId) {
        CmptMeta cmptMeta = MetaSvrGlobalRes.get().getCmptMeta();

        Vector<PaasTopology> containers = new Vector<PaasTopology>();
        cmptMeta.getInstRelations(servInstId, containers);

        JsonArray arr = new JsonArray();
        String vbContainerId = null;

        for (PaasTopology topo : containers) {
            String toeId = topo.getToe(servInstId);
            if (toeId == null || toeId.isEmpty()) {
                continue;
            }

            PaasInstance containerInst = cmptMeta.getInstance(toeId);
            PaasMetaCmpt cmpt = cmptMeta.getCmptById(containerInst.getCmptId());
            if (cmpt.getCmptName().equals(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER)) {
                vbContainerId = toeId;
                break;
            }
        }

        Vector<PaasTopology> vbTopos = new Vector<PaasTopology>();
        if (vbContainerId != null) {
            cmptMeta.getInstRelations(vbContainerId, vbTopos);
        }

        for (PaasTopology topo : vbTopos) {
            String vbrokerId = topo.getToe(vbContainerId);
            if (vbrokerId == null || vbrokerId.isEmpty())
                continue;

            Vector<PaasTopology> brokerTopos = new Vector<PaasTopology>();
            cmptMeta.getInstRelations(vbrokerId, brokerTopos);

            for (PaasTopology brokerTopo : brokerTopos) {
                String brokerId = brokerTopo.getToe(vbrokerId);

                String sshId = cmptMeta.getInstAttr(brokerId, 116).getAttrValue();  // 116, 'SSH_ID'
                PaasSsh ssh = cmptMeta.getSshById(sshId);
                String ip = ssh.getServerIp();

                String port = cmptMeta.getInstAttr(brokerId, 126).getAttrValue();   // 126, 'LISTEN_PORT'
                String role = cmptMeta.getInstAttr(brokerId, 128).getAttrValue();   // 128, 'BROKER_ROLE'

                String brokerName = String.format("%s:%s(%s)", ip, port, role);

                JsonObject item = new JsonObject();
                item.put(FixHeader.HEADER_BROKER_NAME, brokerName);
                item.put(FixHeader.HEADER_INST_ID, brokerId);

                arr.add(item);
            }
        }

        retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
        retval.put(FixHeader.HEADER_RET_INFO, arr);
    }



}
