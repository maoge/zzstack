package com.zzstack.paas.underlying.dbclient.constants;

public class DBConstants {

    public static final String SQL_NEXT_SEQ_LOCK = "select current_value as CURR_VALUE from t_meta_sequence where seq_name = ? for update";
    public static final String SQL_NEXT_SEQ_UPDATE = "update t_meta_sequence set current_value = current_value + %d where seq_name = ?";

}
