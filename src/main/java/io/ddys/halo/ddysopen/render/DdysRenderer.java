package io.ddys.halo.ddysopen.render;

import io.ddys.halo.ddysopen.config.DdysSettings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DdysRenderer {

    public String renderMovieList(Map<String, Object> response, DdysSettings settings, String title, String layout) {
        List<Map<String, Object>> cards = DataViews.cards(response, settings.siteBaseUrl());
        String chosenLayout = layout == null || layout.isBlank() ? settings.defaultLayout() : layout;
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"ddys-open ddys-open--")
            .append(Html.escape(settings.theme()))
            .append(" ddys-open-layout-")
            .append(Html.escape(chosenLayout))
            .append("\">");
        html.append("<div class=\"ddys-open__header\"><h2>").append(Html.escape(title)).append("</h2></div>");
        if (cards.isEmpty()) {
            html.append("<p class=\"ddys-open__empty\">No items found.</p>");
        } else {
            html.append("<div class=\"ddys-open__grid\">");
            cards.forEach(card -> html.append(renderCard(card, settings)));
            html.append("</div>");
        }
        appendAttribution(html, settings);
        html.append("</section>");
        return html.toString();
    }

    public String renderCard(Map<String, Object> card, DdysSettings settings) {
        String target = settings.openInNewTab() ? " target=\"_blank\" rel=\"noopener noreferrer\"" : "";
        StringBuilder html = new StringBuilder();
        html.append("<article class=\"ddys-open-card\">");
        String poster = value(card, "poster");
        if (!poster.isBlank()) {
            html.append("<a class=\"ddys-open-card__poster\" href=\"")
                .append(Html.escape(value(card, "url")))
                .append("\"")
                .append(target)
                .append("><img loading=\"lazy\" src=\"")
                .append(Html.escape(poster))
                .append("\" alt=\"")
                .append(Html.escape(value(card, "title")))
                .append("\"></a>");
        }
        html.append("<div class=\"ddys-open-card__body\">");
        html.append("<h3><a href=\"").append(Html.escape(value(card, "url"))).append("\"").append(target)
            .append(">").append(Html.escape(value(card, "title"))).append("</a></h3>");
        html.append("<div class=\"ddys-open-card__meta\">")
            .append(joinMeta(card, "year", "type", "region", "rating"))
            .append("</div>");
        String summary = value(card, "summary");
        if (!summary.isBlank()) {
            html.append("<p>").append(Html.escape(summary)).append("</p>");
        }
        html.append("</div></article>");
        return html.toString();
    }

    public String renderDetail(Map<String, Object> response, DdysSettings settings) {
        Map<String, Object> detail = DataViews.detail(response, settings.siteBaseUrl());
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"ddys-open ddys-open-detail ddys-open--")
            .append(Html.escape(settings.theme()))
            .append("\">");
        html.append("<div class=\"ddys-open-detail__hero\">");
        String poster = value(detail, "poster");
        if (!poster.isBlank()) {
            html.append("<img class=\"ddys-open-detail__poster\" src=\"").append(Html.escape(poster))
                .append("\" alt=\"").append(Html.escape(value(detail, "title"))).append("\">");
        }
        html.append("<div class=\"ddys-open-detail__body\"><h2>")
            .append(Html.escape(value(detail, "title")))
            .append("</h2>");
        html.append("<div class=\"ddys-open-card__meta\">")
            .append(joinMeta(detail, "year", "type", "region", "genre", "rating"))
            .append("</div>");
        String summary = value(detail, "summary");
        if (!summary.isBlank()) {
            html.append("<p>").append(Html.escape(summary)).append("</p>");
        }
        html.append("</div></div>");
        appendAttribution(html, settings);
        html.append("</section>");
        return html.toString();
    }

    public String renderSimpleList(Map<String, Object> response, DdysSettings settings, String title) {
        List<Map<String, Object>> rows = DataViews.dataList(response);
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"ddys-open ddys-open-list\"><h2>")
            .append(Html.escape(title))
            .append("</h2><ul>");
        for (Map<String, Object> row : rows) {
            String label = DataViews.first(row, "title", "name", "quality", "download_type",
                "resource_type", "note", "description", "slug", "id", "content", "url");
            String url = DataViews.first(row, "url", "link", "permalink");
            html.append("<li>");
            if (url != null && !url.isBlank()) {
                String safeUrl = url.startsWith("/") ? settings.siteBaseUrl() + url : url;
                html.append("<a href=\"").append(Html.escape(Html.safeUrl(safeUrl, settings.siteBaseUrl()))).append("\">")
                    .append(Html.escape(label)).append("</a>");
            } else {
                html.append(Html.escape(label));
            }
            html.append("</li>");
        }
        html.append("</ul>");
        appendAttribution(html, settings);
        html.append("</section>");
        return html.toString();
    }

    public String renderSearchForm(String query) {
        return """
            <form class="ddys-open-search" action="/ddys/search" method="get">
              <input type="search" name="q" value="%s" placeholder="Search DDYS">
              <button type="submit">Search</button>
            </form>
            """.formatted(Html.escape(query == null ? "" : query));
    }

    public String renderRequestForm(DdysSettings settings) {
        if (!settings.requestFormEnabled() || !settings.authenticatedWritesAllowed()) {
            return "<p class=\"ddys-open__empty\">Request form is not enabled.</p>";
        }
        return """
            <form class="ddys-open-request-form" data-ddys-request-form>
              <label>Title <input name="title" required maxlength="120"></label>
              <label>Year <input name="year" type="number" min="1900" max="2100"></label>
              <label>Type <select name="type"><option value="movie">Movie</option><option value="tv">TV</option><option value="anime">Anime</option></select></label>
              <label>Douban ID <input name="douban_id" maxlength="40"></label>
              <label>Message <textarea name="message" maxlength="500"></textarea></label>
              <button type="submit">Submit</button>
              <p class="ddys-open-request-form__status" role="status"></p>
            </form>
            """;
    }

    public String renderError(Throwable throwable) {
        return "<div class=\"ddys-open ddys-open__error\">"
            + Html.escape(throwable.getMessage())
            + "</div>";
    }

    private void appendAttribution(StringBuilder html, DdysSettings settings) {
        if (settings.showAttribution()) {
            String target = settings.openInNewTab() ? " target=\"_blank\" rel=\"noopener noreferrer\"" : "";
            html.append("<p class=\"ddys-open__source\"><a href=\"")
                .append(Html.escape(settings.siteBaseUrl()))
                .append("\"")
                .append(target)
                .append(">DDYS</a></p>");
        }
    }

    private String joinMeta(Map<String, Object> card, String... keys) {
        StringBuilder html = new StringBuilder();
        for (String key : keys) {
            String value = value(card, key);
            if (!value.isBlank()) {
                html.append("<span>").append(Html.escape(value)).append("</span>");
            }
        }
        return html.toString();
    }

    private String value(Map<String, Object> card, String key) {
        Object value = card.get(key);
        return value == null ? "" : value.toString();
    }
}
