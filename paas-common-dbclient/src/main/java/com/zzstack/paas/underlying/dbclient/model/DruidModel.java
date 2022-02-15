package com.zzstack.paas.underlying.dbclient.model;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidConnectionHolder;
import com.alibaba.druid.pool.DruidDataSource;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.dbclient.exception.DBException.DBERRINFO;
import com.zzstack.paas.underlying.utils.config.DBConfig.Jdbc;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class DruidModel extends DataSourceModel {
    
    private static final Logger logger = LoggerFactory.getLogger(DruidModel.class);
    
    private DruidDataSource dataSource = null;
    
    private static final int QUERY_TIMEOUT = 30;

    public DruidModel(String url, String username, String passwd, String dbType, Jdbc jdbc) {
        super(url, username, passwd, dbType, jdbc);
        
        if (dbType.equals(CONSTS.DB_TYPE_VOLTDB)) {
            // voltdb 不支持 getHoldability()
            DruidConnectionHolder.holdabilityUnsupported = true;
        }
        
        initDataSource();
    }
    
    @Override
    public boolean initDataSourceConf() {
        return true;
    }

    @Override
    public boolean initDataSource() {
        try {
            dataSource = new DruidDataSource();
            dataSource.setDriverClassName(driver);
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);

            // dataSource.setBreakAfterAcquireFailure(true);

            dataSource.setInitialSize(initialSize);
            dataSource.setMinIdle(minIdle);
            dataSource.setMaxActive(maxActive);

            dataSource.setMaxWait(maxWait);
            dataSource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);

            dataSource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
            dataSource.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);
            dataSource.setKeepAlive(keepAlive);

            dataSource.setValidationQuery(validationQuery);

            dataSource.setTestWhileIdle(testWhileIdle);
            dataSource.setTestOnBorrow(testOnBorrow);
            dataSource.setTestOnReturn(testOnReturn);

            dataSource.setPoolPreparedStatements(poolPreparedStatements);
            dataSource.setMaxOpenPreparedStatements(maxOpenPreparedStatements);
            dataSource.setTimeBetweenLogStatsMillis(timeBetweenLogStatsMillis);
            dataSource.setAsyncInit(asyncInit);

            dataSource.setLogAbandoned(logAbandoned);

            dataSource.setPhyMaxUseCount(phyMaxUseCount);
            dataSource.setFilters(filters);
            
            dataSource.setQueryTimeout(QUERY_TIMEOUT);
            dataSource.setTransactionQueryTimeout(QUERY_TIMEOUT);

            isOpen = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return isOpen;
    }

    @Override
    public Connection getConnection() throws DBException {
        Connection conn = null;
        try {
            if (dataSource != null) {
                conn = dataSource.getConnection();
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new DBException(e.getMessage(), e, DBERRINFO.e2);
        }

        return conn;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() throws DBException {
        if (isOpen && dataSource != null) {
            dataSource.close();
            isOpen = false;
        }
        
    }

}
