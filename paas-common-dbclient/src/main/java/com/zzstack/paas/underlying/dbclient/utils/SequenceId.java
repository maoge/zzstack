package com.zzstack.paas.underlying.dbclient.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.DBConsts;
import com.zzstack.paas.underlying.dbclient.LongMargin;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.constants.DBConstants;
import com.zzstack.paas.underlying.dbclient.exception.SeqException;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class SequenceId {
	
	private static Logger logger = LoggerFactory.getLogger(SequenceId.class);
    
    private Map<String, LongMargin> seqMap;
    
    private static volatile SequenceId theInstance;
    private static ReentrantLock intanceLock = null;
    
    private static final int DEFAULT_SEQ_STEP  = 1000;
    
    static {
        intanceLock = new ReentrantLock();
    }
    
    private SequenceId() {
        seqMap = new ConcurrentHashMap<String, LongMargin>();
    }
    
    public static SequenceId get() {
        if (theInstance != null)
            return theInstance;
        
        intanceLock.lock();
        try {
            if (theInstance == null) {
                theInstance = new SequenceId();
            }
        } finally {
            intanceLock.unlock();
        }
        
        return theInstance;
    }
    
    public long getNextId(String seqName) throws SeqException {
        LongMargin margin = seqMap.get(seqName);
        if (margin == null) {
            margin = getNextSeqMargin(seqName, DEFAULT_SEQ_STEP);
            if (margin != null) {
                seqMap.put(seqName, margin);
            } else {
                String info = String.format("%s %s", seqName, DBConsts.ERR_SEQ_NOT_EXISTS);
                throw new SeqException(info);
            }
        }
        
        long id = margin.getNextId();
        if (id == -1) {
            // id取完了，拉取下一步长
            margin = getNextSeqMargin(seqName, DEFAULT_SEQ_STEP);
            id = margin.getNextId();
        }
        
        return id;
    }
    
    public long getNextId(String dbName, String seqName) throws SeqException {
        LongMargin margin = seqMap.get(seqName);
        if (margin == null) {
            margin = getNextSeqMargin(dbName, seqName, DEFAULT_SEQ_STEP);
            if (margin != null) {
                seqMap.put(seqName, margin);
            } else {
                String info = String.format("%s %s", seqName, DBConsts.ERR_SEQ_NOT_EXISTS);
                throw new SeqException(info);
            }
        }
        
        long id = margin.getNextId();
        if (id == -1) {
            // id取完了，拉取下一步长
            margin = getNextSeqMargin(dbName, seqName, DEFAULT_SEQ_STEP);
            id = margin.getNextId();
        }
        
        return id;
    }
    
    public static LongMargin getNextSeqMargin(String seqName, int step) {
        LongMargin ret = null;
        
        try {
            CRUD c = new CRUD();
            
            SqlBean sqlBean1 = new SqlBean(DBConstants.SQL_NEXT_SEQ_LOCK);
            sqlBean1.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean1);
            
            String sql2 = String.format(DBConstants.SQL_NEXT_SEQ_UPDATE, step);
            SqlBean sqlBean2 = new SqlBean(sql2);
            sqlBean2.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean2);
    
            ResultBean result = new ResultBean();
            ret = c.getNextSeqMargin(step, result);
            if (result.getRetCode() == CONSTS.REVOKE_NOK)
                return null;
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return ret;
    }

    public static LongMargin getNextSeqMargin(String dbName, String seqName, int step) {
        LongMargin ret = null;
        
        try {
            CRUD c = new CRUD(dbName);
            
            SqlBean sqlBean1 = new SqlBean(DBConstants.SQL_NEXT_SEQ_LOCK);
            sqlBean1.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean1);
            
            String sql2 = String.format(DBConstants.SQL_NEXT_SEQ_UPDATE, step);
            SqlBean sqlBean2 = new SqlBean(sql2);
            sqlBean2.addParams(new Object[] { seqName });
            c.putSqlBean(sqlBean2);
    
            ResultBean result = new ResultBean();
            ret = c.getNextSeqMargin(step, result);
            if (result.getRetCode() == CONSTS.REVOKE_NOK)
                return null;
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
        return ret;
    }

}
