<template>
  <rich-text-config-form
    ref="configFormRef"
    :form="props.form"
    field="eventContent"
    label="事件"
    required-message="请输入事件内容"
    @update:form="emit('update:form', $event)"
    @submit="emit('submit')"
    @cancel="emit('cancel')"
  />
</template>

<script setup>
import { ref } from "vue"
import RichTextConfigForm from "./RichTextConfigForm.vue"

const props = defineProps({
  form: {
    type: Object,
    default: () => ({})
  },
  loading: Boolean
})

const emit = defineEmits(["update:form", "submit", "cancel"])
const configFormRef = ref()

function handleSubmit() {
  configFormRef.value?.handleSubmit?.()
}

function handleCancel() {
  configFormRef.value?.handleCancel?.()
}

defineExpose({ handleSubmit, handleCancel })
</script>
