package sdk.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import com.alibaba.fastjson.JSONObject;
import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.LoadBalancedDBSrcPool;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;
import com.zzstack.paas.underlying.redis.MultiRedissonClient;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.node.RedissonClientHolder;
import com.zzstack.paas.underlying.redis.utils.RedissonConfParser;
import com.zzstack.paas.underlying.sdk.PaasSDK;
import com.zzstack.paas.underlying.sdk.config.MetaSvrConfigFatory;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.SVarObject;
import com.zzstack.paas.underlying.utils.config.CacheRedisClusterConf;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import com.zzstack.paas.underlying.utils.paas.PaasTopoParser;

public class SdkTestMain {
    
    private static Logger logger = LoggerFactory.getLogger(SdkTestMain.class);

    public static void main(String[] args) throws PaasSdkException, DBException {
        // testPaasDBClient();
        // testPaasLoadBalancedQueue();
        // testRedisClusterCache();
        // testSDK();
        
        // testPulsarClient();
        
        // testVoltDBClient1();
        // testVoltDBClient2();
        
        testClickHouse();
    }
    
    public static void testSKD() {
        PaasSDK paasSDK = new PaasSDK("http://172.20.0.41:10000, http://172.20.0.42:10000", "dev", "abcd.1234");
        try {
            String topo = paasSDK.loadPaasService("f0f33845-5d13-4dd1-981b-fcef016060fd");
            System.out.println(topo);
        } catch (PaasSdkException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static void testPaasDBClient() throws PaasSdkException, DBException {
        Map<String, Object> params = new HashMap<String, Object>();
        // String dgName = (String) params.get(FixHeader.HEADER_DG_NAME);
        // conf.jdbc.decrypt = (boolean) params.get(FixHeader.HEADER_DECRYPT);
        // conf.jdbc.dbSourceModel = (String) params.get(FixHeader.HEADER_DB_SOURCE_MODEL);
        // conf.jdbc.minIdle = (int) params.get(FixHeader.HEADER_MIN_IDLE);
        // conf.jdbc.maxActive = (int) params.get(FixHeader.HEADER_MAX_ACTIVE);
        // conf.jdbc.validationQuery = (String) params.get(FixHeader.HEADER_VALIDATION_QUERY);

        String metadbDgName = "metadb";
        String realdbDgName = "realdb1";
        String dbServID = "0fce373a-d1c9-42d0-9a8c-433973f5048d";

        params.put(FixHeader.HEADER_DG_NAME, metadbDgName);
        params.put(FixHeader.HEADER_DECRYPT, false);
        params.put(FixHeader.HEADER_DB_SOURCE_MODEL, CONSTS.DB_SOURCE_POOL_DRUID);
        params.put(FixHeader.HEADER_MIN_IDLE, 10);
        params.put(FixHeader.HEADER_MAX_ACTIVE, 20);
        params.put(FixHeader.HEADER_VALIDATION_QUERY, CONSTS.DB_TEST_SQL_ORACLE);
        // metadb 查询
        PaasSDK paasSDK = new PaasSDK("http://127.0.0.1:9090", "admin", "admin.1234");
        paasSDK.getPaasDBClient(dbServID, metadbDgName, params);
        String querySql = "SELECT COUNT(*) FROM SMSDB.ERR_LOG";
        boolean queryDbResult = testQueryMetaOrRealDB(querySql, metadbDgName);
        System.out.println("queryDbResult:" + queryDbResult);
        // metadb 插入
//        String insertSql = "INSERT INTO SMSDB.ERR_LOG(PROC_NAME, ERROR_INFO) VALUES(?,?)";
//        List<Object> insertParamsList = new ArrayList<>();
//        insertParamsList.add("linteng-test-metadb");
//        insertParamsList.add("linteng-test-metadb");
//        boolean insertDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(insertSql, metadbDgName, insertParamsList);
//        System.out.println("insertDbResult:" + insertDbResult);
        // metadb 修改
//        String updateSql = "UPDATE SMSDB.ERR_LOG SET PROC_NAME=? WHERE PROC_NAME=?";
//        List<Object> updateParamsList = new ArrayList<>();
//        updateParamsList.add("linteng-test-metadb-update");
//        updateParamsList.add("linteng-test-metadb");
//        boolean updateDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(updateSql, metadbDgName, updateParamsList);
//        System.out.println("updateDbResult:" + updateDbResult);
        // metadb 删除
//        String deleteSql = "DELETE SMSDB.ERR_LOG WHERE PROC_NAME=?";
//        List<Object> deleteParamsList = new ArrayList<>();
//        deleteParamsList.add("linteng-test-metadb-update");
//        boolean deleteDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(deleteSql, metadbDgName, deleteParamsList);
//        System.out.println("deleteDbResult:" + deleteDbResult);

        // realdb1 查询
//        params.put(FixHeader.HEADER_DG_NAME, realdbDgName);
//        paasSDK.getPaasDBClient(dbServID, realdbDgName, params);
//        String querySql = "SELECT COUNT(*) FROM SMSDB.ERR_LOG";
//        boolean queryDbResult = testQueryMetaOrRealDB(querySql, realdbDgName);
//        System.out.println("queryDbResult:" + queryDbResult);

        // realdb1 插入
//        String insertSql = "INSERT INTO SMSDB.ERR_LOG(PROC_NAME, ERROR_INFO) VALUES(?,?)";
//        List<Object> insertParamsList = new ArrayList<>();
//        insertParamsList.add("linteng-test-metadb");
//        insertParamsList.add("linteng-test-metadb");
//        boolean insertDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(insertSql, realdbDgName, insertParamsList);
//        System.out.println("insertDbResult:" + insertDbResult);
        // realdb1 修改
//        String updateSql = "UPDATE SMSDB.ERR_LOG SET PROC_NAME=? WHERE PROC_NAME=?";
//        List<Object> updateParamsList = new ArrayList<>();
//        updateParamsList.add("linteng-test-metadb-update");
//        updateParamsList.add("linteng-test-metadb");
//        boolean updateDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(updateSql, realdbDgName, updateParamsList);
//        System.out.println("updateDbResult:" + updateDbResult);
        // realdb1 删除
//        String deleteSql = "DELETE SMSDB.ERR_LOG WHERE PROC_NAME=?";
//        List<Object> deleteParamsList = new ArrayList<>();
//        deleteParamsList.add("linteng-test-metadb-update");
//        boolean deleteDbResult = testInsertOrUpdateOrDeleteMetaOrRealDB(deleteSql, realdbDgName, deleteParamsList);
//        System.out.println("deleteDbResult:" + deleteDbResult);
    }

    private static boolean testQueryMetaOrRealDB(String querySql, String dgName) throws DBException {
        SqlBean sqlBean = new SqlBean(querySql);
        CRUD c = new CRUD(dgName);
        c.putSqlBean(sqlBean);
        Map<String, Object> queryMap = c.queryForMap();
        return queryMap.size() > 0;
    }

    @SuppressWarnings("unused")
    private static boolean testInsertOrUpdateOrDeleteMetaOrRealDB(String insertOrUpdateOrDeleteSql, String dgName, List<Object> params) throws DBException {
        SqlBean sqlBean = new SqlBean(insertOrUpdateOrDeleteSql, params);
        CRUD c = new CRUD(dgName);
        c.putSqlBean(sqlBean);
        return c.executeUpdate();
    }

    @SuppressWarnings("unused")
    private static void testPaasLoadBalancedQueue() {
        Map<String, Object> params = new HashMap<>();
        params.put(FixHeader.HEADER_CLIENT_NAME, "smsserver-queue");
        params.put(FixHeader.HEADER_SLAVE_MIN_IDLE_SIZE, 10);
        params.put(FixHeader.HEADER_SLAVE_POOL_SIZE, 20);
        params.put(FixHeader.HEADER_MASTER_MIN_IDLE_SIZE, 10);
        params.put(FixHeader.HEADER_MASTER_POOL_SIZE, 20);
        params.put(FixHeader.HEADER_READ_MODE, CONSTS.READ_MODE_MASTER_SLAVE);
        params.put(FixHeader.HEADER_SERVER_MODE, CONSTS.REDIS_SERVER_MODE_CLUSTER);
        PaasSDK paasSDK = new PaasSDK("http://127.0.0.1:9090", "admin", "admin.1234");
        String cacheServID = "4fb2a691-8d9b-4701-add5-576e048135c4";
        String cacheServName = "lt-switch-cache";
        try {
            paasSDK.getPaasLoadBalancedQueue(cacheServID, cacheServName, params);
            WeightedRRLoadBalancer weightedRRLoadBalancer = MultiRedissonClient.get(cacheServName);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("lt", "test");
            for (int i = 0; i < 10; i++) {
                RedissonClientHolder redissonClientHolder = (RedissonClientHolder) weightedRRLoadBalancer.select();
                RedissonClient redissonClient = redissonClientHolder.getRedissonClient();
                //lpushMo(redissonClient, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8), String.valueOf(i));
                rpopMO(redissonClient, i);
            }
        } catch (PaasSdkException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static boolean lpushMo(RedissonClient redissonClient, byte[] taskjson, String custId) {
        String identifier = String.format("mo:queue:%s", custId);
        RDeque<byte[]> deque = redissonClient.getDeque(identifier);
        return deque.add(taskjson);
    }

    public static boolean rpopMO(RedissonClient redissonClient, int custId) {
        String identifier = String.format("mo:queue:%s", custId);
        RDeque<byte[]> deque = redissonClient.getDeque(identifier);
        byte[] msgs = deque.pollLast();
        return msgs != null && msgs.length > 0;
    }

    @SuppressWarnings("unused")
    private static void testPaasRocketMQProducer() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("group_name", "smsserver-queue");
        PaasSDK paasSDK = new PaasSDK("http://127.0.0.1:9090", "admin", "admin.1234");
        String mqServID = "21c30b69-b624-4c62-ad1a-d18cfcda4f65";
        try {
            paasSDK.getPaasRocketMQProducer(mqServID, params);
        } catch (PaasSdkException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static boolean testRedisClusterCache() throws PaasSdkException {
        Map<String, Object> params = new HashMap<>();
        params.put(FixHeader.HEADER_CLIENT_NAME, "lt-cache2");
        params.put(FixHeader.HEADER_SLAVE_MIN_IDLE_SIZE, 10);
        params.put(FixHeader.HEADER_SLAVE_POOL_SIZE, 20);
        params.put(FixHeader.HEADER_MASTER_MIN_IDLE_SIZE, 10);
        params.put(FixHeader.HEADER_MASTER_POOL_SIZE, 20);
        params.put(FixHeader.HEADER_READ_MODE, CONSTS.READ_MODE_MASTER_SLAVE);
        params.put(FixHeader.HEADER_SERVER_MODE, CONSTS.REDIS_SERVER_MODE_CLUSTER);
        String servInstID = "cc1bab3a-e553-449e-9507-74a21ac5d444";
        SVarObject result = new SVarObject();
        MetaSvrConfigFatory configFatory = MetaSvrConfigFatory.getInstance("http://127.0.0.1:9090", "admin", "admin.1234");
        if (!configFatory.loadServiceTopo(servInstID, result)) {
            throw new PaasSdkException(PaasSdkException.SdkErrInfo.e80040001);
        }
        Object o = PaasTopoParser.parseServiceTopo(result.getVal(), params);
        CacheRedisClusterConf redissonConf = (CacheRedisClusterConf) o;
        RedissonClientHolder redissonClientHolder = RedissonConfParser.fromRedissonConf(redissonConf);
        RedissonClient redissonClient = redissonClientHolder.getRedissonClient();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lt", "cache");
        for (int i = 0; i < 10; i++) {
            boolean isSuccess = lpushMo(redissonClient, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8), String.valueOf(i));
            if (!isSuccess) {
                return false;
            }
        }
        return true;
    }
    
    public static void testPulsarClient() throws PaasSdkException {
        PaasSDK paasSDK = new PaasSDK("http://172.20.0.45:10000", "dev", "abcd.1234");
        try {
            PulsarClient pulsarClient = paasSDK.getPulsarClient("2c1e33ad-eaef-46d6-a238-1aaf082e0b83", null);
            
            Producer<byte[]> producer = pulsarClient.newProducer()
                    .topic("persistent://bench-tenant/bench-namespace/persist-topic-bench")
                    .enableBatching(true)
                    .batchingMaxMessages(1000)
                    .batchingMaxPublishDelay(5, TimeUnit.MILLISECONDS)
                    .maxPendingMessages(80000)
                    .maxPendingMessagesAcrossPartitions(80000)
                    .sendTimeout(0, TimeUnit.MILLISECONDS)
                    .messageRoutingMode(MessageRoutingMode.RoundRobinPartition)
                    .hashingScheme(HashingScheme.Murmur3_32Hash)
                    .roundRobinRouterBatchingPartitionSwitchFrequency(200)
                    .compressionType(CompressionType.LZ4)
                    .blockIfQueueFull(true)
                    .autoUpdatePartitions(true)
                    .autoUpdatePartitionsInterval(60, TimeUnit.SECONDS)
                    .create();
            
            String msg = new String("pulsar bench data ...... "
                    + "docker run -it -p 9527:9527 -p 7750:7750 -e REDIRECT_HOST=http://localhost -e REDIRECT_PORT=9527 "
                    + "-e DRIVER_CLASS_NAME=org.postgresql.Driver");
            
            byte[] buf = msg.getBytes();
            // producer.send(buf);
            producer.sendAsync(buf);
            producer.close();
            System.out.println("send ok ......");
            
            Consumer<byte[]> consumer = pulsarClient.newConsumer()
                    .topic("persistent://bench-tenant/bench-namespace/persist-topic-bench")
                    .subscriptionName("persist-subscriber-000")
                    .subscriptionType(SubscriptionType.Shared)
                    .receiverQueueSize(1000)
                    .acknowledgmentGroupTime(10, TimeUnit.MILLISECONDS)
                    .consumerName("consumer-0")
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Latest)
                    .ackTimeout(5000, TimeUnit.MILLISECONDS)
                    .deadLetterPolicy(DeadLetterPolicy.builder().maxRedeliverCount(10).build())
                    .batchReceivePolicy(BatchReceivePolicy.builder().maxNumMessages(1000).maxNumBytes(8 * 1024 * 1024).timeout(3000, TimeUnit.MILLISECONDS).build())
                    .subscriptionTopicsMode(RegexSubscriptionMode.AllTopics)
                    .subscriptionMode(SubscriptionMode.Durable)
                    .autoUpdatePartitions(true)
                    .autoUpdatePartitionsInterval(60, TimeUnit.SECONDS)
                    .enableRetry(true)
                    .enableBatchIndexAcknowledgment(true)
                    .maxPendingChuckedMessage(100)
                    .subscribe();
            
            Message<byte[]> msgBytes = consumer.receive();
            System.out.println("recv:");
            System.out.println(new String(msgBytes.getData()));
            consumer.close();
            
            pulsarClient.close();
            
        } catch (PaasSdkException e) {
            e.printStackTrace();
        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
        
    }
    
    /*
    create table zz_account (
      acc_id varchar(48) not null primary key,
      acc_name varchar(32) not null,
      phone_num varchar(15) not null,
      mail varchar(48) not null,
      passwd varchar(72) not null,
      create_time BIGINT not null
    );
    
    PARTITION TABLE zz_account ON COLUMN acc_id;
    
    create procedure sel_account_by_id 
    as
        select acc_id, acc_name, phone_num, mail, passwd, create_time
        from zz_account where acc_id = ?;
    */
    @SuppressWarnings("unused")
    private static void testVoltDBClient1() {
        PaasSDK paasSDK = new PaasSDK("http://172.20.0.45:10000", "dev", "abcd.1234");
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(FixHeader.HEADER_DB_USER, "admin");
        params.put(FixHeader.HEADER_DB_PASSWD, "admin.1234");
        params.put(FixHeader.HEADER_DECRYPT, false);
        params.put(FixHeader.HEADER_MIN_IDLE, 5);
        params.put(FixHeader.HEADER_MAX_ACTIVE, 10);
        params.put(FixHeader.HEADER_CONN_TIMEOUT, 10000);
        
        Client client = null;
        try {
            client = paasSDK.getVoltDBClient("ff633dea-00cf-4c96-8889-bff72b946762", params);
        } catch (PaasSdkException e) {
            e.printStackTrace();
            return;
        }
        
        try {
            org.voltdb.client.ClientResponse resp = client.callProcedure("sel_account_by_id", 1000);
            VoltTable[] vtable = resp.getResults();
            byte status = resp.getStatus();
            
            if (status != ClientResponse.SUCCESS) {
                System.out.println(resp.getStatusString());
            } else {
                int size = vtable.length;
                if (size == 0)
                    return;
                
                VoltTable table0 = vtable[0];
                int columnCount = table0.getColumnCount();
                StringBuilder columnInfo = new StringBuilder("|");
                for (int i = 0; i < columnCount; ++i) {
                    columnInfo.append(table0.getColumnName(i)).append("|");
                }
                System.out.println(columnInfo.toString());
                
                for (int i = 0; i < size; ++i) {
                    VoltTable table = vtable[i];
                    StringBuilder values = null;
                    while (table.advanceRow()) {
                        if (values == null)
                            values = new StringBuilder("|");
                        
                        values.append((String) table.get(0, org.voltdb.VoltType.STRING)).append("|");
                        values.append((String) table.get(1, org.voltdb.VoltType.STRING)).append("|");
                        values.append((String) table.get(2, org.voltdb.VoltType.STRING)).append("|");
                        values.append((String) table.get(3, org.voltdb.VoltType.STRING)).append("|");
                        values.append((String) table.get(4, org.voltdb.VoltType.STRING)).append("|");
                        values.append((Long) table.get(5, org.voltdb.VoltType.BIGINT)).append("|");
                    }
                    
                    if (values != null)
                        System.out.println(values.toString());
                }
                
            }
            
        } catch (IOException | ProcCallException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (InterruptedException e) {
                
            }
        }
    }
    
    @SuppressWarnings("unused")
    private static void testVoltDBClient2() {
        PaasSDK paasSDK = new PaasSDK("http://172.20.0.45:10000", "dev", "abcd.1234");
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(FixHeader.HEADER_DB_USER, "admin");
        params.put(FixHeader.HEADER_DB_PASSWD, "admin.1234");
        params.put(FixHeader.HEADER_DECRYPT, false);
        params.put(FixHeader.HEADER_MIN_IDLE, 10);
        params.put(FixHeader.HEADER_MAX_ACTIVE, 20);
        params.put(FixHeader.HEADER_CONN_TIMEOUT, 10000);
        params.put(FixHeader.HEADER_DB_SOURCE_MODEL, CONSTS.DB_SOURCE_POOL_DRUID);
        params.put(FixHeader.HEADER_VALIDATION_QUERY, CONSTS.DB_TEST_SQL_VOLTDB);
        
        LoadBalancedDBSrcPool dbPool = null;
        try {
            dbPool = paasSDK.getPaasLoadBalancedDBPool("ff633dea-00cf-4c96-8889-bff72b946762", "voltdb", params);
        } catch (PaasSdkException e) {
            e.printStackTrace();
            return;
        }
        
        String sql = "select acc_id, acc_name, phone_num, mail, passwd, create_time from zz_account where acc_id = ?";
        SqlBean sqlBean = new SqlBean(sql);

        sqlBean.addParams(new Object[] { 1000 });
        CRUD c = new CRUD("voltdb", 1);
        c.putSqlBean(sqlBean);
        try {
            Map<String, Object> resultMap = c.queryForMap();
            Set<Entry<String, Object>> entrySet = resultMap.entrySet();
            Iterator<Entry<String, Object>> it = entrySet.iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                System.out.println(entry.getKey() + " -> " + entry.getValue().toString());
            }
            
        } catch (DBException e) {
            e.printStackTrace();
        } finally {
            if (dbPool != null) {
                LoadBalancedDBSrcPool.destry("voltdb");
            }
        }
    }
    
    private static void testClickHouse() {
        Map<String, Object> params = new HashMap<>();
        // params.put(FixHeader.HEADER_DG_NAME, "clickhouse");
        params.put(FixHeader.HEADER_DECRYPT, false);
        params.put(FixHeader.HEADER_DB_SOURCE_MODEL, CONSTS.DB_SOURCE_POOL_HIKARI);
        params.put(FixHeader.HEADER_MIN_IDLE, 10);
        params.put(FixHeader.HEADER_MAX_ACTIVE, 20);
        params.put(FixHeader.HEADER_VALIDATION_QUERY, CONSTS.DB_TEST_SQL_CLICKHOUSE);
        
        params.put(FixHeader.HEADER_DB_USER, "default");
        params.put(FixHeader.HEADER_DB_PASSWD, "abcd.1234");
        params.put(FixHeader.HEADER_DB_NAME, "smsdb");

        try {
            PaasSDK paasSDK = new PaasSDK("http://172.20.0.162:10000", "dev", "abcd.1234");
            paasSDK.getPaasDBClient("3385be29-50a6-4f9b-8792-b38ef8eb9d2f", "", params);
        } catch (PaasSdkException e) {
            e.printStackTrace();
        }
        
    }

}
