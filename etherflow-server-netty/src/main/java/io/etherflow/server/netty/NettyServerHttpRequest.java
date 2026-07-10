package io.etherflow.server.netty;

import io.etherflow.codec.DataBuffer;
import io.etherflow.codec.DataBufferFactory;
import io.etherflow.codec.DefaultDataBufferFactory;
import io.etherflow.core.Mono;
import io.etherflow.http.HttpHeaders;
import io.etherflow.http.HttpMethod;
import io.etherflow.http.ServerHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

class NettyServerHttpRequest implements ServerHttpRequest {

    private final FullHttpRequest nettyRequest;
    private final URI uri;
    private final HttpHeaders headers;
    private final Map<String, String> pathVariables = new HashMap<>();
    private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    NettyServerHttpRequest(FullHttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        this.uri = URI.create(nettyRequest.uri());
        this.headers = parseHeaders(nettyRequest);
    }

    void pathVariable(String name, String value) {
        pathVariables.put(name, value);
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.from(nettyRequest.method().name());
    }

    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public String path() {
        return uri.getPath();
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Map<String, String> pathVariables() {
        return pathVariables;
    }

    @Override
    public Mono<DataBuffer> body() {
        byte[] bytes = new byte[nettyRequest.content().readableBytes()];
        nettyRequest.content().readBytes(bytes);
        DataBuffer buf = bufferFactory.wrap(bytes);
        return Mono.just(buf);
    }

    @Override
    public Map<String, List<String>> queryParams() {
        QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
        return decoder.parameters();
    }

    private static HttpHeaders parseHeaders(FullHttpRequest request) {
        HttpHeaders headers = new HttpHeaders();
        request.headers().forEach(entry -> headers.add(entry.getKey(), entry.getValue()));
        return headers;
    }
}
