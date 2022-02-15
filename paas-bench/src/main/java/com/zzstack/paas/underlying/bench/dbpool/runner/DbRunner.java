package com.zzstack.paas.underlying.bench.dbpool.runner;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.constants.BenchConstants;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.worker.TaskRunner;

public class DbRunner extends TaskRunner {
    
    private static Logger logger = LoggerFactory.getLogger(DbRunner.class);
    
    private static final String SQL_INS_ACC = "insert into t_acc(acc_id, acc_name) values(?, ?)";

    public DbRunner(AtomicLong normalCnt, AtomicLong errorCnt) {
        super(normalCnt, errorCnt);
    }

    @Override
    public void run() {
        int cnt = 0;
        
        while (bRunning) {
            
            try {
                SqlBean sqlBean = new SqlBean(SQL_INS_ACC);
                sqlBean.putParam(cnt++);
                sqlBean.putParam("aaa");
                
                CRUD c = new CRUD(BenchConstants.DB_CONF_NAME);
                c.putSqlBean(sqlBean);
                
                if (c.executeUpdate()) {
                    normalCnt.incrementAndGet();
                }

            } catch (Exception e) {
                errorCnt.incrementAndGet();
                logger.error(e.getMessage(), e);
            }
        }
    }

}
