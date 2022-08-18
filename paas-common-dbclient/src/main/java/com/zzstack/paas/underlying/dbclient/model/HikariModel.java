package com.zzstack.paas.underlying.dbclient.model;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.dbclient.exception.DBException.DBERRINFO;
import com.zzstack.paas.underlying.utils.config.DBConfig.Jdbc;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariModel extends DataSourceModel {
    
    private static final Logger logger = LoggerFactory.getLogger(HikariModel.class);
    
    private HikariConfig hikariConf = null;
    private HikariDataSource dataSource = null;

    public HikariModel(String url, String username, String passwd, String dbType, Jdbc jdbc) {
        super(url, username, passwd, dbType, jdbc);
        
        initDataSourceConf();
        initDataSource();
    }
    
    public boolean initDataSourceConf() {
        hikariConf = new HikariConfig();
        hikariConf.setPoolName(url);
        hikariConf.setDriverClassName(driver);
        hikariConf.setJdbcUrl(url);
        hikariConf.setUsername(username);
        hikariConf.setPassword(password);
        hikariConf.setMaximumPoolSize(maxActive);
        hikariConf.setMinimumIdle(minIdle);
        
        hikariConf.setConnectionTimeout(5000);
        hikariConf.setIdleTimeout(minEvictableIdleTimeMillis);
        hikariConf.setMaxLifetime(maxEvictableIdleTimeMillis);
        hikariConf.setValidationTimeout(5000);
        hikariConf.setConnectionTestQuery(validationQuery);
        hikariConf.setAutoCommit(false);
        
        hikariConf.setLeakDetectionThreshold(300000);
        
        return true;
    }

    @Override
    public boolean initDataSource() {
        try {
            dataSource = new HikariDataSource(hikariConf);
            
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
            if (dataSource == null) {
                dataSource = new HikariDataSource(hikariConf);
            }
            
            conn = dataSource.getConnection();
        } catch (Exception e) {
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
