package com.zzstack.paas.underlying.metasvr.dataservice.dao.sql;

public class MetaDataSql {

    public static final String SQL_SEL_ACCOUNT = "select ACC_ID, ACC_NAME, PHONE_NUM, MAIL, PASSWD, CREATE_TIME from t_account";

    public static final String SQL_SEL_SERVICE_BY_ID = "SELECT INST_ID,SERV_NAME,SERV_CLAZZ,SERV_TYPE,IS_DEPLOYED,IS_PRODUCT,CREATE_TIME,USER,PASSWORD,PSEUDO_DEPLOY_FLAG,VERSION "
            + "FROM t_meta_service WHERE INST_ID = ?";

    public static final String SQL_SEL_META_ATTR = "SELECT ATTR_ID,ATTR_NAME,ATTR_NAME_CN,AUTO_GEN FROM t_meta_attr ORDER BY ATTR_ID";

    public static final String SQL_SEL_META_CMPT = "SELECT CMPT_ID,CMPT_NAME,CMPT_NAME_CN,IS_NEED_DEPLOY,SERV_TYPE,SERV_CLAZZ,NODE_JSON_TYPE,SUB_CMPT_ID "
            + "FROM t_meta_cmpt ORDER BY CMPT_ID";

    public static final String SQL_SEL_META_CMPT_ATTR = "SELECT CMPT_ID,ATTR_ID FROM t_meta_cmpt_attr ORDER BY CMPT_ID,ATTR_ID";

    public static final String SQL_SEL_META_INST = "SELECT INST_ID,CMPT_ID,IS_DEPLOYED,POS_X,POS_Y,WIDTH,HEIGHT,ROW_,COL_ FROM t_meta_instance";

    public static final String SQL_SEL_META_INST_ATTR = "SELECT INST_ID,ATTR_ID,ATTR_NAME,ATTR_VALUE FROM t_meta_instance_attr";

    public static final String SQL_SEL_META_SERVICE = "SELECT INST_ID,SERV_NAME,SERV_CLAZZ,SERV_TYPE,VERSION,IS_DEPLOYED,IS_PRODUCT,CREATE_TIME,USER,PASSWORD,PSEUDO_DEPLOY_FLAG FROM t_meta_service";

    public static final String SQL_SEL_META_TOPO = "SELECT INST_ID1,INST_ID2,TOPO_TYPE FROM t_meta_topology";

    public static final String SQL_SEL_META_DEP_HOST = "SELECT HOST_ID,IP_ADDRESS,USER_NAME,USER_PWD,SSH_PORT,CREATE_TIME FROM t_meta_deploy_host";

    public static final String SQL_SEL_META_DEP_FILE = "SELECT FILE_ID,HOST_ID,SERV_TYPE,VERSION,FILE_NAME,FILE_DIR,CREATE_TIME FROM t_meta_deploy_file";

    public static final String SQL_SEL_META_SERVER = "SELECT SERVER_IP,SERVER_NAME FROM t_meta_server";

    public static final String SQL_SEL_META_SSH = "SELECT SSH_ID,SSH_NAME,SSH_PWD,SSH_PORT,SERV_CLAZZ,SERVER_IP FROM t_meta_ssh";
    
    public static final String SQL_SEL_META_CMPT_VER = "SELECT SERV_TYPE, VERSION from t_meta_cmpt_versions order by SERV_TYPE, VERSION";

    public static final String SQL_COUNT_SERVICE_LIST = "SELECT count(1) count FROM t_meta_service WHERE 1=1 ";
    
    public static final String SQL_COUNT_SERV_TYPE_VER_LIST = "SELECT count(1) count FROM t_meta_cmpt_versions WHERE 1=1 ";

    public static final String SQL_SEL_SERVICE_LIST = "SELECT INST_ID, SERV_NAME, SERV_TYPE, SERV_CLAZZ, VERSION, IS_DEPLOYED, IS_PRODUCT FROM t_meta_service "
            + "WHERE 1=1 %s ORDER BY CREATE_TIME limit %d, %d";
    
