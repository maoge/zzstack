package dbtest;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;

public class ClickHouseTest {

    private static final String INSERT_SQL = "insert into test(user_id, create_date, update_count) values(?, ?, ?)";
    // private static final String UPDATE_SQL = "ALTER TABLE test UPDATE update_count = ? WHERE user_id = ?";
    private static final String UPSERT_SQL = "insert into test(user_id, create_date, update_count) select user_id, create_date, ? from test where user_id = ?";
    private static final String SELECT_SQL = "select user_id, create_date, update_count from test";
    
    /*
    CREATE TABLE smsdb.test\
    (\
        user_id             UInt64,\
        create_date         Date DEFAULT toDate(now()),\
        update_count        UInt8 DEFAULT 0\
    ) ENGINE = ReplacingMergeTree()\
    PARTITION BY toYYYYMMDD(create_date)\
    ORDER BY (user_id)\
    primary key (user_id);
    */

    public static void main(String[] args) {
        ActiveStandbyDBSrcPool.get("clickhouse");

        testInsert();
        testUpdate();
        testSelect();
    }
    
    private static void testInsert() {
        CRUD c = new CRUD("clickhouse");
        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        
        SqlBean sqlBean1 = new SqlBean(INSERT_SQL);
        sqlBean1.addParams(new Object[] { 1000, ts, 0 });
        c.putSqlBean(sqlBean1);
        
        SqlBean sqlBean2 = new SqlBean(INSERT_SQL);
        sqlBean2.addParams(new Object[] { 1001, ts, 0 });
        c.putSqlBean(sqlBean2);
        
        c.batchUpdate();
    }
    
    private static void testUpdate() {
        CRUD c = new CRUD("clickhouse");
        
        SqlBean sqlBean1 = new SqlBean(UPSERT_SQL);
        sqlBean1.addParams(new Object[] { 3, 1000 });
        c.putSqlBean(sqlBean1);
        c.executeUpdate();
        
        SqlBean sqlBean2 = new SqlBean(UPSERT_SQL);
        sqlBean2.addParams(new Object[] { 3, 1001 });
        c.putSqlBean(sqlBean2);
        c.executeUpdate();
        
        // c.batchUpdate();
    }
    
    private static void testSelect() {
        SqlBean sqlBean = new SqlBean(SELECT_SQL);

        CRUD c = new CRUD("clickhouse");
        c.putSqlBean(sqlBean);
        try {
            List<HashMap<String, Object>> result = c.queryForList();
            if (result != null && !result.isEmpty()) {
                for (HashMap<String, Object> map : result) {
                    Set<Entry<String, Object>> entrySet = map.entrySet();
                    for (Entry<String, Object> entry : entrySet) {
                        Object val = entry.getValue();
                        String type = val.getClass().getName();
                        String info = String.format("%s | %s | %s", entry.getKey(), type, val.toString());
                        System.out.println(info);
                    }
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
    }

}
