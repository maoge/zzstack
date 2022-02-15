package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasTopology extends BeanMapper {
	
	private String instId1; // A端INST_ID或父INST_ID
	private String instId2; // Z端INST_ID或子INST_ID
	private int topoType;   // TOPO类型:1 link;2 contain
	
	public PaasTopology() {
		super();
	}
	
	/**
	 * @param instId1
	 * @param instId2
	 * @param topoType
	 */
	public PaasTopology(String instId1, String instId2, int topoType) {
		super();
		this.instId1 = instId1;
		this.instId2 = instId2;
		this.topoType = topoType;
	}
	
	public static PaasTopology convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;
		
		String instId1  = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID1);
		String instId2  = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID2);
		int    topoType = getFixDataAsInt(mapper, FixHeader.HEADER_TOPO_TYPE);
		
		if (instId2 == null)
		    instId2 = "";
		
		return new PaasTopology(instId1, instId2, topoType);
	}

	public String getInstId1() {
		return instId1;
	}

	public void setInstId1(String instId1) {
		this.instId1 = instId1;
	}

	public String getInstId2() {
		return instId2;
	}

	public void setInstId2(String instId2) {
		this.instId2 = instId2;
	}

	public int getTopoType() {
		return topoType;
	}

	public void setTopoType(int topoType) {
		this.topoType = topoType;
	}
    
    public String toJson() {
    	JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_INST_ID1, instId1);
        retval.put(FixHeader.HEADER_INST_ID2, instId2);
        retval.put(FixHeader.HEADER_TOPO_TYPE, topoType);
    	
    	return retval.toString();
    }
    
	public String getToe(String instId) {
		return instId.equals(instId1) ? instId2 : instId1;
	}

	@Override
	public String toString() {
		return "PaasTopology [instId1=" + instId1 + ", instId2=" + instId2 + ", topoType=" + topoType + "]";
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(FixHeader.HEADER_INST_ID1, instId1);
		json.put(FixHeader.HEADER_INST_ID2, instId2);
		json.put(FixHeader.HEADER_TOPO_TYPE, topoType);
		
		return json;
	}
	
	public static PaasTopology fromJson(String jsonStr) {
	    JsonObject json = new JsonObject(jsonStr);
	    String instId1 = json.getString(FixHeader.HEADER_INST_ID1);
	    String instId2 = json.getString(FixHeader.HEADER_INST_ID2);
	    int topoType = json.getInteger(FixHeader.HEADER_TOPO_TYPE);
	    return new PaasTopology(instId1, instId2, topoType);
	}

}
