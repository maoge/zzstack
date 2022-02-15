package com.zzstack.paas.underlying.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.utils.bean.SVarObject;


public class HttpCommonTools {

    private static Logger logger = LoggerFactory.getLogger(HttpCommonTools.class);
    
    public static final int READ_TIMEOUT = 5000;
    public static final int CONN_TIMEOUT = 5000;
    
    public static final String HTTP_PROTOCAL      = "http";
    public static final String HTTPS_PROTOCAL     = "https";
    
    public static final String HTTP_METHOD_GET    = "GET";
    public static final String HTTP_METHOD_POST   = "POST";
    public static final String HTTP_METHOD_PUT    = "PUT";
    public static final String HTTP_METHOD_PATCH  = "PATCH";
    public static final String HTTP_METHOD_DELETE = "DELETE";
    
    public static Map<String, String> REGISTED_METHOD = null;
    
    static {
        REGISTED_METHOD = new ConcurrentHashMap<String, String>();
    }
    
    public static boolean getData(String urlStr, Map<String, String> headers, SVarObject sVar) throws IOException {
        return request(urlStr, HTTP_METHOD_GET, headers, null, sVar);
    }
    
    public static boolean postData(String urlStr, Map<String, String> headers, String reqData, SVarObject sVar) throws IOException {
        return request(urlStr, HTTP_METHOD_POST, headers, reqData, sVar);
    }
    
    public static boolean putData(String urlStr, Map<String, String> headers, String reqData, SVarObject sVar) throws IOException {
        return request(urlStr, HTTP_METHOD_PUT, headers, reqData, sVar);
    }
    
    public static boolean patchData(String urlStr, Map<String, String> headers, String reqData, SVarObject sVar) throws IOException {
        if (!REGISTED_METHOD.containsKey(HTTP_METHOD_PATCH)) {
            allowMethods(HTTP_METHOD_PATCH);
        }
        
        return request(urlStr, HTTP_METHOD_PATCH, headers, reqData, sVar);
    }
    
    public static boolean deleteData(String urlStr, Map<String, String> headers, String reqData, SVarObject sVar) throws IOException {
        return request(urlStr, HTTP_METHOD_DELETE, headers, reqData, sVar);
    }
    
    /**
     * HttpURLConnection do not surpport PATCH, in order to add "PATCH" to HttpURLConnection.methods value, 
     * modify it by JDK reflect.
     *     private static final String[] methods = {
     *         "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
     *     };
     * 
     * @param methods
     */
    private static void allowMethods(String... methods) {
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

            methodsField.setAccessible(true);

            String[] oldMethods = (String[]) methodsField.get(null);
            Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
            methodsSet.addAll(Arrays.asList(methods));
            String[] newMethods = methodsSet.toArray(new String[0]);

            methodsField.set(null/*static field*/, newMethods);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        } finally {
            for (String method : methods) {
                REGISTED_METHOD.put(method, method);
            }
        }
    }

    private static boolean request(String urlStr, String method, Map<String, String> headers, String reqData, SVarObject sVar) throws IOException {
        String res = null;
        URL url = null;
        StringBuffer strBuff = null;
        BufferedReader buffReader = null;
        HttpURLConnection urlConn = null;

        boolean isConn = false;
        boolean ok = true;

        try {
            url = new URL(urlStr);
            urlConn = (HttpURLConnection) url.openConnection();

            urlConn.setRequestMethod(method);
            urlConn.setDoOutput(true);
            urlConn.setReadTimeout(READ_TIMEOUT);
            urlConn.setConnectTimeout(CONN_TIMEOUT);
            urlConn.setDefaultUseCaches(false);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Accept-Charset", "UTF-8");
            urlConn.setRequestProperty("Connection", "keep-alive");
            if (headers != null && !headers.isEmpty()) {
                Set<Entry<String, String>> entrySet = headers.entrySet();
                for (Entry<String, String> entry : entrySet) {
                    urlConn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            isConn = true;

            if (reqData != null && !reqData.isEmpty()) {
                byte[] bypes = reqData.getBytes();
                urlConn.getOutputStream().write(bypes);
            }
            
            int respCode = urlConn.getResponseCode();
            ok = respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_CREATED;
            if (ok) {
                String s = null;
                strBuff = new StringBuffer();
    
                buffReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
                while ((s = buffReader.readLine()) != null) {
                    strBuff.append(s);
                }
    
                res = strBuff.toString();
                strBuff.setLength(0);
            }

        } catch (IOException e) {
            logger.error("{} 异常. error:{}", urlStr, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("{} 异常. error:{}", urlStr, e.getMessage());
            res = null;
            ok = false;
        } finally {
            if (buffReader != null) {
                try {
                    buffReader.close();
                } catch (IOException e) {
                    logger.error("{} 异常. buffReader close error:{}", urlStr, e.getMessage());
                }
            }

            if (urlConn != null && isConn) {
                urlConn.disconnect();
            }
            
            sVar.setVal(res);
        }

        return ok;
    }

}
