package io.etherflow.streams;

public interface Subscription {

    void request(long n);

    void cancel();
}
