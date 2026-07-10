package io.etherflow.web.function;

import io.etherflow.core.Mono;

@FunctionalInterface
public interface HandlerFunction {

    Mono<ServerResponse> handle(ServerRequest request);
}
