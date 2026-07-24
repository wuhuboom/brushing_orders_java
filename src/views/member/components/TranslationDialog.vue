<template>
  <a-modal
    :title="title"
    v-model:open="visible"
    width="70%"
    ok-text="确 定"
    cancel-text="取 消"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-tabs v-model:activeKey="activeLanguage" class="translation-tabs">
      <a-tab-pane
        v-for="language in translationLanguages"
        :key="language.field"
        :tab="`${language.flag} ${language.label}`"
        lazy
      >
        <a-form layout="vertical">
          <a-form-item :label="`${language.flag} ${language.label}`">
            <editor
              v-model="localTranslations[language.field]"
              :min-height="360"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup>
import {
  createEmptyTranslations,
  translationLanguages,
} from "./translationLanguages";

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  title: {
    type: String,
    default: "国际化",
  },
  translations: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["update:modelValue", "submit"]);

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const localTranslations = ref(createEmptyTranslations());
const activeLanguage = ref(translationLanguages[0].field);

watch(
  () => props.translations,
  (value) => {
    localTranslations.value = {
      ...createEmptyTranslations(),
      ...(value || {}),
    };
  },
  { immediate: true, deep: true }
);

function handleCancel() {
  visible.value = false;
}

function handleSubmit() {
  emit("submit", { ...localTranslations.value });
}
</script>

<style scoped>
.translation-tabs :deep(.ant-tabs-nav-wrap) {
  padding-bottom: 4px;
}

.translation-tabs :deep(.ant-tabs-tab) {
  font-weight: 500;
}
</style>
