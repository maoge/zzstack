package com.zzstack.paas.underlying.metasvr.cluster.raft.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.baidu.brpc.server.RpcServer;
import com.googlecode.protobuf.format.JsonFormat;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterService;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.impl.ClusterServiceImpl;
import com.zzstack.paas.underlying.metasvr.cluster.raft.util.RaftAddressUtils;
import com.zzstack.paas.underlying.raft.core.Peer;
import com.zzstack.paas.underlying.raft.core.RaftNode;
import com.zzstack.paas.underlying.raft.core.RaftNode.NodeState;
import com.zzstack.paas.underlying.raft.core.RaftOptions;
import com.zzstack.paas.underlying.raft.core.proto.RaftProto;
import com.zzstack.paas.underlying.raft.core.service.RaftClientService;
import com.zzstack.paas.underlying.raft.core.service.RaftConsensusService;
import com.zzstack.paas.underlying.raft.core.service.impl.RaftClientServiceImpl;
import com.zzstack.paas.underlying.raft.core.service.impl.RaftConsensusServiceImpl;

import io.vertx.core.json.JsonObject;

public class RaftServer {
    
    private RpcServer rpcServer = null;
    private RaftNode raftNode = null;
    
    private String servers = null;
    private String dataPath = null;
    private String self = null;
    private int localId = 0;
    
    public RaftServer(String servers, String dataPath, String self) {
        this.servers = servers;
        this.dataPath = dataPath;
        this.self = self;
        
        init();
    }
    
    private void init() {
        String[] splitArray = servers.split(",");
        List<RaftProto.Server> serverList = new ArrayList<>();
        for (String serverString : splitArray) {
            RaftProto.Server server = RaftAddressUtils.parseServer(serverString);
            serverList.add(server);
        }
        
        // 设置Raft选项，比如：
        RaftOptions raftOptions = new RaftOptions();
        raftOptions.setDataDir(dataPath);
        raftOptions.setSnapshotMinLogSize(10 * 1024);
        raftOptions.setMaxSegmentFileSize(1024 * 1024);
        raftOptions.setSnapshotPeriodSeconds(60);
        raftOptions.setHeartbeatPeriodMilliseconds(10000);
        raftOptions.setElectionTimeoutMilliseconds(20000);
        raftOptions.setMaxAwaitTimeout(3000);
        raftOptions.setRaftConsensusThreadNum(4);
        
        RaftStateMachine stateMachine = new RaftStateMachine(raftOptions.getDataDir());
        
        // local server
        RaftProto.Server localServer = RaftAddressUtils.parseServer(self);
        localId = localServer.getServerId();
        rpcServer = new RpcServer(localServer.getEndpoint().getHost(), localServer.getEndpoint().getPort());
        
        // 初始化RaftNode
        raftNode = new RaftNode(raftOptions, serverList, localServer, stateMachine);
        // 注册Raft节点之间相互调用的服务
        RaftConsensusService raftConsensusService = new RaftConsensusServiceImpl(raftNode);
        rpcServer.registerService(raftConsensusService);
        
        // 注册给Client调用的Raft服务
        RaftClientService raftClientService = new RaftClientServiceImpl(raftNode);
        rpcServer.registerService(raftClientService);
        
        // 注册应用自己提供的服务
        ClusterService clusterService = new ClusterServiceImpl(raftNode, stateMachine);
        rpcServer.registerService(clusterService);
        
        // 启动RPCServer，初始化Raft节点
        rpcServer.start();
        raftNode.init();
    }
    
    public void destry() {
        if (rpcServer != null) {
            rpcServer.shutdown();
            rpcServer = null;
        }
    }
    
    public boolean isLeader() {
        return raftNode.getState() == NodeState.STATE_LEADER;
    }
    
    public NodeState getState() {
        return raftNode.getState();
    }
    
    public int getLocalId() {
        return localId;
    }
    
    public int getLeaderId() {
        return raftNode.getLeaderId();
    }
    
    public String getRaftPeers() {
        StringBuilder sb = new StringBuilder();
        ConcurrentMap<Integer, Peer> peerMap = raftNode.getPeerMap();
        Set<Entry<Integer, Peer>> entrySet = peerMap.entrySet();
        for (Entry<Integer, Peer> entry : entrySet) {
            int id = entry.getKey();
            Peer peer = entry.getValue();
            String s = String.format("%d %s:%d", id, peer.getServer().getEndpoint().getHost(), peer.getServer().getEndpoint().getPort());
            if (sb.length() > 0)
                sb.append(",");
            
            sb.append(s);
        }
        return sb.toString();
    }
    
    public void getRaftClusterState(JsonObject json) {
        raftNode.getLock().lock();
        try {
            JsonFormat fmt = new JsonFormat();
            JsonObject clusterJson = new JsonObject(fmt.printToString(raftNode.getConfiguration()));
            
            json.put("leaderId", raftNode.getLeaderId());
            json.put("cluster", clusterJson);
            
        } finally {
            raftNode.getLock().unlock();
        }
    }

}
