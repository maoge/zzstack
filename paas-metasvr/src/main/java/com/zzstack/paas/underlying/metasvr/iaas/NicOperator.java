package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdNic;
import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

public class NicOperator {

    public static void process(String servIP, List<CollectdRaw> list) {
        // 多张网卡情况下需要先按网卡名字切分
        Map<String, List<CollectdRaw>> nicCollectdDataMap = BaseOperator.splitByPluginInstance(list);
        Set<Entry<String, List<CollectdRaw>>> entrySet = nicCollectdDataMap.entrySet();
        
        
        for (Entry<String, List<CollectdRaw>> entry : entrySet) {
            List<CollectdRaw> subList = entry.getValue();
            processItem(servIP, subList);
        }
    }

    private static void processItem(String servIP, List<CollectdRaw> list) {
        // NIC 
        // dstypes : "derive"
        // type_instance : if_packets[tx,rx], if_octets[tx,rx], if_dropped[tx,rx], if_errors[tx, rx]
        
        long packetsRx = 0, packetsTx = 0, octetsRx = 0, octetsTx = 0;
        long droppedRx = 0, droppedTx = 0, errorsRx = 0, errorsTx = 0;
        
        CollectdRaw packetsRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.IF_PACKETS);
        CollectdRaw octetsRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.IF_OCTETS);
        CollectdRaw droppedRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.IF_DROPPED);
        CollectdRaw errorsRaw = BaseOperator.getCurrentNodeByType(list, FixDefs.IF_ERRORS);
        
        if (octetsRaw == null)
            return;
        
        String nicName = "";
        
        List<CollectdRaw> currentList = new ArrayList<CollectdRaw>(4);
        if (packetsRaw != null)
            currentList.add(packetsRaw);
        
        if (octetsRaw != null)
            currentList.add(octetsRaw);
        
        if (droppedRaw != null)
            currentList.add(droppedRaw);
        
        if (errorsRaw != null)
            currentList.add(errorsRaw);
        
        long ts = 0;
        
        for (CollectdRaw collectRaw : currentList) {
            String key = String.format("%s_%s_%s", collectRaw.getHost(), collectRaw.getPluginInstance(), collectRaw.getType());
            nicName = collectRaw.getPluginInstance();
            
            // get last specified data from redis
            List<String> values = BaseOperator.getLastCollectdRawValue(key, FixDefs.COLLECTD_VALUES, FixDefs.COLLECTD_TIME);
            long rxDiff = 0, txDiff = 0;
            long rx = 0, tx = 0;
            long timeDiff = 0;
            long oldTs = 0;
            if (values != null) {
                String sVal = values.get(0);
                String sTs = values.get(1);
                
                if (sVal != null && sTs != null) {
                    String[] valueArr = sVal.split(FixDefs.PATH_COMMA);
                    long lastRx = Long.valueOf(valueArr[0]);
                    long lastTx = Long.valueOf(valueArr[1]);
                    oldTs = Long.valueOf(sTs);
                    
                    rxDiff = collectRaw.getValues()[0] - lastRx;
                    txDiff = collectRaw.getValues()[1] - lastTx;
                    
                    timeDiff = (collectRaw.getTime() - oldTs) / 1000;
                    
                    rx = rxDiff / timeDiff;
                    tx = txDiff / timeDiff;
                }
            }
            
            // save current to redis
            if (collectRaw.getTime() > oldTs) {
                Map<String, String> hash = new HashMap<String, String>();
                hash.put(FixDefs.COLLECTD_VALUES, String.format("%d,%d", collectRaw.getValues()[0], collectRaw.getValues()[1]));
                hash.put(FixDefs.COLLECTD_TIME,   String.valueOf(collectRaw.getTime()));
                BaseOperator.setCurrentCollectdRawValue(key, hash);
                
                switch (collectRaw.getType()) {
                case FixDefs.IF_PACKETS:
                    packetsRx = rx;
                    packetsTx = tx;
                    break;
                case FixDefs.IF_OCTETS:
                    octetsRx = rx;
                    octetsTx = tx;
                    break;
                case FixDefs.IF_DROPPED:
                    droppedRx = rx;
                    droppedTx = tx;
                    break;
                case FixDefs.IF_ERRORS:
                    errorsRx = rx;
                    errorsTx = tx;
                    break;
                default:
                    break;
                }
                
                ts = collectRaw.getTime();
            }
        }
        
        CollectdNic collectdNic = new CollectdNic(servIP, ts, nicName, packetsRx, packetsTx, octetsRx, octetsTx,
                droppedRx, droppedTx, errorsRx, errorsTx);
        collectdNic.storeData();
    }

}
