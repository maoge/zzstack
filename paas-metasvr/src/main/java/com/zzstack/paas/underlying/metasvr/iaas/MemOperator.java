package com.zzstack.paas.underlying.metasvr.iaas;

import java.util.ArrayList;
import java.util.List;

import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdMemory;
import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdRaw;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;

public class MemOperator {

    public static void process(String servIP, List<CollectdRaw> list) {
        // Memory
        // dstypes : "gauge"
        // type_instance : "used", "free", "buffered", "cached", "slab_unrecl", "slab_recl"
        List<CollectdRaw> listMemUsed       = new ArrayList<CollectdRaw>();
        List<CollectdRaw> listMemFree       = new ArrayList<CollectdRaw>();
        List<CollectdRaw> listMemBuffered   = new ArrayList<CollectdRaw>();
        List<CollectdRaw> listMemCached     = new ArrayList<CollectdRaw>();
        List<CollectdRaw> listMemSlabUnrecl = new ArrayList<CollectdRaw>();
        List<CollectdRaw> listMemSlabRecll  = new ArrayList<CollectdRaw>();
        
        getMemData(list, listMemUsed, listMemFree, listMemBuffered, listMemCached, listMemSlabUnrecl, listMemSlabRecll);
        
        double used       = BaseOperator.getGaugeData(listMemUsed);
        double free       = BaseOperator.getGaugeData(listMemFree);
        double buffered   = BaseOperator.getGaugeData(listMemBuffered);
        double cached     = BaseOperator.getGaugeData(listMemCached);
        double slabUnrecl = BaseOperator.getGaugeData(listMemSlabUnrecl);
        double slabRecl   = BaseOperator.getGaugeData(listMemSlabRecll);
        int    precision  = BaseOperator.PRECISION_MEM;
        
        float memUsed       = BaseOperator.roundDoubleAsFloat(used / BaseOperator.UNIT_GBYTE,       precision);
        float memFree       = BaseOperator.roundDoubleAsFloat(free / BaseOperator.UNIT_GBYTE,       precision);
        float memBuffered   = BaseOperator.roundDoubleAsFloat(buffered/ BaseOperator.UNIT_GBYTE,    precision);
        float memCached     = BaseOperator.roundDoubleAsFloat(cached / BaseOperator.UNIT_GBYTE,     precision);
        float memSlabUnrecl = BaseOperator.roundDoubleAsFloat(slabUnrecl / BaseOperator.UNIT_GBYTE, precision);
        float memSlabRecl   = BaseOperator.roundDoubleAsFloat(slabRecl / BaseOperator.UNIT_GBYTE,   precision);
        
        CollectdRaw raw = list.get(list.size() - 1);
        long ts = raw.getTime();
        
        CollectdMemory collectdMemory = new CollectdMemory(servIP, ts, memUsed, memFree, memBuffered, memCached, memSlabUnrecl, memSlabRecl);
        collectdMemory.storeData();
    }

    public static void getMemData(List<CollectdRaw> list, List<CollectdRaw> listMemRsed, List<CollectdRaw> listMemFree,
            List<CollectdRaw> listMemBuffered, List<CollectdRaw> listMemCached, List<CollectdRaw> listMemSlabUnrecl,
            List<CollectdRaw> listMemSlabRecll) {
        
        int len = list.size();
        for (int i = 0; i < len; ++i) {
            CollectdRaw rawCPU = list.get(i);
            
            switch (rawCPU.getTypeInstance()) {
            case FixDefs.MEM_USED:
                listMemRsed.add(rawCPU);
                break;
            case FixDefs.MEM_FREE:
                listMemFree.add(rawCPU);
                break;
            case FixDefs.MEM_BUFFERED:
                listMemBuffered.add(rawCPU);
                break;
            case FixDefs.MEM_CACHED:
                listMemCached.add(rawCPU);
                break;
            case FixDefs.MEM_SLAB_UNRECL:
                listMemSlabUnrecl.add(rawCPU);
                break;
            case FixDefs.MEM_SLAB_RECL:
                listMemSlabRecll.add(rawCPU);
                break;
            default:
                break;
            }            
        }
    }

}
