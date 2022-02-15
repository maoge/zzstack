package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class PaasService extends BeanMapper {
	
    private String   instId;
    private String   servName;
    private String   servClass;
    private String   servType;
    private String   version;
    private boolean  isDeployed;
    private boolean  isProduct;
    private long     createTime;
    private String   user;
    private String   password;
    private String   pseudoDeployFlag;
    
    public PaasService() {
    	super();
    }
    
	/**
	 * @param instId
	 * @param servName
	 * @param servClass
	 * @param servType
	 * @param isDeployed
	 * @param isProduct
	 * @param createTime
	 * @param user
	 * @param password
	 */
    public PaasService(String instId, String servName, String servClass, String servType, String version,
            boolean isDeployed, boolean isProduct, long createTime, String user, String password,
            String pseudoDeployFlag) {
        super();
        this.instId = instId;
        this.servName = servName;
        this.servClass = servClass;
        this.servType = servType;
        this.version = version;
        this.isDeployed = isDeployed;
        this.isProduct = isProduct;
        this.createTime = createTime;
        this.user = user;
        this.password = password;
        this.pseudoDeployFlag = pseudoDeployFlag;
    }

    public static PaasService convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;
		
	    String  instId     = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
	    String  servName   = getFixDataAsString(mapper, FixHeader.HEADER_SERV_NAME);
	    String  servClass  = getFixDataAsString(mapper, FixHeader.HEADER_SERV_CLAZZ);
	    String  servType   = getFixDataAsString(mapper, FixHeader.HEADER_SERV_TYPE);
	    String  version    = getFixDataAsString(mapper, FixHeader.HEADER_VERSION);
	    boolean isDeployed = getFixDataAsString(mapper, FixHeader.HEADER_IS_DEPLOYED).equals(CONSTS.STR_TRUE) ? true : false;
	    boolean isProduct  = getFixDataAsString(mapper, FixHeader.HEADER_IS_PRODUCT).equals(CONSTS.STR_TRUE) ? true : false;;
	    long    createTime = getFixDataAsLong(mapper, FixHeader.HEADER_CREATE_TIME);
	    String  user       = getFixDataAsString(mapper, FixHeader.HEADER_USER);
	    String  password   = getFixDataAsString(mapper, FixHeader.HEADER_PASSWORD);
	    String  pseudoDeployFlag = getFixDataAsString(mapper, FixHeader.HEADER_PSEUDO_DEPLOY_FLAG);
		
		return new PaasService(instId, servName, servClass, servType, version, isDeployed, isProduct, createTime, user, password, pseudoDeployFlag);
	}
	
	public static void convert(Map<String, Object> mapper, PaasService service) {
		if (mapper == null || mapper.isEmpty())
			return;
		
	    String  instId     = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
	    String  servName   = getFixDataAsString(mapper, FixHeader.HEADER_SERV_NAME);
	    String  servClass  = getFixDataAsString(mapper, FixHeader.HEADER_SERV_CLAZZ);
	    String  servType   = getFixDataAsString(mapper, FixHeader.HEADER_SERV_TYPE);
	    boolean isDeployed = getFixDataAsString(mapper, FixHeader.HEADER_IS_DEPLOYED).equals(CONSTS.STR_TRUE) ? true : false;
	    boolean isProduct  = getFixDataAsString(mapper, FixHeader.HEADER_IS_PRODUCT).equals(CONSTS.STR_TRUE) ? true : false;;
	    long    createTime = getFixDataAsLong(mapper, FixHeader.HEADER_CREATE_TIME);
	    String  user       = getFixDataAsString(mapper, FixHeader.HEADER_USER);
	    String  password   = getFixDataAsString(mapper, FixHeader.HEADER_PASSWORD);
	    String  pseudoDeployFlag = getFixDataAsString(mapper, FixHeader.HEADER_PSEUDO_DEPLOY_FLAG);
	    String  version    = getFixDataAsString(mapper, FixHeader.HEADER_VERSION);
	    
	    service.setInstId(instId);
	    service.setServName(servName);
	    service.setServClass(servClass);
		service.setServType(servType);
		service.setDeployed(isDeployed);
		service.setProduct(isProduct);
		service.setCreateTime(createTime);
		service.setUser(user);
		service.setPassword(password);
		service.setPseudoDeployFlag(pseudoDeployFlag);
		service.setVersion(version);
	}

	public String getInstId() {
		return instId;
	}

	public void setInstId(String instId) {
		this.instId = instId;
	}

	public String getServName() {
		return servName;
	}

	public void setServName(String servName) {
		this.servName = servName;
	}

	public String getServClass() {
		return servClass;
	}

	public void setServClass(String servClass) {
		this.servClass = servClass;
	}

	public String getServType() {
		return servType;
	}

	public void setServType(String servType) {
		this.servType = servType;
	}
	
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

	public boolean isDeployed() {
		return isDeployed;
	}

	public void setDeployed(boolean isDeployed) {
		this.isDeployed = isDeployed;
	}

	public boolean isProduct() {
		return isProduct;
	}

	public void setProduct(boolean isProduct) {
		this.isProduct = isProduct;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
    public String getPseudoDeployFlag() {
        return pseudoDeployFlag;
    }

    public void setPseudoDeployFlag(String pseudoDeployFlag) {
        this.pseudoDeployFlag = pseudoDeployFlag;
    }
    
	public String toJson() {
		JsonObject retval = new JsonObject();
		retval.put(FixHeader.HEADER_INST_ID, instId);
		retval.put(FixHeader.HEADER_SERV_NAME, servName);
		retval.put(FixHeader.HEADER_SERV_CLAZZ, servClass);
		retval.put(FixHeader.HEADER_SERV_TYPE, servType);
		retval.put(FixHeader.HEADER_VERSION, version);
		retval.put(FixHeader.HEADER_IS_DEPLOYED, isDeployed);
		retval.put(FixHeader.HEADER_IS_PRODUCT, isProduct);
		retval.put(FixHeader.HEADER_CREATE_TIME, createTime);
		retval.put(FixHeader.HEADER_USER, user);
		retval.put(FixHeader.HEADER_PASSWORD, password);

		return retval.toString();
	}
	
	public static PaasService fromJson(String strJson) {
		JsonObject obj = new JsonObject(strJson);

		String instId      = obj.getString(FixHeader.HEADER_INST_ID);
		String servName    = obj.getString(FixHeader.HEADER_SERV_NAME);
		String servClass   = obj.getString(FixHeader.HEADER_SERV_CLAZZ);
		String servType    = obj.getString(FixHeader.HEADER_SERV_TYPE);
		String version     = obj.getString(FixHeader.HEADER_VERSION);
		boolean isDeployed = obj.getBoolean(FixHeader.HEADER_IS_DEPLOYED);
		boolean isProduct  = obj.getBoolean(FixHeader.HEADER_IS_PRODUCT);
		long createTime    = obj.getLong(FixHeader.HEADER_CREATE_TIME);
		String user        = obj.getString(FixHeader.HEADER_USER);
		String password    = obj.getString(FixHeader.HEADER_PASSWORD);
		String pseudoDeployFlag = obj.getString(FixHeader.HEADER_PSEUDO_DEPLOY_FLAG);

        return new PaasService(instId, servName, servClass, servType, version, isDeployed, isProduct, createTime, user,
                password, pseudoDeployFlag);
    }

    @Override
    public String toString() {
        return "PaasService [instId=" + instId + ", servName=" + servName + ", servClass=" + servClass + ", servType="
                + servType + ", isDeployed=" + isDeployed + ", isProduct=" + isProduct + ", createTime=" + createTime
                + ", user=" + user + ", password=" + password + ", pseudoDeployFlag=" + pseudoDeployFlag + "]";
    }
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(FixHeader.HEADER_INST_ID, instId);
		json.put(FixHeader.HEADER_SERV_NAME, servName);
		json.put(FixHeader.HEADER_SERV_CLAZZ, servClass);
		json.put(FixHeader.HEADER_SERV_TYPE, servType);
		json.put(FixHeader.HEADER_IS_DEPLOYED, isDeployed);
		json.put(FixHeader.HEADER_IS_PRODUCT, isProduct);
		json.put(FixHeader.HEADER_CREATE_TIME, createTime);
		json.put(FixHeader.HEADER_USER, user);
		json.put(FixHeader.HEADER_PASSWORD, password);
		json.put(FixHeader.HEADER_PSEUDO_DEPLOY_FLAG, pseudoDeployFlag);
		
		return json;
	}
	
}
