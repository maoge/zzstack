package com.zzstack.paas.underlying.dbclient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

public class ActiveStandbyDBSrcPool {

    private static Logger logger = LoggerFactory.getLogger(ActiveStandbyDBSrcPool.class);
    
    private static Map<String, ActiveStandbyDBSrcPool> instMap = null;

    private EnumDBInUse dbInUseType = null;
    
    private DBSrcPool masterDBSrcPool = null;
    private DBSrcPool backupDBSrcPool = null;

    private DBConfig dbConf;
    private String name;
    private boolean isInited = false;

    private static String MYBATIS_CONF = null;
    private static String DEFAULT_DB_NAME = "metadb";
    
    private static Lock lock = null;
    
    static {
        lock = new ReentrantLock();
        instMap = new ConcurrentHashMap<String, ActiveStandbyDBSrcPool>();
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
    
    public static ActiveStandbyDBSrcPool get() {
        ActiveStandbyDBSrcPool dbPool = instMap.get(DEFAULT_DB_NAME);
        if (dbPool != null)
            return dbPool;
        
        lock.lock();
        try {
            dbPool = new ActiveStandbyDBSrcPool(DEFAULT_DB_NAME);
            dbPool.init(false, null);
            instMap.put(DEFAULT_DB_NAME, dbPool);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static ActiveStandbyDBSrcPool get(String dbName) {
        ActiveStandbyDBSrcPool dbPool = instMap.get(dbName);
        if (dbPool != null)
            return dbPool;
        
        lock.lock(); 
        try {
            dbPool = new ActiveStandbyDBSrcPool(dbName);
            dbPool.init(false, null);
            instMap.put(dbName, dbPool);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static ActiveStandbyDBSrcPool get(String dgName, String servInstID, String topoStr, Map<String, Object> params) throws PaasSdkException {
        ActiveStandbyDBSrcPool dbPool = instMap.get(dgName);
        if (dbPool != null)
            return dbPool;
        
        lock.lock();
        try {
            Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
            DBConfig dbConfParseFromTopo = (DBConfig) o;
            
            dbPool = new ActiveStandbyDBSrcPool(dgName);
            dbPool.init(true, dbConfParseFromTopo);
        
            instMap.put(dgName, dbPool);
        } finally {
            lock.unlock();
        }
        
        return dbPool;
    }
    
    public static void destroy() {
        lock.lock();
        try {
            Set<Entry<String, ActiveStandbyDBSrcPool>> entrySet = instMap.entrySet();
            for (Entry<String, ActiveStandbyDBSrcPool> entry : entrySet) {
                ActiveStandbyDBSrcPool dbPool = entry.getValue();
                
                if (dbPool != null) {
                    dbPool.close();
                }
            }
            instMap.clear();
            
        } finally {
            lock.unlock();
        }
    }

    private ActiveStandbyDBSrcPool(String name) {
        this.name = name;
    }
    
    private boolean init(boolean initFromPaas, DBConfig dbConfParseFromTopo) {
        boolean ret = true;

        lock.lock();
        try {
            if (!isInited) {
                if (initFromPaas) {
                    dbConf = dbConfParseFromTopo;
                } else {
                    String yamlFile = String.format("conf/%s.yaml", name);
                    YamlParser parser = new YamlParser(yamlFile);
                    dbConf = (DBConfig) parser.parseObject(DBConfig.class);
                }
                
                if (dbConf.jdbc.activeDBType != null)
                    dbInUseType = dbConf.jdbc.activeDBType.equals(CONSTS.ACTIVE_DB_TYPE_MASTER) ? EnumDBInUse.master : EnumDBInUse.backup;
                else
                    dbInUseType = EnumDBInUse.master;
                
                // TDEngine need load jni lib
                if (dbConf.getDBType().equals(CONSTS.DB_TYPE_TDENGINE)) {
                    TDJniTools.loadTDJniFile(this.getClass());
                }
                
                if (dbInUseType == EnumDBInUse.master) {
                    masterDBSrcPool = new DBSrcPool(dbConf, dbInUseType);
                } else {
                    backupDBSrcPool = new DBSrcPool(dbConf, dbInUseType);
                }
                
                isInited = true;
            }
            
        } catch (DBException e) {
            ret = false;
            logger.error("ActiveStandbyDBSrcPool init error:{}", e.getMessage(), e);
        } finally {
            lock.unlock();
        }

        return ret;
    }
    
    public boolean isInited() {
        return this.isInited;
    }

    public EnumDBInUse getDBInUseType() {
        return dbInUseType;
    }
    
    private void setDBInUseType(EnumDBInUse dbType) {
        this.dbInUseType = dbType;
    }

    public DBSrcPool getDBSrcPool() {
        lock.lock();
        try {
            if (dbInUseType == EnumDBInUse.master) {
                return masterDBSrcPool;
            } else {
                return backupDBSrcPool;
            }
            
        } finally {
            lock.unlock();
        }
    }

    public DataSource getDataSource() {
        lock.lock();
        try {
            if (dbInUseType == EnumDBInUse.master) {
                return masterDBSrcPool.getDataSource();
            } else {
                return backupDBSrcPool.getDataSource();
            }
            
        } finally {
            lock.unlock();
        }
    }

    private void close() {
        if (dbInUseType == EnumDBInUse.master) {
            masterDBSrcPool.close();
            masterDBSrcPool = null;
        } else {
            backupDBSrcPool.close();
            backupDBSrcPool = null;
        }
    }
    
    private DBConfig getDBConf() {
        return this.dbConf;
    }
    
    private void setMasterDBSrcPool(DBSrcPool dbSrcPool) {
        this.masterDBSrcPool = dbSrcPool;
    }

    private void setBackUpDBSrcPool(DBSrcPool dbSrcPool) {
        this.backupDBSrcPool = dbSrcPool;
    }
    
    public static boolean switchDBType(String name, EnumDBInUse dbType) {
        boolean ret = true;
        
        ActiveStandbyDBSrcPool dbPool = instMap.get(name);
        if (dbPool == null)
            return false;

        if (dbType == dbPool.getDBInUseType()) {
            logger.info("ActiveStandbyDBSrcPool dbInUseType:{} 与待切数据换源相同，无需切换", dbType.name());
            return true;
        }

        lock.lock();
        try {
            logger.info("ActiveStandbyDBSrcPool switchDBType dbInUseType:{}", dbType.name());
            
            // dbPool.close();

            DBSrcPool dbSrcPool = new DBSrcPool(dbPool.getDBConf(), dbType);
            if (dbType == EnumDBInUse.master) {
                dbPool.setMasterDBSrcPool(dbSrcPool);
            } else {
                dbPool.setBackUpDBSrcPool(dbSrcPool);
            }
            dbPool.setDBInUseType(dbType);
            
            // 防止切换过程报错, 旧的连接延迟关闭释放
            TimeUnit.MILLISECONDS.sleep(1000);
            
            // close old DBSrcPool
            if (dbType == EnumDBInUse.master) {
                if (dbPool.backupDBSrcPool != null) {
                    dbPool.backupDBSrcPool.close();
                    dbPool.backupDBSrcPool = null;
                }
            } else {
                if (dbPool.masterDBSrcPool != null) {
                    dbPool.masterDBSrcPool.close();
                    dbPool.masterDBSrcPool = null;
                }
            }
            
        } catch (Exception e) {
            logger.error("ActiveStandbyDBSrcPool switchDBSource error:{}", e);
            ret = false;
        } finally {
            lock.unlock();
        }

        return ret;
    }
    
    public static EnumDBInUse getDBInUse(String dbName) {
        ActiveStandbyDBSrcPool dbPool = instMap.get(dbName);
        if (dbPool == null)
            return EnumDBInUse.none;
        
        return dbPool.getDBInUseType();
    }

}
