import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const required = [
  'build.gradle',
  'settings.gradle',
  'src/main/resources/plugin.yaml',
  'src/main/resources/extensions/settings.yaml',
  'src/main/resources/extensions/roleTemplate.yaml',
  'src/main/resources/extensions/reverseProxy.yaml',
  'src/main/resources/extensions/extensions.yaml',
  'src/main/resources/logo.png',
  'src/main/resources/static/frontend.css',
  'src/main/resources/static/frontend.js',
  'src/main/resources/templates/ddys-list.html',
  'src/main/resources/templates/ddys-detail.html',
  'src/main/resources/templates/ddys-search.html',
  'src/main/java/io/ddys/halo/ddysopen/DdysOpenPlugin.java',
  'src/main/java/io/ddys/halo/ddysopen/api/DdysApiClient.java',
  'src/main/java/io/ddys/halo/ddysopen/endpoint/DdysPublicEndpoint.java',
  'src/main/java/io/ddys/halo/ddysopen/endpoint/DdysConsoleEndpoint.java',
  'src/main/java/io/ddys/halo/ddysopen/endpoint/DdysRateLimiter.java',
  'src/main/java/io/ddys/halo/ddysopen/finder/DdysFinderImpl.java',
  'src/main/java/io/ddys/halo/ddysopen/route/DdysRouter.java',
  'ui/src/index.ts',
  'ui/src/views/HomeView.vue',
  'README.md',
  'README.zh-CN.md',
];

const forbidden = [
  new RegExp('g' + 'hp_[A-Za-z0-9_]+'),
  new RegExp('n' + 'pm_[A-Za-z0-9_]+'),
  new RegExp('20' + '26facai', 'i'),
  new RegExp('x9' + 'kNx', 'i'),
  new RegExp('Do not ' + 'bundle', 'i'),
  new RegExp('不要' + '把'),
  new RegExp('浣' + '庣|褰' + '辫|涓' + '嶈'),
  new RegExp('TO' + 'DO|FIX' + 'ME'),
  new RegExp('console' + '\\.log\\('),
  new RegExp('System\\.out\\.' + 'println'),
];

const publicEndpointSnippets = [
  '/movies',
  '/movies/{slug}',
  '/movies/{slug}/sources',
  '/movies/{slug}/related',
  '/movies/{slug}/comments',
  '/search',
  '/suggest',
  '/hot',
  '/latest',
  '/calendar',
  '/types',
  '/genres',
  '/regions',
  '/collections',
  '/collections/{slug}',
  '/shares',
  '/shares/{id}',
  '/requests',
  '/activities',
  '/user/{username}',
];

const shortcodeNames = [
  'ddys_movies',
  'ddys_latest',
  'ddys_hot',
  'ddys_search',
  'ddys_suggest',
  'ddys_calendar',
  'ddys_movie',
  'ddys_sources',
  'ddys_related',
  'ddys_comments',
  'ddys_collections',
  'ddys_collection',
  'ddys_shares',
  'ddys_share',
  'ddys_requests',
  'ddys_activities',
  'ddys_user',
  'ddys_types',
  'ddys_genres',
  'ddys_regions',
  'ddys_request_form',
];

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8');
}

function walk(dir) {
  const items = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (['.gradle', '.git', 'build', 'node_modules', 'dist'].includes(entry.name)) {
      continue;
    }
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      items.push(...walk(full));
    } else {
      items.push(full);
    }
  }
  return items;
}

const errors = [];

for (const file of required) {
  if (!fs.existsSync(path.join(root, file))) {
    errors.push(`Missing required file: ${file}`);
  }
}

const allFiles = walk(root);
const textFiles = allFiles.filter((file) => {
  const ext = path.extname(file).toLowerCase();
  return !['.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico', '.jar'].includes(ext);
});

for (const file of textFiles) {
  const relative = path.relative(root, file);
  const content = fs.readFileSync(file, 'utf8');
  for (const pattern of forbidden) {
    if (pattern.test(content)) {
      errors.push(`Forbidden pattern ${pattern} in ${relative}`);
    }
  }
}

const stray = allFiles
  .map((file) => path.relative(root, file))
  .filter((file) => /(^|[\\/])(\.env|开发分析|开发文档)|\.(zip|log|bak)$/i.test(file));
if (stray.length) {
  errors.push(`Stray files: ${stray.join(', ')}`);
}

const manifest = read('src/main/resources/plugin.yaml');
for (const snippet of ['PluginDdysOpen', 'configMapName', 'settingName', '>=2.23.0']) {
  if (!manifest.includes(snippet)) {
    errors.push(`plugin.yaml missing ${snippet}`);
  }
}

const settings = read('src/main/resources/extensions/settings.yaml');
for (const group of ['basic', 'cache', 'display', 'features', 'auth']) {
  if (!settings.includes(`group: ${group}`)) {
    errors.push(`settings.yaml missing group ${group}`);
  }
}

const publicEndpoint = read('src/main/java/io/ddys/halo/ddysopen/endpoint/DdysPublicEndpoint.java');
for (const snippet of publicEndpointSnippets) {
  const literal = snippet
    .replace('{slug}', '" + safe(request, "slug") + "')
    .replace('{id}', '" + safe(request, "id") + "')
    .replace('{username}', '" + safe(request, "username") + "');
  const simplified = snippet.replace(/\{[^}]+}/g, '');
  if (!publicEndpoint.includes(simplified.split('/').filter(Boolean).at(-1) ?? 'movies')) {
    errors.push(`Public endpoint may be missing ${snippet}`);
  }
  void literal;
}

const shortcodes = read('src/main/java/io/ddys/halo/ddysopen/render/ShortcodeService.java');
for (const shortcode of shortcodeNames) {
  if (!shortcodes.includes(shortcode)) {
    errors.push(`Shortcode missing: ${shortcode}`);
  }
}

const enReadme = read('README.md');
const zhReadme = read('README.zh-CN.md');
if (!enReadme.includes('[DDYS](https://ddys.io/)')) {
  errors.push('README.md must use DDYS as official website link text.');
}
if (!zhReadme.includes('[低端影视](https://ddys.io/)')) {
  errors.push('README.zh-CN.md must use 低端影视 as official website link text.');
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}

process.stdout.write(`${JSON.stringify({
  ok: true,
  files: allFiles.length,
  shortcodes: shortcodeNames.length,
  required: required.length,
}, null, 2)}\n`);
