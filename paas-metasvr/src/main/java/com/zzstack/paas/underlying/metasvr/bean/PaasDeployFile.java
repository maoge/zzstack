package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;

import io.vertx.core.json.JsonObject;

public class PaasDeployFile extends BeanMapper {
	
	private int fileId;
	private int hostId;
	private String servType;
	private String version;
    private String fileName;
	private String fileDir;
	private long createTime;
	
	public PaasDeployFile() {
		super();
	}
	
	/**
	 * @param fileId
	 * @param hostId
	 * @param servType
	 * @param fileName
	 * @param fileDir
	 * @param createTime
	 */
	public PaasDeployFile(int fileId, int hostId, String servType, String version, String fileName, String fileDir, long createTime) {
		super();
		this.fileId = fileId;
		this.hostId = hostId;
		this.servType = servType;
		this.version = version;
		this.fileName = fileName;
		this.fileDir = fileDir;
		this.createTime = createTime;
	}
	
	public static PaasDeployFile convert(Map<String, Object> mapper) {
		if (mapper == null || mapper.isEmpty())
			return null;
		
		int    fileId     = getFixDataAsInt(mapper, FixHeader.HEADER_FILE_ID);
		int    hostId     = getFixDataAsInt(mapper, FixHeader.HEADER_HOST_ID);
		String servType   = getFixDataAsString(mapper, FixHeader.HEADER_SERV_TYPE);
		String version    = getFixDataAsString(mapper, FixHeader.HEADER_VERSION);
		String fileName   = getFixDataAsString(mapper, FixHeader.HEADER_FILE_NAME);
		String fileDir    = getFixDataAsString(mapper, FixHeader.HEADER_FILE_DIR);
		long   createTime = getFixDataAsLong(mapper, FixHeader.HEADER_CREATE_TIME);
		
		return new PaasDeployFile(fileId, hostId, servType, version, fileName, fileDir, createTime);
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		this.fileId = fileId;
	}

	public int getHostId() {
		return hostId;
	}

	public void setHostId(int hostId) {
		this.hostId = hostId;
	}

	public String getServType() {
		return servType;
	}

	public void setServType(String servType) {
		this.servType = servType;
	}
	
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileDir() {
		return fileDir;
	}

	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}
	
	@Override
    public String toString() {
        return "PaasDeployFile [fileId=" + fileId + ", hostId=" + hostId + ", servType=" + servType + ", version="
                + version + ", fileName=" + fileName + ", fileDir=" + fileDir + ", createTime=" + createTime + "]";
    }

    public JsonObject toJsonObject() {
		JsonObject json = new JsonObject();
		json.put(FixHeader.HEADER_FILE_ID, fileId);
		json.put(FixHeader.HEADER_HOST_ID, hostId);
		json.put(FixHeader.HEADER_SERV_TYPE, servType);
		json.put(FixHeader.HEADER_VERSION, version);
		json.put(FixHeader.HEADER_FILE_NAME, fileName);
		json.put(FixHeader.HEADER_FILE_DIR, fileDir);
		json.put(FixHeader.HEADER_CREATE_TIME, createTime);
	
		return json;
	}

}
