package com.zzstack.paas.underlying.httpserver.singleton;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.bean.ApiInfo;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class AllServiceMap {
	
	private Map<String, Service> servMap;
	private Map<String, ApiInfo> apiInfoMap;
	
	private static AllServiceMap theInstance;
	private static Object mtx = null;
	
	static {
		mtx = new Object();
	}
	
	private AllServiceMap() {
		servMap = new ConcurrentHashMap<String, Service>();
		apiInfoMap = new ConcurrentHashMap<String, ApiInfo>();
	}
	
	public static AllServiceMap get() {
		if (theInstance != null) {
			return theInstance;
		}
		
		synchronized(mtx) {
			if (theInstance == null) {
				theInstance = new AllServiceMap();
			}
		}
		
		return AllServiceMap.theInstance;
	}
	
	public void add(String path, Service service) {
		servMap.put(path, service);
	}
	
	public Service find(String path) {
		return servMap.get(path);
	}
	
	public void addApiInfo(String uri, ApiInfo api) {
		apiInfoMap.put(uri, api);
	}
	
	public ApiInfo getApi(String uri) {
		return apiInfoMap.get(uri);
	}
	
	public JsonArray getAllApiJson() {
		JsonArray arr = new JsonArray();
		Set<Entry<String, ApiInfo>> entrySet = apiInfoMap.entrySet();
		for (Entry<String, ApiInfo> entry : entrySet) {
			ApiInfo api = entry.getValue();
			arr.add(api.toJsonObject());
		}
		
		return arr;
	}
	
	public JsonObject getApiJson(String uri) {
		ApiInfo api = apiInfoMap.get(uri);
		return api == null ? new JsonObject() : api.toJsonObject();
	}

}
