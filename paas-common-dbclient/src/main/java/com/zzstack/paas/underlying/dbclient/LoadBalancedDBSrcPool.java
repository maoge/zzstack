package com.zzstack.paas.underlying.dbclient;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.dbclient.utils.DBInUse.EnumDBInUse;
import com.zzstack.paas.underlying.utils.TDJniTools;
import com.zzstack.paas.underlying.utils.YamlParser;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.paas.PaasTopoParser;

public class LoadBalancedDBSrcPool {
    
    private static Logger logger = LoggerFactory.getLogger(LoadBalancedDBSrcPool.class);
    
    private static Map<String, LoadBalancedDBSrcPool> instMap = null;

    private DBSrcPool dbSrcPool = null;

    private DBConfig dbConf;
    private String name;
    private boolean isInited = false;

    private static String MYBATIS_CONF = null;
    private static String DEFAULT_DB_NAME = "metadb";
    
    private static Lock lock = null;
    
    static {
        lock = new ReentrantLock();
        instMap = new ConcurrentHashMap<String, LoadBalancedDBSrcPool>();
    }
    
    public static void setMyBatisConf(String conf) {
        MYBATIS_CONF = conf;
    }
    
    public static String getMyBatisConf() {
        return MYBATIS_CONF;
    }
    
    public static String getDefaultDBName() {
        return DEFAULT_DB_NAME;
    }

    public static void setDefaultDBName(String defaultDBName) {
        DEFAULT_DB_NAME = defaultDBName;
    }
    
    public static LoadBalancedDBSrcPool get() {
        LoadBalancedDBSrcPool dbPool = instMap.get(DEFAULT_DB_NAME);
        if (dbPool != null)
            return dbPool;
        
        try {
            lock.lock();
            dbPool = new LoadBalancedDBSrcPool(DEFAULT_DB_NAME);
            dbPool.init(false, null);
            instMap.put(DEFAULT_DB_NAME, dbPool);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static LoadBalancedDBSrcPool get(String dbName) {
        LoadBalancedDBSrcPool dbPool = instMap.get(dbName);
        if (dbPool != null)
            return dbPool;
        
        try {
            lock.lock();
            dbPool = new LoadBalancedDBSrcPool(dbName);
            dbPool.init(false, null);
            instMap.put(dbName, dbPool);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static LoadBalancedDBSrcPool get(String dbName, String servInstID, String topoStr, Map<String, Object> params) throws PaasSdkException {
        LoadBalancedDBSrcPool dbPool = instMap.get(dbName);
        if (dbPool != null)
            return dbPool;
        
        try {
            lock.lock();
            Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
            DBConfig dbConfParseFromTopo = (DBConfig) o;
            
            dbPool = new LoadBalancedDBSrcPool(dbName);
            dbPool.init(true, dbConfParseFromTopo);
        
            instMap.put(dbName, dbPool);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static void destroy() {
        try {
            lock.lock();
            
            Set<Entry<String, LoadBalancedDBSrcPool>> entrySet = instMap.entrySet();
            for (Entry<String, LoadBalancedDBSrcPool> entry : entrySet) {
                LoadBalancedDBSrcPool dbPool = entry.getValue();
                
                if (dbPool != null) {
                    dbPool.close();
                }
            }
            instMap.clear();
            
        } finally {
            lock.unlock();
        }
    }
    
    public static void destry(String dbName) {
        try {
            lock.lock();
            
            if (instMap.containsKey(dbName)) {
                LoadBalancedDBSrcPool dbPool = instMap.remove(dbName);
                dbPool.close();
            }
        } finally {
            lock.unlock();
        }
    }
    
    private LoadBalancedDBSrcPool(String name) {
        this.name = name;
    }
    
    private boolean init(boolean initFromPaas, DBConfig dbConfParseFromTopo) {
        boolean ret = true;

        try {
            lock.lock();
            
            if (!isInited) {
                if (initFromPaas) {
                    dbConf = dbConfParseFromTopo;
                } else {
                    String yamlFile = String.format("conf/%s.yaml", name);
                    YamlParser parser = new YamlParser(yamlFile);
                    dbConf = (DBConfig) parser.parseObject(DBConfig.class);
                }
                
                // TDEngine need load jni lib
                if (dbConf.getDBType().equals(CONSTS.DB_TYPE_TDENGINE)) {
                    TDJniTools.loadTDJniFile(this.getClass());
                }
                
                dbSrcPool = new DBSrcPool(dbConf, EnumDBInUse.master);
                
                isInited = true;
            }
            
        } catch (DBException e) {
            ret = false;
            logger.error("LoadBalancedDBSrcPool init error:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }

        return ret;
    }
    
    public boolean isInited() {
        return this.isInited;
    }
    
    public EnumDBInUse getDBInUseType() {
        return EnumDBInUse.master;
    }
    
    public DBSrcPool getDBSrcPool() {
        try {
            lock.lock();

            return dbSrcPool;
            
        } finally {
            lock.unlock();
        }
    }
    
    public DataSource getDataSource() {
        try {
            lock.lock();
            
            return dbSrcPool.getDataSource();
            
        } finally {
            lock.unlock();
        }
    }
    
    private void close() {
        if (dbSrcPool != null) {
            dbSrcPool.close();
            dbSrcPool = null;
        }
    }
    
    public static EnumDBInUse getDBInUse(String dbName) {
        LoadBalancedDBSrcPool dbPool = instMap.get(dbName);
        if (dbPool == null)
            return EnumDBInUse.none;
        
        return dbPool.getDBInUseType();
    }

}
