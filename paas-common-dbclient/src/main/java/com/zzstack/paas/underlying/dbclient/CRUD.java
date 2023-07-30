package com.zzstack.paas.underlying.dbclient;

import java.math.BigInteger;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.dbclient.exception.DBException.DBERRINFO;
import com.zzstack.paas.underlying.dbclient.pool.DataSourcePool;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

public class CRUD {

    private static final Logger logger = LoggerFactory.getLogger("CRUD");

    private String dbName = null;
    private int loadBalanceType = 0;  // 0: ActiveStandby; 1: LoadBalanced
    private Connection conn = null;
    private ConcurrentLinkedQueue<SqlBean> queue;
    private DataSourcePool pool = null;

    public CRUD() {

    }

    public CRUD(String dbName) {
        this.dbName = dbName;
    }
    
    public CRUD(String dbName, int loadBalaneType) {
        this.dbName = dbName;
        this.loadBalanceType = loadBalaneType;
    } 

    public void close() {
        if (null != conn)
            pool.recycle(conn);
    }

    public void putSqlBean(SqlBean bean) {
        if (queue == null)
            queue = new ConcurrentLinkedQueue<SqlBean>();

        queue.add(bean);
    }

    @SuppressWarnings("unchecked")
    public void putSql(String sql, Object params) {
        if (params == null) {
            SqlBean bean = new SqlBean(sql);
            putSqlBean(bean);
        } else {
            if (params instanceof List) {
                SqlBean bean = new SqlBean(sql, (List<Object>) params);
                putSqlBean(bean);
            } else if (params instanceof Object[]) {
                SqlBean bean = new SqlBean(sql, Arrays.asList((Object[]) params));
                putSqlBean(bean);
            }
        }
    }

    private void getConn() throws DBException {
        if (pool == null) {
            if (loadBalanceType == 0) {
                pool = dbName == null ? ActiveStandbyDBSrcPool.get().getDBSrcPool().getDataSourcePool()
                        : ActiveStandbyDBSrcPool.get(dbName).getDBSrcPool().getDataSourcePool();
            } else {
                pool = dbName == null ? LoadBalancedDBSrcPool.get().getDBSrcPool().getDataSourcePool()
                        : LoadBalancedDBSrcPool.get(dbName).getDBSrcPool().getDataSourcePool();
            }
        }

        if (pool == null) {
            logger.error("连接池异常");
            throw new DBException("连接池异常", new Throwable(), DBERRINFO.e1);
        }
        conn = pool.getConnection();
        if (conn == null) {
            logger.error("获取连接异常", new DBException("获取连接异常", new Throwable(), DBERRINFO.e1));

            if (dbName == null)
                ActiveStandbyDBSrcPool.get().getDBSrcPool().removeBrokenPool(pool.getName());
            else
                ActiveStandbyDBSrcPool.get(dbName).getDBSrcPool().removeBrokenPool(pool.getName());

            throw new DBException("获取连接异常", new Throwable(), DBERRINFO.e2);
        }
    }

    public boolean commit(boolean recycle) {
        boolean res = false;
        if (conn != null) {
            try {
                conn.commit();
                res = true;
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (recycle) {
                    if (null != conn)
                        pool.recycle(conn);
                }
            }
        }
        return res;
    }
    
    public boolean executeUpdate() {
        return executeUpdate(true, false);
    }
    
    public boolean executeDDL() {
        return executeUpdate(true, false);
    }
    
