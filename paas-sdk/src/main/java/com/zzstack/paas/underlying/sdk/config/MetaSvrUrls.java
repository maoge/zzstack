package com.zzstack.paas.underlying.sdk.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.utils.paas.PaasHttpOperationUtils;

public class MetaSvrUrls {
    
    private static Logger logger = LoggerFactory.getLogger(MetaSvrUrls.class);

    private List<String> validUrls = null;
    private List<String> invalidUrls = null;
    
    private ReentrantLock lock = null;
    
    private BrokenUrlChecker checker;
    private Thread checkThread;
    
    private AtomicLong metaSvrIdx = null;
    
    private static final long CHECK_INTERVAL = 1000L;
    
    public MetaSvrUrls() {
        super();
        
        lock = new ReentrantLock();
        validUrls = new ArrayList<String>();
        invalidUrls = new ArrayList<String>();
        
        metaSvrIdx = new AtomicLong(0);
    }
    
    public void release() {
        if (checker != null) {
            checker.stopRunning();
        }
    }
    
    public void addToValidUrls(String url) {
        try {
            lock.lock();
            validUrls.add(url);
        } finally {
            lock.unlock();
        }
    }
    
    public void moveToValidUrls(String url) {
        try {
            lock.lock();
            if (invalidUrls.remove(url)) {
                validUrls.add(url);
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void moveToInvalidUrls(String url) {
        try {
            lock.lock();
            if (validUrls.remove(url)) {
                invalidUrls.add(url);
                logger.info("{} is broken ......", url);
            }
        } finally {
            lock.unlock();
        }
        
        startChecker();
    }
    
    public List<String> getValidUrls() {
        return validUrls;
    }
    
    public List<String> getInvalidUrls() {
        return invalidUrls;
    }
    
    public boolean isValidUrlsEmpty() {
        boolean result = false;
        try {
            lock.lock();
            result = validUrls.isEmpty();
        } finally {
            lock.unlock();
        }
        
        return result;
    }
    
    public boolean isInvalidUrlsEmpty() {
        boolean result = false;
        try {
            lock.lock();
            result = invalidUrls.isEmpty();
        } finally {
            lock.unlock();
        }
        
        return result;
    }
    
    public String getRandomValidUrl() {
        String result = null;
        try {
            lock.lock();
            int size = validUrls.size();
            if (size > 1) {
                int idx = (int) (metaSvrIdx.incrementAndGet() % size);
                result = validUrls.get(idx);
            } else if (size == 1) {
                result = validUrls.get(0);
            }
            
        } finally {
            lock.unlock();
        }
        
        return result;
    }
    
    public void startChecker() {
        try {
            lock.lock();
            
            if (checker == null) {
                checker = new BrokenUrlChecker(this);
            }
            
            if (checkThread == null) {
                checkThread = new Thread(checker);
                checkThread.setDaemon(false);
                checkThread.setName("MetaSvrUrls.Checker");
                checkThread.start();
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    public void stopChecker() {
        try {
            if (checker != null) {
                checker.stopRunning();
                checker = null;
            }
            
            if (checkThread != null) {
                checkThread = null;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    private static class BrokenUrlChecker implements Runnable {
        
        private static Logger logger = LoggerFactory.getLogger(BrokenUrlChecker.class);

        private MetaSvrUrls metaSvrUrls;
        private volatile boolean bRunning = true;
        
        public BrokenUrlChecker(MetaSvrUrls metaSvrUrls) {
            this.metaSvrUrls = metaSvrUrls;
        }

        @Override
        public void run() {
            while (bRunning) {
                try {
                    if (metaSvrUrls.isInvalidUrlsEmpty()) {
                        bRunning = false;
                        metaSvrUrls.stopChecker();
                    }
                    
                    List<String> invalidUrls = metaSvrUrls.getInvalidUrls();
                    int size = invalidUrls.size();
                    for (int i = size - 1; i >= 0; --i) {
                        String url = invalidUrls.get(i);
                        if (PaasHttpOperationUtils.testUrl(url)) {
                            metaSvrUrls.moveToValidUrls(url);
                            logger.info("{} is recovered ......", url);
                        }
                    }
                    
                    TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        
        public void stopRunning() {
            bRunning = false;
        }

    }

}
