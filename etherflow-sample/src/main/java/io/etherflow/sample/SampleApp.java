package io.etherflow.sample;

import io.etherflow.core.Mono;
import io.etherflow.server.netty.NettyServer;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.*;

public class SampleApp {

    public static void main(String[] args) throws Exception {
        RouterFunction routes = RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("Hello EtherFlow!")))
                .GET("/users/{id}", req -> {
                    String id = req.pathVariable("id");
                    return Mono.just(ServerResponse.ok("{\"id\":\"" + id + "\",\"name\":\"Alice\"}"));
                })
                .POST("/echo", req ->
                        req.bodyTo(String.class)
                                .flatMap(body -> Mono.just(ServerResponse.ok(body))))
                .build();

        DispatcherHandler dispatcher = new DispatcherHandler();
        dispatcher.addHandlerMapping(new RouterFunctionMapping(routes));
        dispatcher.addHandlerAdapter(new RouterFunctionMapping(routes));

        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        NettyServer server = new NettyServer(port, dispatcher);
        server.start();
        System.out.println("EtherFlow sample app running on http://localhost:" + port);
        System.out.println("Endpoints:");
        System.out.println("  GET  /hello");
        System.out.println("  GET  /users/{id}");
        System.out.println("  POST /echo");
        server.await();
    }
}
