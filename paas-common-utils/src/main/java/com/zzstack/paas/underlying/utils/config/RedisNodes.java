package com.zzstack.paas.underlying.utils.config;

import java.util.Map;

public class RedisNodes {

    public String id;
    public int weight;
    public String[] nodeAddresses;
    public String instId;
    public Map<String, String> instMap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String[] getNodeAddresses() {
        return nodeAddresses;
    }

    public void setNodeAddresses(String[] nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public Map<String, String> getInstMap() {
        return instMap;
    }

    public void setInstMap(Map<String, String> instMap) {
        this.instMap = instMap;
    }
}
