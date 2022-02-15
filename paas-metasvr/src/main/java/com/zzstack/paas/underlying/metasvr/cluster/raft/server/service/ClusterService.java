package com.zzstack.paas.underlying.metasvr.cluster.raft.server.service;

public interface ClusterService {

    ClusterProto.SetResponse set(ClusterProto.SetRequest request);

    ClusterProto.GetResponse get(ClusterProto.GetRequest request);

}
