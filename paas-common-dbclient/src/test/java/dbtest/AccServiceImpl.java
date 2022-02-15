package dbtest;

import dbtest.dao.bean.Acc;
import dbtest.dao.sql.DataAccessDao;
import dbtest.service.exception.ServiceException;

import com.google.inject.Inject;

public class AccServiceImpl implements AccService {
    
    @Inject
    private DataAccessDao dataAccessDao;

    @Override
    public void insertAcc(Acc acc) throws ServiceException {
        dataAccessDao.insertAcc(acc);
    }

}
