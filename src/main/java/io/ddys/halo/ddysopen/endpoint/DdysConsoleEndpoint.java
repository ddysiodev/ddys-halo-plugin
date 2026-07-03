package io.ddys.halo.ddysopen.endpoint;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.cache.DdysCache;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import io.ddys.halo.ddysopen.render.ShortcodeService;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import run.halo.app.plugin.PluginContext;

@Component
public class DdysConsoleEndpoint implements CustomEndpoint {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private static final Set<String> PREVIEW_ENDPOINTS = Set.of(
        "/movies", "/search", "/suggest", "/hot", "/latest", "/calendar", "/types", "/genres",
        "/regions", "/collections", "/shares", "/requests", "/activities"
    );

    private final PluginContext pluginContext;
    private final DdysSettingsService settingsService;
    private final DdysApiClient apiClient;
    private final DdysCache cache;
    private final ShortcodeService shortcodeService;
    private final DdysRateLimiter rateLimiter;

    public DdysConsoleEndpoint(
        PluginContext pluginContext,
        DdysSettingsService settingsService,
        DdysApiClient apiClient,
        DdysCache cache,
        ShortcodeService shortcodeService,
        DdysRateLimiter rateLimiter
    ) {
        this.pluginContext = pluginContext;
        this.settingsService = settingsService;
        this.apiClient = apiClient;
        this.cache = cache;
        this.shortcodeService = shortcodeService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route(GET("ddys/status"), this::status)
            .andRoute(GET("ddys/diagnostics"), this::diagnostics)
            .andRoute(GET("ddys/test"), this::test)
            .andRoute(GET("ddys/preview"), this::preview)
            .andRoute(GET("ddys/request"), this::request)
            .andRoute(GET("ddys/me"), this::me)
            .andRoute(POST("ddys/comments"), this::createComment)
            .andRoute(DELETE("ddys/comments/{id}"), this::deleteComment)
            .andRoute(POST("ddys/report"), this::report)
            .andRoute(POST("ddys/follow"), this::follow)
            .andRoute(GET("ddys/shortcodes"), request -> EndpointSupport.ok(shortcodeService.definitions()))
            .andRoute(POST("ddys/cache/flush"), this::flush)
            .andRoute(POST("ddys/cache/prune"), this::prune);
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("console.api.ddys.io/v1alpha1");
    }

    private Mono<ServerResponse> status(ServerRequest request) {
        return settingsService.fetch()
            .map(settings -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("plugin", pluginInfo());
                data.put("settings", settings.safeSnapshot());
                data.put("cache", cache.stats());
                data.put("rateLimitBuckets", rateLimiter.bucketCount());
                data.put("time", Instant.now().toString());
                return data;
            })
            .flatMap(EndpointSupport::ok);
    }

    private Mono<ServerResponse> diagnostics(ServerRequest request) {
        return settingsService.fetch()
            .flatMap(settings -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("plugin", pluginInfo());
                data.put("settings", settings.safeSnapshot());
                data.put("cache", cache.stats());
                data.put("rateLimitBuckets", rateLimiter.bucketCount());
                data.put("shortcodeCount", shortcodeService.definitions().size());
                return apiClient.get("/types", Map.of())
                    .map(response -> {
                        data.put("apiReachable", true);
                        data.put("apiSample", response);
                        return data;
                    })
                    .onErrorResume(error -> {
                        data.put("apiReachable", false);
                        data.put("apiError", error.getMessage());
                        return Mono.just(data);
                    });
            })
            .flatMap(EndpointSupport::ok);
    }

    private Mono<ServerResponse> test(ServerRequest request) {
        return apiClient.get("/types", Map.of())
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> preview(ServerRequest request) {
        String shortcode = request.queryParam("shortcode").orElse("[ddys_latest limit=\"6\"]");
        return shortcodeService.renderRawShortcode(shortcode)
            .map(html -> Map.of("success", true, "html", html))
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> request(ServerRequest request) {
        Map<String, Object> params = EndpointSupport.query(request);
        Object rawPath = params.remove("path");
        String path = rawPath == null ? "/latest" : rawPath.toString();
        if (!PREVIEW_ENDPOINTS.contains(path)) {
            return EndpointSupport.error(new IllegalArgumentException("Unsupported preview path."));
        }
        return apiClient.get(path, params)
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> me(ServerRequest request) {
        return apiClient.getAuthenticated("/me", Map.of())
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> createComment(ServerRequest request) {
        return request.bodyToMono(MAP_TYPE)
            .defaultIfEmpty(Map.of())
            .map(this::cleanComment)
            .flatMap(body -> apiClient.post("/comments", body))
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> deleteComment(ServerRequest request) {
        return apiClient.delete("/comments/" + DdysApiClient.safeSegment(request.pathVariable("id")))
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> report(ServerRequest request) {
        return request.bodyToMono(MAP_TYPE)
            .defaultIfEmpty(Map.of())
            .map(body -> allowBody(body, "target_type", "target_id", "reason", "message"))
            .flatMap(body -> apiClient.post("/report", body))
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> follow(ServerRequest request) {
        return request.bodyToMono(MAP_TYPE)
            .defaultIfEmpty(Map.of())
            .map(body -> allowBody(body, "slug", "movie_slug", "type", "action"))
            .flatMap(body -> apiClient.post("/follow", body))
            .flatMap(EndpointSupport::ok)
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> flush(ServerRequest request) {
        int removed = cache.clear();
        return EndpointSupport.ok(Map.of("success", true, "removed", removed, "cache", cache.stats()));
    }

    private Mono<ServerResponse> prune(ServerRequest request) {
        int removed = cache.pruneExpired();
        return EndpointSupport.ok(Map.of("success", true, "removed", removed, "cache", cache.stats()));
    }

    private Map<String, Object> pluginInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", pluginContext.getName());
        info.put("version", pluginContext.getVersion());
        return info;
    }

    private Map<String, Object> cleanComment(Map<String, Object> raw) {
        Map<String, Object> body = allowBody(raw, "slug", "movie_slug", "content", "parent_id");
        Object content = body.get("content");
        if (content == null || content.toString().isBlank()) {
            throw new IllegalArgumentException("Comment content is required.");
        }
        return body;
    }

    private Map<String, Object> allowBody(Map<String, Object> raw, String... keys) {
        Map<String, Object> body = new HashMap<>();
        for (String key : keys) {
            Object value = raw.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof String string) {
                String trimmed = string.trim();
                if (!trimmed.isBlank()) {
                    body.put(key, trimmed.length() > 1000 ? trimmed.substring(0, 1000) : trimmed);
                }
            } else {
                body.put(key, value);
            }
        }
        return body;
    }
}
