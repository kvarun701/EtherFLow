package io.etherflow.client;

import io.etherflow.codec.MediaType;
import io.etherflow.core.Mono;

import java.util.*;

public class RequestHeadersSpec {
    private final HttpClient httpClient;
    private final String method;
    private String uri;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private MediaType acceptType = MediaType.APPLICATION_JSON;

    RequestHeadersSpec(HttpClient httpClient, String method, String uri) {
        this.httpClient = httpClient;
        this.method = method;
        this.uri = uri;
    }

    public RequestHeadersSpec uri(String uri, Object... vars) {
        if (vars != null && vars.length > 0) {
            for (Object var : vars) {
                int idx = uri.indexOf('{');
                int end = uri.indexOf('}', idx);
                if (idx >= 0 && end > idx) {
                    uri = uri.substring(0, idx) + var + uri.substring(end + 1);
                }
            }
        }
        this.uri = uri;
        return this;
    }

    public RequestHeadersSpec header(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    public RequestHeadersSpec accept(MediaType mediaType) {
        this.acceptType = mediaType;
        return this;
    }

    public RequestBodySpec body(Object body) {
        RequestBodySpec bodySpec = new RequestBodySpec(httpClient, method, uri);
        copyTo(bodySpec);
        return bodySpec.body(body);
    }

    public Mono<ClientResponse> exchange() {
        return httpClient.execute(method, uri, headers, acceptType, null, null);
    }

    public ResponseSpec retrieve() {
        return new ResponseSpec(httpClient, method, uri, headers, acceptType, null, null);
    }

    String getMethod() { return method; }
    String getUri() { return uri; }

    void copyTo(RequestHeadersSpec target) {
        target.headers.putAll(this.headers);
        target.acceptType = this.acceptType;
    }
}
