package io.etherflow.client;

import io.etherflow.codec.json.JacksonCodec;
import io.etherflow.core.Mono;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.*;

public class ClientResponse {
    private final int statusCode;
    private final String statusText;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final JacksonCodec codec;

    ClientResponse(int statusCode, String statusText, Map<String, List<String>> headers,
                   byte[] body, JacksonCodec codec) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.headers = headers;
        this.body = body;
        this.codec = codec;
    }

    static ClientResponse fromOkHttp(Response response, JacksonCodec codec) {
        int code = response.code();
        String message = response.message();
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (String name : response.headers().names()) {
            headers.put(name, response.headers().values(name));
        }
        byte[] bodyBytes = {};
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            try {
                bodyBytes = responseBody.bytes();
            } catch (Exception ignored) {}
        }
        return new ClientResponse(code, message, headers, bodyBytes, codec);
    }

    public int statusCode() { return statusCode; }
    public String statusText() { return statusText; }
    public Map<String, List<String>> headers() { return headers; }

    public String header(String name) {
        List<String> values = headers.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    public List<String> headers(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    public byte[] body() { return body; }

    public String bodyAsString() {
        return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    public <T> Mono<T> body(Class<T> type) {
        return Mono.fromCallable(() -> codec.objectMapper().readValue(body, type));
    }

    public boolean isSuccess() { return statusCode >= 200 && statusCode < 300; }
    public boolean isError() { return !isSuccess(); }

    @Override
    public String toString() {
        return "ClientResponse{status=" + statusCode + ' ' + statusText + '}';
    }
}
