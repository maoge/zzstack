package dbtest;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;

public class TiDBTest {

    /*
     * 
     *   use test;
     *   create table t(
     *       ts  BIGINT,
     *       id  INT,
     *       val DOUBLE);
     *   
     *   create user test identified by 'test';
     *   grant all on test.* to test;
     * 
     */
    
    private static final String INS_SQL = "insert into t(ts, id, val) values(?, ?, ?)";

    public static void main(String[] args) {

        ActiveStandbyDBSrcPool.get("tidb");

        SqlBean sqlBean = new SqlBean(INS_SQL);

        long ts = System.currentTimeMillis();
        sqlBean.addParams(new Object[] { ts, 2, 3.0D });
        CRUD c = new CRUD("tidb");
        c.putSqlBean(sqlBean);
        c.executeUpdate();
    }

}
