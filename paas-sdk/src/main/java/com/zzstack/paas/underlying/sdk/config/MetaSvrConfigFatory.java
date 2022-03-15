package com.zzstack.paas.underlying.sdk.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.paas.PaasHttpOperationUtils;

public class MetaSvrConfigFatory {
    
    private static Logger logger = LoggerFactory.getLogger(MetaSvrConfigFatory.class);
    
    private MetaSvrUrls metaSvrUrls = null;
    
    private String user = null;
    private String passwd = null;
    private String magicKey = null;
    private AtomicBoolean needAuth = new AtomicBoolean(true);
    
    private static ReentrantLock lock = null;
    private static MetaSvrConfigFatory theInstance = null;
    
    static {
        lock = new ReentrantLock();
    }
    
    private MetaSvrConfigFatory(String urls, String user, String passwd) {
        this.user = user;
        this.passwd = passwd;
        
        metaSvrUrls = new MetaSvrUrls();
        if (urls == null || urls.isEmpty()) {
            logger.error("MetaSvrUrls: {} is null or empty ......", urls);
            return;
        }
        
        String[] arr = urls.split(CONSTS.METASVR_ADDR_SPLIT);
        for (String addr : arr) {
            if (addr == null || addr.isEmpty())
                continue;
            
            String url = addr.trim();
            if (url.isEmpty())
                continue;
            
            metaSvrUrls.addToValidUrls(url);
        }
        
        if (metaSvrUrls.isValidUrlsEmpty()) {
            logger.error("metaSvrList empty ......");
        }
    }
    
    public static MetaSvrConfigFatory getInstance(String metasvrUrls, String user, String passwd) {
        if (theInstance != null)
            return theInstance;
        
        lock.lock();
        try {
            if (theInstance == null) {
                theInstance = new MetaSvrConfigFatory(metasvrUrls, user, passwd);
            }
        } finally {
            lock.unlock();
        }
        return theInstance;
    }
    
    public boolean loadServiceTopo(String servInstID, SVarObject result) {
        if (needAuth.get()) {
            if (!auth())
                return false;
        }
        
        String metaSvrUrl = metaSvrUrls.getRandomValidUrl();
        boolean res = false;
        try {
            res = PaasHttpOperationUtils.loadServiceTopo(metaSvrUrl, servInstID, magicKey, result);
        } catch (IOException e) {
            logger.error("loadServiceTopo {} caught: {}", metaSvrUrl, e.getMessage(), e);
            metaSvrUrls.moveToInvalidUrls(metaSvrUrl);
        }
        return res;
    }
    
    public boolean loadInstanceMeta(String instID, SVarObject result) {
        if (needAuth.get()) {
            if (!auth())
                return false;
        }
        
        String metaSvrUrl = metaSvrUrls.getRandomValidUrl();
        boolean res = false;
        try {
            res = PaasHttpOperationUtils.loadInstanceMeta(metaSvrUrl, instID, magicKey, result);
        } catch (IOException e) {
            logger.error("loadInstanceMeta {} caught: {}", metaSvrUrl, e.getMessage(), e);
            metaSvrUrls.moveToInvalidUrls(metaSvrUrl);
        }
        return res;
    }
    
    public boolean auth() {
        if (!needAuth.get()) {
            return true;
        }
        
        String metaSvrUrl = metaSvrUrls.getRandomValidUrl();
        SVarObject result = new SVarObject();
        
        boolean res = false;
        try {
            res = PaasHttpOperationUtils.doMetaSvrAuth(metaSvrUrl, user, passwd, result);
            if (res) {
                JSONObject obj = JSONObject.parseObject(result.getVal());
                if (obj != null) {
                    int retCode = obj.getIntValue(FixHeader.HEADER_RET_CODE);
                    if (retCode == CONSTS.REVOKE_OK) {
                        magicKey = obj.getString(FixHeader.HEADER_RET_INFO);
                        needAuth.set(false);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("auth {} caught: {}", metaSvrUrl, e.getMessage(), e);
            metaSvrUrls.moveToInvalidUrls(metaSvrUrl);
        }
        
        return res;
    }
    
    public void postCollectData(String uri, Map<String, String> headers, String data) throws PaasSdkException {
        String metaSvrUrl = metaSvrUrls.getRandomValidUrl();
        String url = String.format("%s%s", metaSvrUrl, uri);
        
        try {
            PaasHttpOperationUtils.postCollectData(url, headers, data);
        } catch (IOException e) {
            logger.error("auth {} caught: {}", metaSvrUrl, e.getMessage(), e);
            metaSvrUrls.moveToInvalidUrls(metaSvrUrl);
        }
    }

}
