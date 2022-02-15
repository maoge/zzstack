package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.metasvr.dataservice.dao.MetaDataDao;
import com.zzstack.paas.underlying.metasvr.singleton.MetaSvrGlobalRes;
import com.zzstack.paas.underlying.metasvr.utils.MagicKeyUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.UUIDUtils;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/metadata")
public class MetaDataHander implements IServerHandler {

    @Service(id = "getMetaSvrClusterState", method = HttpMethodEnum.GET, auth = false, bwswitch = true)
    public static void getMetaSvrClusterState(RoutingContext ctx) {
        JsonObject json = new JsonObject();
        MetaSvrGlobalRes.get().getClusterState(json);
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getCmptMetaData", method = HttpMethodEnum.GET, auth = false, bwswitch = false)
    public static void getCmptMetaData(RoutingContext ctx) {
        JsonObject json = MetaSvrGlobalRes.get().getCmptMetaAsJson();
        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getServiceCount", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_NAME, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_CLAZZ, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = false) })
    public static void getServiceCount(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servName = bodyJson.getString(FixHeader.HEADER_SERV_NAME);
        String servClazz = bodyJson.getString(FixHeader.HEADER_SERV_CLAZZ);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);

        JsonObject json = new JsonObject();
        MetaDataDao.getServiceCnt(json, servName, servClazz, servType);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getServiceList", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_NAME, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_CLAZZ, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getServiceList(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstID = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        String servName = bodyJson.getString(FixHeader.HEADER_SERV_NAME);
        String servClazz = bodyJson.getString(FixHeader.HEADER_SERV_CLAZZ);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);

        JsonObject json = new JsonObject();
        MetaDataDao.getServiceList(json, pageSize, pageNum, servInstID, servName, servClazz, servType);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getServTypeVerCount", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = false) })
    public static void getServTypeVerCount(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);

        JsonObject json = new JsonObject();
        MetaDataDao.getServTypeVerCount(json, servType);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getServTypeVerListByPage", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getServTypeVerListByPage(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);

        JsonObject json = new JsonObject();
        MetaDataDao.getServTypeVerListByPage(json, pageSize, pageNum, servType);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    
    @Service(id = "getClickHouseDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getClickHouseDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getClickHouseDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getVoltDBDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getVoltDBDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getVoltDBDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getRocketMQDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getRocketMQDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getRocketMQDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getTiDBDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getTiDBDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getTiDBDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getPulsarDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getPulsarDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getPulsarDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getYBDashboardAddr", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getYBDashboardAddr(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getYugaByteDashboardAddr(json, servInstId);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "addService", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_NAME, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_CLAZZ, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_VERSION, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_IS_PRODUCT, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_USER, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_PASSWORD, type = ParamType.ParamString, required = true) })
    public static void addService(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String instId = UUIDUtils.genUUID();
        String servName = bodyJson.getString(FixHeader.HEADER_SERV_NAME);
        String servClazz = bodyJson.getString(FixHeader.HEADER_SERV_CLAZZ);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String version = bodyJson.getString(FixHeader.HEADER_VERSION);
        String isProduct = bodyJson.getString(FixHeader.HEADER_IS_PRODUCT);
        String user = bodyJson.getString(FixHeader.HEADER_USER);
        String password = bodyJson.getString(FixHeader.HEADER_PASSWORD);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.addService(json, instId, servName, servClazz, servType, version, isProduct, user, password, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "delService", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true) })
    public static void delService(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.delService(json, instId, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "modService", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_NAME, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_VERSION, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_IS_PRODUCT, type = ParamType.ParamString, required = true) })
    public static void modService(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servName = bodyJson.getString(FixHeader.HEADER_SERV_NAME);
        String version = bodyJson.getString(FixHeader.HEADER_VERSION);
        String isProduct = bodyJson.getString(FixHeader.HEADER_IS_PRODUCT);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.modService(json, instId, servName, version, isProduct, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "modServiceVersion", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_VERSION, type = ParamType.ParamString, required = true) })
    public static void modServiceVersion(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String version = bodyJson.getString(FixHeader.HEADER_VERSION);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.modServiceVersion(json, instId, version, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getServerCount", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERVER_NAME, type = ParamType.ParamString, required = false) })
    public static void getServerCount(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String servName = bodyJson.getString(FixHeader.HEADER_SERVER_NAME);

        JsonObject json = new JsonObject();
        MetaDataDao.getServerCnt(json, servIp, servName);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getServerList", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_SERVER_NAME, type = ParamType.ParamString, required = false),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getServerList(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String servName = bodyJson.getString(FixHeader.HEADER_SERVER_NAME);
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);

        JsonObject json = new JsonObject();
        MetaDataDao.getServerList(json, pageSize, pageNum, servIp, servName);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "addServer", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERVER_NAME, type = ParamType.ParamString, required = true) })
    public static void addServer(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String servName = bodyJson.getString(FixHeader.HEADER_SERVER_NAME);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.addServer(json, servIp, servName, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "delServer", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true) })
    public static void delServer(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject res = new JsonObject();
        MetaDataDao.delServer(res, servIp, magicKey);

        HttpUtils.outJsonObject(ctx, res);
    }

    @Service(id = "getSSHCountByIP", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true) })
    public static void getSSHCountByIP(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        JsonObject json = new JsonObject();
        MetaDataDao.getSshCntByIp(json, servIp);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getSSHListByIP", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_SIZE, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_PAGE_NUMBER, type = ParamType.ParamInt, required = true) })
    public static void getSSHListByIP(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        int pageSize = bodyJson.getInteger(FixHeader.HEADER_PAGE_SIZE);
        int pageNum = bodyJson.getInteger(FixHeader.HEADER_PAGE_NUMBER);

        JsonObject json = new JsonObject();
        MetaDataDao.getSshListByIp(json, servIp, pageSize, pageNum);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "addSSH", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SSH_NAME, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SSH_PWD, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SSH_PORT, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_CLAZZ, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true) })
    public static void addSSH(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String sshName = bodyJson.getString(FixHeader.HEADER_SSH_NAME);
        String sshPwd = bodyJson.getString(FixHeader.HEADER_SSH_PWD);
        int sshPort = bodyJson.getInteger(FixHeader.HEADER_SSH_PORT);
        String servClazz = bodyJson.getString(FixHeader.HEADER_SERV_CLAZZ);
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.addSsh(json, sshName, sshPwd, sshPort, servClazz, servIp, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "modSSH", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SSH_NAME, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SSH_PWD, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SSH_PORT, type = ParamType.ParamInt, required = true),
            @Parameter(name = FixHeader.HEADER_SSH_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true) })
    public static void modSSH(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String sshName = bodyJson.getString(FixHeader.HEADER_SSH_NAME);
        String sshPwd = bodyJson.getString(FixHeader.HEADER_SSH_PWD);
        int sshPort = bodyJson.getInteger(FixHeader.HEADER_SSH_PORT);
        String sshId = bodyJson.getString(FixHeader.HEADER_SSH_ID);
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.modSsh(json, sshName, sshPwd, sshPort, sshId, servIp, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "delSSH", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SSH_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERVER_IP, type = ParamType.ParamString, required = true) })
    public static void delSSH(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String sshId = bodyJson.getString(FixHeader.HEADER_SSH_ID);
        String servIp = bodyJson.getString(FixHeader.HEADER_SERVER_IP);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.delSsh(json, sshId, servIp, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getUserByServiceType", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_CLAZZ, type = ParamType.ParamString, required = true) })
    public static void getUserByServiceType(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servClazz = bodyJson.getString(FixHeader.HEADER_SERV_CLAZZ);

        JsonObject json = new JsonObject();
        MetaDataDao.getUserByServClazzFromCache(json, servClazz);  // getUserByServClazzFromDB(json, servClazz);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getServList", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true) })
    public static void getServList(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);

        JsonObject json = new JsonObject();
        MetaDataDao.getServListFromCache(json, servType);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "loadServiceTopo", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true) })
    public static void loadServiceTopo(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);

        JsonObject json = new JsonObject();
        MetaDataDao.loadServiceTopo(json, instId);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "loadInstanceMeta", auth = false, bwswitch = false, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true) })
    public static void loadInstanceMeta(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);

        JsonObject json = new JsonObject();
        MetaDataDao.loadInstanceMeta(json, instId);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveServiceTopoSkeleton", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_TOPO_JSON, type = ParamType.ParamObject, required = true) })
    public static void saveServiceTopoSkeleton(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        JsonObject topoJson = bodyJson.getJsonObject(FixHeader.HEADER_TOPO_JSON);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.saveServTopoSkeleton(json, topoJson, servType, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "saveServiceNode", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_NODE_JSON, type = ParamType.ParamObject, required = true),
            @Parameter(name = FixHeader.HEADER_PARENT_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_OP_TYPE, type = ParamType.ParamInt, required = true) })
    public static void saveServiceNode(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        JsonObject nodeJson = bodyJson.getJsonObject(FixHeader.HEADER_NODE_JSON);
        String parentId = bodyJson.getString(FixHeader.HEADER_PARENT_ID);
        int opType = bodyJson.getInteger(FixHeader.HEADER_OP_TYPE);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.saveServiceNode(parentId, opType, nodeJson, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "delServiceNode", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_PARENT_ID, type = ParamType.ParamString, required = true) })
    public static void delServiceNode(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String parentId = bodyJson.getString(FixHeader.HEADER_PARENT_ID);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        MetaDataDao.delServNode(parentId, instId, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getMetaTree", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getMetaTree(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);

        JsonObject json = new JsonObject();
        MetaDataDao.getMetaDataTreeByInstId(instId, json);

        HttpUtils.outJsonObject(ctx, json);
    }

    @Service(id = "getMetaData", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getMetaData(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getMetaDataNodeByInstId(instId, json);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getSmsABQueueWeightInfo", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true) })
    public static void getSmsABQueueWeightInfo(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstId = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        
        JsonObject json = new JsonObject();
        MetaDataDao.getSmsABQueueWeightInfo(servInstId, json);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "adjustSmsABQueueWeightInfo", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_QUEUE_SERV_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_TOPO_JSON, type = ParamType.ParamObject, required = true) })
    public static void adjustSmsABQueueWeightInfo(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstID = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        String queueServInstID = bodyJson.getString(FixHeader.HEADER_QUEUE_SERV_INST_ID);
        JsonObject topoJson = bodyJson.getJsonObject(FixHeader.HEADER_TOPO_JSON);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.adjustSmsABQueueWeightInfo(servInstID, queueServInstID, topoJson, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "switchSmsDBType", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_DB_SERV_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_ACTIVE_DB_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_DB_NAME, type = ParamType.ParamString, required = true) })
    public static void switchSmsDBType(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servInstID = bodyJson.getString(FixHeader.HEADER_SERV_INST_ID);
        String dbServInstID = bodyJson.getString(FixHeader.HEADER_DB_SERV_INST_ID);
        String dbType = bodyJson.getString(FixHeader.HEADER_ACTIVE_DB_TYPE);
        String dbName = bodyJson.getString(FixHeader.HEADER_DB_NAME);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.switchSmsDBType(servInstID, dbServInstID, dbType, dbName, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getServTypeVerList", auth = true, bwswitch = true)
    public static void getServTypeVerList(RoutingContext ctx) {
        JsonObject json = new JsonObject();
        MetaDataDao.getServTypeVerList(json);
        
        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "getServTypeList", auth = true, bwswitch = true)
    public static void getServTypeList(RoutingContext ctx) {
        JsonObject json = new JsonObject();
        MetaDataDao.getServTypeList(json);
        
        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "addCmptVersion", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_VERSION, type = ParamType.ParamString, required = true) })
    public static void addCmptVersion(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String version = bodyJson.getString(FixHeader.HEADER_VERSION);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.addCmptVersion(servType, version, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }
    
    @Service(id = "delCmptVersion", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_VERSION, type = ParamType.ParamString, required = true) })
    public static void delCmptVersion(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String version = bodyJson.getString(FixHeader.HEADER_VERSION);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        JsonObject json = new JsonObject();
        MetaDataDao.delCmptVersion(servType, version, json, magicKey);

        HttpUtils.outJsonObject(ctx, json);
    }


    @Service(id = "reloadMetaData", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_RELOAD_TYPE, type = ParamType.ParamString, required = true)})
    public static void reloadMetaData(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String type = bodyJson.getString(FixHeader.HEADER_RELOAD_TYPE);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        JsonObject json = new JsonObject();
        if (MetaDataDao.reloadMetaData(type, magicKey)) {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            json.put(FixHeader.HEADER_RET_INFO, CONSTS.INFO_OK);
        } else {
            json.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_OK);
            json.put(FixHeader.HEADER_RET_INFO, "reloadMetaData fail");
        }
        
        HttpUtils.outJsonObject(ctx, json);
    }
}