    public static final String SQL_SEL_SERV_TYPE_VER_LIST = "SELECT SERV_TYPE, VERSION FROM t_meta_cmpt_versions "
            + "WHERE 1=1 %s ORDER BY SERV_TYPE, VERSION limit %d, %d";

    public static final String SQL_ADD_SERVICE = "INSERT INTO t_meta_service(INST_ID, SERV_NAME, SERV_CLAZZ, SERV_TYPE, VERSION, IS_DEPLOYED, "
            + "IS_PRODUCT, CREATE_TIME, USER, PASSWORD, PSEUDO_DEPLOY_FLAG) "
            + "VALUES(?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_DEL_SERVICE = "DELETE FROM t_meta_service WHERE inst_id = ?";

    public static final String SQL_DEL_INSTANCE_ATTR = "DELETE FROM t_meta_instance_attr WHERE INST_ID = ?";
    
    public static final String SQL_UPD_INSTANCE_PRE_EMBADDED = "UPDATE t_meta_instance_attr SET ATTR_VALUE= ?  WHERE INST_ID = ? AND ATTR_ID = 320"; // 320 -> 'PRE_EMBEDDED'

    public static final String SQL_CHECK_SSH_USED = "SELECT COUNT(*) CNT FROM t_meta_instance_attr WHERE ATTR_NAME=? and ATTR_VALUE=?";

    public static final String SQL_UPD_INST_DEPLOY = "UPDATE t_meta_instance SET IS_DEPLOYED = ? WHERE INST_ID = ?";

    public static final String SQL_UPD_SERV_DEPLOY = "UPDATE t_meta_service SET IS_DEPLOYED = ? WHERE INST_ID = ?";

    public static final String SQL_DEL_INSTANCE = "DELETE FROM t_meta_instance WHERE INST_ID = ?";

    public static final String SQL_MOD_SERVICE = "UPDATE t_meta_service SET SERV_NAME = ?, VERSION = ?, IS_PRODUCT = ? WHERE INST_ID = ?";
    public static final String SQL_MOD_SERVICE_VERSION = "UPDATE t_meta_service SET VERSION = ? WHERE INST_ID = ?";
    
    public static final String SQL_MOD_SERVICE_PSEUDO_DEPLOY_FLAG = "UPDATE t_meta_service SET PSEUDO_DEPLOY_FLAG = ? WHERE INST_ID = ?";

    public static final String SQL_COUNT_SERVER_LIST = "SELECT count(1) count FROM t_meta_server WHERE 1=1 %s";

    public static final String SQL_SEL_SERVER_LIST = "SELECT SERVER_IP, SERVER_NAME FROM t_meta_server WHERE 1=1 %s order by SERVER_IP limit %d, %d";

    public static final String SQL_ADD_SERVER = "INSERT INTO t_meta_server(SERVER_IP, SERVER_NAME, CREATE_TIME) VALUES(?,?,?)";

    public static final String SQL_DEL_SERVER = "DELETE FROM t_meta_server WHERE SERVER_IP=?";

    public static final String SQL_SSH_LIST_BY_IP = "SELECT SSH_NAME, SERV_CLAZZ, SSH_ID, SERVER_IP, SSH_PORT FROM t_meta_ssh "
            + "WHERE SERVER_IP=? order by SSH_NAME limit %d, %d ";

    public static final String SQL_SSH_CNT_BY_IP = "SELECT count(1) count FROM t_meta_ssh WHERE SERVER_IP=? ";

    public static final String SQL_ADD_SSH = "INSERT INTO t_meta_ssh(SSH_NAME,SSH_PWD,SSH_PORT,SERV_CLAZZ,SERVER_IP,SSH_ID) VALUES(?,?,?,?,?,?)";

    public static final String SQL_MOD_SSH = "UPDATE t_meta_ssh SET SSH_NAME=?, SSH_PWD=?, SSH_PORT=? WHERE SSH_ID=?";

    public static final String SQL_DEL_SSH = "DELETE FROM t_meta_ssh WHERE SSH_ID=?";

    public static final String SQL_GET_USER_BY_SERVCLAZZ = "SELECT t1.SERVER_IP, t1.SSH_NAME, t1.SSH_ID FROM t_meta_ssh t1 "
            + "LEFT JOIN t_meta_server t2 ON t1.SERVER_IP = t2.SERVER_IP "
            + "WHERE t1.SERV_CLAZZ = ? ORDER BY SERVER_IP";

