package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.AccOperLogDao;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/account")
public class AccountHandler implements IServerHandler {

    @Service(id = "login", method = HttpMethodEnum.POST, auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_USER, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_PASSWORD, type = ParamType.ParamString, required = true) })
    public static void login(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String user = bodyJson.getString(FixHeader.HEADER_USER);
        String passwd = bodyJson.getString(FixHeader.HEADER_PASSWORD);
        
        ResultBean result = new ResultBean();
        MetaDataDao.login(user, passwd, result);
        
        HttpUtils.outResultBean(ctx, result);
    }

    // 自己给自己改密
    @Service(id = "modPassWord", method = HttpMethodEnum.POST, auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_MAGIC_KEY, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_PASSWORD, type = ParamType.ParamString, required = true) })
    public static void modPassWord(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String magicKey = bodyJson.getString(FixHeader.HEADER_MAGIC_KEY);
        String passwd = bodyJson.getString(FixHeader.HEADER_PASSWORD);
        
        JsonObject result = new JsonObject();
        MetaDataDao.modPasswd(magicKey, passwd, result);
        
        HttpUtils.outJsonObject(ctx, result);
    }
    
    @Service(id = "getOpLogCnt", method = HttpMethodEnum.POST, auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_USER, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_START_TS, type = ParamType.ParamNumber, required = true),
            @Parameter(name = FixHeader.HEADER_END_TS, type = ParamType.ParamNumber, required = true) })
    public static void getOpLogCnt(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String accName = bodyJson.getString(FixHeader.HEADER_USER);
        long startTS   = bodyJson.getLong(FixHeader.HEADER_START_TS);
        long endTS     = bodyJson.getLong(FixHeader.HEADER_END_TS);
        
        JsonObject retval = new JsonObject();
        AccOperLogDao.getOpLogCnt(retval, accName, startTS, endTS);
        
        HttpUtils.outJsonObject(ctx, retval);
    }

    @Service(id = "getOpLogList", method = HttpMethodEnum.POST, auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_USER, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_START_TS, type = ParamType.ParamNumber, required = true),
            @Parameter(name = FixHeader.HEADER_END_TS, type = ParamType.ParamNumber, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getOpLogList(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String accName = bodyJson.getString(FixHeader.HEADER_USER);
        long startTS   = bodyJson.getLong(FixHeader.HEADER_START_TS);
        long endTS     = bodyJson.getLong(FixHeader.HEADER_END_TS);
        int pageSize   = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum    = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);
        
        JsonObject retval = new JsonObject();
        AccOperLogDao.getOpLogList(retval, pageSize, pageNum, accName, startTS, endTS);
        
        HttpUtils.outJsonObject(ctx, retval);
    }

}
