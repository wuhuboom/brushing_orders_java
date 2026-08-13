<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    width="900px"
    ok-text="确 定"
    cancel-text="取 消"
    destroy-on-close
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-tabs v-model:activeKey="activeLanguage" class="notice-translation-tabs">
      <a-tab-pane
        v-for="language in noticeLanguages"
        :key="language.field"
        :tab="`${language.flag} ${language.label}`"
      >
        <a-form layout="vertical">
          <a-form-item label="标题">
            <a-input v-model:value="localModels[language.field].title" />
          </a-form-item>
          <a-form-item label="内容">
            <editor
              v-model="localModels[language.field].content"
              :min-height="300"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
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
  min-height: 470px;
}
</style>
