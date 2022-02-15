package com.zzstack.paas.underlying.metasvr.iaas;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdPushData;

public class Dashboard {

    private PhysicalResMonitor physicalResMon;
    
    private static volatile Dashboard INSTANCE;
    
    private Dashboard() {
        super();
        this.physicalResMon = new PhysicalResMonitor();
    }
    
    public static Dashboard get() {
        if (INSTANCE == null) {
            synchronized (Dashboard.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Dashboard();
                }
            }
        }

        return INSTANCE;
    }
    
    public void pushCollectdMonitorData(CollectdPushData data) {
        physicalResMon.push(data);
    }

}
