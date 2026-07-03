package io.ddys.halo.ddysopen.config;

public class CacheSettings {
    private Boolean enabled = true;
    private Integer maxEntries = 512;
    private Integer defaultTtlSeconds = 600;
    private Integer realtimeTtlSeconds = 300;
    private Integer detailTtlSeconds = 1800;
    private Integer dictionaryTtlSeconds = 86400;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(Integer maxEntries) {
        this.maxEntries = maxEntries;
    }

    public Integer getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(Integer defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public Integer getRealtimeTtlSeconds() {
        return realtimeTtlSeconds;
    }

    public void setRealtimeTtlSeconds(Integer realtimeTtlSeconds) {
        this.realtimeTtlSeconds = realtimeTtlSeconds;
    }

    public Integer getDetailTtlSeconds() {
        return detailTtlSeconds;
    }

    public void setDetailTtlSeconds(Integer detailTtlSeconds) {
        this.detailTtlSeconds = detailTtlSeconds;
    }

    public Integer getDictionaryTtlSeconds() {
        return dictionaryTtlSeconds;
    }

    public void setDictionaryTtlSeconds(Integer dictionaryTtlSeconds) {
        this.dictionaryTtlSeconds = dictionaryTtlSeconds;
    }
}

