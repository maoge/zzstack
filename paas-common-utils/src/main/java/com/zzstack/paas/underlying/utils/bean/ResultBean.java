package com.zzstack.paas.underlying.utils.bean;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class ResultBean {
	
	private int retCode;
	private String retInfo;
	
	public ResultBean() {
		this.retCode = CONSTS.REVOKE_OK;
		this.retInfo = "";
	}

	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public String getRetInfo() {
		return retInfo;
	}

	public void setRetInfo(String retInfo) {
		this.retInfo = retInfo;
	}
	
	@Override
	public String toString() {
		return "ResultBean [RET_CODE=" + retCode + ", RET_INFO=" + retInfo + "]";
	}
	
}
