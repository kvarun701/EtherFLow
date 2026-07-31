package io.etherflow.sample;

import io.etherflow.client.python.PythonApiClient;
import io.etherflow.core.Mono;
import io.etherflow.server.netty.NettyServer;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.RouterFunction;
import io.etherflow.web.function.RouterFunctionMapping;
import io.etherflow.web.function.ServerResponse;

import java.util.Map;

/**
 * Runnable sample application showcasing EtherFlow's high-performance reactive integration
 * with Python API services (Flask API on port 5001 and FastAPI on port 5002).
 */
public class PythonApiSample {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8085;

        // Initialize PythonApiClient configured to talk to Flask (5001) & FastAPI (5002)
        PythonApiClient pythonClient = PythonApiClient.builder()
                .flaskUrl("http://localhost:5001")
                .fastApiUrl("http://localhost:5002")
                .build();

        // Define reactive routes acting as an API Gateway to Python microservices
        RouterFunction routes = RouterFunction.route()
                .GET("/python/health", req ->
                        pythonClient.checkHealth()
                                .flatMap(health -> Mono.just(ServerResponse.ok(health))))

                .GET("/python/flask/hello", req -> {
                    String param = req.queryParam("name");
                    String name = (param != null && !param.isEmpty()) ? param : "EtherFlow Dev";
                    return pythonClient.flask()
                            .get("/api/flask/hello?name=" + name, Map.class)
                            .flatMap(res -> Mono.just(ServerResponse.ok(res)));
                })

                .GET("/python/fastapi/hello", req -> {
                    String param = req.queryParam("name");
                    String name = (param != null && !param.isEmpty()) ? param : "EtherFlow Dev";
                    return pythonClient.fastApi()
                            .get("/api/fastapi/hello?name=" + name, Map.class)
                            .flatMap(res -> Mono.just(ServerResponse.ok(res)));
                })

                .POST("/python/flask/predict", req ->
                        req.bodyTo(Map.class)
                                .flatMap(body -> pythonClient.flask()
                                        .post("/api/flask/predict", body, Map.class))
                                .flatMap(res -> Mono.just(ServerResponse.ok(res))))

                .POST("/python/fastapi/analyze", req ->
                        req.bodyTo(Map.class)
                                .flatMap(body -> pythonClient.fastApi()
                                        .post("/api/fastapi/analyze", body, Map.class))
                                .flatMap(res -> Mono.just(ServerResponse.ok(res))))
                .build();

        DispatcherHandler dispatcher = new DispatcherHandler();
        dispatcher.addHandlerMapping(new RouterFunctionMapping(routes));
        dispatcher.addHandlerAdapter(new RouterFunctionMapping(routes));

        NettyServer server = new NettyServer(port, dispatcher);
        server.start();

        System.out.println("================================================================");
        System.out.println(" 🚀 EtherFlow Python API Gateway Running on http://localhost:" + port);
        System.out.println("================================================================");
        System.out.println("  Connected Backends:");
        System.out.println("    - Flask API:   http://localhost:5001");
        System.out.println("    - FastAPI API: http://localhost:5002");
        System.out.println("  Endpoints:");
        System.out.println("    GET  http://localhost:" + port + "/python/health");
        System.out.println("    GET  http://localhost:" + port + "/python/flask/hello?name=Alice");
        System.out.println("    GET  http://localhost:" + port + "/python/fastapi/hello?name=Bob");
        System.out.println("    POST http://localhost:" + port + "/python/flask/predict");
        System.out.println("    POST http://localhost:" + port + "/python/fastapi/analyze");
        System.out.println("================================================================");

        server.await();
    }
}
