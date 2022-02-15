package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.metasvr.bean.PassHostInfo;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.sql.HostStatisticSql;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.metasvr.utils.TDSTableNameUtils;
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

public class HostStatisticDao {

    private static final Logger logger = LoggerFactory.getLogger(HostStatisticDao.class);

    private static final String TD_NAME;

    static {
        TD_NAME = SysConfig.get().getTDYamlName();
    }

    public static void getHostInfo(JsonObject retval, String instId, Long startTimestamp, Long endTimestamp) {
        StringBuilder sql = new StringBuilder(HostStatisticSql.SQL_QUERY_HOST_INFO);
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
            List<JsonObject> passHostInfoList = new ArrayList<>();
            List<HashMap<String, Object>> resultList = c.queryForList();
            if (resultList != null && resultList.size() > 0) {
                resultList.forEach(result -> {
                    PassHostInfo passHostInfo = PassHostInfo.convert(result);
                    passHostInfoList.add(passHostInfo.toJson());
                });
            }
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            retval.put(FixHeader.HEADER_RET_INFO, passHostInfoList);
        } catch (DBException e) {
            logger.error(e.getMessage(), e);
            retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
        }
    }

    public static void saveHostInfo(JsonObject retval, List<PassHostInfo> passHostInfoList) {
        try {
            if (passHostInfoList == null || passHostInfoList.isEmpty()) {
                retval.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
                retval.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
                return;
            }
            CRUD c = new CRUD(TD_NAME);
            passHostInfoList.forEach(passHostInfo -> {
                SqlBean sqlBean = new SqlBean(HostStatisticSql.SQL_ADD_HOST_INFO);
                Long ts = passHostInfo.getTs();
                String instId = passHostInfo.getInstantId();
                Long totalMemory = passHostInfo.getTotalMemory();
                Long usedMemory = passHostInfo.getUsedMemory();
                Double usedUserCpu = passHostInfo.getUsedUserCpu();
                Double usedSysCpu = passHostInfo.getUsedSysCpu();
                Double cpuIdle = passHostInfo.getCpuIdle();
                Long totalDisk = passHostInfo.getTotalDisk();
                Long usedDisk = passHostInfo.getUsedDisk();
                Long unusedDisk = passHostInfo.getUnusedDisk();
                Long userUsedDisk = passHostInfo.getUserUsedDisk();
                Double inputBandWidth = passHostInfo.getInputBandWidth();
                Double outputBandWidth = passHostInfo.getOutputBandWidth();
                String tableName = String.format("host_%s", instId.replace("-", "_"));
                sqlBean.addParams(new Object[] {
                        tableName, instId, new Timestamp(ts), instId, totalMemory, usedMemory, usedUserCpu, usedSysCpu,
                        cpuIdle, totalDisk, usedDisk, unusedDisk, userUsedDisk, inputBandWidth, outputBandWidth
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

    public static void saveCPU(String servIP, long ts, float userCPU, float systemCPU, float waitCPU,
            float interruptCPU, float idleCPU, float niceCPU, float softirqCPU, float stealCPU) {

        try {
            String tableName = TDSTableNameUtils.getSTableForHost(servIP, "cpu");
            
            CRUD c = new CRUD(TD_NAME);
            SqlBean sqlBean = new SqlBean(HostStatisticSql.SQL_INS_CPU_INFO);
            sqlBean.addParams(new Object[] { tableName, servIP, ts, servIP, userCPU, systemCPU, waitCPU, interruptCPU, idleCPU, niceCPU,
                    softirqCPU, stealCPU });
            
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error("saveCPU error:{}", e.getMessage(), e);
        }
    }

    public static void saveMemory(String servIP, long ts, float used, float free, float buffered, float cached,
            float slabUnrecl, float slabRecl) {
        
        try {
            String tableName = TDSTableNameUtils.getSTableForHost(servIP, "mem");
            
            CRUD c = new CRUD(TD_NAME);
            SqlBean sqlBean = new SqlBean(HostStatisticSql.SQL_INS_MEM_INFO);
            sqlBean.addParams(new Object[] { tableName, servIP, ts, servIP, used, free, buffered, cached, slabUnrecl, slabRecl });
            
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error("saveMemory error:{}", e.getMessage(), e);
        }
    }
    
    public static void saveNic(String servIP, long ts, String nicName, long packetsTx, long packetsRx, long octetsTx, long octetsRx,
            long errorsTx, long errorsRx, long droppedTx, long droppedRx) {

        try {
            String tableName = TDSTableNameUtils.getSTableForHost(servIP, "nic", nicName);
            
            CRUD c = new CRUD(TD_NAME);
            SqlBean sqlBean = new SqlBean(HostStatisticSql.SQL_INS_NIC_INFO);
            sqlBean.addParams(new Object[] { tableName, servIP, nicName,
                    ts, servIP, nicName,
                    packetsTx, packetsRx, octetsTx, octetsRx,
                    errorsTx, errorsRx, droppedTx, droppedRx});
            
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error("saveNic error:{}", e.getMessage(), e);
        }
    }
    
    public static void saveDisk(String servIP, long ts, String disk, long diskOpsRead, long diskOpsWrite,
            long diskOctetsRead, long diskOctetsWrite, long diskTimeRead, long diskTimeWrite, long diskIoTimeRead,
            long diskIoTimeWrite, long diskMergedRead, long diskMergedWrite) {

        try {
            String tableName = TDSTableNameUtils.getSTableForHost(servIP, "disk", disk);
            
            CRUD c = new CRUD(TD_NAME);
            SqlBean sqlBean = new SqlBean(HostStatisticSql.SQL_INS_DISK_INFO);
            sqlBean.addParams(new Object[] { tableName, servIP, disk,
                    ts, servIP, disk, diskOpsRead, diskOpsWrite, diskOctetsRead, diskOctetsWrite,
                    diskTimeRead, diskTimeWrite, diskIoTimeRead, diskIoTimeWrite, diskMergedRead, diskMergedWrite });
            
            c.putSqlBean(sqlBean);
            c.executeUpdate();
        } catch (Exception e) {
            logger.error("saveDisk error:{}", e.getMessage(), e);
        }
    }

}
