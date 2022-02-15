package com.zzstack.paas.underlying.httpserver.singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zzstack.paas.underlying.httpserver.annotation.Service;

public class AllServiceMap {
	
	private Map<String, Service> servMap;
	
	private static AllServiceMap theInstance;
	private static Object mtx = null;
	
	static {
		mtx = new Object();
	}
	
	private AllServiceMap() {
		servMap = new ConcurrentHashMap<String, Service>();
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

}
