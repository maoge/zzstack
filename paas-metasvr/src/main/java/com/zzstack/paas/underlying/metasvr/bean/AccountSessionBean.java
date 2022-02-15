package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class AccountSessionBean {
    
    private String accName;
    private String magicKey;
    private long sessionTimeOut;
    
    public AccountSessionBean(String accName, String magicKey) {
        super();
        this.accName = accName;
        this.magicKey = magicKey;
        this.sessionTimeOut = System.currentTimeMillis() + CONSTS.SESSION_TTL;
    }
    
    public AccountSessionBean(String accName, String magicKey, long sessionTimeOut) {
        super();
        this.accName = accName;
        this.magicKey = magicKey;
        this.sessionTimeOut = sessionTimeOut;
    }

    public String getAccName() {
        return accName;
    }

    public void setAccName(String accName) {
        this.accName = accName;
    }

    public String getMagicKey() {
        return magicKey;
    }

    public void setMagicKey(String magicKey) {
        this.magicKey = magicKey;
    }

    public long getSessionTimeOut() {
        return sessionTimeOut;
    }

    public void setSessionTimeOut(long sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }
    
    public boolean isSessionValid() {
        return sessionTimeOut > System.currentTimeMillis();
    }
    
    public static AccountSessionBean fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        
        String accName = json.getString(FixHeader.HEADER_ACC_NAME);
        String magicKey = json.getString(FixHeader.HEADER_MAGIC_KEY);
        long sessionTimeOut = json.getLong(FixHeader.HEADER_SESSION_TIMEOUT);

        return new AccountSessionBean(accName, magicKey, sessionTimeOut);
    }
    
    public String toJson() {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_ACC_NAME, accName);
        json.put(FixHeader.HEADER_MAGIC_KEY, magicKey);
        json.put(FixHeader.HEADER_SESSION_TIMEOUT, sessionTimeOut);
        
        return json.toString();
    }

}
