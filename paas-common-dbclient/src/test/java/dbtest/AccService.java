package dbtest;

import dbtest.dao.bean.Acc;
import dbtest.service.exception.ServiceException;

public interface AccService {
    
    void insertAcc(Acc acc) throws ServiceException;

}
