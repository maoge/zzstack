package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasServer extends BeanMapper {

    private String serverIp;
    private String serverName;

    public PaasServer() {
        super();
    }

    /**
     * @param serverIp
     * @param serverName
     */
    public PaasServer(String serverIp, String serverName) {
        super();
        this.serverIp = serverIp;
        this.serverName = serverName;
    }

    public static PaasServer convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty())
            return null;

        String serverIp = getFixDataAsString(mapper, FixHeader.HEADER_SERVER_IP);
        String serverName = getFixDataAsString(mapper, FixHeader.HEADER_SERVER_NAME);

        return new PaasServer(serverIp, serverName);
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public String toString() {
        return "PaasServer [serverIp=" + serverIp + ", serverName=" + serverName + "]";
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_SERVER_IP, serverIp);
        json.put(FixHeader.HEADER_SERVER_NAME, serverName);

        return json;
    }
    
    public static PaasServer fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        String serverIp = json.getString(FixHeader.HEADER_SERVER_IP);
        String serverName = json.getString(FixHeader.HEADER_SERVER_NAME);
        return new PaasServer(serverIp, serverName);
    }

}
