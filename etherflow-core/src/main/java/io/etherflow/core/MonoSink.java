package io.etherflow.core;

/**
 * Emitter interface for {@link Mono#create(Consumer)}.
 * Provides type-safe success/error emission for bridging callback-based APIs.
 *
 * @param <T> the value type
 */
public interface MonoSink<T> {
    void success(T value);
    void error(Throwable t);
}
