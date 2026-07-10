package io.etherflow.http;

public enum HttpMethod {
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE, CONNECT;

    public static HttpMethod from(String method) {
        return valueOf(method.toUpperCase());
    }
}
