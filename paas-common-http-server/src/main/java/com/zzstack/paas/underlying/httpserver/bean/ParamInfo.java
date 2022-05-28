package com.zzstack.paas.underlying.httpserver.bean;

import io.vertx.core.json.JsonObject;

public class ParamInfo {
	
	private String paramName;
	
	private String paramType;
	
	private boolean required;
	
	public ParamInfo() {
		super();
	}

	public ParamInfo(String paramName, String paramType, boolean required) {
		super();
		this.paramName = paramName;
		this.paramType = paramType;
		this.required = required;
	}

	public String getParamName() {
		return paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamType() {
		return paramType;
	}

	public void setParamType(String paramType) {
		this.paramType = paramType;
	}
	
	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(paramName, paramType);
		json.put("required", required);
		return json;
	}
	
	public String toJsonString() {
		return toJsonObject().toString();
	}
	
}
