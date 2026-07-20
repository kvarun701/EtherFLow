package io.etherflow.client.python;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PythonApiClientTest {

    @Test
    void testClientBuilderDefaultUrls() {
        PythonApiClient client = PythonApiClient.create();
        assertNotNull(client.flask());
        assertNotNull(client.fastApi());
        assertEquals("http://localhost:5001", client.flask().getBaseUrl());
        assertEquals("http://localhost:5002", client.fastApi().getBaseUrl());
    }

    @Test
    void testClientBuilderCustomUrls() {
        PythonApiClient client = PythonApiClient.builder()
                .flaskUrl("http://127.0.0.1:8001/")
                .fastApiUrl("http://127.0.0.1:8002/")
                .build();

        assertEquals("http://127.0.0.1:8001", client.flask().getBaseUrl());
        assertEquals("http://127.0.0.1:8002", client.fastApi().getBaseUrl());
    }

    @Test
    void testIndividualClientCreation() {
        FlaskApiClient flask = FlaskApiClient.create("http://localhost:5001/");
        FastApiClient fastApi = FastApiClient.create("http://localhost:5002/");

        assertEquals("http://localhost:5001", flask.getBaseUrl());
        assertEquals("http://localhost:5002", fastApi.getBaseUrl());
    }
}
