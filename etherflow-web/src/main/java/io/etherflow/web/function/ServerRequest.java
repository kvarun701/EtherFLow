package io.etherflow.web.function;

import io.etherflow.codec.DataBuffer;
import io.etherflow.codec.MediaType;
import io.etherflow.core.Mono;
import io.etherflow.http.HttpHeaders;
import io.etherflow.http.HttpMethod;
import io.etherflow.http.ServerWebExchange;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class ServerRequest {

    private final ServerWebExchange exchange;

    public ServerRequest(ServerWebExchange exchange) {
        this.exchange = exchange;
    }

    public ServerWebExchange exchange() {
        return exchange;
    }

    public HttpMethod method() {
        return exchange.request().method();
    }

    public String path() {
        return exchange.request().path();
    }

    public URI uri() {
        return exchange.request().uri();
    }

    public HttpHeaders headers() {
        return exchange.request().headers();
    }

    public Map<String, String> pathVariables() {
        return exchange.request().pathVariables();
    }

    public String pathVariable(String name) {
        return exchange.request().pathVariables().get(name);
    }

    public Map<String, List<String>> queryParams() {
        return exchange.request().queryParams();
    }

    public String queryParam(String name) {
        return exchange.request().queryParam(name);
    }

    public Mono<DataBuffer> body() {
        return exchange.request().body();
    }

    public <T> Mono<T> bodyTo(Class<T> type) {
        return body().flatMap(buf -> {
            MediaType mediaType = MediaType.APPLICATION_JSON;
            String contentType = headers().getFirst("content-type");
            if (contentType != null) {
                mediaType = MediaType.parse(contentType);
            }
            return new io.etherflow.codec.json.JacksonCodec().readValue(buf, type);
        });
    }
}
