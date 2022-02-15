package com.zzstack.paas.underlying.raft.core.service;

import com.zzstack.paas.underlying.raft.core.proto.RaftProto;

/**
 * raft节点之间相互通信的接口。
 */
public interface RaftConsensusService {

    RaftProto.VoteResponse preVote(RaftProto.VoteRequest request);

    RaftProto.VoteResponse requestVote(RaftProto.VoteRequest request);

    RaftProto.AppendEntriesResponse appendEntries(RaftProto.AppendEntriesRequest request);

    RaftProto.InstallSnapshotResponse installSnapshot(RaftProto.InstallSnapshotRequest request);
}
