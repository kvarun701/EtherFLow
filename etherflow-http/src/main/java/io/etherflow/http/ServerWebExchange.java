package io.etherflow.http;

import io.etherflow.core.Mono;

import java.util.*;

public class ServerWebExchange {

    private final ServerHttpRequest request;
    private final ServerHttpResponse response;
    private final Map<String, Object> attributes = new HashMap<>();

    public ServerWebExchange(ServerHttpRequest request, ServerHttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public ServerHttpRequest request() {
        return request;
    }

    public ServerHttpResponse response() {
        return response;
    }

    @SuppressWarnings("unchecked")
    public <T> T attribute(String key) {
        return (T) attributes.get(key);
    }

    public void attribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
