package io.etherflow.http;

import java.util.*;

public class HttpHeaders {

    private final Map<String, List<String>> headers = new LinkedHashMap<>();

    public HttpHeaders() {}

    public HttpHeaders add(String name, String value) {
        headers.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(value);
        return this;
    }

    public HttpHeaders set(String name, String value) {
        headers.put(name.toLowerCase(), new ArrayList<>(List.of(value)));
        return this;
    }

    public String getFirst(String name) {
        List<String> values = headers.get(name.toLowerCase());
        return values != null && !values.isEmpty() ? values.getFirst() : null;
    }

    public List<String> get(String name) {
        return headers.getOrDefault(name.toLowerCase(), List.of());
    }

    public List<String> getContentType() {
        return get("content-type");
    }

    public HttpHeaders setContentType(String contentType) {
        return set("content-type", contentType);
    }

    public long getContentLength() {
        String val = getFirst("content-length");
        return val != null ? Long.parseLong(val) : -1;
    }

    public Set<String> names() {
        return headers.keySet();
    }

    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(headers);
    }

    public boolean contains(String name) {
        return headers.containsKey(name.toLowerCase());
    }

    public static HttpHeaders of(String key, String value) {
        return new HttpHeaders().add(key, value);
    }

    public static HttpHeaders of(String key1, String value1, String key2, String value2) {
        return new HttpHeaders().add(key1, value1).add(key2, value2);
    }

    @Override
    public String toString() {
        return headers.toString();
    }
}
