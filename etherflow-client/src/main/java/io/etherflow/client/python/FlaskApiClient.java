package io.etherflow.client.python;

import io.etherflow.client.HttpClient;
import io.etherflow.client.ParameterizedTypeReference;
import io.etherflow.client.ResponseSpec;
import io.etherflow.codec.MediaType;
import io.etherflow.core.Mono;

import java.util.Map;

/**
 * Specialized reactive HTTP client for interacting with Python Flask REST APIs.
 */
public class FlaskApiClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public FlaskApiClient(String baseUrl) {
        this(baseUrl, HttpClient.create());
    }

    public FlaskApiClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = httpClient;
    }

    public static FlaskApiClient create(String baseUrl) {
        return new FlaskApiClient(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Executes a reactive GET request against a Flask API path.
     */
    public <T> Mono<T> get(String path, Class<T> responseType) {
        String url = normalizeUrl(path);
        return httpClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyTo(responseType);
    }

    /**
     * Executes a reactive GET request returning complex parameterized types (e.g. Map<String, Object>).
     */
    public <T> Mono<T> get(String path, ParameterizedTypeReference<T> typeRef) {
        String url = normalizeUrl(path);
        return httpClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyTo(typeRef);
    }

    /**
     * Executes a reactive POST request with a JSON payload to a Flask API path.
     */
    public <T> Mono<T> post(String path, Object requestBody, Class<T> responseType) {
        String url = normalizeUrl(path);
        return httpClient.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyTo(responseType);
    }

    /**
     * Convenience method to check the health status of a Flask service.
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> health() {
        return (Mono<Map<String, Object>>) (Mono<?>) get("/api/flask/health", Map.class);
    }

    private String normalizeUrl(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return path.startsWith("/") ? baseUrl + path : baseUrl + "/" + path;
    }
}
