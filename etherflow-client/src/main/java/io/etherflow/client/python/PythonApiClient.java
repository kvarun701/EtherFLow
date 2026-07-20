package io.etherflow.client.python;

import io.etherflow.client.HttpClient;
import io.etherflow.core.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified Reactive API client for calling Python web services (Flask & FastAPI).
 * <p>
 * Allows seamless, high-performance async communication with both Flask and FastAPI endpoints
 * returning EtherFlow {@link Mono} reactive publishers.
 * </p>
 *
 * <pre>{@code
 * PythonApiClient pythonClient = PythonApiClient.builder()
 *         .flaskUrl("http://localhost:5001")
 *         .fastApiUrl("http://localhost:5002")
 *         .build();
 *
 * Mono<Map> flaskRes = pythonClient.flask().get("/api/flask/hello", Map.class);
 * Mono<Map> fastRes  = pythonClient.fastApi().get("/api/fastapi/hello", Map.class);
 * }</pre>
 */
public class PythonApiClient {

    private final FlaskApiClient flaskClient;
    private final FastApiClient fastApiClient;

    private PythonApiClient(Builder builder) {
        HttpClient client = builder.httpClient != null ? builder.httpClient : HttpClient.create();
        this.flaskClient = new FlaskApiClient(builder.flaskUrl, client);
        this.fastApiClient = new FastApiClient(builder.fastApiUrl, client);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PythonApiClient create() {
        return builder().build();
    }

    public static PythonApiClient create(String flaskUrl, String fastApiUrl) {
        return builder().flaskUrl(flaskUrl).fastApiUrl(fastApiUrl).build();
    }

    /**
     * Gets the dedicated Flask API client.
     */
    public FlaskApiClient flask() {
        return flaskClient;
    }

    /**
     * Gets the dedicated FastAPI client.
     */
    public FastApiClient fastApi() {
        return fastApiClient;
    }

    /**
     * Executes a GET request against Flask API.
     */
    public <T> Mono<T> callFlaskGet(String path, Class<T> responseType) {
        return flaskClient.get(path, responseType);
    }

    /**
     * Executes a POST request with payload against Flask API.
     */
    public <T> Mono<T> callFlaskPost(String path, Object body, Class<T> responseType) {
        return flaskClient.post(path, body, responseType);
    }

    /**
     * Executes a GET request against FastAPI.
     */
    public <T> Mono<T> callFastApiGet(String path, Class<T> responseType) {
        return fastApiClient.get(path, responseType);
    }

    /**
     * Executes a POST request with payload against FastAPI.
     */
    public <T> Mono<T> callFastApiPost(String path, Object body, Class<T> responseType) {
        return fastApiClient.post(path, body, responseType);
    }

    /**
     * Asynchronously checks health status of both Flask and FastAPI services.
     */
    public Mono<Map<String, Object>> checkHealth() {
        return flaskClient.health()
                .flatMap(flaskStatus -> fastApiClient.health()
                        .map(fastApiStatus -> {
                            Map<String, Object> combined = new HashMap<>();
                            combined.put("flask", flaskStatus);
                            combined.put("fastapi", fastApiStatus);
                            combined.put("overallStatus", "UP");
                            return combined;
                        }))
                .onErrorResume(err -> {
                    Map<String, Object> fallback = new HashMap<>();
                    fallback.put("overallStatus", "DEGRADED");
                    fallback.put("error", err.getMessage());
                    return Mono.just(fallback);
                });
    }

    public static class Builder {
        private String flaskUrl = "http://localhost:5001";
        private String fastApiUrl = "http://localhost:5002";
        private HttpClient httpClient;

        public Builder flaskUrl(String flaskUrl) {
            this.flaskUrl = flaskUrl;
            return this;
        }

        public Builder fastApiUrl(String fastApiUrl) {
            this.fastApiUrl = fastApiUrl;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public PythonApiClient build() {
            return new PythonApiClient(this);
        }
    }
}
