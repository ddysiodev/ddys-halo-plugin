package io.ddys.halo.ddysopen.render;

import java.util.Map;

public record Shortcode(String raw, String name, Map<String, String> attributes) {
    public String attr(String key, String fallback) {
        String value = attributes.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}

