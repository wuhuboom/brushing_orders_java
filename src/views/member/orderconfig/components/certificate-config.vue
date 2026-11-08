<template>
  <a-form
    ref="configFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-form-item label="证书开关" name="certificateSwitch" extra="开启后，前端接口返回开启提示语，证书内容返回为空；关闭时正常返回证书。">
      <a-radio-group v-model:value="localForm.certificateSwitch">
        <a-radio value="0">关闭</a-radio>
        <a-radio value="1">开启</a-radio>
      </a-radio-group>
    </a-form-item>
    <a-form-item label="开启提示语" name="certificateTip">
      <a-textarea v-model:value="localForm.certificateTip" :rows="3" :placeholder="DEFAULT_CERTIFICATE_TIP" />
    </a-form-item>
    <a-form-item label="证书" name="certificateContent">
      <editor v-model="localForm.certificateContent" :min-height="360" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from "vue"
import {
  DEFAULT_CERTIFICATE_TIP,
  encodeCertificateConfig,
  normalizeCertificateSwitch,
  parseCertificateConfig,
} from "../certificateConfig"

const props = defineProps({
  form: {
    type: Object,
    default: () => ({})
  },
  loading: Boolean
})

const emit = defineEmits(["update:form", "submit", "cancel"])
const configFormRef = ref()
const sourceContent = ref({})
const localForm = reactive({
  certificateSwitch: "0",
  certificateTip: DEFAULT_CERTIFICATE_TIP,
  certificateContent: "",
})
const localRules = computed(() => ({
  certificateSwitch: [{ required: true, message: "请选择证书开关", trigger: "change" }],
  certificateTip: localForm.certificateSwitch === "1"
    ? [{ required: true, whitespace: true, message: "请输入开启提示语", trigger: "blur" }]
    : [],
}))

watch(
  () => props.form.content,
  content => {
    const values = parseCertificateConfig(content)
    sourceContent.value = values
    Object.assign(localForm, {
      certificateSwitch: normalizeCertificateSwitch(values.certificateSwitch),
      certificateTip: values.certificateTip,
      certificateContent: values.certificateContent,
    })
    configFormRef.value?.clearValidate?.()
  },
  { immediate: true }
)

function syncForm() {
  emit("update:form", {
    ...props.form,
    content: encodeCertificateConfig(sourceContent.value, localForm),
  })
}

watch(localForm, syncForm, { deep: true })

async function handleSubmit() {
  try {
    await configFormRef.value?.validate?.()
    syncForm()
    await nextTick()
    emit("submit")
  } catch {
    // The form displays validation errors beside the corresponding field.
  }
}

function handleCancel() {
  emit("cancel")
}

defineExpose({ handleSubmit, handleCancel })
</script>
