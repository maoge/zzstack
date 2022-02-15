CREATE DATABASE tsdb KEEP 180 DAYS 10 BLOCKS 4 UPDATE 1;

USE tsdb;

/* TDengine建表jvm监控信息的sql - PassJvmInfo.class */
DROP TABLE IF EXISTS "t_jvm_info";
CREATE TABLE t_jvm_info
(
    TS                             timestamp,
    INST_ID                        nchar(48),
    JAVA_VERSION                   nchar(20),
    GC_YOUNG_GC_COUNT              int,
    GC_YOUNG_GC_TIME               int,
    GC_FULL_GC_COUNT               int,
    GC_FULL_GC_TIME                int,
    THREAD_COUNT                   int,
    THREAD_DAEMON_THREAD_COUNT     int,
    THREAD_PEEK_THREAD_COUNT       int,
    THREAD_DEADLOCKED_THREAD_COUNT int,
    MEM_EDEN_INIT                  bigint,
    MEM_EDEN_USED                  bigint,
    MEM_EDEN_COMMITTED             bigint,
    MEM_EDEN_MAX                   bigint,
    MEM_EDEN_USEDPERCENT           double,
    MEM_SURVIVOR_INIT              bigint,
    MEM_SURVIVOR_USED              bigint,
    MEM_SURVIVOR_COMMITTED         bigint,
    MEM_SURVIVOR_MAX               bigint,
    MEM_SURVIVOR_USEDPERCENT       double,
    MEM_OLD_INIT                   bigint,
    MEM_OLD_USED                   bigint,
    MEM_OLD_COMMITTED              bigint,
    MEM_OLD_MAX                    bigint,
    MEM_OLD_USEDPERCENT            double,
    MEM_PERM_INIT                  bigint,
    MEM_PERM_USED                  bigint,
    MEM_PERM_COMMITTED             bigint,
    MEM_PERM_MAX                   bigint,
    MEM_PERM_USEDPERCENT           double,
    MEM_CODE_INIT                  bigint,
    MEM_CODE_USED                  bigint,
    MEM_CODE_COMMITTED             bigint,
    MEM_CODE_MAX                   bigint,
    MEM_CODE_USEDPERCENT           double,
    MEM_HEAP_INIT                  bigint,
    MEM_HEAP_USED                  bigint,
    MEM_HEAP_COMMITTED             bigint,
    MEM_HEAP_MAX                   bigint,
    MEM_HEAP_USEDPERCENT           double,
    MEM_NOHEAP_INIT                bigint,
    MEM_NOHEAP_USED                bigint,
    MEM_NOHEAP_COMMITTED           bigint,
    MEM_NOHEAP_MAX                 bigint,
    MEM_NOHEAP_USEDPERCENT         double
) tags(INSTANT_ID nchar(48));

/* TDengine建表redis监控信息的sql - PassRedisInfo.class */
DROP TABLE IF EXISTS "t_redis_info";
CREATE TABLE t_redis_info
(
    TS                        timestamp,
    INST_ID                   nchar(48),
    ROLE                      nchar(20),
    CONNECTED_CLIENTS         int,
    USED_MEMORY               bigint,
    MAXMEMORY                 bigint,
    INSTANTANEOUS_OPS_PER_SEC int,
    INSTANTANEOUS_INPUT_KBPS  int,
    INSTANTANEOUS_OUTPUT_KBPS int,
    SYNC_FULL                 int,
    EXPIRED_KEYS              bigint,
    EVICTED_KEYS              bigint,
    KEYSPACE_HITS             bigint,
    KEYSPACE_MISSES           bigint,
    USED_CPU_SYS              double,
    USED_CPU_USER             double
) tags(INSTANT_ID nchar(48));

/* TDengine建表rocketmq监控信息的sql - PassRocketMqInfo.class */
/*DROP TABLE IF EXISTS "t_rocketmq_info";
CREATE TABLE t_rocketmq_info
(
    TS            timestamp,
    INST_ID       nchar(48),
    TOPIC_NAME    nchar(48),
    CONSUME_GROUP nchar(48),
    DIFF_TOTAL    bigint,
    PRODUCE_TOTAL bigint,
    PRODUCE_TPS   double,
    CONSUME_TOTAL bigint,
    CONSUME_TPS   double
) tags(INSTANT_ID nchar(48),TOPIC nchar(48),CGROUP nchar(48));*/

/* TDengine建表主机监控信息的sql - PassHostInfo.class */
/*DROP TABLE IF EXISTS "t_host_info";
CREATE TABLE t_host_info
(
    TS               timestamp,
    INST_ID          nchar(48),
    MEMORY_TOTAL     bigint,
    MEMORY_USED      bigint,
    USED_CPU_USER    double,
    USED_CPU_SYS     double,
    CPU_IDLE         double,
    TOTAL_DISK       bigint,
    UNUSED_DISK      bigint,
    USER_USED_DISK   bigint,
    INPUT_BANDWIDTH  double,
    OUTPUT_BANDWIDTH double
) tags(INSTANT_ID nchar(48));*/

/* 主机CPU */
DROP TABLE IF EXISTS "t_cpu_info";
CREATE TABLE t_cpu_info
(
    TS               timestamp,
    SERV_IP          nchar(48),
    CPU_USER         float,
    CPU_SYSTEM       float,
    CPU_WAIT         float,
    CPU_INTERRUPT    float,
    CPU_IDLE         float,
    CPU_NICE         float,
    CPU_SOFTIRQ      float,
    CPU_STEAL        float
) tags(SERVER_IP nchar(48));

/* 主机Memory */
DROP TABLE IF EXISTS "t_memory_info";
CREATE TABLE t_memory_info
(
    TS               timestamp,
    SERV_IP          nchar(48),
    MEM_USED         float,
    MEM_FREE         float,
    MEM_BUFFERED     float,
    MEM_CACHED       float,
    MEM_SLAB_UNRECL  float,
    MEM_SLAB_RECL    float
) tags(SERVER_IP nchar(48));

/* 主机nic */
DROP TABLE IF EXISTS "t_nic_info";
CREATE TABLE t_nic_info
(
    TS               timestamp,
    SERV_IP          nchar(48),
    NIC_NAME         nchar(24),
    PACKETS_TX       bigint,
    PACKETS_RX       bigint,
    OCTETS_TX        bigint,
    OCTETS_RX        bigint,
    ERRORS_TX        bigint,
    ERRORS_RX        bigint,
    DROPPED_TX       bigint,
    DROPPED_RX       bigint
) tags(SERVER_IP nchar(48), NIC nchar(24));

/* 主机disk */
DROP TABLE IF EXISTS "t_disk_info";
CREATE TABLE t_disk_info
(
    TS               timestamp,
    SERV_IP          nchar(48),
    DISK_NAME        nchar(24),
    DISK_OPS_R       bigint,
    DISK_OPS_W       bigint,
    DISK_OCTETS_R    bigint,
    DISK_OCTETS_W    bigint,
    DISK_TIME_R      bigint,
    DISK_TIME_W      bigint,
    DISK_IO_TIME_R   bigint,
    DISK_IO_TIME_W   bigint,
    DISK_MERGED_R    bigint,
    DISK_MERGED_W    bigint
) tags(SERVER_IP nchar(48), DISK_ nchar(24));

