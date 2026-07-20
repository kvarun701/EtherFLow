package io.etherflow.client.dotnet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DotNetApiClientTest {

    @Test
    void testDotNetApiClientDefaultUrl() {
        DotNetApiClient client = DotNetApiClient.create();
        assertEquals("http://localhost:5003", client.getBaseUrl());
    }

    @Test
    void testDotNetApiClientCustomUrl() {
        DotNetApiClient client = DotNetApiClient.create("http://127.0.0.1:8080/");
        assertEquals("http://127.0.0.1:8080", client.getBaseUrl());
    }
}
