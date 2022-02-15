package dbtest.service;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dbtest.dao.bean.Acc;
import dbtest.dao.sql.DataAccessDao;
import dbtest.service.exception.ServiceException;

@Singleton
public class DefaultDataAccessServiceImpl implements BaseDataAccessService {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(DefaultDataAccessServiceImpl.class);

    @SuppressWarnings("unused")
    private static String YYYYMMDD = "yyyy-MM-dd";
    @SuppressWarnings("unused")
    private static final int LIMIT_STEP = 100;

    protected static final AtomicLong SUBMIT_ID = new AtomicLong(0);
    protected static final AtomicLong REPORT_ID = new AtomicLong(0);
    protected static final AtomicLong SUBMIT_ID_STEP = new AtomicLong(0);
    protected static final AtomicLong REPORT_ID_STEP = new AtomicLong(0);

    protected final DataAccessDao dataAccessDao;

    @Inject
    public DefaultDataAccessServiceImpl(DataAccessDao dataAccessDao) {
        this.dataAccessDao = dataAccessDao;
    }

    @Override
    public void insertAcc(Acc acc) throws ServiceException {
        dataAccessDao.insertAcc(acc);
    }

}
