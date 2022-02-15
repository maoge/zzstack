package com.zzstack.paas.underlying.metasvr.service.handler;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.ParamType;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IServerHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.metasvr.autodeploy.DeployerMarshell;
import com.zzstack.paas.underlying.metasvr.autodeploy.util.InstanceOperationEnum;
import com.zzstack.paas.underlying.metasvr.utils.MagicKeyUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.bean.ResultBean;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;

@App(path = "/paas/autodeploy")
public class AutoDeployHandler implements IServerHandler {

    @Service(id = "deployService", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void deployService(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String deployFlag = bodyJson.getString(FixHeader.HEADER_DEPLOY_FLAG);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        ResultBean result = new ResultBean();
        DeployerMarshell.deployService(servID, deployFlag, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "undeployService", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void undeployService(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        ResultBean result = new ResultBean();
        DeployerMarshell.undeployService(servID, false, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "deployInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void deployInstance(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        ResultBean result = new ResultBean();
        DeployerMarshell.deployInstance(servID, instID, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "undeployInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void undeployInstance(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        ResultBean result = new ResultBean();
        DeployerMarshell.undeployInstance(servID, instID, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "forceUndeployServ", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void forceUndeployServ(RoutingContext ctx) {

        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);

        ResultBean result = new ResultBean();
        DeployerMarshell.undeployService(servID, true, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "startInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void startInstance(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        ResultBean result = new ResultBean();
        DeployerMarshell.maintainInstance(servID, instID, servType, InstanceOperationEnum.INSTANCE_OPERATION_START, true, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "stopInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void stopInstance(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        ResultBean result = new ResultBean();
        DeployerMarshell.maintainInstance(servID, instID, servType, InstanceOperationEnum.INSTANCE_OPERATION_STOP, true, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "restartInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void restartInstance(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        ResultBean result = new ResultBean();
        DeployerMarshell.maintainInstance(servID, instID, servType, InstanceOperationEnum.INSTANCE_OPERATION_RESTART, true, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "updateInstance", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void updateInstance(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        ResultBean result = new ResultBean();
        DeployerMarshell.maintainInstance(servID, instID, servType, InstanceOperationEnum.INSTANCE_OPERATION_UPDATE, true, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "batchUpdateInst", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID_LIST, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void batchUpdateInst(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID_LIST);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        String[] instIdArr = instId.split(",");
        
        ResultBean result = new ResultBean();
        DeployerMarshell.batchUpdateInst(servID, instIdArr, servType, logKey, magicKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "checkInstanceStatus", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_SERV_TYPE, type = ParamType.ParamString, required = true) })
    public static void checkInstanceStatus(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();
        
        String servID = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instID = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String servType = bodyJson.getString(FixHeader.HEADER_SERV_TYPE);
        String magicKey = MagicKeyUtils.getMagicKey(ctx);
        
        ResultBean result = new ResultBean();
        DeployerMarshell.checkInstanceStatus(servID, instID, servType, magicKey, result);
        
        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "getDeployLog", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_LOG_KEY, type = ParamType.ParamString, required = true) })
    public static void getDeployLog(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String logKey = bodyJson.getString(FixHeader.HEADER_LOG_KEY);

        ResultBean result = new ResultBean();
        DeployerMarshell.getDeployLog(logKey, result);

        HttpUtils.outResultBean(ctx, result);
    }

    @Service(id = "getAppLog", auth = true, bwswitch = true, bodyParams = {
            @Parameter(name = FixHeader.HEADER_SERV_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_INST_ID, type = ParamType.ParamString, required = true),
            @Parameter(name = FixHeader.HEADER_LOG_TYPE, type = ParamType.ParamString, required = true) })
    public static void getAppLog(RoutingContext ctx) {
        RequestParameters params = HttpUtils.getValidateParams(ctx);
        RequestParameter body = params.body();
        JsonObject bodyJson = body.getJsonObject();

        String servId = bodyJson.getString(FixHeader.HEADER_SERV_ID);
        String instId = bodyJson.getString(FixHeader.HEADER_INST_ID);
        String logType = bodyJson.getString(FixHeader.HEADER_LOG_TYPE);

        ResultBean result = new ResultBean();
        DeployerMarshell.getAppLog(servId, instId, logType, result);

        HttpUtils.outResultBean(ctx, result);
    }

}
