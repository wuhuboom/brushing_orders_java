<template>
  <div class="json-tree-cell">
    <button type="button" class="json-root" @click="expanded = !expanded">
      <span class="json-key">"root"</span>
      <span class="json-punctuation">:</span>
      <span class="json-punctuation">{</span>
      <span v-if="!expanded" class="json-ellipsis">...</span>
      <span class="json-punctuation">}</span>
      <span class="json-count">{{ itemCount }} items</span>
    </button>

    <div v-if="expanded" class="json-children">
      <div class="json-copy-row">
        <a-button type="text" size="small" aria-label="Copy to clipboard" @click="copyJson">
          <CopyOutlined />
        </a-button>
      </div>
      <div v-for="entry in entries" :key="entry.key" class="json-entry">
        <span class="json-key">"{{ entry.key }}"</span>
        <span class="json-punctuation">:</span>
        <span class="json-type">{{ entry.type }}</span>
        <span :class="['json-value', `json-value-${entry.type}`]">{{ entry.value }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CopyOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  value: {
    type: [String, Object, Array],
    default: ''
  }
})

const expanded = ref(false)

const parsedValue = computed(() => {
  if (props.value && typeof props.value === 'object') {
    return props.value
  }
  if (typeof props.value !== 'string' || !props.value.trim()) {
    return {}
  }
  try {
    const parsed = JSON.parse(props.value)
    return parsed && typeof parsed === 'object' ? parsed : { value: parsed }
  } catch {
    return { value: props.value }
  }
})

const entries = computed(() => Object.entries(parsedValue.value).map(([key, value]) => ({
  key,
  type: value === null ? 'null' : Array.isArray(value) ? 'array' : typeof value,
  value: formatValue(value)
})))

const itemCount = computed(() => entries.value.length)

function formatValue(value) {
  if (typeof value === 'string') return `"${value}"`
  if (value === null) return 'null'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

async function copyJson(event) {
  event.stopPropagation()
  try {
    await navigator.clipboard.writeText(JSON.stringify(parsedValue.value, null, 2))
  } catch {
    // Clipboard permissions can be unavailable in an embedded browser.
  }
}
</script>

<style scoped>
.json-tree-cell {
  min-width: 210px;
  color: rgba(0, 0, 0, 0.88);
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.json-root {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  color: inherit;
  font: inherit;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.json-key {
  color: #92278f;
}

.json-punctuation {
  color: rgba(0, 0, 0, 0.65);
}

.json-ellipsis {
  color: rgba(0, 0, 0, 0.45);
}

.json-count,
.json-type {
  color: rgba(0, 0, 0, 0.45);
}

.json-children {
  position: relative;
  min-width: 340px;
  max-width: 620px;
  margin-top: 6px;
  padding: 6px 8px 8px 18px;
  overflow-wrap: anywhere;
  border-left: 1px dashed #d9d9d9;
}

.json-copy-row {
  position: absolute;
  top: 0;
  right: 0;
}

.json-entry {
  display: grid;
  grid-template-columns: max-content 10px max-content minmax(80px, 1fr);
  gap: 4px;
  padding: 2px 28px 2px 0;
}

.json-value-string {
  color: #22863a;
}

.json-value-number,
.json-value-boolean {
  color: #005cc5;
}

.json-value-null {
  color: #b31d28;
}
</style>
