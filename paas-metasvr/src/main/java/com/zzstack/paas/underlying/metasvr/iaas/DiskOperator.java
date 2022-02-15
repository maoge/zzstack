package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdDisk;
import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

public class DiskOperator {
    
    public static void process(String servIP, List<CollectdRaw> list) {
        List<CollectdRaw> currList = new ArrayList<CollectdRaw>();
        
        CollectdRaw diskOpsRaw    = BaseOperator.getCurrentNodeByType(list, FixDefs.DISK_OPS);
        CollectdRaw diskOctetsRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.DISK_OCTETS);
        CollectdRaw diskTimeRaw   = BaseOperator.getCurrentNodeByType(list, FixDefs.DISK_TIME);
        CollectdRaw diskIoTimeRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.DISK_IO_TIME);
        CollectdRaw diskMergedRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.DISK_MERGED);
        
        if (diskOpsRaw == null || diskOctetsRaw == null)
            return;
        
        if (diskOpsRaw != null) currList.add(diskOpsRaw);
        if (diskOctetsRaw != null) currList.add(diskOctetsRaw);
        if (diskTimeRaw != null) currList.add(diskTimeRaw);
        if (diskIoTimeRaw != null) currList.add(diskIoTimeRaw);
        if (diskMergedRaw != null) currList.add(diskMergedRaw);
        
        long ts = 0;
        
        long diskOpsRead = 0, diskOpsWrite = 0;
        long diskOctetsRead = 0, diskOctetsWrite = 0;
        long diskTimeRead = 0, diskTimeWrite = 0;
        long diskIoTimeRead = 0, diskIoTimeWrite = 0;
        long diskMergedRead = 0, diskMergedWrite = 0;
        String diskName = "";
        
        // DISK
        // dstypes : "derive"
        // type_instance : disk_time["read","write"], disk_ops["read","write"], disk_octets["read","write"], disk_merged["read","write"], disk_io_time["read","write"]
        for (CollectdRaw collectRaw : currList) {
            String key = String.format("%s_%s_%s_%s", collectRaw.getHost(), collectRaw.getPlugin(),
                    collectRaw.getPluginInstance(), collectRaw.getType());
            
            // get last specified data from redis
            List<String> values = BaseOperator.getLastCollectdRawValue(key, FixDefs.COLLECTD_VALUES, FixDefs.COLLECTD_TIME);
            long readDiff = 0, writeDiff = 0;
            long read = 0, write = 0;
            long oldTs = 0;
            long timeDiff = 0;
            if (values != null && values.size() == 2) {
                String sVal = values.get(0);
                String sTs = values.get(1);
                
                if (sVal != null && sTs != null) {
                    String[] valueArr = sVal.split(FixDefs.PATH_COMMA);
                    long lastRead = Long.valueOf(valueArr[0]);
                    long lastWrite = Long.valueOf(valueArr[1]);
                    
                    readDiff = collectRaw.getValues()[0] - lastRead;
                    writeDiff = collectRaw.getValues()[1] - lastWrite;
                    
                    oldTs = Long.valueOf(sTs);
                    timeDiff = (collectRaw.getTime() - oldTs) / 1000;
                    
                    read = readDiff / timeDiff;
                    write = writeDiff / timeDiff;
                }
            }
            
            // save current to redis
            if (collectRaw.getTime() > oldTs) {
                Map<String, String> hash = new HashMap<String, String>();
                hash.put(FixDefs.COLLECTD_VALUES, String.format("%d,%d", collectRaw.getValues()[0], collectRaw.getValues()[1]));
                hash.put(FixDefs.COLLECTD_TIME,   String.valueOf(collectRaw.getTime()));
                BaseOperator.setCurrentCollectdRawValue(key, hash);
                
                switch (collectRaw.getTypeInstance()) {
                case FixDefs.DISK_OPS:
                    { diskOpsRead = read; diskOpsWrite = write; }
                    break;
                case FixDefs.DISK_OCTETS:
                    { diskOctetsRead = read; diskOctetsWrite = write; }
                    break;
                case FixDefs.DISK_TIME:
                    { diskTimeRead = read; diskTimeWrite = write; }
                    break;
                case FixDefs.DISK_IO_TIME:
                    { diskIoTimeRead = 0; diskIoTimeWrite = 0; }
                    break;
                case FixDefs.DISK_MERGED:
                    { diskMergedRead = 0; diskMergedWrite = 0; }
                    break;
                default:
                    break;
                }
                
                ts = collectRaw.getTime();
                diskName = collectRaw.getPluginInstance();
            }
        }
        
        CollectdDisk collectdDisk = new CollectdDisk(servIP, ts, diskName, diskOpsRead, diskOpsWrite,
                diskOctetsRead, diskOctetsWrite, diskTimeRead, diskTimeWrite,
                diskIoTimeRead, diskIoTimeWrite, diskMergedRead, diskMergedWrite);
        collectdDisk.storeData();
    }

}
