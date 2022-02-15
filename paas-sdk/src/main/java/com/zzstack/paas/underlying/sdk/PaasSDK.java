package com.zzstack.paas.underlying.sdk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;

import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;
import com.zzstack.paas.underlying.dbclient.LoadBalancedDBSrcPool;
import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.node.RedissonClientHolder;
import com.zzstack.paas.underlying.redis.utils.RedissonConfParser;
import com.zzstack.paas.underlying.sdk.config.MetaSvrConfigFatory;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import com.zzstack.paas.underlying.utils.config.CacheRedisClusterConf;
import com.zzstack.paas.underlying.utils.config.DBConfig;
import com.zzstack.paas.underlying.utils.config.PulsarConf;
import com.zzstack.paas.underlying.utils.config.RocketMqConf;
import com.zzstack.paas.underlying.utils.config.DBConfig.DBNode;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException.SdkErrInfo;
import com.zzstack.paas.underlying.utils.paas.PaasTopoParser;

public class PaasSDK {

    private static Logger logger = LoggerFactory.getLogger(PaasSDK.class);

    private static final int RETRY = 30;
    private static final long RETRY_INTERVAL = 1000L;
    
    private MetaSvrConfigFatory configFatory;

    public PaasSDK(String metasvrUrls, String user, String passwd) {
        configFatory = MetaSvrConfigFatory.getInstance(metasvrUrls, user, passwd);
    }

    public String loadPaasService(String servInstID) throws PaasSdkException {
        SVarObject result = new SVarObject();
        int cnt = 0;
        boolean isOK = false;
        
        while (++cnt < RETRY) {
            isOK = configFatory.loadServiceTopo(servInstID, result);
            if (isOK) {
                break;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
        
        if (!isOK) {
            logger.error("paas sdk init service fail, serInstID: {}, error: {}", servInstID, result.getVal());
            throw new PaasSdkException(SdkErrInfo.e80040001);
        }

        return result.getVal();
    }

    public String loadPaasInstance(String instID) throws PaasSdkException {
        SVarObject result = new SVarObject();
        int cnt = 0;
        boolean isOK = false;
        
        while (++cnt < RETRY) {
            isOK = configFatory.loadServiceTopo(instID, result);
            if (isOK) {
                break;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    ;
                }
            }
        }
        
        if (!isOK) {
            logger.error("paas sdk load instance fail, InstID: {}, error: {}", instID, result.getVal());
            throw new PaasSdkException(SdkErrInfo.e80040001);
        }

        return result.getVal();
    }

    public ActiveStandbyDBSrcPool getPaasDBClient(String servInstID, String dgName, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }

        return ActiveStandbyDBSrcPool.get(dgName, servInstID, topoStr, params);
    }
    
    public LoadBalancedDBSrcPool getPaasLoadBalancedDBPool(String servInstID, String dbName, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }

        return LoadBalancedDBSrcPool.get(dbName, servInstID, topoStr, params);
    }

    public WeightedRRLoadBalancer getPaasLoadBalancedQueue(String servInstID, String name, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }

        return MultiRedissonClient.get(servInstID, name, topoStr, params);
    }

    public RedissonClientHolder getRedisClusterClient(String servInstID, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }

        Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
        CacheRedisClusterConf redissonConf = (CacheRedisClusterConf) o;

        return RedissonConfParser.fromRedissonConf(redissonConf);
    }

    public DefaultMQProducer getPaasRocketMQProducer(String servInstID, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }
        Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
        RocketMqConf rocketMqConf = (RocketMqConf) o;

        DefaultMQProducer producer = null;
        try {
            String groupName = String.valueOf(params.get("group_name"));
            producer = new DefaultMQProducer(groupName);
            producer.setNamesrvAddr(rocketMqConf.rockConf.nameSrvUrl);

            producer.setSendMsgTimeout(10000);
            producer.start();
        } catch (MQClientException e) {
            throw new PaasSdkException(SdkErrInfo.e80050002);
        }

        return producer;
    }
    
    public PulsarClient getPulsarClient(String servInstID, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }
        
        Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
        PulsarConf pulsarConf = (PulsarConf) o;
        
        PulsarClient pulsarClient = null;
        try {
            pulsarClient = PulsarClient.builder().serviceUrl(pulsarConf.brokerConf.brokerAddr).build();
        } catch (PulsarClientException e) {
            throw new PaasSdkException(SdkErrInfo.e80051002);
        }
        
        return pulsarClient;
    }
    
    public Client getVoltDBClient(String servInstID, Map<String, Object> params) throws PaasSdkException {
        String topoStr = null;
        try {
            topoStr = loadPaasService(servInstID);
        } catch (PaasSdkException e) {
            logger.error("loadPaasService servInstID:{}, error:{}", servInstID, e.getMessage(), e);
            throw e;
        }
        
        String user = (String) params.get(FixHeader.HEADER_DB_USER);
        String password = (String) params.get(FixHeader.HEADER_DB_PASSWD);
        int connTimeout = (Integer) params.get(FixHeader.HEADER_CONN_TIMEOUT);
        
        Object o = PaasTopoParser.parseServiceTopo(topoStr, params);
        DBConfig dbConf = (DBConfig) o;
        
        ClientConfig config = null;
        config = new ClientConfig(user, password);
        config.setTopologyChangeAware(true);
        config.setConnectionResponseTimeout(connTimeout);

        Client client = ClientFactory.createClient(config);
        
        List<DBNode> dbNodes = dbConf.jdbc.masterDBSources.nodes;
        DBNode dbNode = dbNodes.get(0);
        String jdbcUrl = dbNode.url;
        // jdbc:voltdb://%s:%s,%s:%s?autoreconnect=true
        int start = jdbcUrl.indexOf("//") + 2;
        int end = jdbcUrl.indexOf("?");
        String nodeStr = jdbcUrl.substring(start, end);
        String[] nodeArr = nodeStr.split(",");
        
        boolean nok = true;
        for (int i = 0; i < nodeArr.length; ++i) {
            String node = nodeArr[i];
            String[] addrSplit = node.split(":");
            try {
                client.createConnection(addrSplit[0], Integer.valueOf(addrSplit[1]));
                nok = false;
                break;
            } catch (Exception e) {
                logger.error("voltdb node:{}, client create with exception:{}", node, e.getMessage(), e);
            }
        }
        
        if (nok) {
            throw new PaasSdkException(SdkErrInfo.e80042002);
        }
        
        return client;
    }

    public void postCollectData(String uri, Map<String, String> headers, String data) throws PaasSdkException {
        configFatory.postCollectData(uri, headers, data);
    }

}
