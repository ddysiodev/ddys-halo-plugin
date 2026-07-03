package io.ddys.halo.ddysopen.render;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ShortcodeService {

    private final ShortcodeParser parser;
    private final DdysApiClient apiClient;
    private final DdysSettingsService settingsService;
    private final DdysRenderer renderer;

    public ShortcodeService(
        ShortcodeParser parser,
        DdysApiClient apiClient,
        DdysSettingsService settingsService,
        DdysRenderer renderer
    ) {
        this.parser = parser;
        this.apiClient = apiClient;
        this.settingsService = settingsService;
        this.renderer = renderer;
    }

    public Mono<String> renderContent(String content) {
        List<Shortcode> shortcodes = parser.parse(content);
        if (shortcodes.isEmpty()) {
            return Mono.just(content);
        }
        Mono<String> current = Mono.just(content);
        for (Shortcode shortcode : shortcodes) {
            current = current.flatMap(existing -> render(shortcode)
                .map(html -> existing.replace(shortcode.raw(), html)));
        }
        return current;
    }

    public Mono<String> renderRawShortcode(String raw) {
        List<Shortcode> shortcodes = parser.parse(raw);
        if (shortcodes.isEmpty()) {
            return Mono.just("");
        }
        return render(shortcodes.get(0));
    }

    public List<Map<String, Object>> definitions() {
        return List.of(
            def("ddys_movies", "Movie list", "[ddys_movies type=\"movie\" per_page=\"24\"]"),
            def("ddys_latest", "Latest movies", "[ddys_latest type=\"movie\" limit=\"12\" layout=\"grid\"]"),
            def("ddys_hot", "Hot movies", "[ddys_hot limit=\"10\" layout=\"list\"]"),
            def("ddys_search", "Search form or results", "[ddys_search q=\"interstellar\"]"),
            def("ddys_suggest", "Search suggestions", "[ddys_suggest q=\"interstellar\"]"),
            def("ddys_calendar", "Release calendar", "[ddys_calendar year=\"2026\" month=\"7\"]"),
            def("ddys_movie", "Movie detail", "[ddys_movie slug=\"interstellar\"]"),
            def("ddys_sources", "Movie sources", "[ddys_sources slug=\"interstellar\"]"),
            def("ddys_related", "Related movies", "[ddys_related slug=\"interstellar\"]"),
            def("ddys_comments", "Movie comments", "[ddys_comments slug=\"interstellar\"]"),
            def("ddys_collections", "Collections", "[ddys_collections per_page=\"10\"]"),
            def("ddys_collection", "Collection detail", "[ddys_collection slug=\"best-sci-fi\" per_page=\"12\"]"),
            def("ddys_shares", "Shares", "[ddys_shares per_page=\"10\"]"),
            def("ddys_share", "Share detail", "[ddys_share id=\"1081\"]"),
            def("ddys_requests", "Requests", "[ddys_requests per_page=\"10\"]"),
            def("ddys_activities", "Activities", "[ddys_activities type=\"share\" per_page=\"10\"]"),
            def("ddys_user", "User profile", "[ddys_user username=\"diduan\"]"),
            def("ddys_types", "Types", "[ddys_types]"),
            def("ddys_genres", "Genres", "[ddys_genres]"),
            def("ddys_regions", "Regions", "[ddys_regions]"),
            def("ddys_request_form", "Request form", "[ddys_request_form]")
        );
    }

    private Mono<String> render(Shortcode shortcode) {
        return settingsService.fetch().flatMap(settings -> switch (shortcode.name()) {
            case "ddys_movies" -> apiClient.get("/movies", params(shortcode, "type", "genre", "region", "page", "per_page"))
                .map(response -> renderer.renderMovieList(response, settings, "Movies", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_latest" -> apiClient.get("/latest", withLimit(shortcode, "type"))
                .map(response -> renderer.renderMovieList(response, settings, "Latest", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_hot" -> apiClient.get("/hot", withLimit(shortcode))
                .map(response -> renderer.renderMovieList(response, settings, "Hot", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_search" -> {
                String query = shortcode.attr("q", "");
                if (query.isBlank()) {
                    yield Mono.just(renderer.renderSearchForm(""));
                }
                yield apiClient.get("/search", Map.of("q", query, "limit", shortcode.attr("limit", "12")))
                    .map(response -> renderer.renderSearchForm(query)
                        + renderer.renderMovieList(response, settings, "Search", shortcode.attr("layout", settings.defaultLayout())));
            }
            case "ddys_suggest" -> apiClient.get("/suggest", Map.of("q", shortcode.attr("q", "")))
                .map(response -> renderer.renderSimpleList(response, settings, "Suggestions"));
            case "ddys_calendar" -> apiClient.get("/calendar", params(shortcode, "year", "month"))
                .map(response -> renderer.renderMovieList(response, settings, "Calendar", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_movie" -> apiClient.get("/movies/" + DdysApiClient.safeSegment(shortcode.attr("slug", "")), Map.of())
                .map(response -> renderer.renderDetail(response, settings));
            case "ddys_sources" -> apiClient.get("/movies/" + DdysApiClient.safeSegment(shortcode.attr("slug", "")) + "/sources", Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "Sources"));
            case "ddys_related" -> apiClient.get("/movies/" + DdysApiClient.safeSegment(shortcode.attr("slug", "")) + "/related", Map.of())
                .map(response -> renderer.renderMovieList(response, settings, "Related", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_comments" -> apiClient.get("/movies/" + DdysApiClient.safeSegment(shortcode.attr("slug", "")) + "/comments", params(shortcode, "page", "per_page"))
                .map(response -> renderer.renderSimpleList(response, settings, "Comments"));
            case "ddys_collections" -> apiClient.get("/collections", params(shortcode, "page", "per_page"))
                .map(response -> renderer.renderMovieList(response, settings, "Collections", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_collection" -> apiClient.get("/collections/" + DdysApiClient.safeSegment(shortcode.attr("slug", "")), params(shortcode, "page", "per_page"))
                .map(response -> renderer.renderMovieList(response, settings, "Collection", shortcode.attr("layout", settings.defaultLayout())));
            case "ddys_shares" -> apiClient.get("/shares", params(shortcode, "page", "per_page"))
                .map(response -> renderer.renderSimpleList(response, settings, "Shares"));
            case "ddys_share" -> apiClient.get("/shares/" + DdysApiClient.safeSegment(shortcode.attr("id", "")), Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "Share"));
            case "ddys_requests" -> apiClient.get("/requests", params(shortcode, "page", "per_page"))
                .map(response -> renderer.renderSimpleList(response, settings, "Requests"));
            case "ddys_activities" -> apiClient.get("/activities", params(shortcode, "type", "page", "per_page"))
                .map(response -> renderer.renderSimpleList(response, settings, "Activities"));
            case "ddys_user" -> apiClient.get("/user/" + DdysApiClient.safeSegment(shortcode.attr("username", "")), Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "User"));
            case "ddys_types" -> apiClient.get("/types", Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "Types"));
            case "ddys_genres" -> apiClient.get("/genres", Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "Genres"));
            case "ddys_regions" -> apiClient.get("/regions", Map.of())
                .map(response -> renderer.renderSimpleList(response, settings, "Regions"));
            case "ddys_request_form" -> Mono.just(renderer.renderRequestForm(settings));
            default -> Mono.just("");
        }).onErrorResume(error -> Mono.just(renderer.renderError(error)));
    }

    private Map<String, Object> withLimit(Shortcode shortcode, String... extra) {
        Map<String, Object> values = params(shortcode, extra);
        values.put("limit", shortcode.attr("limit", shortcode.attr("per_page", "12")));
        return values;
    }

    private Map<String, Object> params(Shortcode shortcode, String... names) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String name : names) {
            String value = shortcode.attributes().get(name);
            if (value != null && !value.isBlank()) {
                values.put(name, value);
            }
        }
        return values;
    }

    private Map<String, Object> def(String name, String description, String example) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("description", description);
        value.put("example", example);
        return value;
    }
}

