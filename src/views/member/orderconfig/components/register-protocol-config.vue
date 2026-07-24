<template>
  <rich-text-config-form
    ref="configFormRef"
    :form="props.form"
    field="protocolContent"
    label="注册协议"
    required-message="请输入注册协议内容"
    show-sort
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
