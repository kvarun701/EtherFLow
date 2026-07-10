package io.etherflow.sample;

import io.etherflow.core.Mono;
import io.etherflow.http.*;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.*;

public class SampleApp implements HttpHandler {

    private final DispatcherHandler handler = new DispatcherHandler();

    public SampleApp() {
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

        handler.addHandlerMapping(new RouterFunctionMapping(routes));
        handler.addHandlerAdapter(new RouterFunctionMapping(routes));
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        return handler.handle(exchange);
    }

    public static void main(String[] args) {
        System.out.println("EtherFlow sample app ready");
        System.out.println("Endpoints:");
        System.out.println("  GET  /hello");
        System.out.println("  GET  /users/{id}");
        System.out.println("  POST /echo");
    }
}
