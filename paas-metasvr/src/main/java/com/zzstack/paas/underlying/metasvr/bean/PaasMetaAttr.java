package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class PaasMetaAttr extends BeanMapper {
	
    private int     attrId;
    private String  attrName;
    private String  attrNameCn;
    private boolean autoGen;
    
	public PaasMetaAttr() {
		super();
	}

	/**
	 * @param attrId
	 * @param attrName
	 * @param attrNameCn
	 * @param autoGen
	 */
	public PaasMetaAttr(int attrId, String attrName, String attrNameCn, boolean autoGen) {
		super();
		this.attrId = attrId;
		this.attrName = attrName;
		this.attrNameCn = attrNameCn;
		this.autoGen = autoGen;
	}
	
	public static PaasMetaAttr convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;

		int     attrId     = getFixDataAsInt(mapper, FixHeader.HEADER_ATTR_ID);
	    String  attrName   = getFixDataAsString(mapper, FixHeader.HEADER_ATTR_NAME);
	    String  attrNameCn = getFixDataAsString(mapper, FixHeader.HEADER_ATTR_NAME_CN);
	    boolean autoGen    = getFixDataAsString(mapper, FixHeader.HEADER_AUTO_GEN).equals(CONSTS.STR_TRUE);

		return new PaasMetaAttr(attrId, attrName, attrNameCn, autoGen);
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
	
	public String getAttrNameCn() {
		return attrNameCn;
	}
	
	public void setAttrNameCn(String attrNameCn) {
		this.attrNameCn = attrNameCn;
	}
	
	public boolean isAutoGen() {
		return autoGen;
	}
	
	public void setAutoGen(boolean autoGen) {
		this.autoGen = autoGen;
	}

	@Override
	public String toString() {
		return "PaasMetaAttr [attrId=" + attrId + ", attrName=" + attrName + ", attrNameCn=" + attrNameCn + ", autoGen="
				+ autoGen + "]";
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put("attrId", attrId);
		json.put("attrName", attrName);
		json.put("attrNameCn", attrNameCn);
		json.put("autoGen", autoGen);
		
		return json;
	}
    
}
