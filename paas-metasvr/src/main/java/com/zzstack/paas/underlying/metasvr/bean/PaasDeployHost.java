package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasDeployHost extends BeanMapper {

	private int hostId;
	private String ipAddress;
	private String userName;
	private String userPwd;
	private String sshPort;
	private long createTime;
	
	public PaasDeployHost() {
		super();
	}
	
	/**
	 * @param hostId
	 * @param ipAddress
	 * @param userName
	 * @param userPwd
	 * @param sshPort
	 * @param createTime
	 */
	public PaasDeployHost(int hostId, String ipAddress, String userName, String userPwd, String sshPort,
			long createTime) {
		super();
		this.hostId = hostId;
		this.ipAddress = ipAddress;
		this.userName = userName;
		this.userPwd = userPwd;
		this.sshPort = sshPort;
		this.createTime = createTime;
	}
	
	public static PaasDeployHost convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;

		int hostId       = getFixDataAsInt(mapper, FixHeader.HEADER_HOST_ID);
		String ipAddress = getFixDataAsString(mapper, FixHeader.HEADER_IP_ADDRESS);
		String userName  = getFixDataAsString(mapper, FixHeader.HEADER_USER_NAME);
		String userPwd   = getFixDataAsString(mapper, FixHeader.HEADER_USER_PWD);
		String sshPort   = getFixDataAsString(mapper, FixHeader.HEADER_SSH_PORT);
		long createTime  = getFixDataAsLong(mapper, FixHeader.HEADER_CREATE_TIME);

		return new PaasDeployHost(hostId, ipAddress, userName, userPwd, sshPort, createTime);
	}

	public int getHostId() {
		return hostId;
	}

	public void setHostId(int hostId) {
		this.hostId = hostId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserPwd() {
		return userPwd;
	}

	public void setUserPwd(String userPwd) {
		this.userPwd = userPwd;
	}

	public String getSshPort() {
		return sshPort;
	}

	public void setSshPort(String sshPort) {
		this.sshPort = sshPort;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	@Override
	public String toString() {
		return "PaasDeployHost [hostId=" + hostId + ", ipAddress=" + ipAddress + ", userName=" + userName + ", userPwd="
				+ userPwd + ", sshPort=" + sshPort + ", createTime=" + createTime + "]";
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put("hostId", hostId);
		json.put("ipAddress", ipAddress);
		json.put("userName", userName);
		json.put("userPwd", "******");
		json.put("sshPort", sshPort);
		json.put("createTime", createTime);
		
		return json;
	}

}
