package com.zzstack.paas.underlying.metasvr.threadpool;

import com.zzstack.paas.underlying.metasvr.utils.SysConfig;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WorkerPool {
    private static WorkerPool pool;
    private static Object mtx = null;

    static {
        mtx = new Object();
    }

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int keepAliveTime;
    private final int workQueueLen;
    private final ThreadPoolExecutor poolExecutor;

    public static WorkerPool get() {
        if (pool != null) {
            return pool;
        }

        synchronized (mtx) {
            if (pool == null) {
                pool = new WorkerPool();
            }
        }

        return WorkerPool.pool;
    }

    private WorkerPool() {
        corePoolSize = SysConfig.get().getThreadPoolCoreSize();
        maxPoolSize = SysConfig.get().getThreadPoolMaxSize();
        keepAliveTime = SysConfig.get().getThreadPoolKeepaliveTime();
        workQueueLen = SysConfig.get().getThreadPoolWorkQueueLen();

        poolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(workQueueLen));
    }

    public static void release() {
        synchronized (mtx) {
            if (WorkerPool.pool != null) {
                WorkerPool.pool.destroy();
                WorkerPool.pool = null;
            }
        }
    }

    public boolean isBusy() {
        return poolExecutor.getQueue().size() > 2 * poolExecutor.getMaximumPoolSize();
    }

    public void execute(Runnable command) {
        poolExecutor.execute(command);
    }

    public int getWorkQueueSize() {
        return poolExecutor.getQueue().size();
    }

    private void destroy() {
        poolExecutor.shutdown();
    }

}
