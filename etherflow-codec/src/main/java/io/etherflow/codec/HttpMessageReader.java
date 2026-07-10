package io.etherflow.codec;

import io.etherflow.core.Mono;

public interface HttpMessageReader<T> {

    Mono<T> read(Class<? extends T> type, DataBuffer buffer, MediaType mediaType);

    boolean canRead(Class<?> type, MediaType mediaType);
}
