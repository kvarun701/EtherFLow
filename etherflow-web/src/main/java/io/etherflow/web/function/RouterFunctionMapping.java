package io.etherflow.web.function;

import io.etherflow.http.ServerWebExchange;
import io.etherflow.core.Mono;
import io.etherflow.web.HandlerAdapter;
import io.etherflow.web.HandlerMapping;
import io.etherflow.web.HandlerResult;

public class RouterFunctionMapping implements HandlerMapping, HandlerAdapter {

    private final RouterFunction routerFunction;

    public RouterFunctionMapping(RouterFunction routerFunction) {
        this.routerFunction = routerFunction;
    }

    @Override
    public Mono<Object> getHandler(ServerWebExchange exchange) {
        ServerRequest request = new ServerRequest(exchange);
        return routerFunction.route(request)
                .map(handler -> (Object) handler);
    }

    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerFunction;
    }

    @Override
    public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
        HandlerFunction handlerFunction = (HandlerFunction) handler;
        ServerRequest request = new ServerRequest(exchange);
        return handlerFunction.handle(request)
                .flatMap(response -> response.writeTo(exchange.response())
                        .thenReturn(new HandlerResult(handler, Mono.empty())));
    }
}
