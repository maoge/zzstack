package com.zzstack.paas.underlying.collectd.global;

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.collectd.constants.CollectdConstants;
import com.zzstack.paas.underlying.collectd.runner.CollectRunner;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;
import com.zzstack.paas.underlying.sdk.PaasSDK;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;

import io.vertx.core.json.JsonObject;

public class CollectdGlobalData {

    private static Logger logger = LoggerFactory.getLogger(CollectdGlobalData.class);

    private PaasSDK sdk = null;
    private JsonObject servTopo = null;

    private HttpServerMarshell serverMarshell = null;

    private CollectRunner collectRunner = null;

    private static CollectdGlobalData theInstance = null;
    private static ReentrantLock intanceLock = null;

    static {
        intanceLock = new ReentrantLock();
    }

    public CollectdGlobalData() {
        sdk = new PaasSDK(CollectdConstants.metaSvrUrl, CollectdConstants.metaSvrUsr, CollectdConstants.metaSvrPasswd);
        init();
    }

    public static CollectdGlobalData get() {
        try {
            intanceLock.lock();
            if (theInstance != null) {
                return theInstance;
            } else {
                theInstance = new CollectdGlobalData();
                theInstance.init();
            }
        } finally {
            intanceLock.unlock();
        }

        return theInstance;
    }

    public static void destroy() {
        try {
            intanceLock.lock();
            if (theInstance != null) {
                if (theInstance.serverMarshell != null) {
                    theInstance.serverMarshell.destroy();
                    theInstance.serverMarshell = null;
                }

                if (theInstance.collectRunner != null) {
                    theInstance.collectRunner.destroy();
                    theInstance.collectRunner = null;
                }

                theInstance.servTopo = null;
                theInstance.sdk = null;

                theInstance = null;
            }
        } finally {
            intanceLock.unlock();
        }
    }

    public void init() {
        loadServTopo();
    }

    private void loadServTopo() {
        String topStr = null;
        try {
            topStr = sdk.loadPaasService(CollectdConstants.servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadServTopo fail, {}", e.getMessage());
        }

        if (topStr == null || topStr.isEmpty())
            return;

        servTopo = new JsonObject(topStr);
    }

    public JsonObject getServTopo() {
        return servTopo;
    }

    public HttpServerMarshell getServerMarshell() {
        return serverMarshell;
    }

    public void setServerMarshell(HttpServerMarshell serverMarshell) {
        this.serverMarshell = serverMarshell;
    }

    public CollectRunner getCollectRunner() {
        return collectRunner;
    }

    public void setCollectRunner(CollectRunner collectRunner) {
        this.collectRunner = collectRunner;
    }

    public PaasSDK getSdk() {
        return sdk;
    }

    public void setSdk(PaasSDK sdk) {
        this.sdk = sdk;
    }


}
