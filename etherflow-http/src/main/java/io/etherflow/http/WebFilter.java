package io.etherflow.http;

import io.etherflow.core.Mono;

@FunctionalInterface
public interface WebFilter {

    Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain);

    interface WebFilterChain {
        Mono<Void> filter(ServerWebExchange exchange);
    }
}
