package com.zzstack.paas.underlying.metasvr.autodeploy.util;

import com.jcraft.jsch.UserInfo;

public class JschUserInfo implements UserInfo {

	private String user;
	private String passwd;
	private String host;
	private int sshPort;
	
	public JschUserInfo(String user, String passwd, String host, int sshPort) {
		this.user = user;
		this.passwd = passwd;
		this.host = host;
		this.sshPort = sshPort;
	}

	@Override
	public String getPassphrase() {
		return null;
	}

	@Override
	public String getPassword() {
		return passwd;
	}

	@Override
	public boolean promptPassphrase(String paramString) {
		return true;
	}

	@Override
	public boolean promptPassword(String paramString) {
		return true;
	}

	@Override
	public boolean promptYesNo(String paramString) {
		return true;
	}

	@Override
	public void showMessage(String paramString) {
		
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getSshPort() {
		return sshPort;
	}

	public void setSshPort(int sshPort) {
		this.sshPort = sshPort;
	}

    @Override
    public String toString() {
        return "JschUserInfo [user=" + user + ", passwd=" + passwd + ", host=" + host + ", sshPort=" + sshPort + "]";
    }
	
}
