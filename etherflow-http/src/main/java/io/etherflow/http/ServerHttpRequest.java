package io.etherflow.http;

import io.etherflow.codec.DataBuffer;
import io.etherflow.core.Mono;

import java.net.URI;
import java.util.*;

public interface ServerHttpRequest {

    HttpMethod method();

    URI uri();

    String path();

    HttpHeaders headers();

    Map<String, String> pathVariables();

    Mono<DataBuffer> body();

    Map<String, List<String>> queryParams();

    default String queryParam(String name) {
        Map<String, List<String>> params = queryParams();
        List<String> values = params.get(name);
        return values != null && !values.isEmpty() ? values.getFirst() : null;
    }
}
