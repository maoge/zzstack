package com.zzstack.paas.underlying.dbclient.model;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public abstract class DataSourceModel {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceModel.class);

    protected String driver = "";
    protected String url = "";
    protected String username = "";
    protected String password = "";

    protected int initialSize = 5;
    protected int minIdle = 10;
    protected int maxActive = 20;
    protected long maxWait = 60000L;
    protected long timeBetweenEvictionRunsMillis = 10000L;

    protected long minEvictableIdleTimeMillis = 300000L;
    protected long maxEvictableIdleTimeMillis = 600000L;
    protected boolean keepAlive = false;

    protected String validationQuery = "SELECT 1 from dual";

    protected boolean testWhileIdle = true;
    protected boolean testOnBorrow = false;
    protected boolean testOnReturn = false;

    protected boolean poolPreparedStatements = true;
    protected int maxOpenPreparedStatements = 100;
    protected long timeBetweenLogStatsMillis = 300000L;

    protected boolean asyncInit = true;
    protected boolean logAbandoned = true;

    protected int phyMaxUseCount = -1;

    protected String filters = "";

    protected boolean isOpen = false;

    private static Map<String, String> DB_DRIVER_MAP = null;

    static {
        DB_DRIVER_MAP = new HashMap<String, String>();
        
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_ORCL,       CONSTS.DB_DRIVER_ORCL);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_MYSQL,      CONSTS.DB_DRIVER_MYSQL);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_PG,         CONSTS.DB_DRIVER_PG);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_VOLTDB,     CONSTS.DB_DRIVER_VOLTDB);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_TDENGINE,   CONSTS.DB_DRIVER_TDENGINE);
        DB_DRIVER_MAP.put(CONSTS.DB_TYPE_CLICKHOUSE, CONSTS.DB_DRIVER_CLICKHOUSE);
    }

    public DataSourceModel(String url, String username, String passwd, String dbType, DBConfig.Jdbc jdbc) {

        this.driver = DB_DRIVER_MAP.get(dbType);
        if (this.driver == null || this.driver.isEmpty()) {
            logger.error("driver not found");
        }

        this.url = url;
        this.username = username;
        this.password = passwd;

        this.initialSize = jdbc.initialSize;
        this.minIdle = jdbc.minIdle;
        this.maxActive = jdbc.maxActive;

        this.maxWait = jdbc.maxWait;
        this.timeBetweenEvictionRunsMillis = jdbc.timeBetweenEvictionRunsMillis;

        this.minEvictableIdleTimeMillis = jdbc.minEvictableIdleTimeMillis;
        this.maxEvictableIdleTimeMillis = jdbc.maxEvictableIdleTimeMillis;
        this.keepAlive = jdbc.keepAlive;

        this.validationQuery = jdbc.validationQuery;

        this.testWhileIdle = jdbc.testWhileIdle;
        this.testOnBorrow = jdbc.testOnBorrow;
        this.testOnReturn = jdbc.testOnReturn;

        this.poolPreparedStatements = jdbc.poolPreparedStatements;
        this.maxOpenPreparedStatements = jdbc.maxOpenPreparedStatements;
        this.timeBetweenLogStatsMillis = jdbc.timeBetweenLogStatsMillis;

        this.logAbandoned = jdbc.logAbandoned;

        this.phyMaxUseCount = jdbc.phyMaxUseCount;
        this.filters = jdbc.filters;
    }

    public String getTestQuery() {
        return validationQuery;
    }
    
    public void setDBOpenState(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public abstract boolean initDataSourceConf();
    public abstract boolean initDataSource();

    public abstract Connection getConnection() throws DBException;

    public abstract DataSource getDataSource();

    public abstract void close() throws DBException;

}
