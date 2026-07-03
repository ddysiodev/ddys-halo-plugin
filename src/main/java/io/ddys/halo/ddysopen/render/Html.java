package io.ddys.halo.ddysopen.render;

import java.net.URI;
import java.util.Locale;

public final class Html {

    private Html() {
    }

    public static String escape(Object raw) {
        if (raw == null) {
            return "";
        }
        return raw.toString()
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    public static String attr(String name, Object value) {
        String raw = value == null ? "" : value.toString();
        if (raw.isBlank()) {
            return "";
        }
        return " " + name + "=\"" + escape(raw) + "\"";
    }

    public static String safeUrl(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String value = raw.trim();
        if (value.startsWith("//")) {
            return fallback;
        }
        if (value.startsWith("/") || value.startsWith("#")) {
            return value;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ("http".equals(scheme) || "https".equals(scheme) || "magnet".equals(scheme)
                || "ed2k".equals(scheme) || "thunder".equals(scheme)) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
        return fallback;
    }
}

