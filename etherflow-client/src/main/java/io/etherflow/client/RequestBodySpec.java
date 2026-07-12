package io.etherflow.client;

import io.etherflow.codec.MediaType;

public class RequestBodySpec extends RequestHeadersSpec {

    private Object body;
    private MediaType bodyContentType;

    RequestBodySpec(HttpClient httpClient, String method, String uri) {
        super(httpClient, method, uri);
    }

    public RequestBodySpec body(Object body) {
        this.body = body;
        return this;
    }

    public RequestBodySpec contentType(MediaType mediaType) {
        this.bodyContentType = mediaType;
        return this;
    }

    Object getBody() { return body; }
    MediaType getBodyContentType() { return bodyContentType; }

    @Override
    public RequestBodySpec header(String name, String value) {
        super.header(name, value);
        return this;
    }

    @Override
    public RequestBodySpec accept(MediaType mediaType) {
        super.accept(mediaType);
        return this;
    }
}
