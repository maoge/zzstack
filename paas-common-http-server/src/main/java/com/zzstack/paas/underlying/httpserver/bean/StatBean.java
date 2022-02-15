package com.zzstack.paas.underlying.httpserver.bean;

public class StatBean {

    private volatile long privTotalCnt;// 初始接口调用次数
    private volatile long currTotalCnt;// 最终接口调用次数

    private long tps;

    public StatBean() {
        privTotalCnt = 0L;
        currTotalCnt = 0L;
        tps = 0L;
    }

    // 计数
    public void incCnt() {
        currTotalCnt++;
    }

    public void computeTPS(long diffTS) {
        long diffCnt = currTotalCnt - privTotalCnt;
        tps = diffCnt * 1000 / diffTS;
        privTotalCnt = currTotalCnt;
    }

    public long getTps() {
        return tps;
    }

    public void setTps(long tps) {
        this.tps = tps;
    }

    public long getPrivTotalCnt() {
        return privTotalCnt;
    }

    public void setPrivTotalCnt(long privTotalCnt) {
        this.privTotalCnt = privTotalCnt;
    }

    public long getCurrTotalCnt() {
        return currTotalCnt;
    }

    public void setCurrTotalCnt(long currTotalCnt) {
        this.currTotalCnt = currTotalCnt;
    }

}
