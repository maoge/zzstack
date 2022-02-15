package dbtest;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;

public class VoltDBTest {
    
    private static final String INSERT_SQL = "insert into zz_account(acc_id, acc_name, phone_num, mail, passwd, create_time) values(?, ?, ?, ?, ?, ?)";
    private static final String SELECT_SQL = "select acc_id, acc_name, phone_num, mail, passwd, create_time from zz_account";
    
    /*
    create table zz_account (
      acc_id varchar(48) not null primary key,
      acc_name varchar(32) not null,
      phone_num varchar(15) not null,
      mail varchar(48) not null,
      passwd varchar(72) not null,
      create_time BIGINT not null
    );
    
    PARTITION TABLE zz_account ON COLUMN acc_id;
    */

    public static void main(String[] args) {
        ActiveStandbyDBSrcPool.get("voltdb");

        testInsert();
        testSelect();
    }
    
    private static void testInsert() {
        CRUD c = new CRUD("voltdb");
        
        SqlBean sqlBean1 = new SqlBean(INSERT_SQL);
        sqlBean1.addParams(new Object[] { "1000", "zhangsan", "13788888888", "a@b", "123456", System.currentTimeMillis() });
        c.putSqlBean(sqlBean1);
        
        SqlBean sqlBean2 = new SqlBean(INSERT_SQL);
        sqlBean2.addParams(new Object[] { "1001", "lisi", "13988888888", "b@b", "123456", System.currentTimeMillis() });
        c.putSqlBean(sqlBean2);
        
        c.batchUpdate(true);
    }
    
    private static void testSelect() {
        SqlBean sqlBean = new SqlBean(SELECT_SQL);

        CRUD c = new CRUD("voltdb");
        c.putSqlBean(sqlBean);
        try {
            List<HashMap<String, Object>> result = c.queryForList();
            if (result != null && !result.isEmpty()) {
                for (HashMap<String, Object> map : result) {
                    Set<Entry<String, Object>> entrySet = map.entrySet();
                    StringBuilder line = new StringBuilder();
                    for (Entry<String, Object> entry : entrySet) {
                        Object val = entry.getValue();
                        String type = val.getClass().getName();
                        String info = String.format("%s  %s  %s |", entry.getKey(), type, val.toString());
                        
                        if (line.length() > 0)
                            line.append(", ");
                        
                        line.append(info);
                    }
                    System.out.println(line.toString());
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
    }

}
