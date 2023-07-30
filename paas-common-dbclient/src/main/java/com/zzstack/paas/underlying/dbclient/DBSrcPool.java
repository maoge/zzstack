package com.zzstack.paas.underlying.dbclient;

import java.util.ArrayList;
import java.util.List;
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
import com.zzstack.paas.underlying.dbclient.exception.DBException.DBERRINFO;
import com.zzstack.paas.underlying.dbclient.pool.DataSourcePool;
import com.zzstack.paas.underlying.dbclient.pool.InstancePoolImpl;
import com.zzstack.paas.underlying.dbclient.utils.DBInUse.EnumDBInUse;
import com.zzstack.paas.underlying.utils.CryptTools;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.config.DBConfig.DBNode;
import com.zzstack.paas.underlying.utils.config.DBConfig.DBSource;

public class DBSrcPool {

    private static Logger logger = LoggerFactory.getLogger(DBSrcPool.class);

    private Lock lock = null;

    private ConcurrentHashMap<String, DataSourcePool> validDBMap = null;
    private ConcurrentHashMap<String, DataSourcePool> invalidDBMap = null;
    private List<String> validIdList;
    private List<String> invalidIdList;
    private long index = 0L;

    private Thread checkThread = null;
    private DBPoolRecoveryChecker checker = null;
    private volatile boolean isCheckerRunning = false;

    private EnumDBInUse dbInUseType;
    private String id;
    private String dbType;

    private static long DBPOOL_RECONNECT_INTERVAL = 1000L;

    public DBSrcPool(DBConfig dbConf, EnumDBInUse dbInUseType) throws DBException {
        this.dbInUseType = dbInUseType;
        this.lock = new ReentrantLock();

        validDBMap = new ConcurrentHashMap<String, DataSourcePool>();
        invalidDBMap = new ConcurrentHashMap<String, DataSourcePool>();
        validIdList = new ArrayList<String>();
        invalidIdList = new ArrayList<String>();

        DBConfig.Jdbc jdbc = dbConf.jdbc;
        dbType = jdbc.dbType;
        DBSource dbSource = null;
        switch (dbInUseType) {
        case master:
            dbSource = jdbc.masterDBSources;
            break;
        case backup:
            dbSource = jdbc.backupDBSources;
            break;
        default:
            break;
        }

        if (dbSource != null && dbSource.nodes != null && !dbSource.nodes.isEmpty()) {
            id = dbSource.id;
            for (DBNode node : dbSource.nodes) {
                String url = node.url;
                String username = node.username;
                String password = node.password;

                if (jdbc.decrypt) {
                    try {
                        url = CryptTools.decrypt(url);
                        username = CryptTools.decrypt(username);
                        password = CryptTools.decrypt(password);
                    } catch (Exception e) {
                        throw new DBException("解密出错", e, DBERRINFO.e6);
                    }
                }

                addPool(url, username, password, dbType, jdbc);
            }

            startChecker();
        }

    }

    public EnumDBInUse getDBInUseType() {
        return dbInUseType;
    }

    public String getID() {
        return id;
    }

