package com.zzstack.paas.underlying.metasvr.autocheck.probe;

public class PulsarProber extends BaseProber implements CmptProber {

    public PulsarProber(String servInstID, String servType) {
        super(servInstID, servType);
    }

    @Override
    public boolean doCheck(final String instID, final String servType) {
        return false;
    }

}
