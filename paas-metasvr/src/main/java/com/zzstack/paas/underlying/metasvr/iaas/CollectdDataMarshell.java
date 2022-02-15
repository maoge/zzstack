package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.List;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectdDataMarshell {

    public static void process(String data, String servIP) {
        Object o = parseAsJson(data);
        if (o instanceof JsonArray) {
            JsonArray array = (JsonArray) o;
            if (array != null && !array.isEmpty()) {
                processArray(servIP, array);
            }
        } else if (o instanceof JsonObject) {
            JsonObject obj = (JsonObject) o;
            processObject(obj);
        }
    }

    public static Object parseAsJson(String data) {
        Object o = null;
        if (data.startsWith(FixDefs.JSON_ARRAY_PREFIX)) {
            o = new JsonArray(data);
        } else if (data.startsWith(FixDefs.JSON_OBJECT_PREFIX)) {
            o = new JsonObject(data);
        }
        return o;
    }

    public static void processArray(String servIP, JsonArray array) {
        List<CollectdRaw> list = CollectdRaw.parseFromJsonArray(array);
        if (list == null)
            return;
        
        List<CollectdRaw> cpuList = BaseOperator.getCurrentNodeListBySpecifiedPlugin(list, FixDefs.PLUGIN_CPU);
        List<CollectdRaw> memList = BaseOperator.getCurrentNodeListBySpecifiedPlugin(list, FixDefs.PLUGIN_MEMORY);
        List<CollectdRaw> nicList = BaseOperator.getCurrentNodeListBySpecifiedPlugin(list, FixDefs.PLUGIN_INTERFACE);
        List<CollectdRaw> diskList = BaseOperator.getCurrentNodeListBySpecifiedPlugin(list, FixDefs.PLUGIN_DISK);
        
        if (cpuList != null)
            CPUOperator.process(servIP, cpuList);
        
        if (memList != null)
            MemOperator.process(servIP, memList);
        
        if (nicList != null)
            NicOperator.process(servIP, nicList);
        
        if (diskList != null)
            DiskOperator.process(servIP, diskList);
    }

    public static void processObject(JsonObject obj) {
        return;
    }

}
