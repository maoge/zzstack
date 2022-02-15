package com.zzstack.paas.underlying.utils.config;

import java.util.List;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class DBConfig implements IPaasConfig {

    public Jdbc jdbc;

    public static class Jdbc {

        public boolean decrypt = false;
        public String dbType;
        public String dbSourceModel;
        public String activeDBType;
        public DBSource masterDBSources;
        public DBSource backupDBSources;
        public int initialSize = 5;
        public int minIdle = 10;
        public int maxActive = 25;
        public int maxWait = 60000;
        public int timeBetweenEvictionRunsMillis = 10000;
        public int minEvictableIdleTimeMillis = 300000;
        public int maxEvictableIdleTimeMillis = 600000;
        public int timeBetweenLogStatsMillis = 300000;
        public boolean keepAlive = false;
        public String validationQuery = "SELECT 1 from dual";
        public boolean testWhileIdle = true;
        public boolean testOnBorrow = false;
        public boolean testOnReturn = false;
        public boolean poolPreparedStatements = true;
        public int maxOpenPreparedStatements = 100;
        public boolean removeAbandoned = true;
        public int removeAbandonedTimeout = 1800;
        public boolean logAbandoned = true;
        public int phyMaxUseCount = -1;
        public String filters = "";

    }

    public static class DBSource {

        public String id;
        public List<DBNode> nodes;

    }

    public static class DBNode {

        public String url;
        public String username;
        public String password;

    }
    
    @Override
    public String getServClazzType() {
        return CONSTS.SERV_CLAZZ_DB;
    }

    @Override
    public String getDBType() {
        return jdbc.dbType;
    }

}
