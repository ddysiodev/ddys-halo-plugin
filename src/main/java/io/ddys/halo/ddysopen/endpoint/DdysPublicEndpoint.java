package io.ddys.halo.ddysopen.endpoint;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

@Component
public class DdysPublicEndpoint implements CustomEndpoint {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<>() {
        };

    private final DdysApiClient apiClient;
    private final DdysSettingsService settingsService;
    private final DdysRateLimiter rateLimiter;

    public DdysPublicEndpoint(
        DdysApiClient apiClient,
        DdysSettingsService settingsService,
        DdysRateLimiter rateLimiter
    ) {
        this.apiClient = apiClient;
        this.settingsService = settingsService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return route(GET("ddys/movies/{slug}/sources"), request ->
                proxyGet("/movies/" + safe(request, "slug") + "/sources", request))
            .andRoute(GET("ddys/movies/{slug}/related"), request ->
                proxyGet("/movies/" + safe(request, "slug") + "/related", request))
            .andRoute(GET("ddys/movies/{slug}/comments"), request ->
                proxyGet("/movies/" + safe(request, "slug") + "/comments", request))
            .andRoute(GET("ddys/movies/{slug}"), request ->
                proxyGet("/movies/" + safe(request, "slug"), request))
            .andRoute(GET("ddys/movies"), request -> proxyGet("/movies", request))
            .andRoute(GET("ddys/search"), request -> proxyGet("/search", request))
            .andRoute(GET("ddys/suggest"), request -> proxyGet("/suggest", request))
            .andRoute(GET("ddys/hot"), request -> proxyGet("/hot", request))
            .andRoute(GET("ddys/latest"), request -> proxyGet("/latest", request))
            .andRoute(GET("ddys/calendar"), request -> proxyGet("/calendar", request))
            .andRoute(GET("ddys/types"), request -> proxyGet("/types", request))
            .andRoute(GET("ddys/genres"), request -> proxyGet("/genres", request))
            .andRoute(GET("ddys/regions"), request -> proxyGet("/regions", request))
            .andRoute(GET("ddys/collections/{slug}"), request ->
                proxyGet("/collections/" + safe(request, "slug"), request))
            .andRoute(GET("ddys/collections"), request -> proxyGet("/collections", request))
            .andRoute(GET("ddys/shares/{id}"), request ->
                proxyGet("/shares/" + safe(request, "id"), request))
            .andRoute(GET("ddys/shares"), request -> proxyGet("/shares", request))
            .andRoute(GET("ddys/requests"), request -> proxyGet("/requests", request))
            .andRoute(POST("ddys/requests"), this::createRequest)
            .andRoute(GET("ddys/activities"), request -> proxyGet("/activities", request))
            .andRoute(GET("ddys/user/{username}"), request ->
                proxyGet("/user/" + safe(request, "username"), request));
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.ddys.io/v1alpha1");
    }

    private Mono<ServerResponse> proxyGet(String path, ServerRequest request) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.publicApiEnabled()) {
                    return ServerResponse.status(HttpStatus.NOT_FOUND).build();
                }
                return apiClient.get(path, EndpointSupport.query(request))
                    .flatMap(EndpointSupport::ok);
            })
            .onErrorResume(EndpointSupport::error);
    }

    private Mono<ServerResponse> createRequest(ServerRequest request) {
        return settingsService.fetch()
            .flatMap(settings -> {
                if (!settings.requestFormEnabled() || !settings.authenticatedWritesAllowed()) {
                    return ServerResponse.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("success", false, "message", "Request form is not enabled."));
                }
                if (!rateLimiter.allow(clientKey(request), 5, Duration.ofMinutes(1))) {
                    return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("success", false, "message", "Too many requests."));
                }
                return request.bodyToMono(MAP_TYPE)
                    .defaultIfEmpty(Map.of())
                    .map(this::cleanRequestPayload)
                    .flatMap(payload -> apiClient.post("/requests", payload))
                    .flatMap(EndpointSupport::ok);
            })
            .onErrorResume(EndpointSupport::error);
    }

    private Map<String, Object> cleanRequestPayload(Map<String, Object> raw) {
        Map<String, Object> body = new LinkedHashMap<>();
        String title = string(raw.get("title"), 120);
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title is required.");
        }
        body.put("title", title);
        String type = string(raw.get("type"), 20);
        body.put("type", switch (type) {
            case "series", "tv", "anime", "variety", "documentary", "show" -> type;
            default -> "movie";
        });
        String year = string(raw.get("year"), 4);
        if (!year.isBlank()) {
            body.put("year", year);
        }
        String doubanId = string(raw.get("douban_id"), 40);
        if (!doubanId.isBlank()) {
            body.put("douban_id", doubanId);
        }
        String message = string(raw.get("message"), 500);
        if (!message.isBlank()) {
            body.put("message", message);
        }
        return body;
    }

    private String safe(ServerRequest request, String name) {
        return DdysApiClient.safeSegment(request.pathVariable(name));
    }

    private String string(Object raw, int max) {
        if (raw == null) {
            return "";
        }
        String value = raw.toString().trim();
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String clientKey(ServerRequest request) {
        String forwarded = request.headers().firstHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.remoteAddress()
            .map(address -> address.getAddress() == null
                ? address.getHostString()
                : address.getAddress().getHostAddress())
            .orElse("anonymous");
    }
}
