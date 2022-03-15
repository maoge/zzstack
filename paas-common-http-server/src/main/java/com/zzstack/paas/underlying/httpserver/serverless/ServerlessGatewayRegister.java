package com.zzstack.paas.underlying.httpserver.serverless;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;
import com.zzstack.paas.underlying.utils.HttpCommonTools;
import com.zzstack.paas.underlying.utils.bean.SVarObject;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class ServerlessGatewayRegister {
    
    private static Logger logger = LoggerFactory.getLogger(ServerlessGatewayRegister.class);
    
    private List<String> validGwList;
    
    private ReentrantLock lock = null;
    private AtomicInteger validSize = null;
    private volatile long index = 0;
    
    private static final String API_UPSTREAMS = "upstreams";
    private static final String API_SERVICES = "services";
    private static final String API_ROUTES = "routes";
    private static final String UPSTREAM_NODES = "nodes";
    
    public static final int DEFAULT_WEIGHT = 10;
    
    public static Map<String, String> HEADERS = null;
    
    static {
        HEADERS = new ConcurrentHashMap<String, String>();
    }
    
    public ServerlessGatewayRegister(String gwAddrs) {
        this.lock = new ReentrantLock();
        this.validSize = new AtomicInteger(0);
        
        this.validGwList = new ArrayList<String>();
        String[] arr = gwAddrs.split(",");
        for (String addr : arr) {
            validGwList.add(addr);
            validSize.incrementAndGet();
        }
        
        if (HEADERS.isEmpty()) {
            initHeaders();
        }
    }
    
    private String getGateway() {
        String addr = null;
        lock.lock();
        try {
            int i = (int) (index++ % validSize.get());
            addr = validGwList.get(i);
        } finally {
            lock.unlock();
        }
        
        return addr;
    }
    
    private void initHeaders() {
        String xApiKey = ServiceData.get().getXApiKey();
        HEADERS.put("X-API-KEY", xApiKey);
        HEADERS.put("CONTENT-TYPE", "application/json");
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+------------REQ BODY-----------+-------------------instructions------------+
     *  |    PUT     | /apisix/admin/upstreams/{id}  | {...}                         | create upstream by identified id          |
     *  +------------+-------------------------------+-------------------------------+-------------------------------------------+
     *  
     *  {
     *    "id": "${id}",
     *    "name": "upstream-xxx",      # upstream 名称
     *    "desc": "hello world",       # upstream 描述
     *    "retries": 3,
     *    "type": "roundrobin",
     *    "nodes": {
     *      "127.0.0.1:80":1,
     *      "127.0.0.2:80":2,
     *      "foo.com:80":3
     *    }
     *    "checks": {},                # 配置健康检查的参数
     *    "hash_on": "",
     *    "key": ""
     *  }
     *  
     *  type说明:
     *    roundrobin: 带权重的 roundrobin
     *    chash: 一致性哈希
     *    ewma: 选择延迟最小的节点
     *    least_conn: 选择 (active_conn + 1) / weight 最小的节点
     *  
     */
    public boolean createUpstream(String upstreamID) {
        String rootUrl = getGateway();
        
        String url = String.format("%s/%s/%s", rootUrl, API_UPSTREAMS, upstreamID);
        SVarObject sVar = new SVarObject();
        
        JsonObject json = new JsonObject();
        json.put("retries", 3);
        json.put("type", "roundrobin");
        
        JsonObject nodes = new JsonObject();
        json.put("nodes", nodes);
        
        boolean res = false;
        try {
            res = HttpCommonTools.putData(url, HEADERS, json.toString(), sVar);
            if (!res) {
                logger.error("createUpstream: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("createUpstream: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   PATCH    | /apisix/admin/upstreams/{id}  | {...}                                   | modify upstream attributes                     |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean addUpstreamNode(String node, int weight) {
        String rootUrl = getGateway();
        String upstreamID = ServiceData.get().getUpstreamID();

        // String nodeIP = ServiceData.get().getIP();
        // int nodePort = ServiceData.get().getPort();
        // String nodeAddr = String.format("%s:%d", nodeIP, nodePort);

        String url = String.format("%s/%s/%s", rootUrl, API_UPSTREAMS, upstreamID);
        SVarObject sVar = new SVarObject();

        JsonObject item = new JsonObject();
        // item.put(nodeAddr, DEFAULT_WEIGHT);
        item.put(node, weight);

        JsonObject items = new JsonObject();
        items.put("nodes", item);

        boolean res = false;
        try {
            res = HttpCommonTools.patchData(url, HEADERS, items.toString(), sVar);
            if (!res) {
                logger.error("createUpstream: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("createUpstream: {} fail, {}", url, e.getMessage(), e);
        }

        return res;
    }
    
    /**
     * 
     * @param node
     *        eg: "172.20.0.49:9090"
     * @return
     * 
     * 
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   PATCH    | /apisix/admin/upstreams/{id}  | {...}                                   | modify upstream attributes                     |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     *  
     *  {
     *      "172.20.0.49:9090":{}
     *  }
     */
    public boolean delUpstreamNode(String upstreamID, String node) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_UPSTREAMS, upstreamID);
        SVarObject sVar = new SVarObject();
        
        JsonObject item = new JsonObject();
        item.put(node, null);
        
        JsonObject items = new JsonObject();
        items.put("nodes", item);
        
        boolean res = false;
        try {
            res = HttpCommonTools.patchData(url, HEADERS, items.toString(), sVar);
            if (!res) {
                logger.error("delUpstreamNode: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("delUpstreamNode: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     * 
     * @param node
     *        eg: "172.20.0.49:9090"
     * @return
     * 
     * 
     *  +---METHOD---+---------------REQ URI--------------+-----------------REQ BODY----------------+-------------------instructions------------+
     *  |   PATCH    | /apisix/admin/upstreams/{id}/nodes | {...}                                   | modify upstream attributes                |
     *  +------------+------------------------------------+-----------------------------------------+-------------------------------------------+
     *  
     *  {
     *      "172.20.0.49:9090": 10
     *  }
     */
    public boolean updUpstreamNode(String node, int weight) {
        String rootUrl = getGateway();
        String upstreamID = ServiceData.get().getUpstreamID();
        
        String url = String.format("%s/%s/%s/%s", rootUrl, API_UPSTREAMS, upstreamID, UPSTREAM_NODES);
        SVarObject sVar = new SVarObject();
        
        JsonObject item = new JsonObject();
        item.put(node, weight);
        
        boolean res = false;
        try {
            res = HttpCommonTools.patchData(url, HEADERS, item.toString(), sVar);
            if (!res) {
                logger.error("updUpstreamNode: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("updUpstreamNode: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/upstreams       | NULL                                    | get all upstream list                          |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean getUpstreams() {
        String rootUrl = getGateway();
        String url = String.format("%s/%s", rootUrl, API_UPSTREAMS);
        SVarObject sVar = new SVarObject();
        
        boolean res = false;
        try {
            res = HttpCommonTools.getData(url, HEADERS, sVar);
            if (!res) {
                logger.error("getUpstreams: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("getUpstreams: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/upstreams/{id}  | NULL                                    | get upstream list by id                        |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean getUpstreamByID(String streamID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_UPSTREAMS, streamID);
        SVarObject sVar = new SVarObject();
        
        boolean res = false;
        try {
            res = HttpCommonTools.getData(url, HEADERS, sVar);
            if (!res) {
                logger.error("getUpstreamByID: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("getUpstreamByID: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    public boolean isUpstreamNodeExist(String streamID, String node) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_UPSTREAMS, streamID);
        SVarObject sVar = new SVarObject();
        boolean result = false;
        
        try {
            if (!HttpCommonTools.getData(url, HEADERS, sVar)) {
                logger.error("getUpstreamByID: {} fail ......", url);
            }
            
            // {"count":"1","action":"get","node":{"value":{"pass_host":"pass","nodes":[{"host":"172.20.0.171","weight":10,"port":9095}],
            //  "update_time":1614590623,"name":"100","type":"roundrobin","create_time":1614590274,
            //  "timeout":{"read":6000,"send":6000,"connect":6000},"id":"100"},"key":"\/apisix\/upstreams\/100"}}
    
            JsonObject streamJson = new JsonObject(sVar.getVal());
            JsonObject nodeJson = streamJson.getJsonObject("node");
            JsonObject valueJson = nodeJson.getJsonObject("value");
            JsonObject nodes = valueJson.getJsonObject("nodes");
            
            result = nodes.containsKey(node);
        } catch (IOException e) {
            logger.error("isUpstreamNodeExist: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    PUT     | /apisix/admin/services/{id}   | {...}                                   | create service by identified id                |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     *  
     *  {
     *    "id": "1",                # id
     *    "desc": "hello world",    # service 描述
     *    "name": "test_svc",       # service 名称
     *    "plugins": {},            # 指定 service 绑定的插件
     *    "upstream_id": "1",       # upstream 对象在 etcd 中的 id ，建议使用此值
     *    "upstream": {},           # upstream 信息对象，不建议使用
     *    "enable_websocket": true, # 启动 websocket 功能
     *  }
     */
    public boolean addService(String serviceID, String upstreamID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_SERVICES, serviceID);
        SVarObject sVar = new SVarObject();
        
        JsonObject json = new JsonObject();
        JsonObject plugins = new JsonObject();
        
        json.put("plugins", plugins);
        json.put("upstream_id", upstreamID);
        
        boolean res = false;
        try {
            res = HttpCommonTools.putData(url, HEADERS, json.toString(), sVar);
            if (!res) {
                logger.error("addService: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("addService: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   DELETE   | /apisix/admin/services/{id}   | NULL                                    | delete identified service by id                |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean delService(String serviceID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_SERVICES, serviceID);
        SVarObject sVar = new SVarObject();
        
        boolean res = false;
        try {
            res = HttpCommonTools.deleteData(url, HEADERS, "", sVar);
            if (!res) {
                logger.error("delService: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("delService: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }

    /**
     *  +---METHOD---+------------REQ URI-------------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   PATCH    | /apisix/admin/services/{id}/{path}   | NULL                                    | delete identified service by id                |
     *  +------------+--------------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean updService(String serviceID, String path, String key, Map<String, Object> attrMap) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s/%s", rootUrl, API_SERVICES, serviceID, path);
        SVarObject sVar = new SVarObject();
        
        JsonObject attributes = new JsonObject();
        Set<Entry<String, Object>> entrySet = attrMap.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            String attrKey = entry.getKey();
            Object attrVal = entry.getValue();
            if ((attrKey == null || attrKey.isEmpty())
                    || attrVal == null) {
                continue;
            }
            
            attributes.put(attrKey, attrVal);
        }
        
        JsonObject json = new JsonObject();
        json.put(key, attributes);
        
        boolean res = false;
        try {
            res = HttpCommonTools.patchData(url, HEADERS, json.toString(), sVar);
            if (!res) {
                logger.error("updService: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("updService: {} fail, {}", url, e.getMessage(), e);
        }
        
        return res;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/services/{id}   | NULL                                    | get service by id                              |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean isServiceExist(String serviceID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_SERVICES, serviceID);
        SVarObject sVar = new SVarObject();
        boolean result = false;
        
        try {
            if (!HttpCommonTools.getData(url, HEADERS, sVar))
                return false;
            
            String info = sVar.getVal().trim();
            if (info == null || info.isEmpty()) {
                return false;
            }
            
            JsonObject serviceJson = new JsonObject(info);
            JsonObject node = serviceJson.getJsonObject("node");
            JsonObject value = node.getJsonObject("value");
            String id = value.getString("id");
            
            result = id.equals(serviceID);
        } catch (IOException e) {
            logger.error("isServiceExist: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/services/{id}   | NULL                                    | get service by id                              |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean getServiceByID(String serviceID, SVarObject sVar) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_SERVICES, serviceID);
        boolean result = false;
        
        try {
            result = HttpCommonTools.getData(url, HEADERS, sVar);
        } catch (IOException e) {
            logger.error("getServiceByID: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/routes          | NULL                                    | get all route list                             |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean getRoutes(SVarObject sVar) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s", rootUrl, API_ROUTES);
        boolean result = false;
        
        try {
            result = HttpCommonTools.getData(url, HEADERS, sVar);
            if (!result) {
                logger.error("getRoutes: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("getRoutes: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    GET     | /apisix/admin/routes/{id}     | NULL                                    | get the identified route by id                 |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean getRouteByID(String routeID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_ROUTES, routeID);
        SVarObject sVar = new SVarObject();
        boolean result = false;
        
        try {
            result = HttpCommonTools.getData(url, HEADERS, sVar);
            if (!result) {
                logger.error("getRouteByID: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("getRouteByID: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |    PUT     | /apisix/admin/routes/{id}     | {...}                                   | get all route list                             |
     *  +------------+-------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean addRoute(String routeID, String serviceID, String uri, String[] methods) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_ROUTES, routeID);
        SVarObject sVar = new SVarObject();
        
        JsonArray jsonMethods = new JsonArray();
        for (String method : methods) {
            jsonMethods.add(method);
        }
        
        JsonObject route = new JsonObject();
        route.put("id", routeID);
        route.put("name", routeID);
        route.put("uri", uri);
        route.put("methods", methods);
        route.put("service_id", serviceID);
        
        // "remote_addrs": ["127.0.0.1"]
        // "plugins": {},
        // "priority": 0,
        
        boolean result = false;
        try {
            result = HttpCommonTools.putData(url, HEADERS, route.toString(), sVar);
            if (!result) {
                logger.error("addRoute: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("addRoute: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI----------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   DELETE   | /apisix/admin/routes/{id}         | NULL                                    | delete the identified route by id              |
     *  +------------+-----------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean delRouteByID(String routeID) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s", rootUrl, API_ROUTES, routeID);
        SVarObject sVar = new SVarObject();
        boolean result = false;
        
        try {
            result = HttpCommonTools.deleteData(url, HEADERS, "", sVar);
            if (!result) {
                logger.error("delRouteByID: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("delRouteByID: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     *  +---METHOD---+------------REQ URI----------------+-----------------REQ BODY----------------+-------------------instructions-----------------+
     *  |   PATCH    | /apisix/admin/routes/{id}/{node}  | NULL                                    | update the identified route by id              |
     *  +------------+-----------------------------------+-----------------------------------------+------------------------------------------------+
     */
    public boolean updRoute(String routeID, String node, Object data) {
        String rootUrl = getGateway();
        String url = String.format("%s/%s/%s/%s", rootUrl, API_ROUTES, routeID, node);
        SVarObject sVar = new SVarObject();
        boolean result = false;
        
        try {
            result = HttpCommonTools.patchData(url, HEADERS, data.toString(), sVar);
            if (!result) {
                logger.error("updRouteMethod: {} fail ......", url);
            }
        } catch (IOException e) {
            logger.error("delRouteByID: {} fail, {}", url, e.getMessage(), e);
        }
        
        return result;
    }

}
