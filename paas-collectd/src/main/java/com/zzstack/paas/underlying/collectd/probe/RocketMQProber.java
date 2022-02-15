package com.zzstack.paas.underlying.collectd.probe;

import com.alibaba.fastjson.JSON;
import com.zzstack.paas.underlying.collectd.global.CollectdGlobalData;
import com.zzstack.paas.underlying.sdk.PaasSDK;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;
import com.zzstack.paas.underlying.utils.exception.PaasCollectException;

import com.zzstack.paas.underlying.utils.exception.PaasSdkException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;

import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.*;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class RocketMQProber implements Prober {
    private static Logger logger = LoggerFactory.getLogger(RocketMQProber.class);
    private Map<String, Long> mapLastTimeProduce = new HashMap<String, Long>();
    private Map<String, Long> mapLastTimeConsume = new HashMap<String, Long>();
    
    private JsonObject jsonToReport = null;


    @Override
    public void doCollect(JsonObject topoJson) {
        JsonObject servJson = topoJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_SERV_CONTAINER);
        String strServerInstId = topoJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_SERV_CONTAINER).getString(FixHeader.HEADER_INST_ID);
        JsonObject rocketMqNameSrvContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_NAMESRV_CONTAINER);
        JsonArray rocketMqNameSrv = rocketMqNameSrvContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_NAMESRV);
        String namesrvAddr = "";
        for (int i = 0; i < rocketMqNameSrv.size(); i++) {
            JsonObject jsonRokectMq = rocketMqNameSrv.getJsonObject(i);
            namesrvAddr += jsonRokectMq.getString(FixHeader.HEADER_IP) +
                    ":" + jsonRokectMq.getString(FixHeader.HEADER_LISTEN_PORT) + ",";
        }

        if (namesrvAddr != null && !"".equals(namesrvAddr)) {
            namesrvAddr = namesrvAddr.substring(0, namesrvAddr.length() - 1);
        }

        //初始化监控客户端
        DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt();
        //defaultMQAdminExt.setNamesrvAddr("name-server1-ip:port;name-server2-ip:port");
        defaultMQAdminExt.setInstanceName("admin-" + System.currentTimeMillis());
        defaultMQAdminExt.setNamesrvAddr(namesrvAddr);  //TODO 从拓扑中获取
        TopicList topicList = null;
        try {
            defaultMQAdminExt.start();
            topicList = defaultMQAdminExt.fetchAllTopicList();

        } catch (Exception ex) {
            logger.error(String.format("collectTopicOffset-exception comes getting topic list from namesrv, address is %s",
                    JSON.toJSONString(defaultMQAdminExt.getNameServerAddressList())));
            defaultMQAdminExt.shutdown();
            return;
        }

        Set<String> topicSet = topicList != null ? topicList.getTopicList() : null;
        if (topicSet == null || topicSet.isEmpty()) {
            logger.error(String.format("collectTopicOffset-the topic list is empty. the namesrv address is %s",
                    JSON.toJSONString(defaultMQAdminExt.getNameServerAddressList())));
            defaultMQAdminExt.shutdown();
            return;
        }
        JsonObject rocketMqVBrokerContainer = servJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_VBROKER_CONTAINER);
        JsonArray rocketMqVBroker = rocketMqVBrokerContainer.getJsonArray(FixHeader.HEADER_ROCKETMQ_VBROKER);
        //过滤字段
        Set<String> filterTopicSet = new HashSet<String>();
        filterTopicSet.add("RMQ_SYS_TRANS_HALF_TOPIC");
        filterTopicSet.add("BenchmarkTest");
        filterTopicSet.add("OFFSET_MOVED_EVENT");
        filterTopicSet.add("TBW102");
        filterTopicSet.add("%RETRY%TOOLS_CONSUMER");
        filterTopicSet.add("SELF_TEST_TOPIC");
        for (int i = 0; i < rocketMqVBroker.size(); i++) {
            JsonObject jsonRocketbroker = rocketMqVBroker.getJsonObject(i);
            String vbrokerInstId = jsonRocketbroker.getString(FixHeader.HEADER_INST_ID);
            filterTopicSet.add(vbrokerInstId);
            JsonArray jsonBrokerArray = jsonRocketbroker.getJsonArray(FixHeader.HEADER_ROCKETMQ_BROKER);
            for (int j = 0; j < jsonBrokerArray.size(); j++) {
                filterTopicSet.add(jsonBrokerArray.getJsonObject(i).getString(FixHeader.HEADER_INST_ID));
            }
        }

        JsonArray jsonAllTopicData = new JsonArray();
        for (String topic : topicSet) {

            if (filterTopicSet.contains(topic)) {
                continue;
            }

            if (topic.indexOf("%RETRY%") != -1) {
                continue;
            }

            try {
                GroupList groupList = defaultMQAdminExt.queryTopicConsumeByWho(topic);

                Set<String> groupSet = groupList != null ? groupList.getGroupList() : null;
                if (groupSet == null) {
                    logger.error(String.format("queryTopicConsumeByWho-the group list is null. the namesrv address is %s",
                            JSON.toJSONString(defaultMQAdminExt.getNameServerAddressList())));
                    defaultMQAdminExt.shutdown();
                    return;
                }
                JsonObject jsonTopic = new JsonObject();
                jsonTopic.put(FixHeader.HEADER_TOPIC_NAME, topic);
                JsonArray jsonGroupArray = new JsonArray();
                for (String groupName : groupSet) {
                    //一个消费组多条数据
                    JsonArray diffNum = getDiffNum(defaultMQAdminExt, groupName);
                    jsonGroupArray.addAll(diffNum);
                }


                jsonTopic.put("topic_detail", jsonGroupArray);
                jsonAllTopicData.add(jsonTopic);
            } catch (Exception ex) {
                logger.error("fetch topic and consume_group data failed... ");
                defaultMQAdminExt.shutdown();
                return;
            }

        }
        defaultMQAdminExt.shutdown();

        //计算所有
        JsonObject jsonTopicTotal = new JsonObject();
        jsonTopicTotal.put(FixHeader.HEADER_TOPIC_NAME, "");

        double consumeTps = 0;
        double produceTps = 0;
        int consumeTotal = 0;
        int produceTotal = 0;
        int diffTotal = 0;
        for (int i = 0; i < jsonAllTopicData.size(); i++) {
            JsonObject jsonObject = jsonAllTopicData.getJsonObject(i);
            JsonArray topic_detail = jsonObject.getJsonArray("topic_detail");
            for (int j = 0; j < topic_detail.size(); j++) {
                consumeTps += topic_detail.getJsonObject(j).getDouble(FixHeader.HEADER_CONSUME_TPS);
                produceTps += topic_detail.getJsonObject(j).getDouble(FixHeader.HEADER_PRODUCE_TPS);
                consumeTotal += topic_detail.getJsonObject(j).getInteger(FixHeader.HEADER_CONSUME_TOTAL);
                produceTotal += topic_detail.getJsonObject(j).getInteger(FixHeader.HEADER_PRODUCE_TOTAL);
                diffTotal += topic_detail.getJsonObject(j).getInteger(FixHeader.HEADER_DIFF_TOTAL);
            }

        }
        JsonObject jsonAllConsume = new JsonObject();
        jsonAllConsume.put(FixHeader.HEADER_CONSUME_GROUP, "");
        jsonAllConsume.put(FixHeader.HEADER_INST_ID, strServerInstId);
        jsonAllConsume.put(FixHeader.HEADER_TS, System.currentTimeMillis());
        jsonAllConsume.put(FixHeader.HEADER_CONSUME_TPS, consumeTps);
        jsonAllConsume.put(FixHeader.HEADER_PRODUCE_TPS, produceTps);
        jsonAllConsume.put(FixHeader.HEADER_CONSUME_TOTAL, consumeTotal);
        jsonAllConsume.put(FixHeader.HEADER_PRODUCE_TOTAL, produceTotal);
        jsonAllConsume.put(FixHeader.HEADER_DIFF_TOTAL, diffTotal);
        JsonArray jsonGroupArray = new JsonArray();
        jsonGroupArray.add(jsonAllConsume);
        jsonTopicTotal.put("topic_detail", jsonGroupArray);
        jsonAllTopicData.add(jsonTopicTotal);

        topoJson.put(FixHeader.HEADER_ROCKETMQ_INFO_JSON_ARRAY, jsonAllTopicData);

        jsonToReport = new JsonObject();
        jsonToReport.put(FixHeader.HEADER_ROCKETMQ_DATA, topoJson);
    }
    
    @Override
    public void doReport() throws PaasSdkException {
        if (jsonToReport != null) {
            PaasSDK paasSdk = CollectdGlobalData.get().getSdk();
            Map<String, String> postHeads = new HashMap<>();
            postHeads.put("CONTENT-TYPE", "application/json");
            paasSdk.postCollectData("/paas/statistic/saveRocketmqInfo", postHeads, jsonToReport.toString());
        }
    }

    @Override
    public void doAlarm() throws PaasCollectException {
        // TODO Auto-generated method stub

    }

    @Override
    public void doRecover() throws PaasCollectException {
        // TODO Auto-generated method stub

    }


    /**
     * 获取消息堆积量
     */
    public JsonArray getDiffNum(DefaultMQAdminExt defaultMQAdminExt, String groupName) {
        long diffTotal = 0L;
        long produceTotal = 0L;
        long consumeTotal = 0L;
        JsonArray jsonStatArray = new JsonArray();
        try {
            //当消费端未消费时，此方法会报错
            ConsumeStats consumeStats = defaultMQAdminExt.examineConsumeStats(groupName);
            List<MessageQueue> mqList = new LinkedList<MessageQueue>();
            HashMap<MessageQueue, OffsetWrapper> offsetTable = consumeStats.getOffsetTable();

            mqList.addAll(offsetTable.keySet());
            Collections.sort(mqList);
            //根据broker_name进行分组
            Map<String, List<MessageQueue>> mapInstGroup = new HashMap<String, List<MessageQueue>>();
            for (MessageQueue mq : mqList) {

                List<MessageQueue> instIdMq = null;
                if (mapInstGroup.get(mq.getBrokerName()) != null) {
                    instIdMq = mapInstGroup.get(mq.getBrokerName());
                } else {
                    instIdMq = new ArrayList<MessageQueue>();
                }
                instIdMq.add(mq);
                mapInstGroup.put(mq.getBrokerName(), instIdMq);
            }

            for (String instId : mapInstGroup.keySet()) {
                List<MessageQueue> messageQueues = mapInstGroup.get(instId);
                for (MessageQueue mq : messageQueues) {
                    OffsetWrapper offsetWrapper = (OffsetWrapper) offsetTable.get(mq);
                    //生产队列消息量 - 消息消费量 = 消息堆积量
                    produceTotal += offsetWrapper.getBrokerOffset();
                    consumeTotal += offsetWrapper.getConsumerOffset();

                    long diff = offsetWrapper.getBrokerOffset() - offsetWrapper.getConsumerOffset();
                    diffTotal += diff;

                }
//              double consumeTps = consumeStats.getConsumeTps();
                JsonObject jsonTotal = new JsonObject();
                jsonTotal.put(FixHeader.HEADER_CONSUME_GROUP, groupName);
                jsonTotal.put(FixHeader.HEADER_DIFF_TOTAL, diffTotal);
                jsonTotal.put(FixHeader.HEADER_PRODUCE_TOTAL, produceTotal);
                jsonTotal.put(FixHeader.HEADER_CONSUME_TOTAL, consumeTotal);
                jsonTotal.put(FixHeader.HEADER_INST_ID, instId);
                jsonTotal.put(FixHeader.HEADER_TS, System.currentTimeMillis());
                double produceTps = 0;
                double consumeTps = 0;
                String uniqueKey = messageQueues.get(0).getTopic();
                String key = instId + ":" + uniqueKey;
                Long produceCounter = mapLastTimeProduce.get(key);
                if (produceCounter == null) {
                    mapLastTimeProduce.put(key, produceTotal);
                    produceTps = 0;
                } else {
                    produceTps = (produceTotal - produceCounter) / CONSTS.PROBER_COLLECT_INTERVAL;
                    mapLastTimeProduce.put(key, produceTotal);
                }

                Long consumeCounter = mapLastTimeConsume.get(key);

                if (consumeCounter == null) {
                    mapLastTimeConsume.put(key, consumeTotal);
                    consumeTps = 0;
                } else {
                    consumeTps = (consumeTotal - consumeCounter) / CONSTS.PROBER_COLLECT_INTERVAL;
                    mapLastTimeConsume.put(key, consumeTotal);
                }

                jsonTotal.put(FixHeader.HEADER_CONSUME_TPS, consumeTps);
                jsonTotal.put(FixHeader.HEADER_PRODUCE_TPS, produceTps);

                jsonStatArray.add(jsonTotal);
            }

        } catch (Throwable e) {
            logger.error("监控客户端获取消息堆积量异常，未能正常获取消息堆积量", e);
        }

        return jsonStatArray;
    }

}
