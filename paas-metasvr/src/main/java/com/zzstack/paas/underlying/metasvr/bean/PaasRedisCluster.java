package com.zzstack.paas.underlying.metasvr.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class PaasRedisCluster {
	
    private Map<String, PaasRedisNode> masterNodes;
    private Map<String, PaasRedisNode> slaveNodes;
    
	public PaasRedisCluster() {
		super();
		masterNodes = new HashMap<String, PaasRedisNode>();
		slaveNodes = new HashMap<String, PaasRedisNode>();
	}

	public void parse(String info) {
		String[] vs = info.split("\n");

		for (String str : vs) {
			int idxAt = str.indexOf("@");
			if (idxAt == CONSTS.STR_NOT_FOUND) {
				continue;
			}
			
			if (str.charAt(str.length() - 1) == '\r') {
			    str = str.substring(0,  str.length() - 2);
			}

			// f3607081ae714026d94b4a3a74edaf5981aaf91b 192.168.31.215:8104@18104 slave
			// 171c0a4f7ed5486a399f13908f97512a86105b4b 0 1597899045000 2 connected
			// 171c0a4f7ed5486a399f13908f97512a86105b4b 192.168.31.215:8101@18101 master - 0
			// 1597899047000 2 connected 5461-10922
			// ba9e95e78c6fee2829b63f9c29e1e6a365d4ba64 192.168.31.215:8100@18100
			// myself,master - 0 1597899047000 1 connected 0-5460
			PaasRedisNode node = new PaasRedisNode();
			int idxAddr = str.indexOf(" ", 0);
			node.setNodeId(str.substring(0, idxAddr));

			int idxSem = str.indexOf(":", 0);
			String ip = str.substring(idxAddr + 1, idxSem);
			String port = str.substring(idxSem + 1, idxAt);
			node.setIp(ip);
			node.setPort(port);

			int idxMaster = str.indexOf("master", idxAt);
			int idxSlave = str.indexOf("slave", idxAt);

			if (idxMaster != CONSTS.STR_NOT_FOUND) {
				node.setRedisRole(CONSTS.REDIS_ROLE_MASTER);

				int idxSlot = str.indexOf("connected ", idxMaster);
				if (idxSlot != CONSTS.STR_NOT_FOUND) {
					String slotRange = str.substring(idxSlot + "connected ".length());
					node.setSlotRange(slotRange);
				}

				masterNodes.put(node.getNodeId(), node);
			} else if (idxSlave != CONSTS.STR_NOT_FOUND) {
				node.setRedisRole(CONSTS.REDIS_ROLE_SLAVE);

				int idxIdBeg = idxSlave + "slave ".length();
				int idxIdEnd = str.indexOf(" ", idxIdBeg);

				String masterId = str.substring(idxIdBeg, idxIdEnd);
				node.setMasterId(masterId);

				slaveNodes.put(node.getNodeId(), node);
			}

			// String debug = String.format("role:%d, port:%s, slot:%s",
			// node.getRedisRole(), node.getPort(), node.getSlotRange());
			// System.out.println(debug);
		}

		Set<Entry<String, PaasRedisNode>> entrySet = slaveNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
		    PaasRedisNode slave = entry.getValue();
			String masterId = slave.getMasterId();

			PaasRedisNode master = masterNodes.get(masterId);
			if (master != null) {
				master.addSlaveId(slave.getNodeId());
			}
		}
	}
	
	public int getMasterCount() {
		int cnt = 0;
		Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
		    PaasRedisNode master = entry.getValue();
			if (!master.getNodeId().isEmpty() && !master.getSlotRange().isEmpty()) {
				++cnt;
			}
		}
		return cnt;
	}
	
	public String getAloneMaster() {
		String ret = "";

		Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
		    PaasRedisNode master = entry.getValue();
			if (master.isSlaveEmpty()) {
				ret = master.getNodeId();
				break;
			}
		}
		return ret;
	}
	
	public String getOneMasterAddr() {
		StringBuilder addr = new StringBuilder("");

		Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
		    PaasRedisNode master = entry.getValue();
			if (!master.getIp().isEmpty() && !master.getPort().isEmpty()) {
				addr.append(master.getIp()).append(":").append(master.getPort());
				break;
			}
		}
		return addr.toString();
	}
	
	public String getMasterIdList() {
		StringBuilder masterIds = new StringBuilder("");
		boolean filled = false;
		Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
		    PaasRedisNode master = entry.getValue();
			if (!master.getNodeId().isEmpty() && !master.getSlotRange().isEmpty()) {
				if (filled)
					masterIds.append(",");
				masterIds.append(master.getNodeId());

				if (!filled)
					filled = true;
			}
		}
		return masterIds.toString();
	}
	
	public PaasRedisNode getSelfInfo(String ip, String port) {
		Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
			PaasRedisNode node = entry.getValue();
			if (node.getIp().equals(ip) && node.getPort().equals(port)) {
				return node;
			}
		}

		entrySet = slaveNodes.entrySet();
		for (Entry<String, PaasRedisNode> entry : entrySet) {
			PaasRedisNode node = entry.getValue();
			if (node.getIp().equals(ip) && node.getPort().equals(port)) {
				return node;
			}
		}

		return null;
	}
	
	public void getSlaves(String masterId, ArrayList<PaasRedisNode> slaves) {	    
	    PaasRedisNode master = masterNodes.get(masterId);
	    if (master != null) {
	        ArrayList<String> slaveIds = master.getSlaveIds();
	        for (String slaveId : slaveIds) {
	            PaasRedisNode slave = slaveNodes.get(slaveId);
	            slaves.add(slave);
	        }
	    }
	}
	
    public void getSlotedMasters(ArrayList<PaasRedisNode> masters) {
        Set<Entry<String, PaasRedisNode>> entrySet = masterNodes.entrySet();
        for (Entry<String, PaasRedisNode> entry : entrySet) {
            masters.add(entry.getValue());
        }
    }
    
    public int getNodeSlotCount(String nodeId) {
        PaasRedisNode node = masterNodes.get(nodeId);
        if (node == null) return 0;
        
        String slotRange = node.getSlotRange();
        
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

}
