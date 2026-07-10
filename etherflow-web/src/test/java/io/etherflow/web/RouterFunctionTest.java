package io.etherflow.web;

import io.etherflow.codec.DataBuffer;
import io.etherflow.codec.DefaultDataBufferFactory;
import io.etherflow.codec.MediaType;
import io.etherflow.codec.json.JacksonCodec;
import io.etherflow.core.Mono;
import io.etherflow.http.*;
import io.etherflow.web.function.*;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RouterFunctionTest {

    static class Message {
        public String text;
        public Message() {}
        public Message(String text) { this.text = text; }
    }

    private final ServerRequest testRequest = createRequest("GET", "/hello", Map.of());

    private static ServerRequest createRequest(String method, String path, Map<String, String> pathVars) {
        return new ServerRequest(new ServerWebExchange(
                new ServerHttpRequest() {
                    @Override public HttpMethod method() { return HttpMethod.from(method); }
                    @Override public URI uri() { return URI.create(path); }
                    @Override public String path() { return path; }
                    @Override public HttpHeaders headers() { return new HttpHeaders(); }
                    @Override public Map<String, String> pathVariables() { return pathVars; }
                    @Override public Mono<DataBuffer> body() { return Mono.empty(); }
                    @Override public Map<String, List<String>> queryParams() { return Map.of(); }
                },
                new ServerHttpResponse() {
                    private int status;
                    private final HttpHeaders headers = new HttpHeaders();
                    private DataBuffer written;

                    @Override public HttpHeaders headers() { return headers; }
                    @Override public int statusCode() { return status; }
                    @Override public ServerHttpResponse statusCode(int code) { status = code; return this; }
                    @Override public Mono<Void> writeWith(DataBuffer buf) { written = buf; return Mono.empty(); }
                    @Override public Mono<Void> writeAndFlushWith(DataBuffer buf) { return writeWith(buf); }
                }
        ));
    }

    @Test
    void routeMatch() {
        RouterFunction router = RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("world")))
                .build();

        Mono<HandlerFunction> result = router.route(testRequest);
        assertNotNull(result.block());
    }

    @Test
    void routeNoMatch() {
        RouterFunction router = RouterFunction.route()
                .GET("/bye", req -> Mono.just(ServerResponse.ok("world")))
                .build();

        Mono<HandlerFunction> result = router.route(testRequest);
        assertNull(result.block());
    }

    @Test
    void handlerReturnsOk() {
        HandlerFunction handler = req -> Mono.just(ServerResponse.ok("done"));
        ServerResponse response = handler.handle(testRequest).block();
        assertNotNull(response);
        assertEquals(200, response.statusCode());
    }

    @Test
    void handlerReturnsNotFound() {
        HandlerFunction handler = req -> Mono.just(ServerResponse.notFound());
        ServerResponse response = handler.handle(testRequest).block();
        assertEquals(404, response.statusCode());
    }

    @Test
    void predicateMethod() {
        assertTrue(RequestPredicate.GET("/hello").test(testRequest));
        assertFalse(RequestPredicate.POST("/hello").test(testRequest));
    }

    @Test
    void predicatePathVars() {
        assertTrue(RequestPredicate.path("/hello").test(testRequest));
        assertFalse(RequestPredicate.path("/world").test(testRequest));
    }
}
