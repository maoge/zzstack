package com.zzstack.paas.underlying.metasvr.dataservice.dao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.zzstack.paas.underlying.dbclient.DBConsts;
import com.zzstack.paas.underlying.dbclient.LongMargin;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.exception.SeqException;

public class SequenceId {
    
    private Map<String, LongMargin> seqMap;
    
    private static volatile SequenceId theInstance;
    private static ReentrantLock intanceLock = null;
    
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
            margin = MetaDataDao.getNextSeqMargin(seqName, FixDefs.DEFAULT_SEQ_STEP);
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
            margin = MetaDataDao.getNextSeqMargin(seqName, FixDefs.DEFAULT_SEQ_STEP);
            id = margin.getNextId();
        }
        
        return id;
    }

}
