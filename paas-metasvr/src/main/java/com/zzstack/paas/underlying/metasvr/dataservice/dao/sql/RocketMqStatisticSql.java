package com.zzstack.paas.underlying.metasvr.dataservice.dao.sql;

public class RocketMqStatisticSql {

    public static final String SQL_QUERY_ROCKETMQ_INFO = "SELECT TS,INST_ID,TOPIC_NAME,CONSUME_GROUP," +
            "DIFF_TOTAL,PRODUCE_TOTAL,PRODUCE_TPS,CONSUME_TOTAL,CONSUME_TPS FROM T_ROCKETMQ_INFO ";
    
    public static final String SQL_ADD_ROCKETMQ_INFO = "INSERT INTO ? (TS,INST_ID,TOPIC_NAME,CONSUME_GROUP,DIFF_TOTAL,"
            + "PRODUCE_TOTAL,PRODUCE_TPS,CONSUME_TOTAL,CONSUME_TPS) "
            + "USING T_ROCKETMQ_INFO (INSTANT_ID,TOPIC,CGROUP) TAGS (?,?,?) VALUES (?,?,?,?,?,?,?,?,?)";
    
    public static final String SQL_SHOW_TABLES = "SELECT TBNAME FROM T_ROCKETMQ_INFO";

}
