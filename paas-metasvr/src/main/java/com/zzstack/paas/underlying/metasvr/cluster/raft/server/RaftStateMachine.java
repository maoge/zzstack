package com.zzstack.paas.underlying.metasvr.cluster.raft.server;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.rocksdb.Checkpoint;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.metasvr.cluster.raft.server.service.ClusterProto;
import com.zzstack.paas.underlying.raft.core.StateMachine;

public class RaftStateMachine implements StateMachine {

    private static final Logger LOG = LoggerFactory.getLogger(RaftStateMachine.class);

    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;
    private String raftDataDir;

    public RaftStateMachine(String raftDataDir) {
        this.raftDataDir = raftDataDir;
    }

    @Override
    public void writeSnapshot(String snapshotDir) {
        Checkpoint checkpoint = Checkpoint.create(db);
        try {
            checkpoint.createCheckpoint(snapshotDir);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.warn("writeSnapshot meet exception, dir={}, msg={}",
                    snapshotDir, ex.getMessage());
        }
    }

    @Override
    public void readSnapshot(String snapshotDir) {
        try {
            // copy snapshot dir to data dir
            if (db != null) {
                db.close();
            }
            String dataDir = raftDataDir + File.separator + "rocksdb_data";
            File dataFile = new File(dataDir);
            if (dataFile.exists()) {
                FileUtils.deleteDirectory(dataFile);
            }
            File snapshotFile = new File(snapshotDir);
            if (snapshotFile.exists()) {
                FileUtils.copyDirectory(snapshotFile, dataFile);
            }
            // open rocksdb data dir
            org.rocksdb.Options options = new org.rocksdb.Options();
            options.setCreateIfMissing(true);
            db = RocksDB.open(options, dataDir);
        } catch (Exception ex) {
            LOG.warn("meet exception, msg={}", ex.getMessage());
        }
    }

    @Override
    public void apply(byte[] dataBytes) {
        try {
            ClusterProto.SetRequest request = ClusterProto.SetRequest.parseFrom(dataBytes);
            db.put(request.getKey().getBytes(), request.getValue().getBytes());
        } catch (Exception ex) {
            LOG.warn("meet exception, msg={}", ex.getMessage());
        }
    }

    public ClusterProto.GetResponse get(ClusterProto.GetRequest request) {
        try {
            ClusterProto.GetResponse.Builder responseBuilder = ClusterProto.GetResponse.newBuilder();
            byte[] keyBytes = request.getKey().getBytes();
            byte[] valueBytes = db.get(keyBytes);
            if (valueBytes != null) {
                String value = new String(valueBytes);
                responseBuilder.setValue(value);
            }
            ClusterProto.GetResponse response = responseBuilder.build();
            return response;
        } catch (RocksDBException ex) {
            LOG.warn("read rockdb error, msg={}", ex.getMessage());
            return null;
        }
    }

}
