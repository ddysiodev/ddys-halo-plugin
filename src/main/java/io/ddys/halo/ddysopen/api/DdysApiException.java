package io.ddys.halo.ddysopen.api;

public class DdysApiException extends RuntimeException {
    private final int status;
    private final String endpoint;

    public DdysApiException(int status, String endpoint, String message) {
        super(message);
        this.status = status;
        this.endpoint = endpoint;
    }

    public int getStatus() {
        return status;
    }

    public String getEndpoint() {
        return endpoint;
    }
}

