package com.zzstack.paas.underlying.metasvr.autocheck.probe;

public interface CmptProber {
    
    boolean doCheck(final String instID, final String servType);
    
}
