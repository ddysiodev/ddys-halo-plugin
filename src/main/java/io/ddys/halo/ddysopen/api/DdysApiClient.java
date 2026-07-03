package io.ddys.halo.ddysopen.api;

import io.ddys.halo.ddysopen.cache.DdysCache;
import io.ddys.halo.ddysopen.config.DdysSettings;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class DdysApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final DdysSettingsService settingsService;
    private final DdysCache cache;

    public DdysApiClient(DdysSettingsService settingsService, DdysCache cache) {
        this.settingsService = settingsService;
        this.cache = cache;
    }

    public Mono<Map<String, Object>> get(String path, Map<String, ?> query) {
        return settingsService.fetch()
            .flatMap(settings -> get(settings, path, query == null ? Map.of() : query));
    }

    public Mono<Map<String, Object>> getAuthenticated(String path, Map<String, ?> query) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.hasApiKey()) {
                    return Mono.error(new DdysApiException(403, path, "API key is not configured."));
                }
                return executeGet(settings, normalizePath(path), query == null ? Map.of() : query, true);
            });
    }

    public Mono<Map<String, Object>> post(String path, Map<String, ?> body) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.authenticatedWritesAllowed()) {
                    return Mono.error(new DdysApiException(403, path, "Authenticated writes are disabled."));
                }
                return executePost(settings, path, body == null ? Map.of() : body);
            });
    }

    public Mono<Map<String, Object>> delete(String path) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.authenticatedWritesAllowed()) {
                    return Mono.error(new DdysApiException(403, path, "Authenticated writes are disabled."));
                }
                return executeDelete(settings, path);
            });
    }

    private Mono<Map<String, Object>> get(DdysSettings settings, String path, Map<String, ?> query) {
        String safePath = normalizePath(path);
        int ttl = settings.ttlFor(safePath);
        String key = cacheKey(settings.apiBaseUrl(), safePath, query);
        Mono<Map<String, Object>> supplier = executeGet(settings, safePath, query, false);
        if (!settings.cacheEnabled()) {
            return supplier;
        }
        return cache.getOrCompute(key, ttl, settings.maxCacheEntries(), () -> supplier);
    }

    private Mono<Map<String, Object>> executeGet(
        DdysSettings settings,
        String path,
        Map<String, ?> query,
        boolean authenticated
    ) {
        var spec = client(settings)
            .get()
            .uri(builder -> buildUri(builder, path, query))
            .accept(MediaType.APPLICATION_JSON);
        if (authenticated) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey());
        }
        Mono<Map<String, Object>> request = spec
            .retrieve()
            .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new DdysApiException(response.statusCode().value(), path, body)))
            .bodyToMono(MAP_TYPE)
            .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()));
        if (settings.retryCount() <= 0) {
            return request;
        }
        return request.retryWhen(Retry.backoff(settings.retryCount(), Duration.ofMillis(200))
            .filter(this::shouldRetry));
    }

    private Mono<Map<String, Object>> executePost(DdysSettings settings, String path, Map<String, ?> body) {
        String safePath = normalizePath(path);
        return client(settings)
            .post()
            .uri(builder -> buildUri(builder, safePath, Map.of()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(raw -> new DdysApiException(response.statusCode().value(), safePath, raw)))
            .bodyToMono(MAP_TYPE)
            .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()));
    }

    private Mono<Map<String, Object>> executeDelete(DdysSettings settings, String path) {
        String safePath = normalizePath(path);
        return client(settings)
            .delete()
            .uri(builder -> buildUri(builder, safePath, Map.of()))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.apiKey())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(raw -> new DdysApiException(response.statusCode().value(), safePath, raw)))
            .bodyToMono(MAP_TYPE)
            .timeout(Duration.ofSeconds(settings.requestTimeoutSeconds()));
    }

    private WebClient client(DdysSettings settings) {
        return WebClient.builder()
            .baseUrl(settings.apiBaseUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, "ddys-halo-plugin/0.1.0")
            .build();
    }

    private URI buildUri(UriBuilder builder, String path, Map<String, ?> query) {
        UriBuilder uri = builder.path(path);
        query.forEach((key, value) -> {
            if (value instanceof Iterable<?> values) {
                values.forEach(item -> addQuery(uri, key, item));
            } else {
                addQuery(uri, key, value);
            }
        });
        return uri.build();
    }

    private void addQuery(UriBuilder builder, String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        String raw = value.toString();
        if (!raw.isBlank()) {
            builder.queryParam(key, raw);
        }
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof DdysApiException exception) {
            return exception.getStatus() == 429 || exception.getStatus() >= 500;
        }
        return true;
    }

    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    public static String safeSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Path segment is required.");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 180 || !trimmed.matches("[A-Za-z0-9._~:-]+")) {
            throw new IllegalArgumentException("Invalid path segment.");
        }
        return UriUtils.encodePathSegment(trimmed, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String cacheKey(String baseUrl, String path, Map<String, ?> query) {
        List<String> parts = new ArrayList<>();
        query.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            if (value instanceof Iterable<?> values) {
                values.forEach(item -> parts.add(key + "=" + item));
            } else {
                parts.add(key + "=" + value);
            }
        });
        parts.sort(String::compareTo);
        return baseUrl.toLowerCase(Locale.ROOT) + "|" + path + "|" + String.join("&", parts);
    }

    public static Map<String, Object> responseError(Throwable throwable) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        if (throwable instanceof DdysApiException apiException) {
            error.put("status", apiException.getStatus());
            error.put("endpoint", apiException.getEndpoint());
        }
        error.put("message", throwable.getMessage());
        return error;
    }
}
