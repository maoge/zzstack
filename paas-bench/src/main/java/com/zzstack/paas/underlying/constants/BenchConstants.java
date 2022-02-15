package com.zzstack.paas.underlying.constants;

public class BenchConstants {
    
    public static final int SERVER_PORT = 9090;
    public static final boolean USE_SSL = false;
    public static final int SERVER_EVENT_GRP_SIZE = 4;
    public static final int SERVER_WORKER_SIZE = 16;
    public static final long TASK_TIMEOUT = 15000;
    
    public static final String QUEUE_FMT = "Q_%02d";
    public static final String LOGGER_REDIS_PUSH = "RedisPush";
    public static final String LOGGER_REDIS_POP = "RedisPop";
    public static final String LOGGER_BENCH_STAT = "BenchStat";
    public static final String LOGGER_DB_BENCH = "DbBench";
    
    public static final String DB_CONF_NAME = "metadb";
    
    public static final String REDIS_CONF_FILE = "redis-cluster-queue";
    public static final String REDIS_INFO_STATS = "stats";
    public static final String REDIS_INFO_INSTANT_OPS = "instantaneous_ops_per_sec:";
    
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String REDIS_LINE_SEPARATOR = "\r\n";
    
    public static final String DB_MASTER = "master";
    public static final String DB_BACKUP = "backup";
    
    public static final String LOGGER_SPLIT = "--------------------------------------------------------------------------------------------------";
    

}
