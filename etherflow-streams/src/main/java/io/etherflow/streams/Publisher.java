package io.etherflow.streams;

@FunctionalInterface
public interface Publisher<T> {

    void subscribe(Subscriber<? super T> subscriber);
}
