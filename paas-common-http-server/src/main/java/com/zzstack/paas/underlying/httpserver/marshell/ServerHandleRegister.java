package com.zzstack.paas.underlying.httpserver.marshell;

import static io.vertx.ext.web.validation.builder.Parameters.optionalParam;
import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.booleanSchema;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.numberSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

import static io.vertx.ext.web.validation.builder.Bodies.*;
import static io.vertx.json.schema.draft7.dsl.Schemas.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.httpserver.annotation.App;
import com.zzstack.paas.underlying.httpserver.annotation.HttpMethodEnum;
import com.zzstack.paas.underlying.httpserver.annotation.Parameter;
import com.zzstack.paas.underlying.httpserver.annotation.Service;
import com.zzstack.paas.underlying.httpserver.bean.ApiInfo;
import com.zzstack.paas.underlying.httpserver.consts.HttpServerConstants;
import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.httpserver.serverless.ServerlessGatewayRegister;
import com.zzstack.paas.underlying.httpserver.singleton.AllServiceMap;
import com.zzstack.paas.underlying.httpserver.singleton.ServiceData;
import com.zzstack.paas.underlying.httpserver.stats.StatisticHandler;
import com.zzstack.paas.underlying.httpserver.utils.HttpUtils;
import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.ParameterProcessorException;
import io.vertx.ext.web.validation.RequestPredicateException;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;
import io.vertx.json.schema.common.dsl.SchemaBuilder;

@SuppressWarnings("deprecation")
public class ServerHandleRegister {

    private static Logger logger = LoggerFactory.getLogger(ServerHandleRegister.class);

    private static JsonObject rejectJson;
    private static JsonObject errJson;
    private static JsonObject ipLimitJosn;
    private static long TASK_TIMEOUT = CONSTS.HTTP_TASK_TIMEOUE;
    public static final String SERVICE_ID_SPLITTER = "_";

    static {
        rejectJson = new JsonObject();
        rejectJson.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_AUTH_FAIL);
        rejectJson.put(FixHeader.HEADER_RET_INFO, "not authorized or session is timeout, service call reject!");

