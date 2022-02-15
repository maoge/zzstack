package com.zzstack.paas.underlying.metasvr.global;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.zzstack.paas.underlying.metasvr.bean.LogBean;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class DeployLog {
	
	private static final long EXPIRE_TIME = 10*60*1000;    // 过期时间10分钟
	private ConcurrentHashMap<String, LogBean> logMap;
	
	private static ThreadPoolExecutor expiredExecutor;
	private static BlockingQueue<Runnable> expiredWorksQueue;
	private Runnable expiredCleaner;
	
	private static DeployLog INSTANCE = null;
	
	static {
		expiredWorksQueue = new ArrayBlockingQueue<Runnable>(10);
		expiredExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, expiredWorksQueue, new ThreadPoolExecutor.DiscardPolicy());
	}
	
	public DeployLog() {
		logMap = new ConcurrentHashMap<String, LogBean>();
		expiredCleaner = new ExpiredCleaner();
		expiredExecutor.execute(expiredCleaner);
	}
	
	public static DeployLog get() {
		if (DeployLog.INSTANCE == null) {
			INSTANCE = new DeployLog();
		}
		
		return DeployLog.INSTANCE;
	}
	
	private void putLogBean(final String logKey, LogBean bean) {
		logMap.put(logKey, bean);
	}
	
	private static ConcurrentHashMap<String, LogBean> getLogMap() {
		DeployLog instance = DeployLog.get();
		return instance.logMap;
	}
	
	private static LogBean getLogBean(final String logKey) {
		ConcurrentHashMap<String, LogBean> instance = DeployLog.getLogMap();
		return instance.get(logKey);
	}
	
	public static void pubLog(final String logKey, String log) {
		if (logKey == null || logKey.length() == 0)
			return;
		
		final LogBean logBean = DeployLog.getLogBean(logKey);
		
		String newLog = log.replaceAll(CONSTS.LINE_END, CONSTS.HTML_LINE_END);
		
		if (logBean == null) {
			LogBean newLogBean = new LogBean();
			newLogBean.putLog(newLog);
			
			DeployLog instance = DeployLog.get();
			instance.putLogBean(logKey, newLogBean);
		} else {
			logBean.putLog(newLog);
		}
	}
	
	public static void pubSuccessLog(final String logKey, final String log) {
		if (logKey != null && !logKey.isEmpty()) {
			StringBuffer logSB = new StringBuffer();
			logSB.append(CONSTS.DEPLOY_SINGLE_SUCCESS_BEGIN_STYLE);
			logSB.append(log);
			logSB.append(CONSTS.END_STYLE);
			pubLog(logKey, logSB.toString());
		}
	}
	
	public static void pubFailLog(final String logKey, final String log) {
		if (logKey != null && !logKey.isEmpty()) {
			StringBuffer logSB = new StringBuffer();
			logSB.append(CONSTS.DEPLOY_SINGLE_FAIL_BEGIN_STYLE);
			logSB.append(log);
			logSB.append(CONSTS.END_STYLE);
			pubLog(logKey, logSB.toString());
		}
	}
	
	public static void pubErrorLog(String logKey, String log) {
		if (logKey != null && !logKey.isEmpty()) {
			StringBuffer logSB = new StringBuffer();
			logSB.append(CONSTS.DEPLOY_SINGLE_FAIL_BEGIN_STYLE);
			logSB.append(log);
			logSB.append(CONSTS.END_STYLE);
			pubLog(logKey, logSB.toString());
		}
	}
	
	public static String getLog(String sessionKey) {
		final LogBean logBean = DeployLog.getLogBean(sessionKey);
		if (logBean == null) {
			return "";
		} else {
			return logBean.getLog();
		}
	}
	
	private static void elimExpired() {
		final ConcurrentHashMap<String, LogBean> logMap = DeployLog.getLogMap();
		Iterator<Map.Entry<String, LogBean>> iter = logMap.entrySet().iterator();
		
		long ts = System.currentTimeMillis();
		List<String> removeList = new LinkedList<String>();
		
		while (iter.hasNext()) {
			Map.Entry<String, LogBean> entry = iter.next();
			String key = entry.getKey();
			LogBean logBean = entry.getValue();
			
			if (ts - logBean.getLastTimestamp() > EXPIRE_TIME) {
				logBean.clear();
				removeList.add(key);
			}
		}
		
		if (!removeList.isEmpty()) {
			Iterator<String> removeIter = removeList.iterator();
			while (removeIter.hasNext()) {
				String key = removeIter.next();
				logMap.remove(key);
			}
		}
		
		removeList.clear();
	}
	
	private static class ExpiredCleaner implements Runnable {
		
		public ExpiredCleaner() {
			super();
		}

		@Override
		public void run() {
			DeployLog.elimExpired();
		}
		
	}
	
}
