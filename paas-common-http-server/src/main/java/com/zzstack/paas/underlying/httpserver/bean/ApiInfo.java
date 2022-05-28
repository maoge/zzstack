package com.zzstack.paas.underlying.httpserver.bean;

import com.zzstack.paas.underlying.httpserver.annotation.Parameter;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ApiInfo {
	
	private String uri;
	private String method;
	
	private ParamInfo[] headerParams;
	private ParamInfo[] queryParams;
	private ParamInfo[] pathParams;
	private ParamInfo[] bodyParams;
	
	public ApiInfo() {
		super();
	}

	public ApiInfo(String uri, String method, ParamInfo[] headerParams, ParamInfo[] queryParams, ParamInfo[] pathParams,
			ParamInfo[] bodyParams) {
		super();
		this.uri = uri;
		this.method = method;
		this.headerParams = headerParams;
		this.queryParams = queryParams;
		this.pathParams = pathParams;
		this.bodyParams = bodyParams;
	}
	 
	public ApiInfo(String uri, String method, Parameter[] headerParams, Parameter[] queryParams, Parameter[] pathParams,
			Parameter[] bodyParams) {
		super();
		this.uri = uri;
		this.method = method;
		this.headerParams = marshellParam(headerParams);
		this.queryParams = marshellParam(queryParams);
		this.pathParams = marshellParam(pathParams);
		this.bodyParams = marshellParam(bodyParams);
	}

	private ParamInfo[] marshellParam(Parameter[] params) {
		if (params == null || params.length == 0) {
			return null;
		}
		
		int len = params.length;
		ParamInfo[] infos = new ParamInfo[len];
		for (int i = 0; i < len; i++ ) {
			Parameter param = params[i];
			ParamInfo info = transformParam(param);
			infos[i] = info;
		}
		
		return infos;
	}
	
	private ParamInfo transformParam(Parameter param) {
		if (param == null) {
			return null;
		}
		
		return new ParamInfo(param.name(), param.type().name(), param.required());
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public ParamInfo[] getHeaderParams() {
		return headerParams;
	}

	public void setHeaderParams(ParamInfo[] headerParams) {
		this.headerParams = headerParams;
	}

	public ParamInfo[] getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(ParamInfo[] queryParams) {
		this.queryParams = queryParams;
	}

	public ParamInfo[] getPathParams() {
		return pathParams;
	}

	public void setPathParams(ParamInfo[] pathParams) {
		this.pathParams = pathParams;
	}

	public ParamInfo[] getBodyParams() {
		return bodyParams;
	}

	public void setBodyParams(ParamInfo[] bodyParams) {
		this.bodyParams = bodyParams;
	}
	
	private JsonArray getParamArr(ParamInfo[] params) {
		JsonArray arr = new JsonArray();
		for (ParamInfo param : params) {
			JsonObject json = param.toJsonObject();
			arr.add(json);
		}
		
		return arr;
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put("URI", uri);
		json.put("Method", method);
		
		if (headerParams != null) {
			JsonArray headArr = getParamArr(headerParams);
			json.put("HeadParams", headArr);
		}
		
		if (queryParams != null) {
			JsonArray headArr = getParamArr(queryParams);
			json.put("QueryParams", headArr);
		}
		
		if (pathParams != null) {
			JsonArray pathArr = getParamArr(pathParams);
			json.put("PathParams", pathArr);
		}
		
		if (bodyParams != null) {
			JsonArray bodyArr = getParamArr(bodyParams);
			json.put("BodyParams", bodyArr);
		}
		
		return json;
	}
	
	public String toJsonString() {
		return this.toJsonObject().toString();
	}

}
