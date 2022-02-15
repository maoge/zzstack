package com.zzstack.paas.underlying.utils.paas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.bean.SVarObject;

public class PaasHttpOperationUtils {
    
    public static final String AUTH_URI = "/paas/account/login";
    public static final String LOAD_SERVICE_TOPO_URI = "/paas/metadata/loadServiceTopo";
    public static final String LOAD_INSTANCE_META_URI = "/paas/metadata/loadInstanceMeta";
    public static final String PAAS_TEST_URI = "/paas/bench/test";
    
    public static Map<String, String> POST_HEADERS = null;
    static {
        POST_HEADERS = new HashMap<String, String>();
        POST_HEADERS.put("Content-Type", "application/json");
    }
    
    public static boolean doMetaSvrAuth(String metaSvrUrl, String user, String passwd, SVarObject result) throws IOException {
        String url = String.format("%s%s", metaSvrUrl, AUTH_URI);
        
        JSONObject reqData = new JSONObject();
        reqData.put(FixHeader.HEADER_USER, user);
        reqData.put(FixHeader.HEADER_PASSWORD, passwd);
        
        return HttpCommonTools.postData(url, POST_HEADERS, reqData.toString(), result);
    }
    
    public static boolean loadServiceTopo(String metaSvrUrl, String servInstID, String magicKey, SVarObject result) throws IOException {
        String url = String.format("%s%s", metaSvrUrl, LOAD_SERVICE_TOPO_URI);
        
        JSONObject reqData = new JSONObject();
        reqData.put(FixHeader.HEADER_INST_ID, servInstID);
        
        SVarObject httpResult = new SVarObject();
        Map<String, String> headers = null;
        if (magicKey != null && !magicKey.isEmpty()) {
            headers = new HashMap<String, String>();
            headers.put("Content-Type",             "application/json");
            headers.put(FixHeader.HEADER_MAGIC_KEY, magicKey);
        } else {
            headers = POST_HEADERS;
        }
        if (!HttpCommonTools.postData(url, headers, reqData.toString(), httpResult))
            return false;
        
        JSONObject topoJson = JSONObject.parseObject(httpResult.getVal());
        JSONObject servJson = topoJson.getJSONObject(FixHeader.HEADER_RET_INFO);
        result.setVal(servJson.toString());
        
        return true;
    }
    
    public static boolean loadInstanceMeta(String metaSvrUrl, String servInstID, String magicKey, SVarObject result) throws IOException {
        String url = String.format("%s%s", metaSvrUrl, LOAD_INSTANCE_META_URI);
        
        JSONObject reqData = new JSONObject();
        reqData.put(FixHeader.HEADER_INST_ID, servInstID);
        
        SVarObject httpResult = new SVarObject();
        Map<String, String> headers = null;
        if (magicKey != null && !magicKey.isEmpty()) {
            headers = new HashMap<String, String>();
            headers.put("Content-Type",             "application/json");
            headers.put(FixHeader.HEADER_MAGIC_KEY, magicKey);
        } else {
            headers = POST_HEADERS;
        }
        if (!HttpCommonTools.postData(url, headers, reqData.toString(), httpResult))
            return false;
        
        JSONObject topoJson = JSONObject.parseObject(httpResult.getVal());
        JSONObject servJson = topoJson.getJSONObject(FixHeader.HEADER_RET_INFO);
        result.setVal(servJson.toString());
        
        return true;
    }
    
    public static void postCollectData(String metaSvrUrl, Map<String, String> headers, String data) throws IOException {
        SVarObject httpResult = new SVarObject();
        HttpCommonTools.postData(metaSvrUrl, headers, data, httpResult);
    }

    public static boolean testUrl(String metaSvrUrl) {
        String url = String.format("%s%s", metaSvrUrl, PAAS_TEST_URI);
        
        boolean result = true;
        try {
            SVarObject httpResult = new SVarObject();
            result = HttpCommonTools.getData(url, null, httpResult);
        } catch (IOException e) {
            result = false;
        }
        
        return result;
    }
    
}
