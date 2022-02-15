package com.zzstack.paas.underlying.dbclient.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.dbclient.model.DataSourceModel;
import com.zzstack.paas.underlying.dbclient.model.DruidModel;
import com.zzstack.paas.underlying.dbclient.model.HikariModel;
import com.zzstack.paas.underlying.utils.config.DBConfig;

public class InstancePoolImpl implements DataSourcePool {

    private static Logger logger = LoggerFactory.getLogger(InstancePoolImpl.class);

    private DataSourceModel model;
    private String id;
    private String dbType;
    
    private static final String DB_SOURCE_MODEL_DRUID = "Druid";
    private static final String DB_SOURCE_MODEL_HIKARI = "Hikari";

    public InstancePoolImpl(String url, String username, String passwd, String dbType, DBConfig.Jdbc jdbc) {
        this.id = url;
        this.dbType = dbType;
        
        if (jdbc.dbSourceModel.equals(DB_SOURCE_MODEL_DRUID))
            this.model = new DruidModel(url, username, passwd, dbType, jdbc);
        else if (jdbc.dbSourceModel.equals(DB_SOURCE_MODEL_HIKARI))
            this.model = new HikariModel(url, username, passwd, dbType, jdbc);
        else
            logger.error("no compatial dbSourceModel, check jdbc yaml ......");
    }

    @Override
    public void close() throws Exception {
        model.close();
    }

    @Override
    public Connection getConnection() throws DBException {
        return model.getConnection();
    }

    @Override
    public DataSource getDataSource() {
        if (model == null)
            return null;

        return model.getDataSource();
    }
    
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    @Override
    public void recycle(Connection conn) {
        try {
            if (conn != null)
                conn.close(); // call close on connection to return to the pool.
        } catch (SQLException e) {
            // logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public boolean check() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        boolean ret = false;

        try {
            conn = getConnection();
            if (conn == null)
                return false;

            ps = conn.prepareStatement(model.getTestQuery());
            rs = ps.executeQuery();

            if (rs != null)
                rs.close();

            if (ps != null)
                ps.close();

            ret = true;
            
            model.setDBOpenState(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            recycle(conn);
        }

        return ret;
    }

    @Override
    public String getDBInfo() {
        return "{id=" + id + ", dbType=" + dbType + "}";
    }
}
