package io.etherflow.web.function;

import io.etherflow.codec.DataBuffer;
import io.etherflow.codec.DefaultDataBufferFactory;
import io.etherflow.codec.MediaType;
import io.etherflow.codec.json.JacksonCodec;
import io.etherflow.core.Mono;
import io.etherflow.http.HttpHeaders;
import io.etherflow.http.ServerHttpResponse;

public class ServerResponse {

    private final int statusCode;
    private final HttpHeaders headers;
    private final Object body;
    private final MediaType contentType;

    private ServerResponse(int statusCode, HttpHeaders headers, Object body, MediaType contentType) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.contentType = contentType;
    }

    public int statusCode() {
        return statusCode;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public Object body() {
        return body;
    }

    public MediaType contentType() {
        return contentType;
    }

    public static ServerResponse ok() {
        return new ServerResponse(200, new HttpHeaders(), null, null);
    }

    public static ServerResponse ok(Object body) {
        return new ServerResponse(200, new HttpHeaders(), body, MediaType.APPLICATION_JSON);
    }

    public static ServerResponse created() {
        return new ServerResponse(201, new HttpHeaders(), null, null);
    }

    public static ServerResponse noContent() {
        return new ServerResponse(204, new HttpHeaders(), null, null);
    }

    public static ServerResponse badRequest() {
        return new ServerResponse(400, new HttpHeaders(), null, null);
    }

    public static ServerResponse notFound() {
        return new ServerResponse(404, new HttpHeaders(), null, null);
    }

    public static ServerResponse status(int statusCode) {
        return new ServerResponse(statusCode, new HttpHeaders(), null, null);
    }

    public ServerResponse headers(HttpHeaders headers) {
        return new ServerResponse(statusCode, headers, body, contentType);
    }

    public ServerResponse contentType(MediaType mediaType) {
        return new ServerResponse(statusCode, headers, body, mediaType);
    }

    public ServerResponse body(Object body) {
        return new ServerResponse(statusCode, headers, body, contentType);
    }

    public Mono<Void> writeTo(ServerHttpResponse httpResponse) {
        httpResponse.statusCode(statusCode);

        if (contentType != null) {
            httpResponse.contentType(contentType);
        }

        if (headers != null) {
            for (String name : headers.names()) {
                for (String value : headers.get(name)) {
                    httpResponse.headers().add(name, value);
                }
            }
        }

        if (body == null) {
            return Mono.empty();
        }

        MediaType responseContentType = contentType != null ? contentType : MediaType.APPLICATION_JSON;
        httpResponse.contentType(responseContentType);

        JacksonCodec codec = new JacksonCodec();
        return codec.write(body, responseContentType)
                .flatMap(httpResponse::writeWith);
    }
}
