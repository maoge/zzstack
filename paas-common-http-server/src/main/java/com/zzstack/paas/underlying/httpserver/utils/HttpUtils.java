package com.zzstack.paas.underlying.httpserver.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class HttpUtils {

	private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	
	private static final int READ_TIMEOUT = 3000;
	private static final int CONN_TIMEOUT = 3000;
	
	private static final String PASSWD_PATTERN_STR = "^(?![a-zA-z\\d]*$)(?![!@#$%^&*_-]+$)[a-zA-Z\\d!@#$%^&*_-]+$";
	private static final Pattern PASSWD_PATTERN;
	
	public static final String PATH_SPLIT = File.separator;
	
	static {
		PASSWD_PATTERN = Pattern.compile(PASSWD_PATTERN_STR);
	}
	
    public static Map<String, Object> getParamForMap(RoutingContext ctx) {
        HttpServerRequest request = ctx.request();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        MultiMap map = null;
        if (request != null) {
            HttpMethod method = request.method();
            request.toString();
            if (method.equals(HttpMethod.GET)) {
                map = request.params();
            } else if (method.equals(HttpMethod.POST)) {
                String contentType = request.getHeader(CONSTS.CONTENT_TYPE);
                if (contentType.indexOf(CONSTS.CONTENT_TYPE_APP_JSON) != -1) {
                    JsonObject json = ctx.getBodyAsJson();
                    parseJsonParams(json, paramMap);
                } else {
                    map = request.formAttributes();
                }
            }

            String magicKey = request.getHeader(FixHeader.HEADER_MAGIC_KEY);
            if (magicKey != null) {
                paramMap.put(FixHeader.HEADER_MAGIC_KEY, magicKey);
            }

            if (map != null) {
                Iterator<Entry<String, String>> it = map.iterator();
                while (it.hasNext()) {
                    Entry<String, String> e = it.next();
                    paramMap.put(e.getKey(), e.getValue());
                }
            }
        }

        return paramMap;
    }
	
	public static RequestParameters getValidateParams(RoutingContext routeContext) {
		return routeContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);
	}
	
	private static void parseJsonParams(JsonObject json, Map<String, Object> paramMap) {
		if (json == null)
			return;
		
		Iterator<Entry<String, Object>> it = json.iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();
			paramMap.put(entry.getKey(), entry.getValue());
		}
	}
	
	//	Host -> 127.0.0.1
	//	X-Real-IP -> 127.0.0.1
	//	X-Forwarded-For -> 127.0.0.1
	//	Connection -> close
	//	Content-Length -> 51
	//	Accept -> application/json, text/javascript, */*; q=0.01
	//	X-Requested-With -> XMLHttpRequest
	//	User-Agent -> Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36
	//	Content-Type -> application/json; charset=UTF-8
	//	Origin -> http://127.0.0.1:10000
	//	Sec-Fetch-Site -> same-origin
	//	Sec-Fetch-Mode -> cors
	//	Sec-Fetch-Dest -> empty
	//	Referer -> http://127.0.0.1:10000/console/index.html
	//	Accept-Encoding -> gzip, deflate, br
	//	Accept-Language -> zh-CN,zh;q=0.9
	public static void printHeaders(MultiMap headers) {
		if (headers == null)
			return;
		
		Iterator<Entry<String, String>> it = headers.iterator();
		while (it.hasNext()) {
			Entry<String, String> e = it.next();
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
	}
	
	public static void outResultBean(RoutingContext ctx, ResultBean result) {
	    JsonObject json = new JsonObject();
	    json.put(FixHeader.HEADER_RET_CODE, result.getRetCode());
        json.put(FixHeader.HEADER_RET_INFO, result.getRetInfo());
        outJsonObject(ctx.response(), json);
	}

    public static void outTextPlain(RoutingContext ctx, String result) {
        HttpServerResponse response = ctx.response();
        response.putHeader("Content-type", "text/plain; charset=UTF-8");
        response.end(result != null ? result : "");
    }

	public static void outString(RoutingContext ctx, String result) {
	    HttpServerResponse response = ctx.response();
	    
	    response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, MAGIC_KEY, Accept");
        response.putHeader("Content-type", "application/json; charset=UTF-8");
        
        response.end(result != null ? result : "");
	}
	
	public static void outJsonObject(RoutingContext ctx, JsonObject json) {
		outJsonObject(ctx.response(), json);
	}

	public static void outJsonObject(HttpServerResponse response, JsonObject json) {
		response.putHeader("Access-Control-Allow-Origin", "*");
		response.putHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, MAGIC_KEY, Accept");
		response.putHeader("Content-type", "application/json; charset=UTF-8");
		
		response.end(json != null ? json.toString() : "{}");
	}

	public static void outJsonArray(RoutingContext ctx, JsonArray jsonArray) {
		MultiMap headers = ctx.request().headers();
		boolean isOrigin = headers.get("Origin") != null;
		outJsonArray(ctx.response(), jsonArray, isOrigin);
	}

	public static void outJsonArray(HttpServerResponse response, JsonArray jsonArray, boolean isOrigin) {
		if (isOrigin)
			response.putHeader("Access-Control-Allow-Origin", "*");
		response.putHeader("Content-type", "application/json; charset=UTF-8");
		if (jsonArray != null) {
			response.end(jsonArray.toString());
		} else
			response.end("[]");
	}
	
	public static void outChunkedBuff(RoutingContext ctx, byte[] bytes) {
		HttpServerResponse response = ctx.response();
		response.putHeader("Access-Control-Allow-Origin", "*");
		response.putHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, MAGIC_KEY, Accept");
		response.putHeader("Content-type", "application/octet-stream"); // image/gif, image/jpeg, image/png
		response.putHeader("Transfer-Encoding", "chunked");
		
		Buffer buff = Buffer.buffer(bytes);
		response.end(buff);
	}
	
	public static void outChunkedFile(RoutingContext ctx, String file) {
    	OpenOptions openOpts = new OpenOptions();
    	openOpts.setRead(true);
    	openOpts.setWrite(false);
    	openOpts.setCreate(false);
    	openOpts.setCreateNew(false);
    	
    	ServiceData.get().getVertx().fileSystem().open(file, openOpts, asyncEvent -> {

            if (asyncEvent.failed()) {
            	ctx.response().setStatusCode(500).end("file not found");
                return;
            }

            AsyncFile asyncFile = asyncEvent.result();
            ctx.response().setChunked(true);

            Pump pump = Pump.pump(asyncFile, ctx.response());
            pump.start();

            asyncFile.endHandler(aVoid -> {
                asyncFile.close();
                ctx.response().end();
            });
        });
	}

	public static void inChunkedFile(RoutingContext ctx, String path, String saveFileName) {
		Buffer body = ctx.getBody();
		int len = body.length();
		String saveFile = String.format("%s%s%s", path, HttpUtils.PATH_SPLIT, saveFileName);
		ServiceData.get().getVertx().fileSystem().open(saveFile, new OpenOptions(), asyncEvent -> {
			
			AsyncFile asyncFile = asyncEvent.result();
			asyncFile.write(body, 0, aResult -> {
				if (aResult.succeeded()) {
					logger.info("save file success, file:{}, size:{}", saveFile, len);
					asyncFile.flush();
				}
				
				asyncFile.close();
			});
			
		});
		
		ctx.response().end();
	}
	
	public static void writeRetBuffer(HttpServerResponse response, int retCode, String retInfo) {
		response.putHeader("Access-Control-Allow-Origin", "*");
		response.putHeader("Content-type", "application/json; charset=UTF-8");
		String sRet = getRetInfo(retCode, retInfo);
		response.end(sRet);
	}

	public static void outMessage(RoutingContext ctx, String str) {
		outMessage(ctx.response(), str);
	}

	public static void outMessage(HttpServerResponse response, String str) {
		String conTentType = "application/json; charset=UTF-8";
		if (!isJson(str)) {
			conTentType = "text/html; charset=UTF-8";
		}
		response.putHeader("Access-Control-Allow-Origin", "*");
		response.putHeader("Content-type", conTentType);
		if (HttpUtils.isNotNull(str))
			response.end(str);
		else
			response.end();
	}
	
	public static void logRequest(RoutingContext ctx) {
	    HttpServerRequest req = ctx.request();
	    String uri = req.uri();
	    logger.info("request uri: {}", uri);
	    
	    RequestParameters params = HttpUtils.getValidateParams(ctx);
	    RequestParameter paramBody = params.body();
	    if (paramBody != null) {
	        logger.info("body : {}", paramBody.toString());
	    }
	    
	    List<String> headParamNames = params.headerParametersNames();
	    if (headParamNames != null && !headParamNames.isEmpty()) {
	        logger.info("head parameters:");
            for (String name : headParamNames) {
                logger.info("{} : {}", name, params.headerParameter(name));
            }
	    }
	    
	    List<String> pathParamNames = params.pathParametersNames();
	    if (pathParamNames != null && !pathParamNames.isEmpty()) {
	        logger.info("path parameters:");
	        for (String name : pathParamNames) {
	            logger.info("{} : {}", name, params.pathParameter(name));
	        }
	    }
	    
	    List<String> queryParamNames = params.queryParametersNames();
	    if (queryParamNames != null && !queryParamNames.isEmpty()) {
	        logger.info("query parameters:");
	        for (String name : queryParamNames) {
	            logger.info("{} : {}", name, params.queryParameter(name));
	        }
	    }
	}

	private static String getRetInfo(int retCode, String retInfo) {
		JsonObject jsonObj = new JsonObject();

		jsonObj.put(FixHeader.HEADER_RET_CODE, retCode);
		jsonObj.put(FixHeader.HEADER_RET_INFO, retInfo);

		return jsonObj.toString();
	}

	public static boolean isNotNull(Object obj) {
		if (obj == null || "".equals(obj) || "null".equalsIgnoreCase(String.valueOf(obj))) {
			return false;
		} else {
			return true;
		}
	}
	
	public static boolean isNull(Object obj) {
		return (obj == null || "".equals(obj)) ? true : false;
	}
	
	public static String getUrlData(String urlString) throws IOException {
		String res = null;
		URL url;
		StringBuffer bs = null;
		BufferedReader buffer = null;
		HttpURLConnection urlcon = null;
		
		boolean isConn = false;
		
		try {
			url = new URL(urlString);
			urlcon = (HttpURLConnection) url.openConnection();
			
			urlcon.setRequestMethod(CONSTS.HTTP_METHOD_GET);
			urlcon.setDoOutput(false);
			urlcon.setReadTimeout(READ_TIMEOUT);
			urlcon.setConnectTimeout(CONN_TIMEOUT);
			
			isConn = true;
			
			buffer = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
			bs = new StringBuffer();
			String s = null;
			while ((s = buffer.readLine()) != null) {
				bs.append(s);
			}
			res = bs.toString();
		} catch (IOException e) {
			throw new IOException(urlString + "\r url调用异常", e);
		} finally {
			if (buffer != null)
				try {
					buffer.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			if (urlcon != null && isConn) {
				urlcon.disconnect();
			}
		}
		return res;
	}

	public static String getUrlData(String urlString, String userName, String userPwd)
			throws IOException, ConnectException {
		String res = null;
		URL url;
		StringBuffer bs = null;
		BufferedReader buffer = null;
		HttpURLConnection urlcon = null;
		
		boolean isConn = false;
		
		try {
			url = new URL(urlString);
			urlcon = (HttpURLConnection) url.openConnection();
			String userPassword = userName + ":" + userPwd;
			urlcon.setRequestProperty("Authorization", "Basic " + HttpUtils.base64(userPassword));
			
			urlcon.setRequestMethod(CONSTS.HTTP_METHOD_GET);
			urlcon.setDoOutput(false);
			urlcon.setReadTimeout(READ_TIMEOUT);
			urlcon.setConnectTimeout(CONN_TIMEOUT);
			
			isConn = true;
			
			buffer = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
			bs = new StringBuffer();
			String s = null;
			while ((s = buffer.readLine()) != null) {
				bs.append(s);
			}
			res = bs.toString();
		} catch (IOException e) {
			throw new IOException(urlString + "\r url调用异常", e);
		} finally {
			if (buffer != null)
				try {
					buffer.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			if (urlcon != null && isConn) {
				urlcon.disconnect();
			}
		}
		return res;
	}

	public static BufferedReader getPrometheusMetrics(String urlString) {
		URL url;
		HttpURLConnection urlcon = null;
		BufferedReader buffer = null;
		boolean isConn = false;

		try {
			url = new URL(urlString);

			urlcon = (HttpURLConnection) url.openConnection();
			urlcon.setRequestMethod("POST");
			urlcon.setDoOutput(false);
			urlcon.setReadTimeout(50000);
			urlcon.setConnectTimeout(50000);
			urlcon.setRequestProperty("Accept", "text/plain;version=0.0.4;q=1,*/*;q=0.1");
			urlcon.setRequestProperty("User-Agent", "Prometheus/2.2.1");
			urlcon.setRequestProperty("content-type", "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited");

			buffer = new BufferedReader(new InputStreamReader(urlcon.getInputStream()));
			isConn = true;

		} catch (IOException e) {
		    logger.error(e.getMessage(), e);
		} finally {
			if(buffer != null) {
				try {
					buffer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			if (urlcon != null && isConn) {
				urlcon.disconnect();
			}
		}

		return buffer;
	}
	public static Map<String, Object> jsonStrToMap(String str) {
		return jsonObjectToMap(strToJSON(str));
	}

	public static JsonObject strToJSON(String str) {
		JsonObject json = null;
		if (isNotNull(str) && !str.equals("[]")) {
			json = new JsonObject(str);
		}
		return json;
	}

	public static Map<String, Object> jsonObjectToMap(JsonObject jsonObj) {
		Map<String, Object> map = null;
		if (jsonObj != null) {
			Iterator<Entry<String, Object>> it = jsonObj.iterator();
			map = new HashMap<String, Object>();
			while (it.hasNext()) {
				Entry<String, Object> entry = it.next();
				map.put(entry.getKey(), entry.getValue());
			}
		}
		return map;
	}

	public static String base64(String str) {
		return Base64.getEncoder().encodeToString(str.getBytes());
	}

	public static boolean isJson(String value) {
		boolean res = false;
		if (isNotNull(value)) {
			String sign = value.substring(0, 1) + value.substring(value.length() - 1);
			if (sign.equals("{}") || sign.equals("[]")) {
				res = true;
			}
		}
		return res;
	}

	public static String getCurrMonth() {
		DecimalFormat df = new DecimalFormat("00");
		Calendar cal = Calendar.getInstance();
		String res = df.format(cal.get(Calendar.MONTH) + 1);
		return res;
	}

	public static String objToStr(Object obj) {
		if (null == obj || obj.equals("null") || String.valueOf(obj).startsWith("-") || obj.equals("")
				|| obj.equals("0.0"))
			return "0";
		else
			return String.valueOf(obj);
	}

	public static String strFormat(Object obj) {
		return numFormat(Double.parseDouble(objToStr(obj)));
	}

	public static String strFormat(Object obj, int k) {
		return numFormat(Double.parseDouble(objToStr(obj)) / k);
	}

	public static double strFormatForDouble(Object obj) {
		return Double.parseDouble(objToStr(obj));
	}

	public static String numFormat(double s) {
		DecimalFormat df = new DecimalFormat();
		df.applyPattern("#");
		return df.format(s);
	}

	public static long getCurrTimestamp() {
		return System.currentTimeMillis();
	}

	public static Map<String, Object> convertBean(Object bean)
			throws IntrospectionException, IllegalAccessException, InvocationTargetException {
		Class<?> type = bean.getClass();
		Map<String, Object> returnMap = new HashMap<String, Object>();
		BeanInfo beanInfo = Introspector.getBeanInfo(type);
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (int i = 0; i < propertyDescriptors.length; i++) {
			PropertyDescriptor descriptor = propertyDescriptors[i];
			String propertyName = descriptor.getName();
			if (!propertyName.equals("class")) {
				Method readMethod = descriptor.getReadMethod();
				Object result = readMethod.invoke(bean, new Object[0]);
				if (result != null) {
					returnMap.put(propertyName, result);
				} else {
					returnMap.put(propertyName, "");
				}
			}
		}
		return returnMap;
	}
	
	public static String getMonthByTimestamp(String simestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(Long.parseLong(simestamp)));
		int month = cal.get(Calendar.MONTH);
		
		return String.format("%02d", month + 1);
	}
	
	public static String concateArray(String[] arr) {
		if (arr == null || arr.length == 0)
			return "";
		
		int i = arr.length - 1;
		StringBuilder sb = new StringBuilder();
		for (; i >= 0; i--) {
			sb.append(arr[i]);
			if (i > 0)
				sb.append(CONSTS.PATH_COMMA);
		}
		
		return sb.toString();
	}
	
	public static boolean chkPasswdComplexity(String passwd) {
		boolean ret = false;
		try {
			Matcher match = PASSWD_PATTERN.matcher(passwd);
			ret = match.find();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		
		return ret;
	}

	public static <T> JsonObject mapToJson(Map<String, T> map) {
		JsonObject json = new JsonObject();

		if(map == null || map.size() == 0)
			return json;

		Set<Entry<String, T>> entrySet =  map.entrySet();
		for(Entry<String, T> entry : entrySet) {
			String key = entry.getKey();
			T t = entry.getValue();
			String s = Json.encode(t);
			JsonObject subJson = new JsonObject(s);
			json.put(key, subJson);
		}

		return json;
	}

	public static <T> Map<String, T> jsonToMap(JsonObject json, Class<T> clazz) {
		if(isNull(json)) {
			return null;
		}

		Iterator<Map.Entry<String, Object>> iter = json.iterator();
		Map<String, T> map = new ConcurrentHashMap<>();
		while(iter.hasNext()) {
			Map.Entry<String, Object> entry = iter.next();
			String key = entry.getKey();
			JsonObject value = (JsonObject) entry.getValue();
			T t = Json.decodeValue(value.toString(), clazz);
			map.put(key, t);
		}

		return map;
	}
}
