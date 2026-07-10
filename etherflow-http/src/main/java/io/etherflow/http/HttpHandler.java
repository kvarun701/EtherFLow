package io.etherflow.http;

import io.etherflow.core.Mono;

@FunctionalInterface
public interface HttpHandler {

    Mono<Void> handle(ServerWebExchange exchange);
}
