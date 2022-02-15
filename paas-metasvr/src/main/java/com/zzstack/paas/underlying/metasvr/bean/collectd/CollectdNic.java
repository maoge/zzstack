package com.zzstack.paas.underlying.metasvr.bean.collectd;

import com.zzstack.paas.underlying.metasvr.dataservice.dao.HostStatisticDao;

public class CollectdNic implements ICollectd {

    private String servIP;
    private long   ts;
    private String nicName;
    
    private long packetsRx;
    private long packetsTx;
    
    private long octetsRx;
    private long octetsTx;
    
    private long droppedRx;
    private long droppedTx;
    
    private long errorsRx;
    private long errorsTx;
    
    public CollectdNic(String servIP, long ts, String nicName, long packetsRx, long packetsTx, long octetsRx, long octetsTx, long droppedRx, long droppedTx,
            long errorsRx, long errorsTx) {
        super();
        this.servIP = servIP;
        this.ts = ts;
        this.nicName = nicName;
        this.packetsRx = packetsRx;
        this.packetsTx = packetsTx;
        this.octetsRx = octetsRx;
        this.octetsTx = octetsTx;
        this.droppedRx = droppedRx;
        this.droppedTx = droppedTx;
        this.errorsRx = errorsRx;
        this.errorsTx = errorsTx;
    }

    public String getServIP() {
        return servIP;
    }

    public void setServIP(String servIP) {
        this.servIP = servIP;
    }

    public long getTs() {
        return ts;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }
    
    public String getNicName() {
        return nicName;
    }

    public void setNicName(String nicName) {
        this.nicName = nicName;
    }

    public long getPacketsRx() {
        return packetsRx;
    }

    public void setPacketsRx(long packetsRx) {
        this.packetsRx = packetsRx;
    }

    public long getPacketsTx() {
        return packetsTx;
    }

    public void setPacketsTx(long packetsTx) {
        this.packetsTx = packetsTx;
    }

    public long getOctetsRx() {
        return octetsRx;
    }

    public void setOctetsRx(long octetsRx) {
        this.octetsRx = octetsRx;
    }

    public long getOctetsTx() {
        return octetsTx;
    }

    public void setOctetsTx(long octetsTx) {
        this.octetsTx = octetsTx;
    }

    public long getDroppedRx() {
        return droppedRx;
    }

    public void setDroppedRx(long droppedRx) {
        this.droppedRx = droppedRx;
    }

    public long getDroppedTx() {
        return droppedTx;
    }

    public void setDroppedTx(long droppedTx) {
        this.droppedTx = droppedTx;
    }

    public long getErrorsRx() {
        return errorsRx;
    }

    public void setErrorsRx(long errorsRx) {
        this.errorsRx = errorsRx;
    }

    public long getErrorsTx() {
        return errorsTx;
    }

    public void setErrorsTx(long errorsTx) {
        this.errorsTx = errorsTx;
    }

    @Override
    public void storeData() {
        HostStatisticDao.saveNic(servIP, ts, nicName, packetsTx, packetsRx, octetsTx, octetsRx, errorsTx, errorsRx,
                droppedTx, droppedRx);
    }

}
