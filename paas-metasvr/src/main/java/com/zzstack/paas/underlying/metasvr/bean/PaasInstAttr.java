package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasInstAttr extends BeanMapper {

    private String instId;
    private int attrId;
    private String attrName;
    private String attrValue;

    public PaasInstAttr() {
        super();
    }

    /**
     * @param instId
     * @param attrId
     * @param attrName
     * @param attrValue
     */
    public PaasInstAttr(String instId, int attrId, String attrName, String attrValue) {
        super();
        this.instId = instId;
        this.attrId = attrId;
        this.attrName = attrName;
        this.attrValue = attrValue;
    }

    public static PaasInstAttr convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty())
            return null;

        String instId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        int attrId = getFixDataAsInt(mapper, FixHeader.HEADER_ATTR_ID);
        String attrName = getFixDataAsString(mapper, FixHeader.HEADER_ATTR_NAME);
        String attrValue = getFixDataAsString(mapper, FixHeader.HEADER_ATTR_VALUE);

        return new PaasInstAttr(instId, attrId, attrName, attrValue);
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public int getAttrId() {
        return attrId;
    }

    public void setAttrId(int attrId) {
        this.attrId = attrId;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getAttrValue() {
        return attrValue;
    }

    public void setAttrValue(String attrValue) {
        this.attrValue = attrValue;
    }

    public String toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_INST_ID, instId);
        retval.put(FixHeader.HEADER_ATTR_ID, attrId);
        retval.put(FixHeader.HEADER_ATTR_NAME, attrName);
        retval.put(FixHeader.HEADER_ATTR_VALUE, attrValue);

        return retval.toString();
    }

    @Override
    public String toString() {
        return "PaasInstAttr [instId=" + instId + ", attrId=" + attrId + ", attrName=" + attrName + ", attrValue="
                + attrValue + "]";
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_INST_ID, instId);
        json.put(FixHeader.HEADER_ATTR_ID, attrId);
        json.put(FixHeader.HEADER_ATTR_NAME, attrName);
        json.put(FixHeader.HEADER_ATTR_VALUE, attrValue);

        return json;
    }

    public static PaasInstAttr fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        int attrId = json.getInteger(FixHeader.HEADER_ATTR_ID);
        String attrName = json.getString(FixHeader.HEADER_ATTR_NAME);
        String attrValue = json.getString(FixHeader.HEADER_ATTR_VALUE);
        return new PaasInstAttr(instId, attrId, attrName, attrValue);
    }

}
