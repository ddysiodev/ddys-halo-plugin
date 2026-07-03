import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8');
}

test('plugin manifest is wired to Halo settings and repository metadata', () => {
  const manifest = read('src/main/resources/plugin.yaml');
  assert.match(manifest, /name:\s*PluginDdysOpen/);
  assert.match(manifest, /configMapName:\s*plugin-ddys-open-configmap/);
  assert.match(manifest, /settingName:\s*plugin-ddys-open-settings/);
  assert.match(manifest, /repo:\s*https:\/\/github\.com\/ddysiodev\/ddys-halo-plugin/);
});

test('all planned shortcodes are registered', () => {
  const source = read('src/main/java/io/ddys/halo/ddysopen/render/ShortcodeService.java');
  const matches = [...source.matchAll(/def\("ddys_[a-z_]+"/g)].map((match) => match[0]);
  assert.equal(matches.length, 21);
  assert.match(source, /case "ddys_request_form"/);
});

test('content handlers are declared as extension definitions', () => {
  const extensions = read('src/main/resources/extensions/extensions.yaml');
  assert.match(extensions, /reactive-post-content-handler/);
  assert.match(extensions, /reactive-singlepage-content-handler/);
  assert.match(extensions, /DdysPostContentHandler/);
  assert.match(extensions, /DdysSinglePageContentHandler/);
});

test('public endpoint exposes DDYS query routes and guarded request creation', () => {
  const endpoint = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysPublicEndpoint.java');
  for (const route of [
    'ddys/movies',
    'ddys/search',
    'ddys/suggest',
    'ddys/hot',
    'ddys/latest',
    'ddys/calendar',
    'ddys/types',
    'ddys/genres',
    'ddys/regions',
    'ddys/collections',
    'ddys/shares',
    'ddys/requests',
    'ddys/activities',
    'ddys/user/{username}',
  ]) {
    assert.ok(endpoint.includes(route), `missing route ${route}`);
  }
  assert.match(endpoint, /requestFormEnabled/);
  assert.match(endpoint, /authenticatedWritesAllowed/);
});

test('console endpoint exposes server-side authenticated actions only in admin API', () => {
  const endpoint = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysConsoleEndpoint.java');
  for (const route of [
    'ddys/me',
    'ddys/comments',
    'ddys/comments/{id}',
    'ddys/report',
    'ddys/follow',
  ]) {
    assert.ok(endpoint.includes(route), `missing console route ${route}`);
  }
  assert.match(endpoint, /getAuthenticated/);
  assert.match(endpoint, /apiClient\.post/);
  assert.match(endpoint, /apiClient\.delete/);
});

test('permission template keeps anonymous create scope limited to requests', () => {
  const role = read('src/main/resources/extensions/roleTemplate.yaml');
  assert.match(role, /resources:\s*\[ "ddys", "ddys\/\*" \]\s*\n\s*verbs:\s*\[ "get", "list" \]/);
  assert.match(role, /resources:\s*\[ "ddys\/requests" \]\s*\n\s*verbs:\s*\[ "create" \]/);
});

test('built-in pages disabled state is rendered as 404 not generic upstream failure', () => {
  const router = read('src/main/java/io/ddys/halo/ddysopen/route/DdysRouter.java');
  assert.match(router, /ResponseStatusException\(HttpStatus\.NOT_FOUND/);
  assert.match(router, /ServerResponse\.status\(status\(error\)\)/);
});

test('request payload supports DDYS public type codes', () => {
  const endpoint = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysPublicEndpoint.java');
  for (const type of ['series', 'movie', 'anime', 'variety']) {
    assert.ok(endpoint.includes(type), `missing request type ${type}`);
  }
});

test('public request form has an in-memory rate limiter', () => {
  const endpoint = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysPublicEndpoint.java');
  const limiter = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysRateLimiter.java');
  assert.match(endpoint, /rateLimiter\.allow/);
  assert.match(endpoint, /HttpStatus\.TOO_MANY_REQUESTS/);
  assert.match(endpoint, /X-Forwarded-For/);
  assert.match(limiter, /ConcurrentHashMap/);
  assert.match(limiter, /Duration/);
});

test('renderer prefixes relative DDYS links in simple lists', () => {
  const renderer = read('src/main/java/io/ddys/halo/ddysopen/render/DdysRenderer.java');
  assert.match(renderer, /url\.startsWith\("\/"\) \? settings\.siteBaseUrl\(\) \+ url : url/);
});

test('theme finder exposes all public read capabilities', () => {
  const finder = read('src/main/java/io/ddys/halo/ddysopen/finder/DdysFinder.java');
  for (const method of [
    'movies(',
    'latest(',
    'hot(',
    'search(',
    'suggest(',
    'calendar(',
    'collections(',
    'collection(',
    'movie(',
    'sources(',
    'related(',
    'comments(',
    'shares(',
    'share(',
    'requests(',
    'activities(',
    'user(',
    'types(',
    'genres(',
    'regions(',
  ]) {
    assert.ok(finder.includes(method), `missing finder method ${method}`);
  }
});

test('console UI includes shortcode generator and API preview tools', () => {
  const ui = read('ui/src/views/HomeView.vue');
  assert.match(ui, /loadShortcodes/);
  assert.match(ui, /copyText/);
  assert.match(ui, /document\.execCommand\('copy'\)/);
  assert.match(ui, /API Preview/);
  assert.match(ui, /previewApi/);
  assert.match(ui, /replace\(\/\^\\\?\/, ''\)/);
});

test('api client handles zero retry configuration explicitly', () => {
  const client = read('src/main/java/io/ddys/halo/ddysopen/api/DdysApiClient.java');
  assert.match(client, /retryCount\(\) <= 0/);
  assert.match(client, /Retry\.backoff/);
});

test('readmes use correct official website link text', () => {
  assert.match(read('README.md'), /\[DDYS\]\(https:\/\/ddys\.io\/\)/);
  assert.match(read('README.zh-CN.md'), /\[低端影视\]\(https:\/\/ddys\.io\/\)/);
});
