package io.etherflow.client;

import io.etherflow.codec.MediaType;
import io.etherflow.core.Mono;
import okhttp3.Response;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ResponseSpec {
    private final HttpClient httpClient;
    private final String method;
    private String uri;
    private final Map<String, String> headers;
    private final MediaType acceptType;
    private final Object body;
    private final MediaType bodyContentType;
    private boolean throwOnError = true;

    ResponseSpec(HttpClient httpClient, String method, String uri,
                 Map<String, String> headers, MediaType acceptType,
                 Object body, MediaType bodyContentType) {
        this.httpClient = httpClient;
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.acceptType = acceptType;
        this.body = body;
        this.bodyContentType = bodyContentType;
    }

    public <T> Mono<T> bodyTo(Class<T> type) {
        return exchangeInternal().flatMap(response -> {
            if (throwOnError && response.isError()) {
                return Mono.error(new ClientException(
                    response.statusCode(), response.statusText(), response.bodyAsString()));
            }
            return response.body(type);
        });
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<T> bodyTo(ParameterizedTypeReference<T> typeRef) {
        Type type = typeRef.getType();
        return exchangeInternal().flatMap(response -> {
            if (throwOnError && response.isError()) {
                return Mono.error(new ClientException(
                    response.statusCode(), response.statusText(), response.bodyAsString()));
            }
            return Mono.fromCallable(() -> {
                ObjectMapperAdapter adapter = new ObjectMapperAdapter(httpClient.getObjectMapper());
                return (T) adapter.readValue(response.body(), type);
            });
        });
    }

    public Mono<String> bodyToString() {
        return exchangeInternal().map(response -> {
            if (throwOnError && response.isError()) {
                throw new ClientException(
                    response.statusCode(), response.statusText(), response.bodyAsString());
            }
            return response.bodyAsString();
        });
    }

    public Mono<ClientResponse> toResponse() {
        return exchangeInternal();
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<Result<T>> toResult(Class<T> type) {
        Mono<T> result = (Mono<T>) bodyTo(type);
        return result
            .map(Result::success)
            .onErrorResume(e -> Mono.just((Result<T>) Result.error(e)));
    }

    public ResponseSpec throwOnError(boolean throwOnError) {
        this.throwOnError = throwOnError;
        return this;
    }

    private Mono<ClientResponse> exchangeInternal() {
        return httpClient.execute(method, uri, headers, acceptType, body, bodyContentType);
    }

    private static class ObjectMapperAdapter {
        private final com.fasterxml.jackson.databind.ObjectMapper mapper;

        ObjectMapperAdapter(com.fasterxml.jackson.databind.ObjectMapper mapper) {
            this.mapper = mapper;
        }

        Object readValue(byte[] src, Type type) throws Exception {
            com.fasterxml.jackson.databind.JavaType javaType = mapper.constructType(type);
            return mapper.readValue(src, javaType);
        }
    }
}
