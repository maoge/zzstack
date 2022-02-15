package httpservertest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.streams.Pump;

public class FileUploadClient extends AbstractVerticle {

    public static void main(String[] args) {
        int nEvLoopPoolSize = 1;
        int nWorkerPoolSize = 4;

        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setMaxEventLoopExecuteTime(15000);
        vertxOptions.setEventLoopPoolSize(nEvLoopPoolSize);

        DeploymentOptions deployOptions = new DeploymentOptions();
        deployOptions.setWorker(true);
        deployOptions.setWorkerPoolName("verticle.worker.pool");
        deployOptions.setWorkerPoolSize(nWorkerPoolSize);
        deployOptions.setInstances(nEvLoopPoolSize);

        Vertx vertx = Vertx.vertx(vertxOptions);
        vertx.deployVerticle(FileUploadClient.class.getName(), deployOptions);
    }

    @Override
    public void start() throws Exception {
        // void request(HttpMethod method, int port, String host, String requestURI, Handler<AsyncResult<HttpClientRequest>> handler);
        
        String filename = "//e:/redis_cache.tar.gz";
        FileSystem fs = vertx.fileSystem();        
        
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions());
        httpClient.request(HttpMethod.POST, 9090, "172.16.2.51", "/test/uploadFile", ar -> {
            if (ar.succeeded()) {
                HttpClientRequest req = ar.result();
                
                fs.props(filename, ares -> {
                    FileProps props = ares.result();
                    System.out.println("props is " + props);
                    long size = props.size();
                    System.out.println("size: " + size);
                    
                    // req.headers().set("Content-Type", "multipart/form-data");
                    req.headers().set("content-length", "" + size);
                    fs.open(filename, new OpenOptions(), ares2 -> {
                        AsyncFile file = ares2.result();
                        Pump pump = Pump.pump(file, req);
                        file.endHandler(v -> {
                            req.end();
                        });
                        pump.start();
                    });
                });
            }
        });
        
//        HttpClientRequest req = vertx.createHttpClient(new HttpClientOptions()).put(9090, "172.16.2.51", "/test/uploadFile",
//                resp -> {
//                    System.out.println("Response " + resp.statusCode());
//                });
//        String filename = "//e:/redis_cache.tar.gz";
//        FileSystem fs = vertx.fileSystem();

//        fs.props(filename, ares -> {
//            FileProps props = ares.result();
//            System.out.println("props is " + props);
//            long size = props.size();
//            System.out.println("size: " + size);
//            
//            // req.headers().set("Content-Type", "multipart/form-data");
//            req.headers().set("content-length", "" + size);
//            fs.open(filename, new OpenOptions(), ares2 -> {
//                AsyncFile file = ares2.result();
//                Pump pump = Pump.pump(file, req);
//                file.endHandler(v -> {
//                    req.end();
//                });
//                pump.start();
//            });
//        });

    }

}
