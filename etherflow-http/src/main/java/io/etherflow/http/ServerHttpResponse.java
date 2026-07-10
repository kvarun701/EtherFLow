package io.etherflow.http;

import io.etherflow.codec.DataBuffer;
import io.etherflow.codec.MediaType;
import io.etherflow.core.Mono;

public interface ServerHttpResponse {

    HttpHeaders headers();

    int statusCode();

    ServerHttpResponse statusCode(int statusCode);

    Mono<Void> writeWith(DataBuffer buffer);

    Mono<Void> writeAndFlushWith(DataBuffer buffer);

    default ServerHttpResponse contentType(MediaType mediaType) {
        headers().setContentType(mediaType.toString());
        return this;
    }

    default ServerHttpResponse contentType(String mediaType) {
        headers().setContentType(mediaType);
        return this;
    }
}
