package io.ddys.halo.ddysopen.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record DdysSettings(
    BasicSettings basic,
    CacheSettings cache,
    DisplaySettings display,
    FeatureSettings features,
    AuthSettings auth
) {

    public static DdysSettings defaults() {
        return new DdysSettings(
            new BasicSettings(),
            new CacheSettings(),
            new DisplaySettings(),
            new FeatureSettings(),
            new AuthSettings()
        );
    }

    public String apiBaseUrl() {
        return normalizeUrl(basic.getApiBaseUrl(), "https://ddys.io/api/v1");
    }

    public String siteBaseUrl() {
        return normalizeUrl(basic.getSiteBaseUrl(), "https://ddys.io");
    }

    public int requestTimeoutSeconds() {
        return clamp(basic.getRequestTimeoutSeconds(), 1, 30, 8);
    }

    public int retryCount() {
        return clamp(basic.getRetryCount(), 0, 3, 1);
    }

    public boolean debug() {
        return Boolean.TRUE.equals(basic.getDebug());
    }

    public boolean cacheEnabled() {
        return Boolean.TRUE.equals(cache.getEnabled());
    }

    public int maxCacheEntries() {
        return clamp(cache.getMaxEntries(), 32, 5000, 512);
    }

    public int defaultTtlSeconds() {
        return clamp(cache.getDefaultTtlSeconds(), 0, 86400, 600);
    }

    public int realtimeTtlSeconds() {
        return clamp(cache.getRealtimeTtlSeconds(), 0, 86400, 300);
    }

    public int detailTtlSeconds() {
        return clamp(cache.getDetailTtlSeconds(), 0, 86400, 1800);
    }

    public int dictionaryTtlSeconds() {
        return clamp(cache.getDictionaryTtlSeconds(), 0, 604800, 86400);
    }

    public String defaultLayout() {
        return allow(display.getDefaultLayout(), "grid", "list", "compact") ? display.getDefaultLayout() : "grid";
    }

    public String theme() {
        return allow(display.getTheme(), "auto", "light", "dark") ? display.getTheme() : "auto";
    }

    public int pageSize() {
        return clamp(display.getPageSize(), 1, 60, 12);
    }

    public boolean showAttribution() {
        return !Boolean.FALSE.equals(display.getShowAttribution());
    }

    public boolean openInNewTab() {
        return !Boolean.FALSE.equals(display.getOpenInNewTab());
    }

    public boolean shortcodesEnabled() {
        return !Boolean.FALSE.equals(features.getEnableShortcodes());
    }

    public boolean themeCssEnabled() {
        return !Boolean.FALSE.equals(features.getEnableThemeCss());
    }

    public boolean pagesEnabled() {
        return !Boolean.FALSE.equals(features.getEnablePages());
    }

    public boolean publicApiEnabled() {
        return !Boolean.FALSE.equals(features.getEnablePublicApi());
    }

    public boolean requestFormEnabled() {
        return Boolean.TRUE.equals(features.getEnableRequestForm());
    }

    public boolean authenticatedWritesAllowed() {
        return Boolean.TRUE.equals(auth.getAllowAuthenticatedWrites()) && hasApiKey();
    }

    public boolean hasApiKey() {
        return auth.getApiKey() != null && !auth.getApiKey().isBlank();
    }

    public String apiKey() {
        return auth.getApiKey() == null ? "" : auth.getApiKey().trim();
    }

    public Map<String, Object> safeSnapshot() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("apiBaseUrl", apiBaseUrl());
        value.put("siteBaseUrl", siteBaseUrl());
        value.put("requestTimeoutSeconds", requestTimeoutSeconds());
        value.put("retryCount", retryCount());
        value.put("debug", debug());
        value.put("cacheEnabled", cacheEnabled());
        value.put("maxCacheEntries", maxCacheEntries());
        value.put("defaultTtlSeconds", defaultTtlSeconds());
        value.put("realtimeTtlSeconds", realtimeTtlSeconds());
        value.put("detailTtlSeconds", detailTtlSeconds());
        value.put("dictionaryTtlSeconds", dictionaryTtlSeconds());
        value.put("defaultLayout", defaultLayout());
        value.put("theme", theme());
        value.put("pageSize", pageSize());
        value.put("showAttribution", showAttribution());
        value.put("openInNewTab", openInNewTab());
        value.put("shortcodesEnabled", shortcodesEnabled());
        value.put("themeCssEnabled", themeCssEnabled());
        value.put("pagesEnabled", pagesEnabled());
        value.put("publicApiEnabled", publicApiEnabled());
        value.put("requestFormEnabled", requestFormEnabled());
        value.put("hasApiKey", hasApiKey());
        value.put("authenticatedWritesAllowed", authenticatedWritesAllowed());
        return value;
    }

    public int ttlFor(String path) {
        if (path == null) {
            return defaultTtlSeconds();
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.equals("/types") || lower.equals("/genres") || lower.equals("/regions")) {
            return dictionaryTtlSeconds();
        }
        if (lower.equals("/latest") || lower.equals("/hot") || lower.equals("/search")
            || lower.equals("/suggest") || lower.equals("/calendar")) {
            return realtimeTtlSeconds();
        }
        if (lower.contains("/comments") || lower.equals("/requests") || lower.equals("/activities")) {
            return Math.min(realtimeTtlSeconds(), 300);
        }
        if (lower.startsWith("/movies/") || lower.startsWith("/collections/") || lower.startsWith("/shares/")) {
            return detailTtlSeconds();
        }
        return defaultTtlSeconds();
    }

    private static int clamp(Integer raw, int min, int max, int fallback) {
        if (raw == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, raw));
    }

    private static boolean allow(String raw, String... allowed) {
        if (raw == null) {
            return false;
        }
        for (String item : allowed) {
            if (item.equals(raw)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeUrl(String raw, String fallback) {
        String value = raw == null || raw.isBlank() ? fallback : raw.trim();
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                value = fallback;
            }
        } catch (IllegalArgumentException ignored) {
            value = fallback;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}

