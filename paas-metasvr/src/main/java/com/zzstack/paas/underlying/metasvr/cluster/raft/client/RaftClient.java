package com.zzstack.paas.underlying.metasvr.cluster.raft.client;

import java.util.concurrent.locks.ReentrantLock;

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.googlecode.protobuf.format.JsonFormat;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterProto;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterService;

public class RaftClient {

    private String servers = null;
    private final JsonFormat jsonFormat = new JsonFormat();
    
    private RpcClient rpcClient = null;
    private ClusterService clusterService = null;
    
    private ReentrantLock lock = null;
    
    public RaftClient(String servers) {
        this.servers = servers;
        this.lock = new ReentrantLock();
        
        init();
    }
    
    private void init() {
        rpcClient = new RpcClient(servers);
        clusterService = BrpcProxy.getProxy(rpcClient, ClusterService.class);
    }
    
    public void destroy() {
        lock.lock();
        try {
            if (rpcClient != null) {
                rpcClient.stop();
                rpcClient = null;
            }
        } finally {
            lock.unlock();
        }
    }
    
    public boolean set(String key, String value) {
        boolean res = true;
        
        lock.lock();
        try {
            ClusterProto.SetRequest setRequest = ClusterProto.SetRequest.newBuilder().setKey(key).setValue(value).build();
            ClusterProto.SetResponse setResponse = clusterService.set(setRequest);
            res = setResponse.getSuccess();
            
        } catch (Exception e) {
            res = false;
        } finally {
            lock.unlock();
        }
        
        return res;
    }
    
    public String get(String key) {
        String res = null;
        
        lock.lock();
        try {
            ClusterProto.GetRequest getRequest = ClusterProto.GetRequest.newBuilder().setKey(key).build();
            ClusterProto.GetResponse getResponse = clusterService.get(getRequest);
            
            if (getResponse != null) {
                res = jsonFormat.printToString(getResponse);
            }
            
        } finally {
            lock.unlock();
        }
        
        return res;
    }

}
