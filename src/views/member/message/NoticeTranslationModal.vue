<template>
  <a-drawer
    v-model:open="visible"
    :title="title"
    width="70%"
    :destroy-on-close="true"
    root-class-name="notice-translation-drawer"
    @close="handleCancel"
  >
    <a-tabs v-model:activeKey="activeLanguage" class="notice-translation-tabs">
      <a-tab-pane
        v-for="language in noticeLanguages"
        :key="language.field"
        :tab="`${language.flag} ${language.label}`"
      >
        <a-form layout="vertical" size="large" class="notice-translation-form">
          <a-form-item label="标题">
            <a-textarea v-model:value="localModels[language.field].title" :rows="3" />
          </a-form-item>
          <a-form-item label="内容">
            <editor
              v-model="localModels[language.field].content"
              :min-height="551"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
    <template #footer>
      <div class="notice-translation-footer">
        <a-space>
          <a-button size="large" @click="handleCancel">取 消</a-button>
          <a-button type="primary" size="large" @click="handleSubmit">确 定</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { translationLanguages } from '@/views/member/components/translationLanguages'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '修改'
  },
  translations: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])
const excludedLanguageFields = new Set(['zhCn', 'zhTw', 'thTh', 'ptPt'])
const noticeLanguages = translationLanguages.filter(
  (language) => !excludedLanguageFields.has(language.field)
)
const activeLanguage = ref(noticeLanguages[0].field)
const localModels = ref({})
const originalTranslations = ref({})

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

function parseTranslation(raw) {
  if (raw && typeof raw === 'object') {
    return {
      title: raw.title || '',
      content: raw.content || ''
    }
  }
  if (typeof raw !== 'string' || !raw.trim()) {
    return { title: '', content: '' }
  }
  try {
    const parsed = JSON.parse(raw)
    const value = parsed?.value && typeof parsed.value === 'object'
      ? parsed.value
      : parsed
    return {
      title: value?.title || '',
      content: value?.content || ''
    }
  } catch {
    return { title: '', content: raw }
  }
}

function hydrate(value) {
  originalTranslations.value = { ...(value || {}) }
  localModels.value = Object.fromEntries(
    noticeLanguages.map((language) => [
      language.field,
      parseTranslation(value?.[language.field])
    ])
  )
}

function handleCancel() {
  visible.value = false
}

function handleSubmit() {
  const result = { ...originalTranslations.value }
  noticeLanguages.forEach((language) => {
    result[language.field] = JSON.stringify({
      title: localModels.value[language.field].title || '',
      content: localModels.value[language.field].content || ''
    })
  })
  emit('submit', result)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      activeLanguage.value = noticeLanguages[0].field
      hydrate(props.translations)
    }
  }
)

watch(
  () => props.translations,
  (value) => {
    if (props.modelValue) hydrate(value)
  },
  { deep: true }
)

hydrate(props.translations)
</script>

<style scoped>
.notice-translation-tabs {
  min-height: 640px;
}

.notice-translation-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.notice-translation-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.notice-translation-form :deep(textarea.ant-input) {
  padding: 7px 11px;
  font-size: 16px;
  line-height: 1.5715;
}

:global(.notice-translation-drawer .ant-drawer-body) {
  padding: 24px;
  overflow: auto;
}

:global(.notice-translation-drawer .ant-drawer-header) {
  height: 57px;
  min-height: 57px;
}

:global(.notice-translation-drawer .ant-drawer-footer) {
  padding: 8px 16px;
}

.notice-translation-footer {
  text-align: right;
}
</style>
