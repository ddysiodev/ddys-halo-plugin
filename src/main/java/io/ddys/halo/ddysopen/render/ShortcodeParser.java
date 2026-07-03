package io.ddys.halo.ddysopen.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ShortcodeParser {

    private static final Pattern SHORTCODE = Pattern.compile("\\[(ddys_[a-z_]+)([^\\]]*)\\]");
    private static final Pattern ATTR = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_-]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"']+))");

    public List<Shortcode> parse(String content) {
        List<Shortcode> items = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return items;
        }
        Matcher matcher = SHORTCODE.matcher(content);
        while (matcher.find()) {
            items.add(new Shortcode(
                matcher.group(0),
                matcher.group(1),
                attrs(matcher.group(2))
            ));
        }
        return items;
    }

    private Map<String, String> attrs(String raw) {
        Map<String, String> attrs = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return attrs;
        }
        Matcher matcher = ATTR.matcher(raw);
        while (matcher.find()) {
            String value = matcher.group(3) != null ? matcher.group(3)
                : matcher.group(4) != null ? matcher.group(4)
                : matcher.group(5);
            attrs.put(matcher.group(1), value == null ? "" : value);
        }
        return attrs;
    }
}

