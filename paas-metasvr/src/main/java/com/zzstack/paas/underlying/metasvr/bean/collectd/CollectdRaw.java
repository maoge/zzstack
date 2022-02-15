package com.zzstack.paas.underlying.metasvr.bean.collectd;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectdRaw {

    private String host;
    private String dstype;
    private String plugin;
    private String pluginInstance;
    private String type;
    private String typeInstance;
    private long[] values;
    private int    interval;
    private long   time;

    public CollectdRaw(String host, String dstype, String plugin, String pluginInstance, String type, String typeInstance, long[] values, int interval, long time) {
        super();
        this.host           = host;
        this.dstype         = dstype;
        this.plugin         = plugin;
        this.pluginInstance = pluginInstance;
        this.type           = type;
        this.typeInstance   = typeInstance;
        this.values         = values;
        this.interval       = interval;
        this.time           = time;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDstype() {
        return dstype;
    }

    public void setDstype(String dstype) {
        this.dstype = dstype;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getPluginInstance() {
        return pluginInstance;
    }

    public void setPluginInstance(String pluginInstance) {
        this.pluginInstance = pluginInstance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTypeInstance() {
        return typeInstance;
    }

    public void setTypeInstance(String typeInstance) {
        this.typeInstance = typeInstance;
    }

    public long[] getValues() {
        return values;
    }

    public void setValue(long[] values) {
        this.values = values;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public static CollectdRaw parseFromJsonObject(JsonObject obj) {
        // {"values":[42397],"dstypes":["derive"],"dsnames":["value"],
        //  "time":1638379249.486,"interval":10.000,"host":"paas-133","plugin":"cpu",
        //  "plugin_instance":"0","type":"cpu","type_instance":"system"}
        
        String host = obj.getString(FixDefs.COLLECTD_HOST);
        String dstype = obj.getString(FixDefs.COLLECTD_DSTYPES);
        String plugin = obj.getString(FixDefs.COLLECTD_PLUGIN);
        String pluginInstance = obj.getString(FixDefs.COLLECTD_PLUGIN_INSTANCE);
        String type = obj.getString(FixDefs.COLLECTD_TYPE);
        String typeInstance = obj.getString(FixDefs.COLLECTD_TYPE_INSTANCE);
        int    interval = obj.getInteger(FixDefs.COLLECTD_INTERVAL);
        
        double dtime = obj.getLong(FixDefs.COLLECTD_TIME) * 1000;
        long   time = (long) dtime;
        
        JsonArray valuesArray = obj.getJsonArray(FixDefs.COLLECTD_VALUES);
        int size = valuesArray.size();
        long[] values = new long[size];
        for (int i = 0; i < size; ++i) {
            values[i] = valuesArray.getLong(i);
        }
        
        return new CollectdRaw(host, dstype, plugin, pluginInstance, type, typeInstance, values, interval, time);
    }
    
    public static CollectdRaw parseFromJsonString(String s) {
        JsonObject obj = new JsonObject(s);
        
        String host = obj.getString(FixDefs.COLLECTD_HOST);
        String dstype = obj.getString(FixDefs.COLLECTD_DSTYPES);
        String plugin = obj.getString(FixDefs.COLLECTD_PLUGIN);
        String pluginInstance = obj.getString(FixDefs.COLLECTD_PLUGIN_INSTANCE);
        String type = obj.getString(FixDefs.COLLECTD_TYPE);
        String typeInstance = obj.getString(FixDefs.COLLECTD_TYPE_INSTANCE);
        int    interval = obj.getInteger(FixDefs.COLLECTD_INTERVAL);
        long   time = obj.getLong(FixDefs.COLLECTD_TIME);
        
        JsonArray valuesArray = obj.getJsonArray(FixDefs.COLLECTD_VALUES);
        int size = valuesArray.size();
        long[] values = new long[size];
        for (int i = 0; i < size; ++i) {
            values[i] = valuesArray.getLong(0);
        }
        
        return new CollectdRaw(host, dstype, plugin, pluginInstance, type, typeInstance, values, interval, time);
    }
    
    public String toJsonString() {
        JsonObject obj = new JsonObject();
        obj.put(FixDefs.COLLECTD_HOST,            host);
        obj.put(FixDefs.COLLECTD_DSTYPES,         dstype);
        obj.put(FixDefs.COLLECTD_PLUGIN,          plugin);
        obj.put(FixDefs.COLLECTD_PLUGIN_INSTANCE, pluginInstance);
        obj.put(FixDefs.COLLECTD_TYPE,            type);
        obj.put(FixDefs.COLLECTD_TYPE_INSTANCE,   typeInstance);
        obj.put(FixDefs.COLLECTD_INTERVAL,        interval);
        obj.put(FixDefs.COLLECTD_TIME,            time);
        
        JsonArray valuesArr = new JsonArray();
        for (int i = 0; i < values.length; ++i) {
            valuesArr.add(values[i]);
        }
        obj.put(FixDefs.COLLECTD_VALUES,          valuesArr);
        
        return obj.toString();
    }

    public static List<CollectdRaw> parseFromJsonArray(JsonArray arry) {
        int len = arry.size();
        List<CollectdRaw> list = new ArrayList<CollectdRaw>(len);
        
        for (int i = 0; i < len; ++i) {
            JsonObject obj = arry.getJsonObject(i);
            CollectdRaw cpu = parseFromJsonObject(obj);
            list.add(cpu);
        }
        
        return list;
    }
    
}
