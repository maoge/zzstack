package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AccOperLogDao {
    
    private static Logger logger = LoggerFactory.getLogger(AccOperLogDao.class);

    public static final String INS_OP_LOG = "INSERT INTO t_meta_oplogs(ACC_NAME, EVENT_TYPE, OP_DETAIL, INSERT_TIME) VALUES(?, ?, ?, ?)";
    public static final String SEL_OP_LOG_COUNT = "SELECT count(1) count FROM t_meta_oplogs WHERE 1=1 %s";
    public static final String SEL_OP_LOG_LIST = "SELECT ACC_NAME, EVENT_TYPE, OP_DETAIL, INSERT_TIME FROM t_meta_oplogs "
            + "WHERE 1=1 %s ORDER BY INSERT_TIME ASC %d, %d";
    
    private static String METADB_NAME = null;
    
    static {
        METADB_NAME = SysConfig.get().getMetaDBYamlName();
    }
    
    public static void addOpLog(String accName, String opType, String opDetail, long insertTime) {

        try {
            SqlBean sqlBean = new SqlBean(INS_OP_LOG);
            sqlBean.addParams(new Object[] { accName, opType, opDetail, insertTime });

            CRUD c = new CRUD(METADB_NAME);
            c.putSqlBean(sqlBean);

            boolean res = c.executeUpdate();
            if (!res) {
                logger.error("addOpLog fail, {}", CONSTS.ERR_DB);
                return;
            }

        } catch (Exception e) {
            logger.error("addOpLog fail, caught exception:{}", e.getMessage(), e);
        }
    }
    
    public static void getOpLogCnt(JsonObject retval, String accName, long startTS, long endTS) {
        StringBuilder sqlWhere = new StringBuilder(SEL_OP_LOG_COUNT);
        if (accName != null && !accName.isEmpty()) {
            sqlWhere.append(" AND ACC_NAME = ").append("'").append(accName).append("' ");
        }
        if (startTS > 0) {
            sqlWhere.append(" AND INSERT_TIME >= ").append(startTS);
        }
        if (endTS > 0) {
            sqlWhere.append(" AND INSERT_TIME <= ").append(endTS);
        }

        String sql = String.format(SEL_OP_LOG_COUNT, sqlWhere.toString());
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
    
    public static void getOpLogList(JsonObject retval, int pageSize, int pageNum, String accName, long startTS, long endTS) {
        StringBuilder sqlWhere = new StringBuilder("");
        if (accName != null && !accName.isEmpty()) {
            sqlWhere.append(" AND ACC_NAME = ").append("'").append(accName).append("' ");
        }
        if (startTS > 0) {
            sqlWhere.append(" AND INSERT_TIME >= ").append(startTS);
        }
        if (endTS > 0) {
            sqlWhere.append(" AND INSERT_TIME <= ").append(endTS);
        }

        int start = pageSize * (pageNum - 1);
        String sql = String.format(SEL_OP_LOG_LIST, sqlWhere.toString(), start, pageSize);
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

            JsonArray arr = new JsonArray();
            for (HashMap<String, Object> rowHash : resultList) {
                JsonObject item = new JsonObject();
                for (Entry<String, Object> entry : rowHash.entrySet()) {
                    item.put(entry.getKey(), entry.getValue());
                }
                arr.add(item);
            }

            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, arr);

        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

}
