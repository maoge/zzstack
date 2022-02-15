package com.zzstack.paas.underlying.dbclient;

public class DBConsts {
    
    public static final String DEFAULT_SP_MSG_ID           = "0";
    public static final int    DEFAULT_SUBMIT_RESP_RESULT  = -999;
    public static final String DEFAULT_SP_MSG_SPLITER      = "|";
    
    public static final String REP_STAT_DELIVERD           = "DELIVRD";
    
    public static final int    REVOKE_OK                   = 0;
    public static final int    REVOKE_NOK                  = -1;
    
    public static final Long   TASK_TIMEOUT                = 10000L;  // 10s
    
    public static final String ERR_FETCH_SEQ_SQL_NOT_MATCH = "fetch sequence sql not match ......";
    public static final String ERR_SEQ_NOT_EXISTS          = "sequence not exists ......";
    
    // public static final String DEFAULT_DB_FILE             = "smsdb";
    
    public static final String HEADER_CURR_VALUE           = "CURR_VALUE";

}
