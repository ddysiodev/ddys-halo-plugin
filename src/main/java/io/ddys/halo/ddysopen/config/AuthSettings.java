package io.ddys.halo.ddysopen.config;

public class AuthSettings {
    private String apiKey = "";
    private Boolean allowAuthenticatedWrites = false;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Boolean getAllowAuthenticatedWrites() {
        return allowAuthenticatedWrites;
    }

    public void setAllowAuthenticatedWrites(Boolean allowAuthenticatedWrites) {
        this.allowAuthenticatedWrites = allowAuthenticatedWrites;
    }
}

