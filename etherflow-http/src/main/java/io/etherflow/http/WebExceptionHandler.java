package io.etherflow.http;

import io.etherflow.core.Mono;

@FunctionalInterface
public interface WebExceptionHandler {

    Mono<Void> handle(ServerWebExchange exchange, Throwable exception);
}
