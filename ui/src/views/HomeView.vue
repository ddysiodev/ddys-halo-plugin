<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

type JsonMap = Record<string, unknown>

const endpoint = '/apis/console.api.ddys.io/v1alpha1/ddys'
const status = ref<JsonMap | null>(null)
const diagnostics = ref<JsonMap | null>(null)
const shortcodes = ref<Array<{ name: string; description: string; example: string }>>([])
const previewHtml = ref('')
const apiResult = ref('')
const message = ref('')
const loading = ref(false)
const shortcode = ref('[ddys_latest limit="6" layout="grid"]')
const apiPath = ref('/latest')
const apiQuery = ref('limit=6')

const apiPaths = [
  '/movies',
  '/search',
  '/suggest',
  '/hot',
  '/latest',
  '/calendar',
  '/types',
  '/genres',
  '/regions',
  '/collections',
  '/shares',
  '/requests',
  '/activities',
]

const cache = computed(() => status.value?.cache as JsonMap | undefined)
const settings = computed(() => status.value?.settings as JsonMap | undefined)

async function fetchJson<T = JsonMap>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init)
  const data = await response.json()
  if (!response.ok) {
    throw new Error(data.message || 'Request failed.')
  }
  return data as T
}

async function loadStatus() {
  status.value = await fetchJson(`${endpoint}/status`)
}

async function loadShortcodes() {
  shortcodes.value = await fetchJson<Array<{ name: string; description: string; example: string }>>(`${endpoint}/shortcodes`)
}

async function runDiagnostics() {
  loading.value = true
  message.value = ''
  try {
    diagnostics.value = await fetchJson(`${endpoint}/diagnostics`)
    message.value = 'Diagnostics updated.'
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Diagnostics failed.'
  } finally {
    loading.value = false
  }
}

async function renderPreview() {
  loading.value = true
  message.value = ''
  try {
    const data = await fetchJson<JsonMap>(`${endpoint}/preview?shortcode=${encodeURIComponent(shortcode.value)}`)
    previewHtml.value = String(data.html || '')
    message.value = 'Preview rendered.'
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Preview failed.'
  } finally {
    loading.value = false
  }
}

async function renderExample(example: string) {
  shortcode.value = example
  await renderPreview()
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
  } else {
    const input = document.createElement('textarea')
    input.value = text
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
  }
  message.value = 'Copied.'
}

async function previewApi() {
  loading.value = true
  message.value = ''
  try {
    const query = new URLSearchParams(apiQuery.value.trim().replace(/^\?/, ''))
    query.set('path', apiPath.value)
    const data = await fetchJson<JsonMap>(`${endpoint}/request?${query.toString()}`)
    apiResult.value = JSON.stringify(data, null, 2)
    message.value = 'API preview updated.'
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'API preview failed.'
  } finally {
    loading.value = false
  }
}

async function flushCache() {
  loading.value = true
  message.value = ''
  try {
    await fetchJson(`${endpoint}/cache/flush`, { method: 'POST' })
    await loadStatus()
    message.value = 'Cache flushed.'
  } catch (error) {
    message.value = error instanceof Error ? error.message : 'Cache flush failed.'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStatus()
  await loadShortcodes()
  await renderPreview()
  await previewApi()
})
</script>

