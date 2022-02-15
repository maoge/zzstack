package com.zzstack.paas.underlying.metasvr.bean.collectd;

import com.zzstack.paas.underlying.metasvr.dataservice.dao.HostStatisticDao;

public class CollectdDisk implements ICollectd {

    private String servIP;
    private long   ts;
    private String disk;

    private long   diskOpsRead;
    private long   diskOpsWrite;
    private long   diskOctetsRead;
    private long   diskOctetsWrite;
    private long   diskTimeRead;
    private long   diskTimeWrite;
    private long   diskIoTimeRead;
    private long   diskIoTimeWrite;
    private long   diskMergedRead;
    private long   diskMergedWrite;

    public CollectdDisk(String servIP, long ts, String disk, long diskOpsRead, long diskOpsWrite,
            long diskOctetsRead, long diskOctetsWrite, long diskTimeRead, long diskTimeWrite,
            long diskIoTimeRead, long diskIoTimeWrite, long diskMergedRead, long diskMergedWrite) {
        super();
        this.servIP = servIP;
        this.ts = ts;
        this.disk = disk;
        this.diskOpsRead = diskOpsRead;
        this.diskOpsWrite = diskOpsWrite;
        this.diskOctetsRead = diskOctetsRead;
        this.diskOctetsWrite = diskOctetsWrite;
        this.diskTimeRead = diskTimeRead;
        this.diskTimeWrite = diskTimeWrite;
        this.diskIoTimeRead = diskIoTimeRead;
        this.diskIoTimeWrite = diskIoTimeWrite;
        this.diskMergedRead = diskMergedRead;
        this.diskMergedWrite = diskMergedWrite;
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

    public String getDisk() {
        return disk;
    }

    public void setDisk(String disk) {
        this.disk = disk;
    }

    public long getDiskOpsRead() {
        return diskOpsRead;
    }

    public void setDiskOpsRead(long diskOpsRead) {
        this.diskOpsRead = diskOpsRead;
    }

    public long getDiskOpsWrite() {
        return diskOpsWrite;
    }

    public void setDiskOpsWrite(long diskOpsWrite) {
        this.diskOpsWrite = diskOpsWrite;
    }

    public long getDiskOctetsRead() {
        return diskOctetsRead;
    }

    public void setDiskOctetsRead(long diskOctetsRead) {
        this.diskOctetsRead = diskOctetsRead;
    }

    public long getDiskOctetsWrite() {
        return diskOctetsWrite;
    }

    public void setDiskOctetsWrite(long diskOctetsWrite) {
        this.diskOctetsWrite = diskOctetsWrite;
    }

    public long getDiskTimeRead() {
        return diskTimeRead;
    }

    public void setDiskTimeRead(long diskTimeRead) {
        this.diskTimeRead = diskTimeRead;
    }

    public long getDiskTimeWrite() {
        return diskTimeWrite;
    }

    public void setDiskTimeWrite(long diskTimeWrite) {
        this.diskTimeWrite = diskTimeWrite;
    }

    public long getDiskIoTimeRead() {
        return diskIoTimeRead;
    }

    public void setDiskIoTimeRead(long diskIoTimeRead) {
        this.diskIoTimeRead = diskIoTimeRead;
    }

    public long getDiskIoTimeWrite() {
        return diskIoTimeWrite;
    }

    public void setDiskIoTimeWrite(long diskIoTimeWrite) {
        this.diskIoTimeWrite = diskIoTimeWrite;
    }

    public long getDiskMergedRead() {
        return diskMergedRead;
    }

    public void setDiskMergedRead(long diskMergedRead) {
        this.diskMergedRead = diskMergedRead;
    }

    public long getDiskMergedWrite() {
        return diskMergedWrite;
    }

    public void setDiskMergedWrite(long diskMergedWrite) {
        this.diskMergedWrite = diskMergedWrite;
    }

    @Override
    public void storeData() {
        HostStatisticDao.saveDisk(servIP, ts, disk, diskOpsRead, diskOpsWrite, diskOctetsRead, diskOctetsWrite,
                diskTimeRead, diskTimeWrite, diskIoTimeRead, diskIoTimeWrite, diskMergedRead, diskMergedWrite);
    }
}
