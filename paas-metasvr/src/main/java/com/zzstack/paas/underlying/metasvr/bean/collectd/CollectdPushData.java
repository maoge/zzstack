package com.zzstack.paas.underlying.metasvr.bean.collectd;

public class CollectdPushData {

    private String servIP;
    private String data;

    public CollectdPushData(String servIP, String data) {
        super();
        this.servIP = servIP;
        this.data = data;
    }

    public String getServIP() {
        return servIP;
    }

    public void setServIP(String servIP) {
        this.servIP = servIP;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
