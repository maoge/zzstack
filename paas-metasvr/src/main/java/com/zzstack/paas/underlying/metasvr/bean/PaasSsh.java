package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasSsh extends BeanMapper {
	
	private String sshId;
	private String sshName;
	private String sshPwd;
	private int sshPort;
	private String servClazz;
	private String serverIp;
	
	public PaasSsh() {
		super();
	}
	
	/**
	 * @param sshId
	 * @param sshName
	 * @param sshPwd
	 * @param sshPort
	 * @param servClazz
	 * @param serverIp
	 */
	public PaasSsh(String sshId, String sshName, String sshPwd, int sshPort, String servClazz, String serverIp) {
		super();
		this.sshId = sshId;
		this.sshName = sshName;
		this.sshPwd = sshPwd;
		this.sshPort = sshPort;
		this.servClazz = servClazz;
		this.serverIp = serverIp;
	}
	
	public static PaasSsh convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;

		String sshId     = getFixDataAsString(mapper, FixHeader.HEADER_SSH_ID);
		String sshName   = getFixDataAsString(mapper, FixHeader.HEADER_SSH_NAME);
		String sshPwd    = getFixDataAsString(mapper, FixHeader.HEADER_SSH_PWD);
		int    sshPort   = getFixDataAsInt(mapper, FixHeader.HEADER_SSH_PORT);
		String servClazz = getFixDataAsString(mapper, FixHeader.HEADER_SERV_CLAZZ);
		String serverIp  = getFixDataAsString(mapper, FixHeader.HEADER_SERVER_IP);

		return new PaasSsh(sshId, sshName, sshPwd, sshPort, servClazz, serverIp);
	}

	public String getSshId() {
		return sshId;
	}

	public void setSshId(String sshId) {
		this.sshId = sshId;
	}

	public String getSshName() {
		return sshName;
	}

	public void setSshName(String sshName) {
		this.sshName = sshName;
	}

	public String getSshPwd() {
		return sshPwd;
	}

	public void setSshPwd(String sshPwd) {
		this.sshPwd = sshPwd;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

	public String getServClazz() {
		return servClazz;
	}

	public void setServClazz(String servClazz) {
		this.servClazz = servClazz;
	}

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
	
	public String toJson() {
		JsonObject retval = new JsonObject();
		retval.put(FixHeader.HEADER_SSH_ID,     sshId);
		retval.put(FixHeader.HEADER_SSH_NAME,   sshName);
		retval.put(FixHeader.HEADER_SSH_PWD,    sshPwd);
		retval.put(FixHeader.HEADER_SSH_PORT,   sshPort);
		retval.put(FixHeader.HEADER_SERV_CLAZZ, servClazz);
		retval.put(FixHeader.HEADER_SERVER_IP,  serverIp);

		return retval.toString();
	}

	@Override
	public String toString() {
		return "PaasSsh [sshId=" + sshId + ", sshName=" + sshName + ", sshPwd=" + sshPwd + ", sshPort=" + sshPort
				+ ", servClazz=" + servClazz + ", serverIp=" + serverIp + "]";
	}
	
	public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(FixHeader.HEADER_SSH_ID,     sshId);
		json.put(FixHeader.HEADER_SSH_NAME,   sshName);
		json.put(FixHeader.HEADER_SSH_PWD,    sshPwd);
		json.put(FixHeader.HEADER_SSH_PORT,   sshPort);
		json.put(FixHeader.HEADER_SERV_CLAZZ, servClazz);
		json.put(FixHeader.HEADER_SERVER_IP,  serverIp);
		
		return json;
	}
	
    public static PaasSsh fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        String sshId = json.getString(FixHeader.HEADER_SSH_ID);
        String sshName = json.getString(FixHeader.HEADER_SSH_NAME);
        String sshPwd = json.getString(FixHeader.HEADER_SSH_PWD);
        int sshPort = json.getInteger(FixHeader.HEADER_SSH_PORT);
        String servClazz = json.getString(FixHeader.HEADER_SERV_CLAZZ);
        String serverIp = json.getString(FixHeader.HEADER_SERVER_IP);
        return new PaasSsh(sshId, sshName, sshPwd, sshPort, servClazz, serverIp);
    }

}
