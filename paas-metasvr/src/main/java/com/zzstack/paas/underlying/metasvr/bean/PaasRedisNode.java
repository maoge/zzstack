package com.zzstack.paas.underlying.metasvr.bean;

import java.util.ArrayList;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class PaasRedisNode {
	
	private String nodeId = "";
	private String ip = "";
	private String port = "";
	private int redisRole = CONSTS.REDIS_ROLE_NONE;
	private String slotRange = "";

	private String masterId = ""; // usee by slave role
	private ArrayList<String> slaveIds; // used by master role
	
	public PaasRedisNode() {
		super();
		
		this.slaveIds = new ArrayList<String>();
	}
	
	/**
	 * @param nodeId
	 * @param ip
	 * @param port
	 * @param redisRole
	 * @param slotRange
	 * @param masterId
	 * @param slaveIds
	 */
	public PaasRedisNode(String nodeId, String ip, String port, int redisRole, String slotRange, String masterId) {
		super();
		this.nodeId = nodeId;
		this.ip = ip;
		this.port = port;
		this.redisRole = redisRole;
		this.slotRange = slotRange;
		this.masterId = masterId;
		
		this.slaveIds = new ArrayList<String>();
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public int getRedisRole() {
		return redisRole;
	}

	public void setRedisRole(int redisRole) {
		this.redisRole = redisRole;
	}

	public String getSlotRange() {
		return slotRange;
	}

	public void setSlotRange(String slotRange) {
		this.slotRange = slotRange;
	}

	public String getMasterId() {
		return masterId;
	}

	public void setMasterId(String masterId) {
		this.masterId = masterId;
	}
	
	public void addSlaveId(String slaveId) {
		slaveIds.add(slaveId);
	}
	
	public ArrayList<String> getSlaveIds() {
	    return slaveIds;
	}
	
	public boolean isSlaveEmpty() {
		return slaveIds.isEmpty();
	}
	
    public int getNodeSlotCount() {
        // 0-1364 5461-6825 10923-12287
        String[] vs = slotRange.split(" ");
        int slotCnt = 0;
        for (int i = 0; i < vs.length; ++i) {
            String str = vs[i];
            if (str.isEmpty()) continue;
            
            int splitIdx = str.indexOf("-");
            if (splitIdx == -1) slotCnt += 1;

            String sStart = str.substring(0, splitIdx);
            String sEnd   = str.substring(splitIdx + 1);

            int start = Integer.valueOf(sStart);
            int end   = Integer.valueOf(sEnd);
            int range = end - start + 1;
            slotCnt += range;
        }
        
        return slotCnt;
    }
    
    public String toJson() {
    	JsonObject retval = new JsonObject();
        retval.put("node_id", nodeId);
        retval.put("ip", ip);
        retval.put("port", port);
        retval.put("redis_role", redisRole);
        retval.put("slot_range", slotRange);
        retval.put("master_id", masterId);
    	
    	return retval.toString();
    }

}
