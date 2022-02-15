package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdCPU;
import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

public class CPUOperator {
    
    public static void process(String servIP, List<CollectdRaw> list) {

        List<CollectdRaw> currList = new ArrayList<CollectdRaw>();
        
        CollectdRaw user      = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_USER);
        CollectdRaw system    = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_SYSTEM);
        CollectdRaw wait      = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_WAIT);
        CollectdRaw interrupt = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_INTERRUPT);
        CollectdRaw idle      = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_IDLE);
        CollectdRaw nice      = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_NICE);
        CollectdRaw softirq   = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_SOFTIRQ);
        CollectdRaw steal     = BaseOperator.getCurrentNodeByTypeInstance(list, FixDefs.CPU_STEAL);
        
        if (user != null) currList.add(user);
        if (system != null) currList.add(system);
        if (wait != null) currList.add(wait);
        if (interrupt != null) currList.add(interrupt);
        if (idle != null) currList.add(idle);
        if (nice != null) currList.add(nice);
        if (softirq != null) currList.add(softirq);
        if (steal != null) currList.add(steal);
        
        long ts = 0;
        
        // CPU 
        // dstypes : "derive"
        // type_instance : "user", "system", "wait", "interrupt", "idle", "nice", "softirq", "steal"
        List<Long> userCPUList      = new ArrayList<Long>();
        List<Long> systemCPUList    = new ArrayList<Long>();
        List<Long> waitCPUList      = new ArrayList<Long>();
        List<Long> interruptCPUList = new ArrayList<Long>();
        List<Long> idleCPUList      = new ArrayList<Long>();
        List<Long> niceCPUList      = new ArrayList<Long>();
        List<Long> softirqCPUList   = new ArrayList<Long>();
        List<Long> stealCPUList     = new ArrayList<Long>();
        
        for (CollectdRaw collectRaw : currList) {
            String key = String.format("%s_%s_%s_%s", collectRaw.getHost(), collectRaw.getType(),
                    collectRaw.getTypeInstance(), collectRaw.getPluginInstance());
            
            // get last specified data from redis
            List<String> values = BaseOperator.getLastCollectdRawValue(key, FixDefs.COLLECTD_VALUES, FixDefs.COLLECTD_TIME);
            long valDiff = 0;
            long oldTs = 0;
            if (values != null) {
                String sVal = values.get(0);
                String sTs = values.get(1);
                
                if (sVal != null && sTs != null) {
                    long lastValue = Long.valueOf(sVal);
                    valDiff = collectRaw.getValues()[0] - lastValue;
                    oldTs = Long.valueOf(sTs);
                }
            }
            
            // save current to redis
            if (collectRaw.getTime() > oldTs) {
                Map<String, String> hash = new HashMap<String, String>();
                hash.put(FixDefs.COLLECTD_VALUES, String.valueOf(collectRaw.getValues()[0]));
                hash.put(FixDefs.COLLECTD_TIME,   String.valueOf(collectRaw.getTime()));
                BaseOperator.setCurrentCollectdRawValue(key, hash);
                
                switch (collectRaw.getTypeInstance()) {
                case FixDefs.CPU_USER:
                    userCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_SYSTEM:
                    systemCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_WAIT:
                    waitCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_INTERRUPT:
                    interruptCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_IDLE:
                    idleCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_NICE:
                    niceCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_SOFTIRQ:
                    softirqCPUList.add(valDiff);
                    break;
                case FixDefs.CPU_STEAL:
                    stealCPUList.add(valDiff);
                    break;
                default:
                    break;
                }
                
                ts = collectRaw.getTime();
            }
        }
        
        int  precision       = BaseOperator.PRECISION_CPU;
        long sumUserCPU      = BaseOperator.getSumValue(userCPUList);
        long sumSystemCPU    = BaseOperator.getSumValue(systemCPUList);
        long sumWaitCPU      = BaseOperator.getSumValue(waitCPUList);
        long sumInterruptCPU = BaseOperator.getSumValue(interruptCPUList);
        long sumIdleCPU      = BaseOperator.getSumValue(idleCPUList);
        long sumNiceCPU      = BaseOperator.getSumValue(niceCPUList);
        long sumSoftirqCPU   = BaseOperator.getSumValue(softirqCPUList);
        long sumStealCPU     = BaseOperator.getSumValue(stealCPUList);
        
        long sum             = sumUserCPU + sumSystemCPU + sumWaitCPU + sumInterruptCPU + sumIdleCPU + sumNiceCPU + sumSoftirqCPU + sumStealCPU;
        
        float userCPU = 0.0f, systemCPU = 0.0f, waitCPU = 0.0f, interruptCPU = 0.0f;
        float idleCPU = 0.0f, niceCPU = 0.0f, softirqCPU = 0.0f, stealCPU = 0.0f;
        
        if (sumIdleCPU != 0 && sum != 0) {
            userCPU      = BaseOperator.roundDoubleAsFloat(100 * ((double) sumUserCPU / sum),      precision);
            systemCPU    = BaseOperator.roundDoubleAsFloat(100 * ((double) sumSystemCPU / sum),    precision);
            waitCPU      = BaseOperator.roundDoubleAsFloat(100 * ((double) sumWaitCPU / sum),      precision);
            interruptCPU = BaseOperator.roundDoubleAsFloat(100 * ((double) sumInterruptCPU / sum), precision);
            idleCPU      = BaseOperator.roundDoubleAsFloat(100 * ((double) sumIdleCPU / sum),      precision);
            niceCPU      = BaseOperator.roundDoubleAsFloat(100 * ((double) sumNiceCPU / sum),      precision);
            softirqCPU   = BaseOperator.roundDoubleAsFloat(100 * ((double) sumSoftirqCPU / sum),   precision);
            stealCPU     = BaseOperator.roundDoubleAsFloat(100 * ((double) sumStealCPU / sum),     precision);
        }
        
        CollectdCPU collectdCPU = new CollectdCPU(servIP, ts, userCPU, systemCPU, waitCPU, interruptCPU, idleCPU, niceCPU, softirqCPU, stealCPU);
        collectdCPU.storeData();
    }

}
