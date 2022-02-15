package com.zzstack.paas.underlying.metasvr.singleton;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.httpserver.marshell.HttpServerMarshell;
import com.zzstack.paas.underlying.metasvr.autocheck.CmptCheckTaskGenerator;
import com.zzstack.paas.underlying.metasvr.cluster.raft.client.RaftClient;
import com.zzstack.paas.underlying.metasvr.cluster.raft.server.RaftServer;
import com.zzstack.paas.underlying.metasvr.cluster.raft.util.RaftAddressUtils;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.eventbus.EventDispatcher;
import com.zzstack.paas.underlying.metasvr.metadata.CmptMeta;
import com.zzstack.paas.underlying.metasvr.utils.SysConfig;
import com.zzstack.paas.underlying.utils.JedisUtil;
import com.zzstack.paas.underlying.utils.UUIDUtils;

import io.vertx.core.json.JsonObject;
import redis.clients.jedis.JedisCluster;

public class MetaSvrGlobalRes {

    private static Logger logger = LoggerFactory.getLogger(MetaSvrGlobalRes.class);

    private final String metaServId;

    private JedisCluster jedisCluster;

    private PulsarClient pulsarClient = null;
    private Producer<byte[]> producer = null;

    private EventDispatcher eventDispatcher = null;

    private CmptMeta cmptMeta = null;
    
    private HttpServerMarshell httpServerMarshell = null;  // httpServerMarshell 的释放由Vertical stop回调自己完成, 不在GlobalRes里面做释放

    private RaftServer raftServer = null;
    private RaftClient raftClient = null;
    
    private CmptCheckTaskGenerator cmptCheckTaskGen = null;
    
    private static volatile MetaSvrGlobalRes theInstance = null;
    private static ReentrantLock intanceLock = null;

    static {
        intanceLock = new ReentrantLock();
    }

    private MetaSvrGlobalRes() {
        metaServId = UUIDUtils.genUUID();
    }

    public static MetaSvrGlobalRes get() {
        if (theInstance == null) {
            try {
                intanceLock.lock();
                if (theInstance == null) {
                    theInstance = new MetaSvrGlobalRes();
                    theInstance.init();
                }
            } finally {
                intanceLock.unlock();
            }
        }

        return theInstance;
    }

    private void init() {
        initRedisPool();
        initEventBusBroker();
        initEventDispatcher();
        initCmptMeta();
        initDBSourcePool();
        initRaft();
        initCmptCheckTaskGen();
    }

    public static void release() {
        try {
            intanceLock.lock();

            if (theInstance == null)
                return;

            if (theInstance.jedisCluster != null) {
                theInstance.jedisCluster.close();
                theInstance.jedisCluster = null;
            }

            if (theInstance.producer != null) {
                theInstance.producer.close();
                theInstance.producer = null;
            }

            if (theInstance.pulsarClient != null) {
                theInstance.pulsarClient.close();
                theInstance.pulsarClient = null;
            }

            if (theInstance.eventDispatcher != null) {
                theInstance.eventDispatcher.stop();
                theInstance.eventDispatcher = null;
            }

            if (theInstance.cmptMeta != null) {
                theInstance.cmptMeta.release();
                theInstance.cmptMeta = null;
            }
            
            if (theInstance.httpServerMarshell != null) {
                theInstance.httpServerMarshell.destroy();
                theInstance.httpServerMarshell = null;
            }
            
            theInstance.releaseRaft();
            theInstance.releaseCmptCheckTaskGen();
            
            ActiveStandbyDBSrcPool.destroy();
            
            theInstance = null;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            intanceLock.unlock();
        }
    }
    
    private void releaseRaft() {
        if (raftClient != null) {
            raftClient.destroy();
            raftClient = null;
        }
        
        if (raftServer != null) {
            raftServer.destry();
            raftServer = null;
        }
    }
    
    public void releaseCmptCheckTaskGen() {
        if (cmptCheckTaskGen != null) {
            cmptCheckTaskGen.destory();
            cmptCheckTaskGen = null;
        }
    }
    
