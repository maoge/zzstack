package com.zzstack.paas.underlying.metasvr.autocheck;

import com.zzstack.paas.underlying.metasvr.autocheck.probe.CmptProber;

public class CmptStatusChecker implements Runnable {
    
    private String servInstId;
    private String servType;
    private CmptProber prober;
    
    public CmptStatusChecker(String servInstId, String servType, CmptProber prober) {
        this.servInstId = servInstId;
        this.servType = servType;
        this.prober = prober;
    }

    @Override
    public void run() {
        prober.doCheck(servInstId, servType);
    }

}
