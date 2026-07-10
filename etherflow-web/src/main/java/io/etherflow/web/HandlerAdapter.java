package io.etherflow.web;

import io.etherflow.http.ServerWebExchange;
import io.etherflow.core.Mono;

@FunctionalInterface
public interface HandlerAdapter {

    Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

    default boolean supports(Object handler) {
        return true;
    }
}
