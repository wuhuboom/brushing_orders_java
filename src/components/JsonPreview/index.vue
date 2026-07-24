<template>
  <a-popover v-if="display !== '-'" placement="topLeft" trigger="click" overlay-class-name="json-preview-popover">
    <template #content><pre class="json-preview-full">{{ pretty }}</pre></template>
    <a-typography-text class="json-preview-inline" :title="display">{{ display }}</a-typography-text>
  </a-popover>
  <span v-else>-</span>
</template>

<script setup>
const props = defineProps({ value: { type: [String, Object, Array], default: '' } })
const pretty = computed(() => { if (!props.value) return '-'; if (typeof props.value !== 'string') return JSON.stringify(props.value, null, 2); try { return JSON.stringify(JSON.parse(props.value), null, 2) } catch { return props.value } })
const display = computed(() => pretty.value === '-' ? '-' : pretty.value.replace(/\s+/g, ' ').slice(0, 90))
</script>

<style scoped>
.json-preview-inline { display: inline-block; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.json-preview-full { max-width: 680px; max-height: 360px; margin: 0; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>