    public void getClusterState(JsonObject json) {
        if (!SysConfig.get().isRaftClusterEnabled()) {
            return;
        }
        
        raftServer.getRaftClusterState(json);
    }
    
    public boolean isRaftLeader() {
        if (raftServer == null)
            return false;
        
        return raftServer.isLeader();
    }
    
    public String getMetaServId() {
        return metaServId;
    }

    public JsonObject getCmptMetaAsJson() {
        if (cmptMeta == null)
            return null;

        return cmptMeta.getMetaData2Json();
    }

    private void initRaft() {
        try {
            if (!SysConfig.get().isRaftClusterEnabled()) {
                return;
            }
            
            String raftClusterNodes = SysConfig.get().getRaftClusterNodes();
            String raftSelf = SysConfig.get().getRaftSelf();
            
            raftServer = new RaftServer(raftClusterNodes, FixDefs.RAFT_DATA_DIR, raftSelf);
            raftClient = new RaftClient(RaftAddressUtils.parseClusterAddrs(raftClusterNodes));

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
    
    public void initCmptCheckTaskGen() {
        cmptCheckTaskGen = new CmptCheckTaskGenerator();
    }

    public boolean isCollectEnabled() {
        return SysConfig.get().isCollectEnabled() || (SysConfig.get().isRaftClusterEnabled() && isRaftLeader());
    }
    
    private void initRedisPool() {
        try {
            SysConfig sysConfig = SysConfig.get();
            JedisUtil.setRedisConnPoolMax(sysConfig.getRedisPoolMaxSize());
            JedisUtil.setRedisConnPoolMin(sysConfig.getRedisPoolMinSize());
            JedisUtil.setMaxWaitMillis(sysConfig.getRedisMaxWaitMillis());

            jedisCluster = JedisUtil.getPool(SysConfig.get().getRedisCluster());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void initEventBusBroker() {
        try {
            String topic = String.format("persistent://public/default/%s", FixDefs.SYS_EVENT_TOPIC);
            String uri = String.format("pulsar://%s", SysConfig.get().getEventbusAddress());
            pulsarClient = PulsarClient.builder().serviceUrl(uri).build();

            producer = pulsarClient.newProducer()
                    .topic(topic)
                    .batchingMaxPublishDelay(1, TimeUnit.MILLISECONDS)
                    .sendTimeout(1, TimeUnit.SECONDS)
                    .blockIfQueueFull(true)
                    .create();

        } catch (PulsarClientException e) {
            logger.error("eventbus pulsar borker connect error:" + e.getMessage(), e);
        }
    }

    private void initEventDispatcher() {
        eventDispatcher = new EventDispatcher(pulsarClient);
        eventDispatcher.start();
    }

    private void initCmptMeta() {
        cmptMeta = new CmptMeta();
        cmptMeta.init();
    }
    
    private void initDBSourcePool() {
        // ActiveStandbyDBSrcPool.get(SysConfig.get().getTDYamlName());
        ActiveStandbyDBSrcPool.get(SysConfig.get().getMetaDBYamlName());
    }

    public CmptMeta getCmptMeta() {
        return this.cmptMeta;
    }

    public JedisCluster getRedisClient() {
        JedisCluster client = null;
        try {
            intanceLock.lock();

            if (theInstance != null)
                client = theInstance.jedisCluster;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            intanceLock.unlock();
        }
        return client;
    }

    public PulsarClient getPulsarClient() {
        return pulsarClient;
    }
    
    public Producer<byte[]> getProducer() {
        Producer<byte[]> pulsarProducer = null;
        try {
            intanceLock.lock();

            if (theInstance != null)
                pulsarProducer = theInstance.producer;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            intanceLock.unlock();
        }
        return pulsarProducer;
    }
    
    public void setHttpServerMarshell(HttpServerMarshell httpServerMarshell) {
        this.httpServerMarshell = httpServerMarshell;
    }
    
    public HttpServerMarshell getHttpServerMarshell() {
        return this.httpServerMarshell;
    }
    
    public RaftServer getRaftServer() {
        return raftServer;
    }
    
    public RaftClient getRaftClient() {
        return raftClient;
    }

}
