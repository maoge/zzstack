package dbtest;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;

public class DuckDBTest {

    private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA IF NOT EXISTS s1";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS tt(ts LONG, id INTEGER, val DOUBLE)";
    private static final String INS_SQL = "insert into tt(ts, id, val) values(?, ?, ?)";

    public static void main(String[] args) {
        init();
        
        createSchema();
        
        createTable();

        execInsert();
    }
    
    private static void init() {
        ActiveStandbyDBSrcPool.get("duckdb");
    }
    
    private static void createSchema() {
        SqlBean sqlBean = new SqlBean(CREATE_SCHEMA_SQL);
        
        CRUD c = new CRUD("duckdb");
        c.putSqlBean(sqlBean);
        if (c.executeDDL()) {
            System.out.println("create schema OK");
        } else {
            System.out.println("create schema NOK");
        }
    }
    
    private static void createTable() {
        SqlBean sqlBean = new SqlBean(CREATE_TABLE_SQL);
        
        CRUD c = new CRUD("duckdb");
        c.putSqlBean(sqlBean);
        if (c.executeDDL()) {
            System.out.println("create table OK");
        } else {
            System.out.println("create table NOK");
        }
    }

    private static void execInsert() {
        SqlBean sqlBean = new SqlBean(INS_SQL);

        long ts = System.currentTimeMillis();
        sqlBean.addParams(new Object[] { ts, 2, 3.0D });
        CRUD c = new CRUD("duckdb");
        c.putSqlBean(sqlBean);
        
        if (c.executeUpdate(true, true)) {
            System.out.println("insert OK");
        } else {
            System.out.println("insert NOK");
        }
    }
    
}
