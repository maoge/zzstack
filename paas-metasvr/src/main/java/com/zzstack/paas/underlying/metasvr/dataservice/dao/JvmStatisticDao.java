package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.bean.PassJvmInfo;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.sql.JvmStatisticSql;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JvmStatisticDao {

    private static final Logger logger = LoggerFactory.getLogger(JvmStatisticDao.class);

    private static final String TD_NAME;

    static {
        TD_NAME = SysConfig.get().getTDYamlName();
    }

    public static void getJvmInfo(JsonObject retval, String instId, Long startTimestamp, Long endTimestamp) {
        StringBuilder sql = new StringBuilder(JvmStatisticSql.SQL_QUERY_JVM_INFO);
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
            List<JsonObject> passJvmInfoList = new ArrayList<>();
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList != null && resultList.size() > 0) {
                resultList.forEach(result -> {
                    PassJvmInfo passJvmInfo = PassJvmInfo.convert(result);
                    passJvmInfoList.add(passJvmInfo.toJson());
                });
            }
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, passJvmInfoList);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void saveJvmInfo(JsonObject retval, List<PassJvmInfo> passJvmInfoList) {
        try {
            if (passJvmInfoList == null || passJvmInfoList.size() < 1) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            CRUD c = new CRUD(TD_NAME);
            passJvmInfoList.forEach(passJvmInfo -> {
                SqlBean sqlBean = new SqlBean(JvmStatisticSql.SQL_ADD_JVM_INFO);
                Long ts = passJvmInfo.getTs();
                String instId = passJvmInfo.getInstantId();
                String javaVersion = passJvmInfo.getJavaVersion();
                Integer gcYoungGcCount = passJvmInfo.getGcYoungGcCount();
                Integer gcYoungGcTime = passJvmInfo.getGcYoungGcTime();
                Integer gcFullGcCount = passJvmInfo.getGcFullGcCount();
                Integer gcFullGcTime = passJvmInfo.getGcFullGcTime();
                Integer threadDaemonThreadCount = passJvmInfo.getThreadDaemonThreadCount();
                Integer threadCount = passJvmInfo.getThreadCount();
                Integer threadPeakThreadCount = passJvmInfo.getThreadPeakThreadCount();
                Integer threadDeadLockedThreadCount = passJvmInfo.getThreadDeadLockedThreadCount();
                Long memEdenInit = passJvmInfo.getMemEdenInit();
                Long memEdenUsed = passJvmInfo.getMemEdenUsed();
                Long memEdenCommitted = passJvmInfo.getMemEdenCommitted();
                Long memEdenMax = passJvmInfo.getMemEdenMax();
                Double memEdenUsedPercent = passJvmInfo.getMemEdenUsedPercent();
                Long memSurvivorInit = passJvmInfo.getMemSurvivorInit();
                Long memSurvivorUsed = passJvmInfo.getMemSurvivorUsed();
                Long memSurvivorCommitted = passJvmInfo.getMemSurvivorCommitted();
                Long memSurvivorMax = passJvmInfo.getMemSurvivorMax();
                Double memSurvivorUsedPercent = passJvmInfo.getMemSurvivorUsedPercent();
                Long memOldInit = passJvmInfo.getMemOldInit();
                Long memOldUsed = passJvmInfo.getMemOldUsed();
                Long memOldCommitted = passJvmInfo.getMemOldCommitted();
                Long memOldMax = passJvmInfo.getMemOldMax();
                Double memOldUsedPercent = passJvmInfo.getMemOldUsedPercent();
                Long memPermInit = passJvmInfo.getMemPermInit();
                Long memPermUsed = passJvmInfo.getMemPermUsed();
                Long memPermCommitted = passJvmInfo.getMemPermCommitted();
                Long memPermMax = passJvmInfo.getMemPermMax();
                Double memPermUsedPercent = passJvmInfo.getMemPermUsedPercent();
                Long memCodeInit = passJvmInfo.getMemCodeInit();
                Long memCodeUsed = passJvmInfo.getMemCodeUsed();
                Long memCodeCommitted = passJvmInfo.getMemCodeCommitted();
                Long memCodeMax = passJvmInfo.getMemCodeMax();
                Double memCodeUsedPercent = passJvmInfo.getMemCodeUsedPercent();
                Long memHeapInit = passJvmInfo.getMemHeapInit();
                Long memHeapUsed = passJvmInfo.getMemHeapUsed();
                Long memHeapCommitted = passJvmInfo.getMemHeapCommitted();
                Long memHeapMax = passJvmInfo.getMemHeapMax();
                Double memHeapUsedPercent = passJvmInfo.getMemHeapUsedPercent();
                Long memNoneHeapInit = passJvmInfo.getMemNoHeapInit();
                Long memNoneHeapUsed = passJvmInfo.getMemNoHeapUsed();
                Long memNoneHeapCommitted = passJvmInfo.getMemNoHeapCommitted();
                Long memNoneHeapMax = passJvmInfo.getMemNoHeapMax();
                Double memNoneHeapUsedPercent = passJvmInfo.getMemNoHeapUsedPercent();
                String tableName = String.format("jvm_%s", instId.replace("-", "_"));
                sqlBean.addParams(new Object[]{
                        tableName, instId, new Timestamp(ts), instId, javaVersion, gcYoungGcCount, gcYoungGcTime,
                        gcFullGcCount, gcFullGcTime, threadDaemonThreadCount, threadCount, threadPeakThreadCount,
                        threadDeadLockedThreadCount, memEdenInit, memEdenUsed, memEdenCommitted, memEdenMax,
                        memEdenUsedPercent, memSurvivorInit, memSurvivorUsed, memSurvivorCommitted, memSurvivorMax,
                        memSurvivorUsedPercent, memOldInit, memOldUsed, memOldCommitted, memOldMax, memOldUsedPercent,
                        memPermInit, memPermUsed, memPermCommitted, memPermMax, memPermUsedPercent,
                        memCodeInit, memCodeUsed, memCodeCommitted, memCodeMax, memCodeUsedPercent,
                        memHeapInit, memHeapUsed, memHeapCommitted, memHeapMax, memHeapUsedPercent,
                        memNoneHeapInit, memNoneHeapUsed, memNoneHeapCommitted, memNoneHeapMax, memNoneHeapUsedPercent
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
        List<PassJvmInfo> passJvmInfoList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            PassJvmInfo passJvmInfo = new PassJvmInfo();
            passJvmInfo.setInstantId(i + "-000000000-" + i);
            passJvmInfoList.add(passJvmInfo);
        }
        JvmStatisticDao.saveJvmInfo(retval, passJvmInfoList);
        System.out.println(retval);
    }

}
