package dbtest.service;

import dbtest.dao.bean.Acc;
import dbtest.service.exception.ServiceException;

public interface BaseDataAccessService {

    void insertAcc(Acc acc) throws ServiceException;

}
