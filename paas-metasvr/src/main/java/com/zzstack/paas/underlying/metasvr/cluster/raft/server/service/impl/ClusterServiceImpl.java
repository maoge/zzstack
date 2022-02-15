package com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.impl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.baidu.brpc.client.RpcClientOptions;
import com.baidu.brpc.client.instance.Endpoint;
import com.googlecode.protobuf.format.JsonFormat;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.RaftStateMachine;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterProto;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterService;
import com.zzstack.paas.underlying.raft.core.Peer;
import com.zzstack.paas.underlying.raft.core.RaftNode;
import com.zzstack.paas.underlying.raft.core.proto.RaftProto;

public class ClusterServiceImpl implements ClusterService {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterServiceImpl.class);
    private static JsonFormat jsonFormat = new JsonFormat();

    private RaftNode raftNode;
    private RaftStateMachine stateMachine;
    private int leaderId = -1;
    private RpcClient leaderRpcClient = null;
    private Lock leaderLock = new ReentrantLock();

    public ClusterServiceImpl(RaftNode raftNode, RaftStateMachine stateMachine) {
        this.raftNode = raftNode;
        this.stateMachine = stateMachine;
    }

    private void onLeaderChangeEvent() {
        if (raftNode.getLeaderId() != -1
                && raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()
                && leaderId != raftNode.getLeaderId()) {
            leaderLock.lock();
            if (leaderId != -1 && leaderRpcClient != null) {
                leaderRpcClient.stop();
                leaderRpcClient = null;
                leaderId = -1;
            }
            leaderId = raftNode.getLeaderId();
            Peer peer = raftNode.getPeerMap().get(leaderId);
            Endpoint endpoint = new Endpoint(peer.getServer().getEndpoint().getHost(),
                    peer.getServer().getEndpoint().getPort());
            RpcClientOptions rpcClientOptions = new RpcClientOptions();
            rpcClientOptions.setGlobalThreadPoolSharing(true);
            leaderRpcClient = new RpcClient(endpoint, rpcClientOptions);
            leaderLock.unlock();
        }
    }

    @Override
    public ClusterProto.SetResponse set(ClusterProto.SetRequest request) {
        ClusterProto.SetResponse.Builder responseBuilder = ClusterProto.SetResponse.newBuilder();
        // 如果自己不是leader，将写请求转发给leader
        if (raftNode.getLeaderId() <= 0) {
            responseBuilder.setSuccess(false);
        } else if (raftNode.getLeaderId() != raftNode.getLocalServer().getServerId()) {
            onLeaderChangeEvent();
            ClusterService service = BrpcProxy.getProxy(leaderRpcClient, ClusterService.class);
            ClusterProto.SetResponse responseFromLeader = service.set(request);
            responseBuilder.mergeFrom(responseFromLeader);
        } else {
            // 数据同步写入raft集群
            byte[] data = request.toByteArray();
            boolean success = raftNode.replicate(data, RaftProto.EntryType.ENTRY_TYPE_DATA);
            responseBuilder.setSuccess(success);
        }

        ClusterProto.SetResponse response = responseBuilder.build();
        LOG.info("set request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }

    @Override
    public ClusterProto.GetResponse get(ClusterProto.GetRequest request) {
        ClusterProto.GetResponse response = stateMachine.get(request);
        LOG.info("get request, request={}, response={}", jsonFormat.printToString(request),
                jsonFormat.printToString(response));
        return response;
    }

}
