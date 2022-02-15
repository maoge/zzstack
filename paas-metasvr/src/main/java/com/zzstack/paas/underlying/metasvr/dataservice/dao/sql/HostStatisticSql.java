package com.zzstack.paas.underlying.metasvr.dataservice.dao.sql;

public class HostStatisticSql {

    public static final String SQL_QUERY_HOST_INFO = "SELECT TS,INST_ID,MEMORY_TOTAL,MEMORY_USED,USED_CPU_USER," +
            "USED_CPU_SYS,CPU_IDLE,TOTAL_DISK,USED_DISK,UNUSED_DISK,USER_USED_DISK,INPUT_BANDWIDTH,OUTPUT_BANDWIDTH FROM T_HOST_INFO ";
    
    public static final String SQL_ADD_HOST_INFO = "INSERT INTO ? (TS,INST_ID,MEMORY_TOTAL,MEMORY_USED,USED_CPU_USER," +
            "USED_CPU_SYS,CPU_IDLE,TOTAL_DISK,USED_DISK,UNUSED_DISK,USER_USED_DISK,INPUT_BANDWIDTH,OUTPUT_BANDWIDTH) " +
            "USING T_HOST_INFO (INSTANT_ID) TAGS (?) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INS_CPU_INFO = 
              "INSERT INTO ? (TS,SERV_IP,CPU_USER,CPU_SYSTEM,CPU_WAIT,"
            +     "CPU_INTERRUPT,CPU_IDLE,CPU_NICE,CPU_SOFTIRQ,CPU_STEAL) "
            + "USING T_CPU_INFO (SERVER_IP) TAGS (?) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INS_MEM_INFO = 
            "INSERT INTO ? (TS,SERV_IP,MEM_USED,MEM_FREE,MEM_BUFFERED,MEM_CACHED,MEM_SLAB_UNRECL,MEM_SLAB_RECL) "
          + "USING T_MEMORY_INFO (SERVER_IP) TAGS (?) "
          + "VALUES (?,?,?,?,?,?,?,?)";

    public static final String SQL_INS_NIC_INFO = 
            "INSERT INTO ? (TS,SERV_IP,NIC_NAME,"
          +     "PACKETS_TX,PACKETS_RX,OCTETS_TX,OCTETS_RX,"
          +     "ERRORS_TX,ERRORS_RX,DROPPED_TX,DROPPED_RX) "
          + "USING T_NIC_INFO (SERVER_IP, NIC) TAGS (?, ?) "
          + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

    public static final String SQL_INS_DISK_INFO = 
            "INSERT INTO ? (TS,SERV_IP,DISK_NAME,DISK_OPS_R,DISK_OPS_W,DISK_OCTETS_R,DISK_OCTETS_W,"
          +     "DISK_TIME_R,DISK_TIME_W,DISK_IO_TIME_R,DISK_IO_TIME_W,DISK_MERGED_R,DISK_MERGED_W) "
          + "USING T_DISK_INFO (SERVER_IP, DISK_) TAGS (?, ?) "
          + "VALUES (?,?,?,?,?,?,?,"
          +     "?,?,?,?,?,?)";

}
