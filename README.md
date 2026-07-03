# DDYS Halo Plugin

[English](README.md) | [简体中文](README.zh-CN.md)

Official Halo plugin for the [DDYS](https://ddys.io/) API.

The plugin lets Halo site owners embed DDYS content in posts, pages, templates, and built-in theme pages. It includes shortcodes, theme finders, cache controls, diagnostics, a Console dashboard, and configurable API base URLs for official API or Worker proxy deployments.

## Features

- Halo 2.23+ plugin structure with Java 21 and Gradle.
- Full DDYS public endpoint coverage.
- Shortcode rendering for posts and single pages.
- Theme finder named `ddysFinder`.
- Optional built-in pages for latest, hot, search, calendar, collections, and movie detail.
- In-memory cache with configurable TTL and flush controls.
- Console dashboard for connection testing, diagnostics, API preview, and shortcode generation.
- Public plugin API for theme and frontend integrations.
- Optional server-side authenticated actions for `/me`, comments, report, follow, and request creation.
- Optional request form, disabled by default.
- In-memory rate limiting for the public request form.
- Automatic frontend CSS injection.
- RBAC role templates for view, manage, and anonymous public access.

## Shortcodes

```text
[ddys_movies type="movie" per_page="24"]
[ddys_latest type="movie" limit="12" layout="grid"]
[ddys_hot limit="10" layout="list"]
[ddys_search q="interstellar"]
[ddys_suggest q="interstellar"]
[ddys_calendar year="2026" month="7"]
[ddys_movie slug="interstellar"]
[ddys_sources slug="interstellar"]
[ddys_related slug="interstellar"]
[ddys_comments slug="interstellar"]
[ddys_collections per_page="10"]
[ddys_collection slug="best-sci-fi" per_page="12"]
[ddys_shares per_page="10"]
[ddys_share id="1081"]
[ddys_requests per_page="10"]
[ddys_activities type="share" per_page="10"]
[ddys_user username="diduan"]
[ddys_types]
[ddys_genres]
[ddys_regions]
[ddys_request_form]
```

## Theme Finder

```html
<section th:with="latest=${ddysFinder.latest(12)}">
  <article th:each="movie : ${latest}">
    <a th:href="${movie.url}" th:text="${movie.title}"></a>
  </article>
</section>
```

Available finder methods:

```text
movies(type, genre, region, page, perPage)
latest(limit)
hot(limit)
search(keyword, limit)
suggest(keyword)
calendar(year, month)
collections(limit)
collection(slug)
movie(slug)
sources(slug)
related(slug)
comments(slug, page, perPage)
shares(page, perPage)
share(id)
requests(page, perPage)
activities(type, page, perPage)
user(username)
types()
genres()
regions()
```

## Plugin APIs

Public query API:

```text
/apis/api.ddys.io/v1alpha1/ddys/latest
/apis/api.ddys.io/v1alpha1/ddys/hot
/apis/api.ddys.io/v1alpha1/ddys/search?q=interstellar
/apis/api.ddys.io/v1alpha1/ddys/movies/{slug}
```

Console API:

```text
/apis/console.api.ddys.io/v1alpha1/ddys/status
/apis/console.api.ddys.io/v1alpha1/ddys/diagnostics
/apis/console.api.ddys.io/v1alpha1/ddys/preview
/apis/console.api.ddys.io/v1alpha1/ddys/cache/flush
```

## Built-in Pages

When enabled, the plugin registers:

- `/ddys`
- `/ddys/latest`
- `/ddys/hot`
- `/ddys/search`
- `/ddys/calendar`
- `/ddys/collections`
- `/ddys/movies/{slug}`

Themes may override templates by providing matching template names.

## Requirements

- Halo 2.23+
- Java 21
- Gradle 8+

## Build

```bash
gradle build
```

The built JAR will be available under `build/libs`.

## License

GPL-3.0-or-later
