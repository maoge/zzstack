package com.zzstack.paas.underlying.dbclient;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceFactory;

public class DBSourcePoolFactory implements DataSourceFactory {

    @Override
    public void setProperties(Properties props) {

    }

    @Override
    public DataSource getDataSource() {
        ActiveStandbyDBSrcPool activeStandByDBSrcPool = ActiveStandbyDBSrcPool.get();
        return new DelegatedDataSource(activeStandByDBSrcPool);
    }

}
