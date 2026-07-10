package io.etherflow.web;

import io.etherflow.http.ServerWebExchange;
import io.etherflow.core.Mono;

@FunctionalInterface
public interface HandlerMapping {

    Mono<Object> getHandler(ServerWebExchange exchange);

    default int order() {
        return Integer.MAX_VALUE;
    }
}
