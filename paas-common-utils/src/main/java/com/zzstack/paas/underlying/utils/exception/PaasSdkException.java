package com.zzstack.paas.underlying.utils.exception;

public class PaasSdkException extends Exception {
    
    private static final long serialVersionUID = 3500349670520063570L;

    public static enum SdkErrInfo {
        DEFAULT(80010000, "default"),
        // ---------------------db component------------------------
        e80010001(80010001, "db connection pool exception"),       // DB 连接池异常
        e80010002(80010002, "fetch db connection exception"),      // DB 获取连接异常
        e80010003(80010003, "db exec query error"),                // DB 查询出错
        e80010004(80010004, "db exec update ddl error"),           // DB 更新出错
        e80010005(80010005, "db set auto commit error"),           // DB setAutoCommit(false) error
        e80010006(80010006, "decrpt db passwd error"),             // DB 解密出错
        // --------------------------------------------------------
        
        // ---------------------redis component--------------------
        e80020001(80020001, "cache connection pool exception"),    // CACHE 连接池异常
        e80020002(80020002, "topo missing HA_CONTAINER"),          // topo missing HA_CONTAINER
        e80020003(80020003, "topo missing REDIS_NODE"),            // topo missing REDIS_NODE
        // ---------------------mq component-----------------------
        e80030001(80030001, "cache connection pool exception"),    // 连接池异常
        // --------------------------------------------------------
        
        // ---------------------orcl db component------------------
        e80040001(80040001, "paas sdk init service exception"),    // paas sdk init service fail
        e80040002(80040002, "parse service topo exception"),       // parse service topo fail
        e80040003(80040003, "oracle dg missing"),                  // missing oracle dg
        e80040004(80040004, "paas sdk load instance exception"),   // paas sdk load instance fail
        // --------------------------------------------------------
        
        // ---------------------tidb component---------------------
        e80041001(80041001, "topo missing TIDB_SERVER"),           // topo json missing TIDB_SERVER
        // --------------------------------------------------------

        // ---------------------voltdb component-------------------
        e80042001(80042001, "topo missing VOLTDB_SERVER"),         // topo json missing VOLTDB_SERVER
        e80042002(80042002, "voltdb client create exception"),     // voltdb client create with exception
        // --------------------------------------------------------

        // ---------------------tdengine component-----------------
        e80043001(80043001, "topo missing TD_DNODE"),              // topo json missing TD_DNODE
        // --------------------------------------------------------

        // ---------------------clickhouse component---------------
        e80044001(80044001, "topo missing CLICKHOUSE_REPLICAS"),   // topo json missing CLICKHOUSE_REPLICAS
        // --------------------------------------------------------
        
        // ---------------------rocketmq component-----------------
        e80050001(80050001, "topo missing ROCKETMQ_NAMESRV node"), // ROCKETMQ_NAMESRV missing in topo
        e80050002(80050002, "rocketmq producer start fail"),
        // --------------------------------------------------------

        // ---------------------pulsar component-------------------
        e80051001(80051001, "topo missing PULSAR_BROKER node"),    // PULSAR_BROKER missing in topo
        e80051002(80051002, "pulsar client create exception"),
        // --------------------------------------------------------
        
        // --------------------------PAAS SDK----------------------
        e80060001(80060001, "paas sdk missing bootstrap params");  // paas sdk missing init params
        // --------------------------------------------------------
        

        private int code;
        private String errInfo;
        
        private SdkErrInfo(int code, String errInfo) {
            this.code = code;
            this.errInfo = errInfo;
        }
        
        public int getCode() {
            // 得到枚举值代表的字符串。
            return code;
        }
        
        public String getErrInfo() {
            return errInfo;
        }
    }
    
    private SdkErrInfo sdkErrInfo;
    
    public PaasSdkException(SdkErrInfo sdkErrInfo) {
        super(sdkErrInfo.getErrInfo());
        this.sdkErrInfo = sdkErrInfo;
    }
    
    public SdkErrInfo getSdkErrInfo() {
        return sdkErrInfo;
    }

}
