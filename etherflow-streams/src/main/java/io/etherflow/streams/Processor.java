package io.etherflow.streams;

public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
