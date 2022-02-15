package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.metasvr.consts.FixDefs;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/alarm")
public class AlarmHandler implements IServerHandler {

    @Service(id = "getAlarmCount", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_DEAL_FLAG, type = ParamType.ParamString, required = false) })
    public static void getAlarmCount(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String dealFlag = bodyJson.getString(FixHeader.HEADER_DEAL_FLAG);
        if (dealFlag == null)
            dealFlag = FixDefs.ALARM_ALL;
        
        JsonObject json = new JsonObject();
        MetaDataDao.getAlarmCount(json, dealFlag);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getAlarmList", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_DEAL_FLAG, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getAlarmList(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String dealFlag = bodyJson.getString(FixHeader.HEADER_DEAL_FLAG);
        if (dealFlag == null)
            dealFlag = FixDefs.ALARM_ALL;
        
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getAlarmList(json, pageSize, pageNum, servInstId, instId, dealFlag);
        
        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "clearAlarm", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_ALARM_ID, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_ALARM_TYPE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_DEAL_ACC_NAME, type = ParamType.ParamString, required = true)})
    public static void clearAlarm(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        System.out.println(body.toString());
        
        long alarmId = bodyJson.getLong(FixHeader.HEADER_ALARM_ID);
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String accName = bodyJson.getString(FixHeader.HEADER_DEAL_ACC_NAME);
        int alarmType = bodyJson.getInteger(FixHeader.HEADER_ALARM_TYPE);
        // String magicKey = MagicKeyUtils.getMagicKey(ctx);
        long dealTime = System.currentTimeMillis();
        
        // 清除缓存
        MetaDataDao.clearAlarmCache(instId, alarmType);
        
        ResultBean result = new ResultBean();
        MetaDataDao.updateAlarmStateByAlarmId(alarmId, dealTime, accName, FixDefs.ALARM_DEALED, result);
        HttpUtils.outResultBean(ctx, result);
    }

}
