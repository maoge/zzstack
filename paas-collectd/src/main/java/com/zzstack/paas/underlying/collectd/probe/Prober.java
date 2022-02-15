package com.zzstack.paas.underlying.collectd.probe;

import com.zzstack.paas.underlying.utils.exception.PaasCollectException;

import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import io.vertx.core.json.JsonObject;

public interface Prober {

    void doCollect(JsonObject topoJson) throws PaasCollectException;

    void doReport() throws PaasCollectException, PaasSdkException;

    void doAlarm() throws PaasCollectException;

    void doRecover() throws PaasCollectException;

}
