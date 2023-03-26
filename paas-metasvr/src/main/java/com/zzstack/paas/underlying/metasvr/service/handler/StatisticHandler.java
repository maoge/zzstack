package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.metasvr.bean.PassHostInfo;
import com.zzstack.paas.underlying.metasvr.bean.PassJvmInfo;
import com.zzstack.paas.underlying.metasvr.bean.PassRedisInfo;
import com.zzstack.paas.underlying.metasvr.bean.PassRocketMqInfo;
import com.zzstack.paas.underlying.metasvr.bean.collectd.CollectdPushData;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.HostStatisticDao;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.JvmStatisticDao;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.RedisStatisticDao;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.RocketMqStatisticDao;
import com.zzstack.paas.underlying.metasvr.iaas.Dashboard;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@App(path = "/paas/statistic")
public class StatisticHandler implements IServerHandler {

    // private static Logger logger = LoggerFactory.getLogger(StatisticHandler.class);

    @Service(id = "getJvmInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_START_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_END_TIMESTAMP, type = ParamType.ParamNumber, required = false)})
    public static void getJvmInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        Long startTimestamp = bodyJson.getLong(FixHeader.HEADER_START_TIMESTAMP);
        Long endTimestamp = bodyJson.getLong(FixHeader.HEADER_END_TIMESTAMP);
        JsonObject json = new JsonObject();
        JvmStatisticDao.getJvmInfo(json, instId, startTimestamp, endTimestamp);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveJvmInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_JVM_INFO_JSON_ARRAY, type = ParamType.ParamObject, required = false)
    })
    public static void saveJvmInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject json = new JsonObject();
        if (body == null) {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            json.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            HttpUtils.outJsonObject(ctx, json);
            return;
        }
        JsonObject bodyJson = body.getJsonObject();
        JsonObject jvmData = bodyJson.getJsonObject(FixHeader.HEADER_JVM_DATA);
        JsonArray jvmInfoJsonArray = jvmData.getJsonArray(FixHeader.HEADER_JVM_INFO_JSON_ARRAY);
        List<PassJvmInfo> passJvmInfoList = new ArrayList<>();
        // 根据上传的json元数据生成监控对象PassJvmInfo集合
        jvmInfoJsonArray.forEach(jvmInfoObject -> {
            JsonObject jvmInfoJson = (JsonObject) jvmInfoObject;
            Map<String, Object> jvmInfoMap = jvmInfoJson.getMap();
            PassJvmInfo passJvmInfo = PassJvmInfo.convert(jvmInfoMap);
            passJvmInfoList.add(passJvmInfo);
        });
        if (passJvmInfoList.size() > 0) {
            JvmStatisticDao.saveJvmInfo(json, passJvmInfoList);
        }
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getRedisServNodes", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false)})
    public static void getRedisServNodes(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);

        JsonObject json = new JsonObject();

        RedisStatisticDao.getRedisServNodes(json, servInstId);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getRocketMQServBrokers", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false)})
    public static void getRocketMQServBrokers(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        JsonObject json = new JsonObject();
        RocketMqStatisticDao.getRocketMQServBrokers(json, servInstId);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getRedisInstanceInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_START_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_END_TIMESTAMP, type = ParamType.ParamNumber, required = false)})
    public static void getRedisInstanceInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        if (StringUtils.isBlank(instId)) {
            instId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        }
        Long startTimestamp = bodyJson.getLong(FixHeader.HEADER_START_TIMESTAMP);
        Long endTimestamp = bodyJson.getLong(FixHeader.HEADER_END_TIMESTAMP);
        JsonObject json = new JsonObject();
        RedisStatisticDao.getRedisInstanceInfo(json, instId, startTimestamp, endTimestamp);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getRedisHaServiceInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamArray, required = false),
            @Parameter(name = FixHeader.HEADER_START_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_END_TIMESTAMP, type = ParamType.ParamNumber, required = false)})
    public static void getRedisHaServiceInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        JsonArray instIdArrayJson = bodyJson.getJsonArray(FixHeader.HEADER_INST_ID);
        Long startTimestamp = bodyJson.getLong(FixHeader.HEADER_START_TIMESTAMP);
        Long endTimestamp = bodyJson.getLong(FixHeader.HEADER_END_TIMESTAMP);
        JsonObject json = new JsonObject();
        RedisStatisticDao.getRedisServiceInfo(json, instIdArrayJson.stream().map(obj -> (String) obj).toArray(String[]::new), startTimestamp, endTimestamp);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveRedisInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_REDIS_INFO_JSON_ARRAY, type = ParamType.ParamObject, required = false)
    })
    public static void saveRedisInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject json = new JsonObject();
        if (body == null) {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            json.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            HttpUtils.outJsonObject(ctx, json);
            return;
        }
        JsonObject bodyJson = body.getJsonObject();
        JsonObject redisData = bodyJson.getJsonObject(FixHeader.HEADER_REDIS_DATA);
        JsonArray redisInfoJsonArray = redisData.getJsonArray(FixHeader.HEADER_REDIS_INFO_JSON_ARRAY);
        List<PassRedisInfo> passRedisInfoList = new ArrayList<>();
        // 根据上传的json元数据生成监控对象 PassRedisInfo 集合
        redisInfoJsonArray.forEach(redisInfoObject -> {
            JsonObject redisInfoJson = (JsonObject) redisInfoObject;
            Map<String, Object> redisInfoMap = redisInfoJson.getMap();
            PassRedisInfo passRedisInfo = PassRedisInfo.convert(redisInfoMap);
            passRedisInfoList.add(passRedisInfo);
        });
        if (passRedisInfoList.size() > 0) {
            RedisStatisticDao.saveRedisInfo(json, passRedisInfoList);
        }
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveHostInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_HOST_INFO_JSON_ARRAY, type = ParamType.ParamObject, required = false)
    })
    public static void saveHostInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject json = new JsonObject();
        if (body == null) {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            json.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            HttpUtils.outJsonObject(ctx, json);
            return;
        }
        JsonObject bodyJson = body.getJsonObject();
        JsonObject hostData = bodyJson.getJsonObject(FixHeader.HEADER_HOST_DATA);
        JsonArray hostInfoJsonArray = hostData.getJsonArray(FixHeader.HEADER_HOST_INFO_JSON_ARRAY);
        List<PassHostInfo> passHostInfoList = new ArrayList<>();
        // 根据上传的json元数据生成监控对象 PassHostInfo 集合
        hostInfoJsonArray.forEach(hostInfoObject -> {
            JsonObject hostInfoJson = (JsonObject) hostInfoObject;
            Map<String, Object> hostInfoMap = hostInfoJson.getMap();
            PassHostInfo passHostInfo = PassHostInfo.convert(hostInfoMap);
            passHostInfoList.add(passHostInfo);
        });
        if (passHostInfoList.size() > 0) {
            HostStatisticDao.saveHostInfo(json, passHostInfoList);
        }
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getHostInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_START_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_END_TIMESTAMP, type = ParamType.ParamNumber, required = false)})
    public static void getHostInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        Long startTimestamp = bodyJson.getLong(FixHeader.HEADER_START_TIMESTAMP);
        Long endTimestamp = bodyJson.getLong(FixHeader.HEADER_END_TIMESTAMP);
        JsonObject json = new JsonObject();
        HostStatisticDao.getHostInfo(json, instId, startTimestamp, endTimestamp);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveRocketmqInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_ROCKETMQ_INFO_JSON_ARRAY, type = ParamType.ParamObject, required = false)
    })
    public static void saveRocketmqInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject json = new JsonObject();
        if (body == null) {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
            json.put(FixHeader.HEADER_RET_INFO, CONSTS.ERR_DB);
            HttpUtils.outJsonObject(ctx, json);
            return;
        }

        JsonObject bodyJson = body.getJsonObject();
        JsonArray rocketmqInfoJsonArray = bodyJson.getJsonObject(FixHeader.HEADER_ROCKETMQ_DATA).getJsonArray(FixHeader.HEADER_ROCKETMQ_INFO_JSON_ARRAY);
        List<PassRocketMqInfo> passRocketMqInfoList = new ArrayList<>();
        // 根据上传的json元数据生成监控对象 PassRocketMqInfo 集合
        for (int i = 0; i < rocketmqInfoJsonArray.size(); i++) {
            JsonArray topicDetailArray = rocketmqInfoJsonArray.getJsonObject(i).getJsonArray("topic_detail");

            for (int j = 0; j < topicDetailArray.size(); j++) {
                JsonObject topicDetail = topicDetailArray.getJsonObject(j);
                Map<String, Object> redisInfoMap = new HashMap<String, Object>();
                redisInfoMap.put(FixHeader.HEADER_CREATE_TIME, System.currentTimeMillis());
                redisInfoMap.put(FixHeader.HEADER_UPDATE_TIME, System.currentTimeMillis());
                redisInfoMap.put(FixHeader.HEADER_TOPIC_NAME, rocketmqInfoJsonArray.getJsonObject(i).getString(FixHeader.HEADER_TOPIC_NAME));
                redisInfoMap.put(FixHeader.HEADER_CONSUME_GROUP, topicDetail.getString(FixHeader.HEADER_CONSUME_GROUP));
                redisInfoMap.put(FixHeader.HEADER_DIFF_TOTAL, topicDetail.getLong(FixHeader.HEADER_DIFF_TOTAL));
                redisInfoMap.put(FixHeader.HEADER_PRODUCE_TOTAL, topicDetail.getLong(FixHeader.HEADER_PRODUCE_TOTAL));
                redisInfoMap.put(FixHeader.HEADER_CONSUME_TOTAL, topicDetail.getLong(FixHeader.HEADER_CONSUME_TOTAL));
                redisInfoMap.put(FixHeader.HEADER_PRODUCE_TPS, topicDetail.getDouble(FixHeader.HEADER_PRODUCE_TPS));
                redisInfoMap.put(FixHeader.HEADER_CONSUME_TPS, topicDetail.getDouble(FixHeader.HEADER_CONSUME_TPS));
                redisInfoMap.put(FixHeader.HEADER_INST_ID, topicDetail.getString(FixHeader.HEADER_INST_ID));
                redisInfoMap.put(FixHeader.HEADER_TS, topicDetail.getString(FixHeader.HEADER_TS));
                PassRocketMqInfo passRocketMqInfo = PassRocketMqInfo.convert(redisInfoMap);
                passRocketMqInfoList.add(passRocketMqInfo);
            }

        }

        if (passRocketMqInfoList.size() > 0) {
            RocketMqStatisticDao.saveRocketMqInfo(json, passRocketMqInfoList);
        }
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getTopicNameByInstId", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false)})
    public static void getTopicNameByInstId(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        JsonObject json = new JsonObject();
        RocketMqStatisticDao.getTopicNameByInstId(json, instId);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getConsumeGroupByTopicName", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_TOPIC_NAME, type = ParamType.ParamString, required = false)})
    public static void getConsumeGroupByTopicName(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String topicName = bodyJson.getString(FixHeader.HEADER_TOPIC_NAME);
        JsonObject json = new JsonObject();
        RocketMqStatisticDao.getConsumeGroupByTopicName(json, topicName);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getRocketmqInfo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_START_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_END_TIMESTAMP, type = ParamType.ParamNumber, required = false),
            @Parameter(name = FixHeader.HEADER_TOPIC_NAME, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_CONSUME_GROUP, type = ParamType.ParamString, required = false)})
    public static void getRocketmqInfo(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        if (StringUtils.isBlank(instId)) {
            instId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        }
        Long startTimestamp = bodyJson.getLong(FixHeader.HEADER_START_TIMESTAMP);
        Long endTimestamp = bodyJson.getLong(FixHeader.HEADER_END_TIMESTAMP);
        String topicName = bodyJson.getString(FixHeader.HEADER_TOPIC_NAME);
        String consumeGroup = bodyJson.getString(FixHeader.HEADER_CONSUME_GROUP);
        JsonObject json = new JsonObject();

        RocketMqStatisticDao.getRocketMqServiceInfo(json, instId, topicName, consumeGroup, startTimestamp, endTimestamp);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveCollectd", auth = false, bwswitch = false)
    public static void saveCollectd(RoutingContext ctx) {
        HttpServerRequest req = ctx.request();
        String servIP = req.headers().get(FixHeader.HEADER_SERVER_IP);
        String body = ctx.body().asString();
        
        // logger.info(body);
        
        if (servIP != null && !servIP.isEmpty()) {
            CollectdPushData data = new CollectdPushData(servIP, body);
            Dashboard.get().pushCollectdMonitorData(data);
        }
        
        HttpUtils.outJsonObject(ctx, null);
    }

}
