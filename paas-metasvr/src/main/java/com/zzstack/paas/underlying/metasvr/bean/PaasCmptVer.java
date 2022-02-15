package com.zzstack.paas.underlying.metasvr.bean;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class PaasCmptVer extends BeanMapper {
    
    private String servType;
    private List<String> versionList;
    
    public PaasCmptVer(String servType) {
        super();
        this.servType = servType;
        versionList = new ArrayList<String>();
    }
    
    public PaasCmptVer(String servType, List<String> versionList) {
        super();
        this.servType = servType;
        this.versionList = versionList;
    }

    public String getServType() {
        return servType;
    }

    public void setServType(String servType) {
        this.servType = servType;
    }

    public List<String> getVersionList() {
        return versionList;
    }

    public void setVersionList(List<String> versionList) {
        this.versionList = versionList;
    }
    
    public int getVerionCnt() {
        return versionList.size();
    }
    
    public void addVersion(String version) {
        if (versionList.contains(version))
            return;
        
        versionList.add(version);
    }
    
    public void delVersion(String version) {
        if (!versionList.contains(version))
            return;
        
        versionList.remove(version);
    }
    
    public JsonObject toJsonObject() {
        StringBuilder sb = new StringBuilder();
        for (String s : versionList) {
            if (sb.length() > 0) {
                sb.append(CONSTS.PATH_COMMA);
            }
            sb.append(s);
        }
        
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_SERV_TYPE, servType);
        json.put(FixHeader.HEADER_VERSION, sb.toString());

        return json;
    }

}
