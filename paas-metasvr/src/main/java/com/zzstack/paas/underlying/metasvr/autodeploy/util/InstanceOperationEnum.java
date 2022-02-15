package com.zzstack.paas.underlying.metasvr.autodeploy.util;

public enum InstanceOperationEnum {
    
    INSTANCE_OPERATION_START("start"),
    INSTANCE_OPERATION_STOP("stop"),
    INSTANCE_OPERATION_RESTART("restart"),
    INSTANCE_OPERATION_UPDATE("update");
    
    private final String action;
    
    private InstanceOperationEnum(String action) {
        this.action = action;
    }
    
    public String getAction() {
        return action;
    }

}
