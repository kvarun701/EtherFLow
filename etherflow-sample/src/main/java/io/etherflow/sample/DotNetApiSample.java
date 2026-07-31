package io.etherflow.sample;

import io.etherflow.client.dotnet.DotNetApiClient;
import io.etherflow.core.Mono;
import io.etherflow.server.netty.NettyServer;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.RouterFunction;
import io.etherflow.web.function.RouterFunctionMapping;
import io.etherflow.web.function.ServerResponse;

import java.util.Map;

/**
 * Runnable sample application showcasing EtherFlow's high-performance reactive integration
 * with .NET Framework / ASP.NET Core Web API services (running on port 5003).
 */
public class DotNetApiSample {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8086;

        // Initialize DotNetApiClient configured for .NET Web API server (port 5003)
        DotNetApiClient dotNetClient = DotNetApiClient.create("http://localhost:5003");

        // Define reactive routes acting as an API Gateway to .NET microservices
        RouterFunction routes = RouterFunction.route()
                .GET("/dotnet/health", req ->
                        dotNetClient.health()
                                .flatMap(health -> Mono.just(ServerResponse.ok(health))))

                .GET("/dotnet/hello", req -> {
                    String param = req.queryParam("name");
                    String name = (param != null && !param.isEmpty()) ? param : "EtherFlow Dev";
                    return dotNetClient.get("/api/dotnet/hello?name=" + name, Map.class)
                            .flatMap(res -> Mono.just(ServerResponse.ok(res)));
                })

                .GET("/dotnet/products/{id}", req -> {
                    String id = req.pathVariable("id");
                    return dotNetClient.get("/api/dotnet/products/" + id, Map.class)
                            .flatMap(res -> Mono.just(ServerResponse.ok(res)));
                })

                .POST("/dotnet/process", req ->
                        req.bodyTo(Map.class)
                                .flatMap(body -> dotNetClient.post("/api/dotnet/process", body, Map.class))
                                .flatMap(res -> Mono.just(ServerResponse.ok(res))))
                .build();

        DispatcherHandler dispatcher = new DispatcherHandler();
        dispatcher.addHandlerMapping(new RouterFunctionMapping(routes));
        dispatcher.addHandlerAdapter(new RouterFunctionMapping(routes));

        NettyServer server = new NettyServer(port, dispatcher);
        server.start();

        System.out.println("================================================================");
        System.out.println(" 🚀 EtherFlow .NET API Gateway Running on http://localhost:" + port);
        System.out.println("================================================================");
        System.out.println("  Connected Backend:");
        System.out.println("    - .NET Framework / Web API: http://localhost:5003");
        System.out.println("  Endpoints:");
        System.out.println("    GET  http://localhost:" + port + "/dotnet/health");
        System.out.println("    GET  http://localhost:" + port + "/dotnet/hello?name=EnterpriseUser");
        System.out.println("    GET  http://localhost:" + port + "/dotnet/products/99");
        System.out.println("    POST http://localhost:" + port + "/dotnet/process");
        System.out.println("================================================================");

        server.await();
    }
}
