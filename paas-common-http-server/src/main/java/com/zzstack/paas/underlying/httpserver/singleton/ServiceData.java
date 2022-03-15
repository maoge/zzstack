package com.zzstack.paas.underlying.httpserver.singleton;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.zzstack.paas.underlying.httpserver.marshell.handler.IAuthHandler;
import com.zzstack.paas.underlying.httpserver.serverless.ServerlessGatewayRegister;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.Router;

public class ServiceData {
	
	private HttpServer httpServer = null;
	private SharedData sharedData = null;
	private Vertx vertx = null;
	private Router router = null;
	
	private ServerlessGatewayRegister serverlessGatewayRegister = null;
	private String upstreamID = null;
	private String serviceID = null;
	private String xApiKey = null;
	
	private String ip = CONSTS.HTTP_DEFAULT_BIND_IP;
	private int port = CONSTS.HTTP_DEFAULT_PORT;
	private boolean useSSL = false;
	private int evLoopPoolSize = CONSTS.HTTP_EVENT_LOOP_POOL_SIZE;
	private int workerPoolSize = CONSTS.HTTP_WORKER_POOL_SIZE;
	private long evloopTimeOut = CONSTS.HTTP_EVLOOP_TIMEOUT;
	private long taskTimeOut = CONSTS.HTTP_TASK_TIMEOUE;
	
	private List<Class<?>> handlers;
	private IAuthHandler authHandle;

    private static ServiceData theInstance = null;
	private static ReentrantLock intanceLock = null;
	
	static {
		intanceLock = new ReentrantLock();
	}
	
	private ServiceData() {
		
	}
	
	private void initData() {

	}
	
	public static ServiceData get() {
	    if (theInstance != null) {
	        return theInstance;
	    }
        
        intanceLock.lock();
		try {
			if (theInstance != null) {
				return theInstance;
			} else {
				theInstance = new ServiceData();
				theInstance.initData();
			}
		} finally {
			intanceLock.unlock();
		}
		
		return theInstance;
	}
	
	public HttpServer getHttpServer() {
		return httpServer;
	}

	public void setHttpServer(HttpServer httpServer) {
		this.httpServer = httpServer;
	}
	
	public SharedData getSharedData() {
		return sharedData;
	}

	public void setSharedData(SharedData sharedData) {
		this.sharedData = sharedData;
	}

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }
    
    public String getIP() {
        return ip;
    }

    public void setIP(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public boolean isUseSSL() {
        return useSSL;
    }

    public void setSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

	public Vertx getVertx() {
		return vertx;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
	
	public long getEvloopTimeOut() {
	    return evloopTimeOut;
	}
	
	public void setEvloopTimeOut(long evloopTimeOut) {
	    this.evloopTimeOut = evloopTimeOut;
	}

    public long getTaskTimeOut() {
        return taskTimeOut;
    }

    public void setTaskTimeOut(long taskTimeOut) {
        this.taskTimeOut = taskTimeOut;
    }

    public ServerlessGatewayRegister getServerlessGatewayRegister() {
        return serverlessGatewayRegister;
    }

    public void setServerlessGatewayRegister(ServerlessGatewayRegister serverlessGatewayRegister) {
        this.serverlessGatewayRegister = serverlessGatewayRegister;
    }

    public int getEvLoopPoolSize() {
        return evLoopPoolSize;
    }

    public void setEvLoopPoolSize(int evLoopPoolSize) {
        this.evLoopPoolSize = evLoopPoolSize;
    }

    public int getWorkerPoolSize() {
        return workerPoolSize;
    }

    public void setWorkerPoolSize(int workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
    }

    public List<Class<?>> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<Class<?>> handlers) {
        this.handlers = handlers;
    }

    public IAuthHandler getAuthHandle() {
        return authHandle;
    }

    public void setAuthHandle(IAuthHandler authHandle) {
        this.authHandle = authHandle;
    }

    public String getUpstreamID() {
        return upstreamID;
    }

    public void setUpstreamID(String upstreamID) {
        this.upstreamID = upstreamID;
    }
    
    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getXApiKey() {
        return xApiKey;
    }

    public void setXApiKey(String xApiKey) {
        this.xApiKey = xApiKey;
    }

}
