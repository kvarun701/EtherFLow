package io.etherflow.streams;

public interface Subscriber<T> {

    void onSubscribe(Subscription subscription);

    void onNext(T item);

    void onError(Throwable throwable);

    void onComplete();
}
