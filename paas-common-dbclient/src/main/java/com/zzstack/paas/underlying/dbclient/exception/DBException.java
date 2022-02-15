package com.zzstack.paas.underlying.dbclient.exception;

public class DBException extends Exception {
	private static final long serialVersionUID = 5230083280341862101L;

	public static enum DBERRINFO {
		DEFAULT(80010000),
		e1(80010001),        // 连接池异常
		e2(80010002),        // 获取连接异常
		e3(80010003),        // 查询出错
		e4(80010004),        // 更新出错
		e5(80010005),        // setAutoCommit(false) error
		e6(80010006);        // 解密出错
		
		private int value;
		
		private DBERRINFO(int s) {
			value = s;
		}
		
		public int getValue() {
			// 得到枚举值代表的字符串。
			return value;
		}
	}
	
	private int errorCode;
	
	public DBException(String message, Throwable cause, DBERRINFO errorInfo) {
		super(message, cause);
		this.errorCode = errorInfo.value;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
}
