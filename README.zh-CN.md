# DDYS Halo 插件

[English](README.md) | [简体中文](README.zh-CN.md)

[低端影视](https://ddys.io/) Open API 的官方 Halo 插件。

站长安装后，可以在 Halo 文章、页面、主题模板和插件内置页面里直接展示 DDYS 内容。插件包含短代码、主题 Finder、缓存管理、诊断面板、后台 Console 页面，以及可配置 API Base URL，可使用官方 API 或自建 Worker Proxy。

## 功能

- 使用 Halo 2.23+ 插件结构，Java 21 + Gradle。
- 覆盖 DDYS 全部公开展示接口。
- 文章和页面短代码自动解析。
- 提供主题 Finder：`ddysFinder`。
- 可选内置页面：最新、热门、搜索、日历、片单、影片详情。
- 内存缓存，支持 TTL、上限和后台清理。
- Console 后台：连接测试、诊断、API 预览、短代码生成器、缓存状态。
- 插件公开查询 API，方便主题或前端调用。
- 可选服务端认证能力：`/me`、评论、举报、关注、求片创建。
- 可选求片表单，默认关闭。
- 公开求片表单内置内存限流。
- 自动注入前台 CSS。
- RBAC 权限模板：查看、管理、匿名公开访问。

## 短代码

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

## 主题调用

```html
<section th:with="latest=${ddysFinder.latest(12)}">
  <article th:each="movie : ${latest}">
    <a th:href="${movie.url}" th:text="${movie.title}"></a>
  </article>
</section>
```

可用 Finder 方法：

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

## 插件 API

公开查询 API：

```text
/apis/api.ddys.io/v1alpha1/ddys/latest
/apis/api.ddys.io/v1alpha1/ddys/hot
/apis/api.ddys.io/v1alpha1/ddys/search?q=interstellar
/apis/api.ddys.io/v1alpha1/ddys/movies/{slug}
```

后台管理 API：

```text
/apis/console.api.ddys.io/v1alpha1/ddys/status
/apis/console.api.ddys.io/v1alpha1/ddys/diagnostics
/apis/console.api.ddys.io/v1alpha1/ddys/preview
/apis/console.api.ddys.io/v1alpha1/ddys/cache/flush
```

## 内置页面

开启后插件注册：

- `/ddys`
- `/ddys/latest`
- `/ddys/hot`
- `/ddys/search`
- `/ddys/calendar`
- `/ddys/collections`
- `/ddys/movies/{slug}`

主题可以提供同名模板覆盖插件默认模板。

## 环境要求

- Halo 2.23+
- Java 21
- Gradle 8+

## 构建

```bash
gradle build
```

构建后的 JAR 位于 `build/libs`。

## License

GPL-3.0-or-later
