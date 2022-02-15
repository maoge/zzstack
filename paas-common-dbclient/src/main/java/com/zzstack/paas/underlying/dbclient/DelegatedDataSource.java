package com.zzstack.paas.underlying.dbclient;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class DelegatedDataSource implements DataSource {
    
    private ActiveStandbyDBSrcPool activeStandByDBSrcPool;
    
    public DelegatedDataSource(ActiveStandbyDBSrcPool activeStandByDBSrcPool) {
        this.activeStandByDBSrcPool = activeStandByDBSrcPool;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        dataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource dataSource = activeStandByDBSrcPool.getDataSource();
        return dataSource.getConnection(username, password);
    }

}
