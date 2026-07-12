package io.etherflow.client;

@FunctionalInterface
public interface ClientFilter {
    ClientResponse filter(ClientResponse response);

    static ClientFilter identity() {
        return response -> response;
    }
}
