package com.zzstack.paas.underlying.dbclient;

import java.util.concurrent.locks.ReentrantLock;

public class LongMargin {
	
	private long start;
	private long end;
	
	private volatile long curr;
	
	private ReentrantLock lock = null;
	
	public LongMargin(long start, long end) {
		super();
		this.start = start;
		this.curr = start;
		this.end = end;
		this.lock = new ReentrantLock();
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}
	
	public long getNextId() {
	    if (curr > end)
	        return -1;
	    
	    lock.lock();
	    try {
	        return curr++;
	    } finally {
	        lock.unlock();
	    }
	}

}
