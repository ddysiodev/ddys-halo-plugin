package io.ddys.halo.ddysopen.route;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.config.DdysSettings;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import io.ddys.halo.ddysopen.render.DataViews;
import io.ddys.halo.ddysopen.render.DdysRenderer;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
public class DdysRouter {

    private final DdysSettingsService settingsService;
    private final DdysApiClient apiClient;
    private final DdysRenderer renderer;

    public DdysRouter(DdysSettingsService settingsService, DdysApiClient apiClient, DdysRenderer renderer) {
        this.settingsService = settingsService;
        this.apiClient = apiClient;
        this.renderer = renderer;
    }

    @Bean
    RouterFunction<ServerResponse> ddysThemeRoutes() {
        return route(GET("/ddys"), list("/latest", "Latest", "ddys-list"))
            .andRoute(GET("/ddys/latest"), list("/latest", "Latest", "ddys-list"))
            .andRoute(GET("/ddys/hot"), list("/hot", "Hot", "ddys-list"))
            .andRoute(GET("/ddys/search"), search())
            .andRoute(GET("/ddys/calendar"), calendar())
            .andRoute(GET("/ddys/collections"), list("/collections", "Collections", "ddys-list"))
            .andRoute(GET("/ddys/movies/{slug}"), detail());
    }

    private HandlerFunction<ServerResponse> list(String endpoint, String title, String template) {
        return request -> settingsService.fetch()
            .flatMap(settings -> ensurePages(settings)
                .then(apiClient.get(endpoint, listQuery(request, settings)))
                .flatMap(response -> ServerResponse.ok().render(template, model(settings, title, response))))
            .onErrorResume(error -> ServerResponse.status(status(error))
                .render("ddys-error", Map.of("message", message(error))));
    }

    private HandlerFunction<ServerResponse> search() {
        return request -> settingsService.fetch()
            .flatMap(settings -> ensurePages(settings)
                .then(Mono.defer(() -> {
                    String keyword = query(request, "q", "");
                    if (keyword.isBlank()) {
                        return ServerResponse.ok().render("ddys-search", Map.of(
                            "title", "Search",
                            "query", "",
                            "items", java.util.List.of(),
                            "searchForm", renderer.renderSearchForm("")
                        ));
                    }
                    return apiClient.get("/search", Map.of("q", keyword, "limit", query(request, "limit", settings.pageSize())))
                        .flatMap(response -> ServerResponse.ok().render("ddys-search",
                            model(settings, "Search", response, Map.of("query", keyword,
                                "searchForm", renderer.renderSearchForm(keyword)))));
                })))
            .onErrorResume(error -> ServerResponse.status(status(error))
                .render("ddys-error", Map.of("message", message(error))));
    }

    private HandlerFunction<ServerResponse> calendar() {
        return request -> settingsService.fetch()
            .flatMap(settings -> ensurePages(settings)
                .then(Mono.defer(() -> {
                    YearMonth now = YearMonth.now();
                    Map<String, Object> params = new LinkedHashMap<>();
                    params.put("year", query(request, "year", now.getYear()));
                    params.put("month", query(request, "month", now.getMonthValue()));
                    return apiClient.get("/calendar", params)
                        .flatMap(response -> ServerResponse.ok().render("ddys-list",
                            model(settings, "Calendar", response, params)));
                })))
            .onErrorResume(error -> ServerResponse.status(status(error))
                .render("ddys-error", Map.of("message", message(error))));
    }

    private HandlerFunction<ServerResponse> detail() {
        return request -> settingsService.fetch()
            .flatMap(settings -> ensurePages(settings)
                .then(apiClient.get("/movies/" + DdysApiClient.safeSegment(request.pathVariable("slug")), Map.of()))
                .flatMap(response -> ServerResponse.ok().render("ddys-detail",
                    Map.of(
                        "title", DataViews.detail(response, settings.siteBaseUrl()).get("title"),
                        "movie", DataViews.detail(response, settings.siteBaseUrl()),
                        "detailHtml", renderer.renderDetail(response, settings),
                        "settings", settings.safeSnapshot()
                    ))))
            .onErrorResume(error -> ServerResponse.status(status(error))
                .render("ddys-error", Map.of("message", message(error))));
    }

    private Mono<Void> ensurePages(DdysSettings settings) {
        if (!settings.pagesEnabled()) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Built-in DDYS pages are disabled."));
        }
        return Mono.empty();
    }

    private Map<String, Object> listQuery(ServerRequest request, DdysSettings settings) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (String key : java.util.List.of("type", "genre", "region", "page", "per_page", "limit")) {
            request.queryParam(key).filter(value -> !value.isBlank()).ifPresent(value -> params.put(key, value));
        }
        params.putIfAbsent("limit", settings.pageSize());
        return params;
    }

    private Map<String, Object> model(DdysSettings settings, String title, Map<String, Object> response) {
        return model(settings, title, response, Map.of());
    }

    private Map<String, Object> model(
        DdysSettings settings,
        String title,
        Map<String, Object> response,
        Map<String, Object> extra
    ) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", title);
        model.put("items", DataViews.cards(response, settings.siteBaseUrl()));
        model.put("raw", response);
        model.put("settings", settings.safeSnapshot());
        model.put("listHtml", renderer.renderMovieList(response, settings, title, settings.defaultLayout()));
        model.putAll(extra);
        return model;
    }

    private String query(ServerRequest request, String key, String fallback) {
        return request.queryParam(key).filter(value -> !value.isBlank()).orElse(fallback);
    }

    private Object query(ServerRequest request, String key, Object fallback) {
        return request.queryParam(key).filter(value -> !value.isBlank()).<Object>map(value -> value).orElse(fallback);
    }

    private String message(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
            ? "DDYS request failed."
            : error.getMessage();
    }

    private HttpStatus status(Throwable error) {
        if (error instanceof ResponseStatusException exception) {
            HttpStatus resolved = HttpStatus.resolve(exception.getStatusCode().value());
            return resolved == null ? HttpStatus.BAD_GATEWAY : resolved;
        }
        return HttpStatus.BAD_GATEWAY;
    }
}
