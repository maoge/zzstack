package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class AccountBean extends BeanMapper {
    
    private String accID;
    private String accName;
    private String phoneNum;
    private String mail;
    private String passwd;
    private long createTime;
    
    public AccountBean() {
        super();
    }

    public AccountBean(String accID, String accName, String phoneNum, String mail, String passwd, long createTime) {
        super();
        this.accID = accID;
        this.accName = accName;
        this.phoneNum = phoneNum;
        this.mail = mail;
        this.passwd = passwd;
        this.createTime = createTime;
    }

    public String getAccID() {
        return accID;
    }

    public void setAccID(String accID) {
        this.accID = accID;
    }

    public String getAccName() {
        return accName;
    }

    public void setAccName(String accName) {
        this.accName = accName;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public static AccountBean convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty())
            return null;
        
        String accID    = getFixDataAsString(mapper, FixHeader.HEADER_ACC_ID);
        String accName  = getFixDataAsString(mapper, FixHeader.HEADER_ACC_NAME);
        String phoneNum = getFixDataAsString(mapper, FixHeader.HEADER_PHONE_NUM);
        String mail     = getFixDataAsString(mapper, FixHeader.HEADER_MAIL);
        String passwd   = getFixDataAsString(mapper, FixHeader.HEADER_PASSWD);
        long createTime = getFixDataAsLong(mapper, FixHeader.HEADER_CREATE_TIME);

        return new AccountBean(accID, accName, phoneNum, mail, passwd, createTime);
    }
    
    public String toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_ACC_ID,      accID);
        retval.put(FixHeader.HEADER_ACC_NAME,    accName);
        retval.put(FixHeader.HEADER_PHONE_NUM,   phoneNum);
        retval.put(FixHeader.HEADER_MAIL,        mail);
        retval.put(FixHeader.HEADER_PASSWD,      passwd);
        retval.put(FixHeader.HEADER_CREATE_TIME, createTime);

        return retval.toString();
    }
    
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_ACC_ID,      accID);
        json.put(FixHeader.HEADER_ACC_NAME,    accName);
        json.put(FixHeader.HEADER_PHONE_NUM,   phoneNum);
        json.put(FixHeader.HEADER_MAIL,        mail);
        json.put(FixHeader.HEADER_PASSWD,      passwd);
        json.put(FixHeader.HEADER_CREATE_TIME, createTime);

        return json;
    }
    
    public static AccountBean fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        
        String accID    = json.getString(FixHeader.HEADER_ACC_ID);
        String accName  = json.getString(FixHeader.HEADER_ACC_NAME);
        String phoneNum = json.getString(FixHeader.HEADER_PHONE_NUM);
        String mail     = json.getString(FixHeader.HEADER_MAIL);
        String passwd   = json.getString(FixHeader.HEADER_PASSWD);
        long createTime = json.getLong(FixHeader.HEADER_CREATE_TIME);

        return new AccountBean(accID, accName, phoneNum, mail, passwd, createTime);
    }

}
