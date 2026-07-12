package io.etherflow.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etherflow.codec.MediaType;
import io.etherflow.codec.json.JacksonCodec;
import io.etherflow.core.Mono;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClient {

    private final okhttp3.OkHttpClient okHttpClient;
    private final JacksonCodec codec;
    private final RetrySpec retrySpec;
    private final CacheSpec cacheSpec;
    private final String baseUrl;

    private HttpClient(Builder builder) {
        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        if (builder.connectTimeout != null) okBuilder.connectTimeout(builder.connectTimeout);
        if (builder.readTimeout != null) okBuilder.readTimeout(builder.readTimeout);
        if (builder.writeTimeout != null) okBuilder.writeTimeout(builder.writeTimeout);
        this.okHttpClient = okBuilder.build();
        this.codec = builder.codec != null ? builder.codec : new JacksonCodec();
        this.retrySpec = builder.retrySpec;
        this.cacheSpec = builder.cacheSpec;
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : "";
    }

    public static Builder builder() { return new Builder(); }

    public static HttpClient create() {
        return new Builder().build();
    }

    public RequestHeadersSpec get() {
        return method("GET");
    }

    public RequestHeadersSpec post() {
        return method("POST");
    }

    public RequestHeadersSpec put() {
        return method("PUT");
    }

    public RequestHeadersSpec delete() {
        return method("DELETE");
    }

    public RequestHeadersSpec patch() {
        return method("PATCH");
    }

    public RequestHeadersSpec head() {
        return method("HEAD");
    }

    public RequestHeadersSpec options() {
        return method("OPTIONS");
    }

    public RequestHeadersSpec method(String method) {
        return new RequestHeadersSpec(this, method, baseUrl);
    }

    ObjectMapper getObjectMapper() {
        return codec.objectMapper();
    }

    @SuppressWarnings("unchecked")
    <T> Mono<T> execute(String method, String uri, Map<String, String> headers,
                        MediaType acceptType, Object body, MediaType bodyContentType) {
        String resolvedUri = uri.isEmpty() ? baseUrl : (uri.startsWith("http") ? uri : baseUrl + uri);

        String cacheKey = cacheSpec.isEnabled() ? method + ":" + resolvedUri + headers : null;

        if (cacheKey != null && "GET".equalsIgnoreCase(method)) {
            byte[] cached = cacheSpec.get(cacheKey);
            if (cached != null) {
                try {
                    return (Mono<T>) Mono.just(new ClientResponse(200, "OK (cached)", Map.of(), cached, codec));
                } catch (Exception ignored) {}
            }
        }

        return executeInternal(resolvedUri, method, headers, acceptType, body, bodyContentType, 0)
            .map(response -> {
                if (cacheKey != null && response.isSuccess() && "GET".equalsIgnoreCase(method)) {
                    cacheSpec.put(cacheKey, response.body());
                }
                return (T) response;
            });
    }

    private Mono<ClientResponse> executeInternal(String resolvedUri, String method,
                                                  Map<String, String> headers,
                                                  MediaType acceptType, Object body,
                                                  MediaType bodyContentType,
                                                  int attempt) {
        return Mono.create(sink -> {
            Request.Builder reqBuilder = new Request.Builder().url(resolvedUri);

            for (Map.Entry<String, String> h : headers.entrySet()) {
                reqBuilder.addHeader(h.getKey(), h.getValue());
            }
            reqBuilder.addHeader("Accept", acceptType != null ? acceptType.toString() : "application/json");

            if (body != null) {
                try {
                    byte[] jsonBytes = codec.objectMapper().writeValueAsBytes(body);
                    okhttp3.MediaType mt = okhttp3.MediaType.parse(
                        bodyContentType != null ? bodyContentType.toString() : "application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(jsonBytes, mt);
                    reqBuilder.method(method, requestBody);
                } catch (Exception e) {
                    sink.error(new ClientException("Failed to serialize request body", e));
                    return;
                }
            } else {
                reqBuilder.method(method, null);
            }

            okHttpClient.newCall(reqBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (attempt < retrySpec.maxRetries()) {
                        long delay = retrySpec.computeDelay(attempt + 1);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            sink.error(new ClientException("Request interrupted", ie));
                            return;
                        }
                        executeInternal(resolvedUri, method, headers, acceptType,
                            body, bodyContentType, attempt + 1)
                            .subscribe(new io.etherflow.streams.Subscriber<ClientResponse>() {
                                @Override
                                public void onSubscribe(io.etherflow.streams.Subscription s) { s.request(1); }
                                @Override
                                public void onNext(ClientResponse item) { sink.success(item); }
                                @Override
                                public void onError(Throwable t) { sink.error(t); }
                                @Override
                                public void onComplete() {}
                            });
                    } else {
                        sink.error(new ClientException("Request failed after " + retrySpec.maxRetries()
                            + " retries: " + e.getMessage(), e));
                    }
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) {
                    sink.success(ClientResponse.fromOkHttp(response, codec));
                    response.close();
                }
            });
        });
    }

    public static class Builder {
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(30);
        private Duration writeTimeout = Duration.ofSeconds(30);
        private JacksonCodec codec;
        private RetrySpec retrySpec = RetrySpec.defaults();
        private CacheSpec cacheSpec = CacheSpec.disabled();
        private String baseUrl;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return this;
        }

        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        public Builder readTimeout(Duration timeout) {
            this.readTimeout = timeout;
            return this;
        }

        public Builder writeTimeout(Duration timeout) {
            this.writeTimeout = timeout;
            return this;
        }

        public Builder codec(JacksonCodec codec) {
            this.codec = codec;
            return this;
        }

        public Builder retry(RetrySpec retrySpec) {
            this.retrySpec = retrySpec;
            return this;
        }

        public Builder retry(int maxRetries) {
            this.retrySpec = new RetrySpec(maxRetries, Duration.ofMillis(100), 2.0, Duration.ofSeconds(5));
            return this;
        }

        public Builder cache(CacheSpec cacheSpec) {
            this.cacheSpec = cacheSpec;
            return this;
        }

        public Builder cache(Duration ttl, int maxSize) {
            this.cacheSpec = CacheSpec.of(ttl, maxSize);
            return this;
        }

        public HttpClient build() {
            return new HttpClient(this);
        }
    }
}
