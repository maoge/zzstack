package com.zzstack.paas.underlying.worker;

import java.util.concurrent.atomic.AtomicLong;

public abstract class TaskRunner implements Runnable {
    
    protected AtomicLong normalCnt;
    protected AtomicLong errorCnt;
    protected volatile boolean bRunning = true;
    
    public TaskRunner(AtomicLong normalCnt, AtomicLong errorCnt) {
        super();
        this.normalCnt = normalCnt;
        this.errorCnt = errorCnt;
    }
    
    public void StopRunning() {
        this.bRunning = false;
    }
    
    public boolean isRunning() {
        return bRunning;
    }

}
