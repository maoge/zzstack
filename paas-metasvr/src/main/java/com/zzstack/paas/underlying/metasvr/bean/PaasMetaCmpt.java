package com.zzstack.paas.underlying.metasvr.bean;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class PaasMetaCmpt extends BeanMapper {
	
    private int      cmptId;
    private String   cmptName;
    private String   cmptNameCn;
    private boolean  isNeedDeploy;
    private String   servType;
    private String   servClazz;
    private String   nodeJsonType;
    private Set<Integer> subCmptId;
    
	public PaasMetaCmpt() {
		super();
		this.subCmptId = new HashSet<Integer>();
	}
    
	/**
	 * @param cmptId
	 * @param cmptName
	 * @param cmptNameCn
	 * @param isNeedDeploy
	 * @param servType
	 * @param servClazz
	 * @param nodeJsonType
	 */
	public PaasMetaCmpt(int cmptId, String cmptName, String cmptNameCn, boolean isNeedDeploy, String servType,
			String servClazz, String nodeJsonType) {
		super();
		this.cmptId = cmptId;
		this.cmptName = cmptName;
		this.cmptNameCn = cmptNameCn;
		this.isNeedDeploy = isNeedDeploy;
		this.servType = servType;
		this.servClazz = servClazz;
		this.nodeJsonType = nodeJsonType;
		this.subCmptId = new HashSet<Integer>();
	}
	
	public static PaasMetaCmpt convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;

	    int     cmptId       = getFixDataAsInt(mapper, FixHeader.HEADER_CMPT_ID);
	    String  cmptName     = getFixDataAsString(mapper, FixHeader.HEADER_CMPT_NAME);
	    String  cmptNameCn   = getFixDataAsString(mapper, FixHeader.HEADER_CMPT_NAME_CN);
	    boolean isNeedDeploy = getFixDataAsString(mapper, FixHeader.HEADER_IS_NEED_DEPLOY).equals(CONSTS.STR_TRUE);
	    String  servType     = getFixDataAsString(mapper, FixHeader.HEADER_SERV_TYPE);
	    String  servClazz    = getFixDataAsString(mapper, FixHeader.HEADER_SERV_CLAZZ);
	    String  nodeJsonType = getFixDataAsString(mapper, FixHeader.HEADER_NODE_JSON_TYPE);
	    String  subCmptId    = getFixDataAsString(mapper, FixHeader.HEADER_SUB_CMPT_ID);
	    
	    PaasMetaCmpt cmpt = new PaasMetaCmpt(cmptId, cmptName, cmptNameCn, isNeedDeploy, servType,
				servClazz, nodeJsonType);
	    
	    if (subCmptId != null && !subCmptId.isEmpty()) {
	    	String[] subCmptIdArr = subCmptId.split(",");
	    	for (String id : subCmptIdArr) {
	    		cmpt.addSubCmptId(Integer.valueOf(id));
	    	}
	    }
	    
	    return cmpt;
	}
	
	public void addSubCmptId(Integer cmptId) {
		subCmptId.add(cmptId);
	}
	
	public Set<Integer> getSubCmptId() {
		return subCmptId;
	}

	public int getCmptId() {
		return cmptId;
	}

	public void setCmptId(int cmptId) {
		this.cmptId = cmptId;
	}

	public String getCmptName() {
		return cmptName;
	}

	public void setCmptName(String cmptName) {
		this.cmptName = cmptName;
	}

	public String getCmptNameCn() {
		return cmptNameCn;
	}

	public void setCmptNameCn(String cmptNameCn) {
		this.cmptNameCn = cmptNameCn;
	}

	public boolean isNeedDeploy() {
		return isNeedDeploy;
	}

	public void setNeedDeploy(boolean isNeedDeploy) {
		this.isNeedDeploy = isNeedDeploy;
	}

	public String getServType() {
		return servType;
	}

	public void setServType(String servType) {
		this.servType = servType;
	}

	public String getServClazz() {
		return servClazz;
	}

	public void setServClazz(String servClazz) {
		this.servClazz = servClazz;
	}

	public String getNodeJsonType() {
		return nodeJsonType;
	}

	public void setNodeJsonType(String nodeJsonType) {
		this.nodeJsonType = nodeJsonType;
	}
	
    public boolean haveSubComponent() {
    	return !subCmptId.isEmpty();
    }

	@Override
	public String toString() {
		return "PaasMetaCmpt [cmptId=" + cmptId + ", cmptName=" + cmptName + ", cmptNameCn=" + cmptNameCn
				+ ", isNeedDeploy=" + isNeedDeploy + ", servType=" + servType + ", servClazz=" + servClazz
				+ ", nodeJsonType=" + nodeJsonType + ", subCmptId=" + subCmptId + "]";
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put("cmptId", cmptId);
		json.put("cmptName", cmptName);
		json.put("cmptNameCn", cmptNameCn);
		json.put("isNeedDeploy", isNeedDeploy);
		json.put("servType", servType);
		json.put("servClazz", servClazz);
		json.put("nodeJsonType", nodeJsonType);
		
		JsonArray arr = new JsonArray();
		for (Integer id : subCmptId) {
			arr.add(id);
		}
		json.put("subCmptId", arr);

	    return json;
	}

}
