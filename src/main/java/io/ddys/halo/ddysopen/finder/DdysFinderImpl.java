package io.ddys.halo.ddysopen.finder;

import io.ddys.halo.ddysopen.api.DdysApiClient;
import io.ddys.halo.ddysopen.config.DdysSettingsService;
import io.ddys.halo.ddysopen.render.DataViews;
import java.time.Duration;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import run.halo.app.theme.finders.Finder;

@Finder("ddysFinder")
@Component
public class DdysFinderImpl implements DdysFinder {

    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

    private final DdysApiClient apiClient;
    private final DdysSettingsService settingsService;

    public DdysFinderImpl(DdysApiClient apiClient, DdysSettingsService settingsService) {
        this.apiClient = apiClient;
        this.settingsService = settingsService;
    }

    @Override
    public List<Map<String, Object>> movies(String type, String genre, String region, Integer page, Integer perPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        put(params, "type", type);
        put(params, "genre", genre);
        put(params, "region", region);
        params.put("page", page == null ? 1 : Math.max(1, page));
        params.put("per_page", clamp(perPage, 12));
        return cards("/movies", params);
    }

    @Override
    public List<Map<String, Object>> latest(Integer limit) {
        return cards("/latest", Map.of("limit", clamp(limit, 12)));
    }

    @Override
    public List<Map<String, Object>> hot(Integer limit) {
        return cards("/hot", Map.of("limit", clamp(limit, 12)));
    }

    @Override
    public List<Map<String, Object>> search(String keyword, Integer limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return cards("/search", Map.of("q", keyword, "limit", clamp(limit, 12)));
    }

    @Override
    public List<Map<String, Object>> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return rawList("/suggest", Map.of("q", keyword));
    }

    @Override
    public List<Map<String, Object>> calendar(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("year", year == null ? now.getYear() : year);
        params.put("month", month == null ? now.getMonthValue() : month);
        return cards("/calendar", params);
    }

    @Override
    public List<Map<String, Object>> collections(Integer limit) {
        return cards("/collections", Map.of("per_page", clamp(limit, 12)));
    }

    @Override
    public Map<String, Object> collection(String slug) {
        return object("/collections/" + DdysApiClient.safeSegment(slug), Map.of());
    }

    @Override
    public Map<String, Object> movie(String slug) {
        return object("/movies/" + DdysApiClient.safeSegment(slug), Map.of());
    }

    @Override
    public List<Map<String, Object>> sources(String slug) {
        return rawList("/movies/" + DdysApiClient.safeSegment(slug) + "/sources", Map.of());
    }

    @Override
    public List<Map<String, Object>> related(String slug) {
        return cards("/movies/" + DdysApiClient.safeSegment(slug) + "/related", Map.of());
    }

    @Override
    public List<Map<String, Object>> comments(String slug, Integer page, Integer perPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", page == null ? 1 : Math.max(1, page));
        params.put("per_page", clamp(perPage, 10));
        return rawList("/movies/" + DdysApiClient.safeSegment(slug) + "/comments", params);
    }

    @Override
    public List<Map<String, Object>> shares(Integer page, Integer perPage) {
        return cards("/shares", pageParams(page, perPage, 10));
    }

    @Override
    public Map<String, Object> share(String id) {
        return object("/shares/" + DdysApiClient.safeSegment(id), Map.of());
    }

    @Override
    public List<Map<String, Object>> requests(Integer page, Integer perPage) {
        return cards("/requests", pageParams(page, perPage, 10));
    }

    @Override
    public List<Map<String, Object>> activities(String type, Integer page, Integer perPage) {
        Map<String, Object> params = pageParams(page, perPage, 10);
        put(params, "type", type);
        return cards("/activities", params);
    }

    @Override
    public Map<String, Object> user(String username) {
        return object("/user/" + DdysApiClient.safeSegment(username), Map.of());
    }

    @Override
    public List<Map<String, Object>> types() {
        return rawList("/types", Map.of());
    }

    @Override
    public List<Map<String, Object>> genres() {
        return rawList("/genres", Map.of());
    }

    @Override
    public List<Map<String, Object>> regions() {
        return rawList("/regions", Map.of());
    }

    private List<Map<String, Object>> cards(String path, Map<String, Object> params) {
        return settingsService.fetch()
            .flatMap(settings -> apiClient.get(path, params)
                .map(response -> DataViews.cards(response, settings.siteBaseUrl())))
            .onErrorReturn(List.of())
            .block(BLOCK_TIMEOUT);
    }

    private List<Map<String, Object>> rawList(String path, Map<String, Object> params) {
        return apiClient.get(path, params)
            .map(DataViews::dataList)
            .onErrorReturn(List.of())
            .block(BLOCK_TIMEOUT);
    }

    private Map<String, Object> object(String path, Map<String, Object> params) {
        return apiClient.get(path, params)
            .map(DataViews::dataObject)
            .onErrorReturn(Map.of())
            .block(BLOCK_TIMEOUT);
    }

    private int clamp(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(1, Math.min(60, value));
    }

    private Map<String, Object> pageParams(Integer page, Integer perPage, int fallbackSize) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("page", page == null ? 1 : Math.max(1, page));
        params.put("per_page", clamp(perPage, fallbackSize));
        return params;
    }

    private void put(Map<String, Object> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.put(key, value);
        }
    }
}
