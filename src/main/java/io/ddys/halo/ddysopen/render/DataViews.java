package io.ddys.halo.ddysopen.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DataViews {

    private DataViews() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> dataObject(Map<String, Object> response) {
        Object data = response == null ? null : response.get("data");
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return response == null ? Map.of() : response;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> dataList(Map<String, Object> response) {
        Object data = response == null ? null : response.get("data");
        if (data instanceof List<?> list) {
            return asList(list);
        }
        if (data instanceof Map<?, ?> map) {
            List<Map<String, Object>> sourceItems = sourceList(map);
            if (!sourceItems.isEmpty()) {
                return sourceItems;
            }
            for (String key : List.of("items", "results", "list", "movies", "rows", "data")) {
                Object nested = map.get(key);
                if (nested instanceof List<?> nestedList) {
                    return asList(nestedList);
                }
            }
            return List.of((Map<String, Object>) map);
        }
        if (response != null) {
            for (String key : List.of("items", "results", "list", "movies", "rows")) {
                Object nested = response.get(key);
                if (nested instanceof List<?> nestedList) {
                    return asList(nestedList);
                }
            }
        }
        return List.of();
    }

    public static List<Map<String, Object>> cards(Map<String, Object> response, String siteBaseUrl) {
        return dataList(response).stream()
            .map(item -> card(item, siteBaseUrl))
            .toList();
    }

    public static Map<String, Object> card(Map<String, Object> item, String siteBaseUrl) {
        Map<String, Object> card = new LinkedHashMap<>();
        String title = first(item, "title", "name", "movie_title", "original_title", "slug");
        String slug = first(item, "slug", "id");
        String url = first(item, "url", "link", "permalink");
        if ((url == null || url.isBlank()) && slug != null && !slug.isBlank()) {
            url = trim(siteBaseUrl) + "/" + slug;
        }
        url = absoluteSiteUrl(url, siteBaseUrl);
        card.put("title", title == null || title.isBlank() ? "Untitled" : title);
        card.put("slug", slug == null ? "" : slug);
        card.put("url", Html.safeUrl(url, trim(siteBaseUrl)));
        card.put("poster", Html.safeUrl(first(item, "poster", "cover", "image", "thumbnail", "poster_url"), ""));
        card.put("year", first(item, "year", "release_year"));
        card.put("type", first(item, "type", "category"));
        card.put("region", first(item, "region", "country"));
        card.put("genre", first(item, "genre", "genres"));
        card.put("rating", first(item, "rating", "score", "douban_rating"));
        card.put("summary", first(item, "summary", "description", "intro", "overview"));
        card.put("raw", item);
        return card;
    }

    public static Map<String, Object> detail(Map<String, Object> response, String siteBaseUrl) {
        Map<String, Object> data = dataObject(response);
        if (data.containsKey("movie") && data.get("movie") instanceof Map<?, ?> movie) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) movie;
            data = typed;
        }
        Map<String, Object> card = card(data, siteBaseUrl);
        card.put("summary", first(data, "summary", "description", "intro", "overview", "content"));
        card.put("directors", first(data, "directors", "director"));
        card.put("actors", first(data, "actors", "cast"));
        card.put("language", first(data, "language"));
        card.put("episodes", first(data, "episodes", "episode"));
        card.put("raw", data);
        return card;
    }

    private static List<Map<String, Object>> asList(List<?> list) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                items.add(typed);
            }
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sourceList(Map<?, ?> map) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (String group : List.of("online", "download")) {
            Object value = map.get(group);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        Map<String, Object> typed = new LinkedHashMap<>((Map<String, Object>) itemMap);
                        typed.putIfAbsent("source_group", group);
                        items.add(typed);
                    }
                }
            }
        }
        return items;
    }

    public static String first(Map<String, Object> item, String... keys) {
        if (item == null) {
            return "";
        }
        for (String key : keys) {
            Object value = item.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Iterable<?> values) {
                List<String> parts = new ArrayList<>();
                values.forEach(part -> {
                    if (part != null && !part.toString().isBlank()) {
                        parts.add(part.toString());
                    }
                });
                if (!parts.isEmpty()) {
                    return String.join(", ", parts);
                }
            } else if (!value.toString().isBlank()) {
                return value.toString();
            }
        }
        return "";
    }

    private static String trim(String raw) {
        if (raw == null || raw.isBlank()) {
            return "https://ddys.io";
        }
        String value = raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String absoluteSiteUrl(String raw, String siteBaseUrl) {
        if (raw == null || raw.isBlank()) {
            return trim(siteBaseUrl);
        }
        if (raw.startsWith("/")) {
            return trim(siteBaseUrl) + raw;
        }
        return raw;
    }
}
