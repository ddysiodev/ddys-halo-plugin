package io.ddys.halo.ddysopen.config;

public class BasicSettings {
    private String apiBaseUrl = "https://ddys.io/api/v1";
    private String siteBaseUrl = "https://ddys.io";
    private Integer requestTimeoutSeconds = 8;
    private Integer retryCount = 1;
    private Boolean debug = false;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getSiteBaseUrl() {
        return siteBaseUrl;
    }

    public void setSiteBaseUrl(String siteBaseUrl) {
        this.siteBaseUrl = siteBaseUrl;
    }

    public Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }
}

