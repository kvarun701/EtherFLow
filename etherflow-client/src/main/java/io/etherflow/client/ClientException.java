package io.etherflow.client;

public class ClientException extends RuntimeException {
    private final int statusCode;
    private final String statusText;
    private final String responseBody;

    public ClientException(int statusCode, String statusText, String responseBody) {
        super(statusCode + " " + statusText + ": " + responseBody);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.responseBody = responseBody;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.statusText = null;
        this.responseBody = null;
    }

    public int getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public String getResponseBody() { return responseBody; }

    public boolean isClientError() { return statusCode >= 400 && statusCode < 500; }
    public boolean isServerError() { return statusCode >= 500 && statusCode < 600; }
}
