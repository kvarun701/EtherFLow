package io.etherflow.codec;

import io.etherflow.core.Mono;

public interface HttpMessageWriter<T> {

    Mono<DataBuffer> write(T value, MediaType mediaType);

    boolean canWrite(Class<?> type, MediaType mediaType);
}
