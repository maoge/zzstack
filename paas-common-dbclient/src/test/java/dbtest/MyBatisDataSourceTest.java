package dbtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;

import dbtest.dao.bean.Acc;
import dbtest.service.exception.ServiceException;

public class MyBatisDataSourceTest {
    
    private static Logger logger = LoggerFactory.getLogger(MyBatisDataSourceTest.class);
    
    public static void main(String[] args) {
        ActiveStandbyDBSrcPool.setMyBatisConf("dbtest/dao/config/mybatis-rac-conf.xml");
        ActiveStandbyDBSrcPool.setDefaultDBName("metadb");
        AccService accService = AccInjector.getAccServiceInstance();
        
        try {
            Acc acc = new Acc(100, "abc");
            accService.insertAcc(acc);
        } catch (ServiceException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