    public static final String SQL_INS_INSTANCE = "insert into t_meta_instance(INST_ID,CMPT_ID,IS_DEPLOYED,POS_X,POS_Y,WIDTH,HEIGHT,ROW_,COL_) values(?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INS_INSTANCE_ATTR = "insert into t_meta_instance_attr(INST_ID,ATTR_ID,ATTR_NAME,ATTR_VALUE) values(?,?,?,?)";

    public static final String SQL_INS_TOPOLOGY = "insert into t_meta_topology(INST_ID1,INST_ID2,TOPO_TYPE) values(?,?,?)";
    public static final String SQL_UPD_TOPOLOGY = "update t_meta_topology set INST_ID2 = ? where INST_ID1 = ?";
    
    public static final String SQL_DEL_TOPOLOGY = "DELETE FROM t_meta_topology "
            + "WHERE (INST_ID1 = ? AND INST_ID2 = ?) OR (INST_ID2 = ? and TOPO_TYPE = 2)";
    public static final String SQL_DEL_ALL_SUB_TOPOLOGY = "DELETE FROM t_meta_topology "
            + "WHERE (INST_ID1 = ? AND TOPO_TYPE = 1) OR (INST_ID1 = ? and TOPO_TYPE = 2)";

    public static final String SQL_UPDATE_POS = "UPDATE t_meta_instance SET POS_X=?,POS_Y=?, WIDTH=?, HEIGHT=?,ROW_=?,COL_=? WHERE INST_ID = ?";

    public static final String SQL_MOD_ATTR = "UPDATE t_meta_instance_attr SET ATTR_VALUE = ? WHERE INST_ID = ? AND ATTR_ID = ?";
    
    public static final String SQL_INS_CMPT_VER = "INSERT INTO t_meta_cmpt_versions(SERV_TYPE,VERSION) VALUES(?,?)";
    public static final String SQL_DEL_CMPT_VER = "DELETE FROM t_meta_cmpt_versions WHERE SERV_TYPE=? AND VERSION=?";
    
    public static final String UPD_ACC_PASSWD = "UPDATE t_account SET PASSWD = ? WHERE ACC_NAME = ?";
    
    public static final String SQL_INS_ALARM = "insert into t_meta_alarm(ALARM_ID,SERV_INST_ID,SERV_TYPE,INST_ID,CMPT_NAME,ALARM_TYPE,ALARM_TIME) "
            + "VALUES(?,?,?,?,?,?,?)";
    
    public static final String SQL_UPD_ALARM_STATE_BY_ALARMID = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where ALARM_ID = ?";
    public static final String SQL_UPD_ALARM_STATE_BY_INSTID = "update t_meta_alarm set DEAL_TIME = ?, DEAL_ACC_NAME = ?, IS_DEALED = ? where SERV_INST_ID = ? and INST_ID = ? and ALARM_TYPE = ?";

    public static final String SQL_SEL_ALARM_CNT = "select count(*) cnt from t_meta_alarm where 1=1 ";
    public static final String SQL_SEL_ALARM_LIST = 
              "select a.ALARM_ID,a.SERV_INST_ID,a.SERV_TYPE,a.INST_ID,a.CMPT_NAME,"
            +        "a.ALARM_TYPE,a.ALARM_TIME,a.DEAL_TIME,a.DEAL_ACC_NAME,a.IS_DEALED,"
            +        "s.SERV_NAME,s.SERV_CLAZZ,s.IS_PRODUCT,s.VERSION "
            + "from t_meta_alarm a "
            + "left join t_meta_service s "
            +     "on a.SERV_INST_ID = s.INST_ID "
            + "WHERE 1=1 %s ORDER BY ALARM_TIME DESC limit %d, %d";
    
    public static final String SQL_NEXT_SEQ_LOCK = "select current_value as CURR_VALUE from t_meta_sequence where seq_name = ? for update";
    public static final String SQL_NEXT_SEQ_UPDATE = "update t_meta_sequence set current_value = current_value + %d where seq_name = ?";

}