<template>
  <main class="ddys-console">
    <header class="ddys-console__header">
      <div>
        <h1>DDYS</h1>
        <p>Halo integration dashboard</p>
      </div>
      <div class="ddys-console__actions">
        <button :disabled="loading" @click="runDiagnostics">Diagnostics</button>
        <button :disabled="loading" @click="flushCache">Flush Cache</button>
      </div>
    </header>

    <p v-if="message" class="ddys-console__message">{{ message }}</p>

    <section class="ddys-console__grid">
      <article>
        <h2>Plugin</h2>
        <dl>
          <dt>API</dt>
          <dd>{{ settings?.apiBaseUrl }}</dd>
          <dt>Cache</dt>
          <dd>{{ settings?.cacheEnabled ? 'enabled' : 'disabled' }}</dd>
          <dt>Public API</dt>
          <dd>{{ settings?.publicApiEnabled ? 'enabled' : 'disabled' }}</dd>
          <dt>Request form</dt>
          <dd>{{ settings?.requestFormEnabled ? 'enabled' : 'disabled' }}</dd>
        </dl>
      </article>

      <article>
        <h2>Cache</h2>
        <dl>
          <dt>Size</dt>
          <dd>{{ cache?.size ?? 0 }}</dd>
          <dt>Hits</dt>
          <dd>{{ cache?.hits ?? 0 }}</dd>
          <dt>Misses</dt>
          <dd>{{ cache?.misses ?? 0 }}</dd>
          <dt>Writes</dt>
          <dd>{{ cache?.writes ?? 0 }}</dd>
        </dl>
      </article>

      <article>
        <h2>Diagnostics</h2>
        <dl>
          <dt>API reachable</dt>
          <dd>{{ diagnostics?.apiReachable === true ? 'yes' : diagnostics ? 'no' : '-' }}</dd>
          <dt>Shortcodes</dt>
          <dd>{{ diagnostics?.shortcodeCount ?? '-' }}</dd>
        </dl>
      </article>
    </section>

    <section class="ddys-console__preview">
      <div class="ddys-console__preview-bar">
        <input v-model="shortcode" aria-label="Shortcode">
        <button :disabled="loading" @click="renderPreview">Preview</button>
      </div>
      <div class="ddys-console__preview-frame" v-html="previewHtml"></div>
    </section>

    <section class="ddys-console__tools">
      <article class="ddys-console__shortcodes">
        <h2>Shortcodes</h2>
        <div class="ddys-console__shortcode-list">
          <div v-for="item in shortcodes" :key="item.name" class="ddys-console__shortcode-row">
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.description }}</p>
              <code>{{ item.example }}</code>
            </div>
            <div class="ddys-console__row-actions">
              <button :disabled="loading" @click="renderExample(item.example)">Preview</button>
              <button @click="copyText(item.example)">Copy</button>
            </div>
          </div>
        </div>
      </article>

      <article>
        <h2>API Preview</h2>
        <div class="ddys-console__api-form">
          <select v-model="apiPath" aria-label="API path">
            <option v-for="path in apiPaths" :key="path" :value="path">{{ path }}</option>
          </select>
          <input v-model="apiQuery" aria-label="Query string" placeholder="limit=6">
          <button :disabled="loading" @click="previewApi">Run</button>
        </div>
        <pre class="ddys-console__api-result">{{ apiResult }}</pre>
      </article>
    </section>
  </main>
</template>

<style scoped>
.ddys-console {
  color: #18181b;
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.ddys-console__header {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.ddys-console h1 {
  font-size: 1.5rem;
  margin: 0;
}

.ddys-console h2 {
  font-size: 1rem;
  margin: 0 0 0.75rem;
}

.ddys-console p {
  color: #71717a;
  margin: 0.25rem 0 0;
}

.ddys-console__actions,
.ddys-console__preview-bar,
.ddys-console__row-actions,
.ddys-console__api-form {
  display: flex;
  gap: 0.5rem;
}

.ddys-console button {
  background: #0f766e;
  border: 0;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  min-height: 2.25rem;
  padding: 0 0.8rem;
}

.ddys-console button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.ddys-console__grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.ddys-console article,
.ddys-console__preview {
  background: #fff;
  border: 1px solid #e4e4e7;
  border-radius: 8px;
  padding: 1rem;
}

.ddys-console__tools {
  display: grid;
  gap: 1rem;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
}

.ddys-console__shortcode-list {
  display: grid;
  gap: 0.75rem;
}

.ddys-console__shortcode-row {
  align-items: start;
  border-bottom: 1px solid #e4e4e7;
  display: grid;
  gap: 0.75rem;
  grid-template-columns: minmax(0, 1fr) auto;
  padding-bottom: 0.75rem;
}

.ddys-console__shortcode-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.ddys-console code {
  background: #f4f4f5;
  border-radius: 5px;
  display: inline-block;
  margin-top: 0.35rem;
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 0.25rem 0.4rem;
}

.ddys-console dl {
  display: grid;
  gap: 0.5rem;
  grid-template-columns: 120px 1fr;
  margin: 0;
}

.ddys-console dt {
  color: #71717a;
}

.ddys-console dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.ddys-console input,
.ddys-console select {
  border: 1px solid #d4d4d8;
  border-radius: 6px;
  flex: 1;
  min-height: 2.25rem;
  padding: 0 0.65rem;
}

.ddys-console__api-result {
  background: #18181b;
  border-radius: 8px;
  color: #f4f4f5;
  font-size: 0.82rem;
  line-height: 1.5;
  margin: 1rem 0 0;
  max-height: 420px;
  overflow: auto;
  padding: 1rem;
  white-space: pre-wrap;
}

.ddys-console__message {
  background: #ecfdf5;
  border: 1px solid #99f6e4;
  border-radius: 6px;
  color: #115e59;
  padding: 0.6rem 0.75rem;
}

.ddys-console__preview-frame {
  border-top: 1px solid #e4e4e7;
  margin-top: 1rem;
  padding-top: 1rem;
}

@media (max-width: 900px) {
  .ddys-console__grid {
    grid-template-columns: 1fr;
  }

  .ddys-console__tools {
    grid-template-columns: 1fr;
  }

  .ddys-console__header {
    align-items: stretch;
    flex-direction: column;
  }

  .ddys-console__shortcode-row {
    grid-template-columns: 1fr;
  }

  .ddys-console__api-form {
    flex-direction: column;
  }

  .ddys-console__row-actions {
    justify-content: flex-start;
  }
}
</style>
