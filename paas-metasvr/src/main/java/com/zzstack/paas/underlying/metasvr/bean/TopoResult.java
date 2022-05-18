package com.zzstack.paas.underlying.metasvr.bean;

import io.vertx.core.json.JsonObject;

public class TopoResult {
    
    private JsonObject servJson;
    private String version;
    private boolean ok;
    
    public TopoResult(JsonObject servJson, String version, boolean ok) {
        super();
        this.servJson = servJson;
        this.version = version;
        this.ok = ok;
    }

    public JsonObject getServJson() {
        return servJson;
    }

    public void setServJson(JsonObject servJson) {
        this.servJson = servJson;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

}
