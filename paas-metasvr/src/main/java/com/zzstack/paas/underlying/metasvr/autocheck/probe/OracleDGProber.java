package com.zzstack.paas.underlying.metasvr.autocheck.probe;

public class OracleDGProber extends BaseProber implements CmptProber {

    public OracleDGProber(String servInstID, String servType) {
        super(servInstID, servType);
    }

    @Override
    public boolean doCheck(final String instID, final String servType) {
        return false;
    }

}