        errJson = new JsonObject();
        errJson.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_NOK);
        errJson.put(FixHeader.HEADER_RET_INFO, "internal error!");

        ipLimitJosn = new JsonObject();
        ipLimitJosn.put(FixHeader.HEADER_RET_CODE, CONSTS.REVOKE_AUTH_IP_LIMIT);
        ipLimitJosn.put(FixHeader.HEADER_RET_INFO, "ip limit !");

        TASK_TIMEOUT = ServiceData.get().getTaskTimeOut();
    }

    public static void registVertxRoute(Vertx vertx, int port, List<Class<?>> handlers) {
        Router router = Router.router(vertx);
        ServiceData.get().setRouter(router);
        if (handlers == null || handlers.isEmpty()) {
            logger.info("no http server hander to regist ......");
        }

        registRoute(vertx, router, handlers, null);
    }

    public static void registVertxRoute(Vertx vertx, int port, List<Class<?>> handlers, IAuthHandler authHandle) {
        Router router = Router.router(vertx);
        ServiceData.get().setRouter(router);
        if (handlers == null || handlers.isEmpty()) {
            logger.info("no http server hander to regist ......");
        }

        registRoute(vertx, router, handlers, authHandle);
    }

    private static void registRoute(Vertx vertx, Router router, List<Class<?>> handlers, IAuthHandler authHandle) {
    	// 添加api doc handler
    	handlers.add(StatisticHandler.class);
    	
        for (Class<?> clazz : handlers) {
            App app = clazz.getAnnotation(App.class);
            String rootPath = app.path();
            if (!rootPath.endsWith(CONSTS.PATH_SPLIT))
                rootPath += CONSTS.PATH_SPLIT;

            AllServiceMap serviceMap = AllServiceMap.get();

            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers())) {
                    continue;
                }

                if (!m.isAnnotationPresent(Service.class)) {
                    continue;
                }

                Service s = m.getAnnotation(Service.class);
                HttpMethodEnum httpMethodEnum = s.method();
                String id = s.id();
                String subPath = id.startsWith(CONSTS.PATH_SPLIT) ? s.id().substring(1) : s.id();
                String path = rootPath + subPath;
                serviceMap.add(path, s);

                Parameter[] headerParams = s.headerParams();
                Parameter[] queryParams = s.queryParams();
                Parameter[] pathParams = s.pathParams();
                Parameter[] bodyParams = s.bodyParams();

                SchemaParser parser = SchemaParser.createDraft7SchemaParser(SchemaRouter.create(vertx, new SchemaRouterOptions()));

                ValidationHandlerBuilder builder = ValidationHandlerBuilder.create(parser); // ValidationHandler.builder(parser);
                buildHeaderParams(builder, headerParams);
                buildQueryParams(builder, queryParams);
                buildPathParams(builder, pathParams);
                buildBodyParams(builder, bodyParams);
                ValidationHandler validationHandler = builder.build();
                
                ApiInfo api = new ApiInfo(path, httpMethodEnum.name(), headerParams, queryParams, pathParams, bodyParams);
                serviceMap.addApiInfo(path, api);

                HttpMethod httpMethod = httpMethodEnum == HttpMethodEnum.POST ? HttpMethod.POST : HttpMethod.GET;
                router.route(httpMethod, path).handler(BodyHandler.create()).handler(validationHandler).handler(ctx -> {

                    ctx.vertx().executeBlocking(future -> {
                        try {
                            // 判断是不是OPTIONS请求，是的话 直接通过
                            HttpServerRequest request = ctx.request();
                            HttpMethod method = request.method();
                            if (method.equals(HttpMethod.OPTIONS)) {
                                HttpServerResponse response = ctx.response();
                                response.putHeader("Access-Control-Allow-Origin", "*");
                                response.putHeader("Access-Control-Allow-Headers", "MAGIC_KEY");
                                response.putHeader("Access-Control-Allow-Methods", "OPTIONS,HEAD,GET,POST,PUT,DELETE");
                                response.putHeader("Access-Control-Max-Age", "180000");
                                response.setStatusCode(200);
                                response.end();
                                return;
                            }
                            
                            if (s.auth() && authHandle != null) {
                                if (!authHandle.doAuth(ctx)) {
                                    doAuthFail(ctx);
                                    return;
                                }
                            }

                            // service call statistic
                            // doStatistic(ctx);
                            
                            // do log request
                            doLogRequest(ctx);

                            long start = System.currentTimeMillis();
                            m.invoke(null, ctx);
                            long end = System.currentTimeMillis();
                            long cost = end - start;

                            if (cost > TASK_TIMEOUT) {
                                logger.error("request:{} {} process cost:{} ms", method.getClass().getSimpleName(),
                                        request.path(), cost);
                            }
                        } catch (Exception e) {
                            doError(ctx);
                            logger.error("handler: {} caught error: {}", path, e.getMessage(), e);
                        } finally {
                            future.complete();
                        }

                    }, false, null);
                });
            }
        }
        
        router.errorHandler(400, ctx -> {
            if (ctx.failure() instanceof BadRequestException) {
                HttpServerRequest req = ctx.request();
                Throwable failure = ctx.failure();
                if (failure instanceof ParameterProcessorException) {
                    ParameterProcessorException f = (ParameterProcessorException) failure;
                    logger.error("handler: {} error: {}", req.path(), f.toJson());
                } else if (failure instanceof BodyProcessorException) {
                    BodyProcessorException f = (BodyProcessorException) failure;
                    logger.error("handler: {} error: {}", req.path(), f.toJson());
                } else if (failure instanceof RequestPredicateException) {
                    RequestPredicateException f = (RequestPredicateException) failure;
                    logger.error("handler: {} error: {}", req.path(), f.toJson());
                }
            }
        });
    }
    
    public static boolean registServerlessGateway() {
        logger.info("registServerlessGateway ......");
        
        ServiceData serviceData = ServiceData.get();
        String upstreamID = serviceData.getUpstreamID();
        String serviceID = serviceData.getServiceID();
        String node = String.format("%s:%d", serviceData.getIP(), serviceData.getPort());
        
        ServerlessGatewayRegister serverlessGatewayRegister = ServiceData.get().getServerlessGatewayRegister();
        
        if (!serverlessGatewayRegister.getUpstreamByID(upstreamID)) {
            // 指定upstreamID不存在则创建upstream 
            if (!serverlessGatewayRegister.createUpstream(upstreamID)) {
                logger.error("createUpstream:{} fail ......", upstreamID);
                return false;
            }
        }
        
        if (!serverlessGatewayRegister.isUpstreamNodeExist(upstreamID, node)) {
            if (!serverlessGatewayRegister.addUpstreamNode(node, ServerlessGatewayRegister.DEFAULT_WEIGHT)) {
                logger.error("addUpstreamNode fail, upstream_id:{}, add node:{} ......", upstreamID, node);
                return false;
            }
        }
        
        // create service when serverless service not exist
        if (!serverlessGatewayRegister.isServiceExist(serviceID)) {
            if (!serverlessGatewayRegister.addService(serviceID, upstreamID)) {
                logger.error("upstream_id:{}, add node:{} fail ......", upstreamID, node);
                return false;
            }
        }
        
        List<Class<?>> handlers = ServiceData.get().getHandlers();
        for (Class<?> clazz : handlers) {
            App app = clazz.getAnnotation(App.class);
            String originalRootPath = app.path();
            
            if (!originalRootPath.endsWith(CONSTS.PATH_SPLIT))
                originalRootPath += CONSTS.PATH_SPLIT;

            Method[] methods = clazz.getMethods();
            for (Method m : methods) {
                if (!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers())) {
                    continue;
                }

                if (!m.isAnnotationPresent(Service.class)) {
                    continue;
                }

                Service s = m.getAnnotation(Service.class);
                HttpMethodEnum httpMethodEnum = s.method();
                String id = s.id();
                String subPath = id.startsWith(CONSTS.PATH_SPLIT) ? id.substring(1) : id;
                
                String uri = originalRootPath + subPath;
                String method = httpMethodEnum.name();
                String routeID = uri.replace("/", SERVICE_ID_SPLITTER);
                String[] httpMethods = new String[]{ method };
                
                logger.info("regist route: {} ......", uri);
                
                if (!serverlessGatewayRegister.addRoute(routeID, serviceID, uri, httpMethods)) {
                    logger.error("regist route uri:{}, upstream_id:{}, node:{} fail ......", uri, upstreamID, node);
                    processAfterRegistServerlessFail(serverlessGatewayRegister, upstreamID, node);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private static void processAfterRegistServerlessFail(ServerlessGatewayRegister serverlessGatewayRegister,
            String upstreamID, String node) {
        
        serverlessGatewayRegister.delUpstreamNode(upstreamID, node);
    }

    private static void buildHeaderParams(ValidationHandlerBuilder builder, Parameter[] params) {
        if (params == null || params.length == 0)
            return;

        for (Parameter param : params) {
            String parmName = param.name();
            switch (param.type()) {
            case ParamString:
                builder.headerParameter(param.required() ? param(parmName, stringSchema()) : optionalParam(parmName, stringSchema()));
                break;

            case ParamInt:
                builder.headerParameter(param.required() ? param(parmName, intSchema()) : optionalParam(parmName, intSchema()));
                break;

            case ParamNumber:
                builder.headerParameter(param.required() ? param(parmName, numberSchema()) : optionalParam(parmName, numberSchema()));
                break;

            case ParamBoolean:
                builder.headerParameter(param.required() ? param(parmName, booleanSchema()) : optionalParam(parmName, booleanSchema()));
                break;

            default:
                break;
            }
        }
    }

    private static void buildQueryParams(ValidationHandlerBuilder builder, Parameter[] params) {
        if (params == null || params.length == 0)
            return;

        for (Parameter param : params) {
            String parmName = param.name();
            switch (param.type()) {
            case ParamString:
                builder.queryParameter(param.required() ? param(parmName, stringSchema()) : optionalParam(parmName, stringSchema()));
                break;

            case ParamInt:
                builder.queryParameter(param.required() ? param(parmName, intSchema()) : optionalParam(parmName, stringSchema()));
                break;

            case ParamNumber:
                builder.queryParameter(param.required() ? param(parmName, numberSchema()) : optionalParam(parmName, stringSchema()));
                break;

            case ParamBoolean:
                builder.queryParameter(param.required() ? param(parmName, booleanSchema()) : optionalParam(parmName, stringSchema()));
                break;

            default:
                break;
            }
        }
    }

    private static void buildPathParams(ValidationHandlerBuilder builder, Parameter[] params) {
        if (params == null || params.length == 0)
            return;

        for (Parameter param : params) {
            String parmName = param.name();
            switch (param.type()) {
            case ParamString:
                builder.pathParameter(param.required() ? param(parmName, stringSchema()) : optionalParam(parmName, stringSchema()));
                break;

            case ParamInt:
                builder.pathParameter(param.required() ? param(parmName, intSchema()) : optionalParam(parmName, intSchema()));
                break;

            case ParamNumber:
                builder.pathParameter(param.required() ? param(parmName, numberSchema()) : optionalParam(parmName, numberSchema()));
                break;

            case ParamBoolean:
                builder.pathParameter(param.required() ? param(parmName, booleanSchema()) : optionalParam(parmName, booleanSchema()));
                break;

            default:
                break;
            }
        }
    }
    
    private static void buildBodyParams(ValidationHandlerBuilder builder, Parameter[] params) {
        if (params == null || params.length == 0)
            return;

        ObjectSchemaBuilder objSchemaBuilder = objectSchema();

        for (Parameter param : params) {
            String parmName = param.name();
            switch (param.type()) {
            case ParamString:
                addSchema(objSchemaBuilder, parmName, stringSchema(), param.required());
                break;

            case ParamInt:
                addSchema(objSchemaBuilder, parmName, intSchema(), param.required());
                break;

            case ParamNumber:
                addSchema(objSchemaBuilder, parmName, numberSchema(), param.required());
                break;

            case ParamBoolean:
                addSchema(objSchemaBuilder, parmName, booleanSchema(), param.required());
                break;

            case ParamObject:
                addSchema(objSchemaBuilder, parmName, objectSchema(), param.required());
                break;

            case ParamArray:
                addSchema(objSchemaBuilder, parmName, arraySchema(), param.required());
                break;

            case ParamTuple:
                addSchema(objSchemaBuilder, parmName, tupleSchema(), param.required());
                break;

            default:
                break;
            }
        }
        
        builder.body(json(objSchemaBuilder));
    }

    @SuppressWarnings("rawtypes")
    private static void addSchema(ObjectSchemaBuilder objSchemaBuilder, String paramName, SchemaBuilder schemaBuilder, boolean required) {
        if (required)
            objSchemaBuilder.property(paramName, schemaBuilder);
        else
            objSchemaBuilder.optionalProperty(paramName, schemaBuilder);
    }

    private static void doError(RoutingContext ctx) {
        HttpUtils.outJsonObject(ctx, errJson);
    }
    
    private static void doLogRequest(RoutingContext ctx) {
        if (HttpServerConstants.logRequestEnable()) {
            HttpUtils.logRequest(ctx);
        }
    }

    private static void doAuthFail(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.setStatusCode(401);
        response.end("auth fail!");
    }

}
