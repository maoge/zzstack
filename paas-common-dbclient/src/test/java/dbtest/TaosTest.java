package dbtest;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;

public class TaosTest {

    private static final String BENCH_SQL = "insert into t(ts, id, value) values(?, ?, ?)";

    public static void main(String[] args) {

        ActiveStandbyDBSrcPool.get("tdengine");

        SqlBean sqlBean = new SqlBean(BENCH_SQL);

        long ts = System.currentTimeMillis();
        sqlBean.addParams(new Object[] { ts, 2, 3 });
        CRUD c = new CRUD("tdengine");
        c.putSqlBean(sqlBean);
        c.executeUpdate();
    }

}