    public boolean executeDDL(boolean recycle, boolean autoCommit) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        boolean res = false;
        try {
            if (!autoCommit) {
                conn.setAutoCommit(false);
            }
            
            while (true) {
                SqlBean sb = queue.poll();
                if (sb == null)
                    break;

                String sql = sb.getSql();
                res = EXECUTE(conn, sql);
            }
            
            if (!autoCommit) {
                conn.commit();
            }

        } catch (Exception e) {
            String dbInfo = pool.getDBInfo();
            logger.error("db:{}, message:{}", dbInfo, e.getMessage(), e);
        } finally {
            if (null != conn) {
                pool.recycle(conn);
                conn = null;
            }
        }
        return res;
    }
    
    public boolean executeUpdate(boolean recycle, boolean autoCommit) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        boolean res = false;
        try {
            if (!autoCommit) {
                conn.setAutoCommit(false);
            }
            
            while (true) {
                SqlBean sb = queue.poll();
                if (sb == null)
                    break;

                String sql = sb.getSql();
                List<Object> objs = sb.getParams();
                res = UPDATE(conn, sql, objs != null ? objs.toArray() : null) >= 1;
            }

            if (!autoCommit) {
                conn.commit();
            }

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            
            String dbInfo = pool.getDBInfo();
            logger.error("db:{}, message:{}", dbInfo, e.getMessage(), e);
        } finally {
            if (recycle) {
                if (null != conn) {
                    pool.recycle(conn);
                    conn = null;
                }
            }
        }
        return res;
    }
    
    public boolean executeProc() {
        return executeProc(true);
    }
    
    public boolean executeProc(boolean recycle) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }
        
        boolean res = true;
        try {
            while (true) {
                SqlBean sb = queue.poll();
                if (sb == null)
                    break;

                String sql = sb.getSql();
                List<Object> params = sb.getParams();
                
                CallableStatement callStmt = conn.prepareCall(sql);
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");
                
                if (params != null) {
                    for (int i = 0; i < params.size(); i++) {
                        callStmt.setObject(i + 1, params.get(i));
                    }
                }
                
                res = callStmt.execute();
            }
            
        } catch (Exception e) {
            res = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            
            String dbInfo = pool.getDBInfo();
            logger.error("db:{}, message:{}", dbInfo, e.getMessage(), e);
        } finally {
            if (recycle) {
                if (null != conn) {
                    pool.recycle(conn);
                    conn = null;
                }
            }
        }
        return res;
    }

    public boolean batchUpdate() {
        return batchUpdate(true, false);
    }
    
    // voltdb 不支持conn.setAutoCommit(false) 和 conn.commit();
    public boolean batchUpdate(boolean autoCommit) {
        return batchUpdate(true, autoCommit);
    }

    private boolean batchUpdate(boolean recycle, boolean autoCommit) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        boolean res = true;
        // 用来支持一批传不同的sql及绑定变量
        Map<String, PreparedStatement> psMap = new HashMap<String, PreparedStatement>();
        try {
            if (!autoCommit)
                conn.setAutoCommit(false);

            while (true) {
                SqlBean sb = queue.poll();
                if (sb == null)
                    break;

                List<Object> params = sb.getParams();
                
                PreparedStatement ps = psMap.get(sb.getSql());
                if (ps == null) {
                    String sql = sb.getSql();
                    ps = conn.prepareStatement(sql);
                    psMap.put(sql, ps);
                }

                if (ps != null && params != null) {
                    for (int i = 0; i < params.size(); i++) {
                        Object obj = params.get(i);
                        if (obj instanceof java.util.Date) {
                            java.util.Date dt = (java.util.Date) obj;
                            ps.setObject(i + 1, new java.sql.Timestamp(dt.getTime()));
                        } else {
                            ps.setObject(i + 1, obj);
                        }
                    }
                    ps.addBatch();
                }
            }
            
            Set<Entry<String, PreparedStatement>> entrySet = psMap.entrySet();
            for (Entry<String, PreparedStatement> entry : entrySet) {
                PreparedStatement ps = entry.getValue();
                ps.executeBatch();
            }

            if (!autoCommit)
                conn.commit();

        } catch (Exception e) {
            res = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                
            }
            
            String dbInfo = pool.getDBInfo();
            logger.error("db:{}, message:{}", dbInfo, e.getMessage(), e);
            
        } finally {
            if (psMap != null && !psMap.isEmpty()) {
                try {
                    Set<Entry<String, PreparedStatement>> entrySet = psMap.entrySet();
                    for (Entry<String, PreparedStatement> entry : entrySet) {
                        PreparedStatement ps = entry.getValue();
                        ps.close();
                    }
                } catch (SQLException e) {
                    ;
                } finally {
                    psMap.clear();
                    psMap = null;
                }
            }

            if (recycle) {
                if (null != conn) {
                    pool.recycle(conn);
                    conn = null;
                }
            }
        }
        return res;
    }

    public boolean executeUpdate(ResultBean result) {
        return executeUpdate(true, false, result);
    }

    public boolean executeUpdate(boolean recycle, boolean autoCommit, ResultBean result) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }

        boolean res = true;
        try {
            if (!autoCommit)
                conn.setAutoCommit(false);
            
            while (true) {
                SqlBean sb = queue.poll();
                if (sb == null)
                    break;
                String sql = sb.getSql();

                List<Object> objs = sb.getParams();
                UPDATE(conn, sql, objs != null ? objs.toArray() : null);
            }
            
            if (!autoCommit)
                conn.commit();
            
        } catch (Exception e) {
            res = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                
            }
            
            String dbInfo = pool.getDBInfo();
            logger.error("db:{}, message:{}", dbInfo, e.getMessage(), e);

            result.setRetCode(DBConsts.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        } finally {
            if (recycle) {
                if (null != conn) {
                    pool.recycle(conn);
                    conn = null;
                }
            }
        }
        return res;
    }

    public LongMargin getNextSeqMargin(int step, ResultBean result) {
        return getNextSeqMargin(step, true, result);
    }

    private LongMargin getNextSeqMargin(int step, boolean recycle, ResultBean result) {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        LongMargin res = null;
        try {
            if (queue.size() != 2) {
                result.setRetCode(DBConsts.REVOKE_NOK);
                result.setRetInfo(DBConsts.ERR_FETCH_SEQ_SQL_NOT_MATCH);
                return null;
            }
            
            conn.setAutoCommit(false);

            // first select for update.
            SqlBean sb1 = queue.poll();
            String sql1 = sb1.getSql();
            List<Object> objs1 = sb1.getParams();
            Map<String, Object> selectMap = queryForMap(conn, sql1, objs1 != null ? objs1.toArray() : null);
            Object curr_value = selectMap.get(DBConsts.HEADER_CURR_VALUE);
            if (curr_value == null) {
                conn.commit();
                result.setRetCode(DBConsts.REVOKE_NOK);
                result.setRetInfo(DBConsts.ERR_SEQ_NOT_EXISTS);
                return null;
            }

            // second update current_value to new value.
            SqlBean sb2 = queue.poll();
            String sql2 = sb2.getSql();
            List<Object> objs2 = sb2.getParams();
            UPDATE(conn, sql2, objs2 != null ? objs2.toArray() : null);

            long start;
            if (curr_value instanceof BigInteger)
                start = ((BigInteger) curr_value).longValue();
            else
                start = ((Long) curr_value).longValue();
            
            long end = start + step - 1;
            res = new LongMargin(start, end);
            
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);

            result.setRetCode(DBConsts.REVOKE_NOK);
            result.setRetInfo(e.getMessage());
        } finally {
            if (recycle) {
                if (null != conn) {
                    pool.recycle(conn);
                    conn = null;
                }
            }
        }

        return res;
    }

    public List<HashMap<String, Object>> queryForList() throws DBException {
        if (conn == null) {
            getConn();
        }

        List<HashMap<String, Object>> res = new LinkedList<HashMap<String, Object>>();
        SqlBean sb = null;
        try {
            sb = queue.poll();
            if (sb != null) {
                String sql = sb.getSql();

                List<Object> objs = sb.getParams();
                res = queryForList(conn, sql, objs);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new DBException(e.getMessage(), e, DBERRINFO.e3);
        } finally {
            if (null != conn)
                pool.recycle(conn);
        }
        return res;
    }

    public int queryForCount() throws DBException {
        if (conn == null) {
            getConn();
        }

        int res = 0;
        SqlBean sb = null;
        try {
            sb = queue.poll();
            if (sb != null) {
                String sql = sb.getSql();

                List<Object> objs = sb.getParams();
                res = queryForCount(conn, sql, objs != null ? objs.toArray() : null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new DBException(e.getMessage(), e, DBERRINFO.e3);
        } finally {
            if (null != conn)
                pool.recycle(conn);
        }
        return res;
    }

    public long nextSequence() throws DBException {
        if (conn == null) {
            getConn();
        }

        long res = 0;
        SqlBean sb = null;
        try {
            sb = queue.poll();
            if (sb != null) {
                String sql = sb.getSql();

                List<Object> objs = sb.getParams();
                res = nextSequence(conn, sql, objs != null ? objs.toArray() : null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new DBException(e.getMessage(), e, DBERRINFO.e3);
        } finally {
            if (null != conn)
                pool.recycle(conn);
        }
        return res;
    }

    /**
     *
     * @param conn
     * @param sql
     * @param params
     * @return
     * @throws CRUDException
     */
    private List<HashMap<String, Object>> queryForList(Connection conn, String sql, List<Object> params)
            throws DBException {
        List<HashMap<String, Object>> resultList = new LinkedList<HashMap<String, Object>>();
        if (conn != null) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                ps = conn.prepareStatement(sql);
                if (params != null && !params.isEmpty()) {
                    int size = params.size();
                    for (int i = 0; i < size; i++) {
                        Object obj = params.get(i);
                        ps.setObject(i + 1, obj);

                        if (logger.isDebugEnabled())
                            logger.debug("executeSql-params:{}", obj);
                    }
                }
                rs = ps.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int colnum = metaData.getColumnCount();
                while (rs.next()) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    for (int i = 1; i <= colnum; i++) {
                        String columnName = metaData.getColumnLabel(i).toUpperCase();
                        map.put(columnName, rs.getObject(i));
                    }
                    resultList.add(map);
                }

                if (rs != null)
                    rs.close();

                if (ps != null)
                    ps.close();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new DBException(e.getMessage(), e, DBERRINFO.e3);
            }
        }
        return resultList;
    }

    public int queryForCount(Connection conn, String sql, Object[] params) throws DBException {
        int count = 0;
        if (conn != null) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                ps = conn.prepareStatement(sql);
                if (params != null && params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);

                        if (logger.isDebugEnabled())
                            logger.debug("executeSql-params:" + params[i]);
                    }
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    count = rs.getInt(1);
                }

                if (rs != null)
                    rs.close();

                if (ps != null)
                    ps.close();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new DBException(e.getMessage(), e, DBERRINFO.e3);
            }
        }
        return count;
    }

    public long nextSequence(Connection conn, String sql, Object[] params) throws DBException {
        long currId = 0;
        if (conn != null) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                ps = conn.prepareStatement(sql);
                if (params != null && params.length > 0) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);

                        if (logger.isDebugEnabled())
                            logger.debug("executeSql-params:" + params[i]);
                    }
                }
                rs = ps.executeQuery();
                while (rs.next()) {
                    currId = rs.getLong(1);
                }

                if (rs != null)
                    rs.close();

                if (ps != null)
                    ps.close();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new DBException(e.getMessage(), e, DBERRINFO.e3);
            }
        }
        return currId;
    }

    @SuppressWarnings("unused")
    private void mapping(HashMap<String, Object> resultMap, int column, ResultSet rs, ResultSetMetaData metaData)
            throws SQLException {
        String columnName = metaData.getColumnLabel(column).toUpperCase();
        int columnType = metaData.getColumnType(column);

        switch (columnType) {
        case Types.BIT:
            resultMap.put(columnName, rs.getByte(column));
            break;
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
            resultMap.put(columnName, rs.getInt(column));
            break;
        case Types.DATE:
            resultMap.put(columnName, rs.getDate(column));
            break;
        case Types.TIME:
            resultMap.put(columnName, rs.getTime(column));
            break;
        case Types.DECIMAL:
        case Types.BIGINT:
        case Types.TIMESTAMP:
            resultMap.put(columnName, rs.getLong(column));
            break;
        case Types.FLOAT:
            resultMap.put(columnName, rs.getFloat(column));
            break;
        case Types.REAL:
        case Types.DOUBLE:
        case Types.NUMERIC:
            resultMap.put(columnName, rs.getDouble(column));
            break;
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
            resultMap.put(columnName, rs.getString(column));
            break;
        case Types.BOOLEAN:
            resultMap.put(columnName, rs.getBoolean(column));
            break;
        case Types.NCHAR:
        case Types.NVARCHAR:
        case Types.LONGNVARCHAR:
            resultMap.put(columnName, rs.getNString(column));
            break;
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
            resultMap.put(columnName, rs.getBytes(column));
            break;
        case Types.BLOB:
        case Types.CLOB:
        case Types.NCLOB:
        case Types.NULL:
        case Types.OTHER:
        case Types.JAVA_OBJECT:
        case Types.DISTINCT:
        case Types.STRUCT:
        case Types.ARRAY:
        case Types.REF:
        case Types.DATALINK:
        case Types.SQLXML:
        case Types.REF_CURSOR:
        case Types.TIME_WITH_TIMEZONE:
        case Types.TIMESTAMP_WITH_TIMEZONE:
            logger.error("not surpported type ......");
            break;
        default:
            logger.error("data type undefined ......");
            break;
        }
    }

    public Map<String, Object> queryForMap() throws DBException {
        if (conn == null) {
            try {
                getConn();
            } catch (DBException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        Map<String, Object> res = null;
        SqlBean sb = null;
        try {
            sb = queue.poll();
            if (sb != null) {
                String sql = sb.getSql();

                List<Object> objs = sb.getParams();
                res = queryForMap(conn, sql, objs != null ? objs.toArray() : null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new DBException("sql:" + sb.getSql(), e, DBERRINFO.e3);
        } finally {
            if (null != conn)
                pool.recycle(conn);
        }
        return res;
    }

    private Map<String, Object> queryForMap(Connection conn, String sql, Object[] params) throws DBException {
        Map<String, Object> map = null;
        if (conn != null) {
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                ps = conn.prepareStatement(sql);
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);

                        if (logger.isDebugEnabled())
                            logger.debug("executeSql-params:" + params[i]);
                    }
                }
                rs = ps.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int colnum = metaData.getColumnCount();
                map = new HashMap<String, Object>();
                while (rs.next()) {
                    for (int i = 1; i <= colnum; i++) {
                        String columnName = metaData.getColumnLabel(i).toUpperCase();
                        map.put(columnName, rs.getObject(i));
                    }
                    break;
                }

                if (rs != null)
                    rs.close();

                if (ps != null)
                    ps.close();

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new DBException("sql:" + sql, e, DBERRINFO.DEFAULT);
            }
        }
        return map;
    }
    
    private boolean isArray(Object obj) {
        if (obj == null) {
            return false;
        }

        return obj.getClass().isArray();
    }
    
    private Array createSqlArray(Connection conn, Object[] objArr) throws SQLException {
        if (objArr == null) {
            return null;
        }
        
        String typeName = objArr[0].getClass().getSimpleName();
        return conn.createArrayOf(typeName, objArr);
    }

    /**
     * 数据库的更新操作
     *
     * sql更新语句参数使用"?",类型必须和数据库字段类型匹配
     *
     * @param params
     * @return 出现异常时返回-1
     * @throws CRUDException
     */
    public int UPDATE(Connection conn, String sql, Object[] params) throws DBException {
        int res = -1;

        if (conn != null) {
            PreparedStatement ps = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                ps = conn.prepareStatement(sql);
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        if (isArray(params[i])) {
                            Object[] objArr = (Object[]) params[i];
                            Array sqlArr = createSqlArray(conn, objArr);
                            ps.setArray(i + 1, sqlArr);
                        } else {
                            ps.setObject(i + 1, params[i]);
                        }
                        

                        if (logger.isDebugEnabled()) {
                            logger.debug("executeSql-params:" + params[i]);
                        }
                    }
                }

                res = ps.executeUpdate();

            } catch (SQLException e) {
                StringBuilder sb = new StringBuilder();
                if (params != null && params.length > 0) {
                    for (int i = 0; i < params.length; ++i) {
                        if (i > 0) { sb.append(","); }
                        sb.append(params[i]);
                    }
                }
                
                String errInfo = String.format("error:%s, sql:%s, params:[%s]", e.getMessage(), sql, sb.toString());
                logger.error("{}", errInfo, e);
                throw new DBException("sql:" + sql, e, DBERRINFO.e4);
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException e) {

                    }
                }
            }
        }

        return res;
    }
    
    public boolean EXECUTE(Connection conn, String sql) throws DBException {
        boolean res = false;

        if (conn != null) {
            Statement stmt = null;
            try {
                if (logger.isDebugEnabled())
                    logger.debug("executeSql:[" + sql + "]");

                stmt = conn.createStatement();
                res = stmt.execute(sql);

            } catch (Exception e) {
                String errInfo = String.format("error:%s, sql:%s", e.getMessage(), sql);
                logger.error("{}", errInfo, e);
                throw new DBException("sql:" + sql, e, DBERRINFO.e7);
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {

                    }
                }
            }
        }

        return res;
    }

}