    public void close() {
        stopChecker();

        lock.lock();
        try {
            if (validDBMap != null) {
                Set<Entry<String, DataSourcePool>> entrySet = validDBMap.entrySet();
                for (Entry<String, DataSourcePool> entry : entrySet) {
                    DataSourcePool connPool = entry.getValue();
                    if (connPool == null) {
                        continue;
                    }

                    try {
                        connPool.close();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

            if (invalidDBMap != null) {
                Set<Entry<String, DataSourcePool>> entrySet = invalidDBMap.entrySet();
                for (Entry<String, DataSourcePool> entry : entrySet) {
                    DataSourcePool connPool = entry.getValue();
                    if (connPool == null) {
                        continue;
                    }
                    try {
                        connPool.close();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }

            validIdList.clear();
            validDBMap.clear();

            invalidIdList.clear();
            invalidDBMap.clear();

        } finally {
            lock.unlock();
        }
    }

    public DataSourcePool getDataSourcePool() throws DBException {
        DataSourcePool connPool = null;
        lock.lock();
        try {
            if (validIdList.size() == 0) {
                throw new DBException("db source is empty", new Throwable(), DBERRINFO.e1);
            }

            int seed = (int) (index++ % validIdList.size());
            String id = validIdList.get(seed);
            connPool = validDBMap.get(id);

        } finally {
            lock.unlock();
        }

        return connPool;
    }

    public DataSource getDataSource() {
        DataSourcePool connPool = null;
        try {
            connPool = getDataSourcePool();
        } catch (DBException e) {
            logger.error("DbSrcPool getDataSource err:{}", e);
        }
        if (connPool == null)
            return null;

        return connPool.getDataSource();
    }

    public void removeBrokenPool(String id) {
        lock.lock();
        try {
            if (validDBMap.containsKey(id)) {
                validIdList.remove(id);
                DataSourcePool connPool = validDBMap.remove(id);
                if (connPool != null) {
                    if (invalidDBMap.containsKey(id)) {
                        return;
                    }

                    if (!invalidIdList.contains(id)) {
                        logger.info("db pool:{} broken ......", id);
                        invalidDBMap.put(id, connPool);
                        invalidIdList.add(id);
                    }
                }
            }

        } finally {
            lock.unlock();
        }
    }

    public void mergeRecoveredPool(String id) {
        lock.lock();
        try {
            if (invalidDBMap.containsKey(id)) {
                invalidIdList.remove(id);
                DataSourcePool connPool = invalidDBMap.remove(id);
                if (connPool != null) {
                    if (validDBMap.containsKey(id))
                        return;

                    if (!validIdList.contains(id)) {
                        logger.info("db pool:{} recovered ......", id);
                        validDBMap.put(id, connPool);
                        validIdList.add(id);
                    }

                    if (invalidDBMap.isEmpty()) {
                        stopChecker();
                    }
                }
            }

        } finally {
            lock.unlock();
        }
    }

    public void addPool(String url, String username, String passwd, String dbType, DBConfig.Jdbc jdbc) {
        InstancePoolImpl instancePool = new InstancePoolImpl(url, username, passwd, dbType, jdbc);
        logger.info("Create pool to: {}", url);

        lock.lock();
        try {
            if (instancePool.check()) {
                this.validDBMap.put(url, instancePool);
                this.validIdList.add(url);
            } else {
                this.invalidDBMap.put(url, instancePool);
                this.invalidIdList.add(url);
            }
        } catch (Exception e) {
            logger.error("db:{} init fail:{}", url, e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    // 以后增加配置中心推送remove事件保留
    public void removePool(String id) throws Exception {
        logger.info("Remove pool to " + id);
        DataSourcePool pool = null;
        lock.lock();
        try {
            if (this.validIdList.contains(id)) {
                pool = this.validDBMap.get(id);
                this.validDBMap.remove(id);
                this.validIdList.remove(id);
            } else if (this.invalidIdList.contains(id)) {
                pool = this.invalidDBMap.get(id);
                this.invalidDBMap.remove(id);
                this.invalidIdList.remove(id);
            }

        } finally {
            lock.unlock();
        }

        if (pool != null) {
            pool.close();
        }
    }

    private void startChecker() {
        if (isCheckerRunning)
            return;

        isCheckerRunning = true;
        checker = new DBPoolRecoveryChecker(this);
        checkThread = new Thread(checker);
        checkThread.setName("DBSrcPool.RecoveryChecker");
        checkThread.setDaemon(true);
        checkThread.start();
    }

    private void stopChecker() {
        if (!isCheckerRunning)
            return;

        isCheckerRunning = false;
        if (checker != null) {
            checker.stopRunning();

            checker = null;
            checkThread = null;
        }
    }

    private void checkInvalid() {
        for (int i = 0; i < invalidIdList.size(); i++) {
            String id = invalidIdList.get(i);
            logger.info("DBSource Checking:{} ......", id);

            DataSourcePool connPool = invalidDBMap.get(id);
            if (connPool.check()) {
                mergeRecoveredPool(id);
            }
        }
    }
    
    public String getDBInfo() {
        StringBuilder sb = new StringBuilder();
        Set<Entry<String, DataSourcePool>> entrySet = validDBMap.entrySet();
        for (Entry<String, DataSourcePool> entry : entrySet) {
            InstancePoolImpl pool = (InstancePoolImpl) entry.getValue();
            
            if (sb.length() > 0)
                sb.append(",");
            
            sb.append(pool.toString());
        }
        return sb.toString();
    }

    private static class DBPoolRecoveryChecker implements Runnable {

        private volatile boolean running = false;
        private DBSrcPool dbSrcPool = null;

        public DBPoolRecoveryChecker(DBSrcPool dbSrcPool) {
            this.dbSrcPool = dbSrcPool;
        }

        @Override
        public void run() {
            running = true;

            while (running) {
                try {
                    if (dbSrcPool != null) {
                        dbSrcPool.checkInvalid();
                    }
                } catch (Exception e) {
                    logger.warn("DBSource check error...", e);
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(DBPOOL_RECONNECT_INTERVAL);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        public void stopRunning() {
            running = false;
        }

    }

}
