<template>
  <a-form
    ref="configFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row v-if="showSort" :gutter="[20, 0]">
      <a-col :span="24">
        <a-form-item label="序号" name="sort">
          <a-input-number v-model:value="localForm.sort" :min="0" class="full-width" placeholder="请输入序号" />
        </a-form-item>
      </a-col>
    </a-row>

    <a-form-item :label="label" :name="field">
      <editor v-model="localForm[field]" :min-height="minHeight" />
    </a-form-item>
  </a-form>
</template>

<script setup>
import { computed, reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"

const props = defineProps({
  form: {
    type: Object,
    default: () => ({})
  },
  field: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  requiredMessage: {
    type: String,
    default: ""
  },
  minHeight: {
    type: Number,
    default: 360
  },
  showSort: Boolean
})

const emit = defineEmits(["update:form", "submit", "cancel"])
const configFormRef = ref()
const localForm = reactive({
  sort: props.form.sort,
  [props.field]: ""
})

const localRules = computed(() => ({
  ...(props.showSort
    ? { sort: [{ required: true, message: "序号不能为空", trigger: "blur" }] }
    : {})
}))

watch(
  () => props.form.sort,
  value => {
    if (props.showSort) {
      localForm.sort = value
    }
  },
  { immediate: true }
)

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      localForm[props.field] = parsed[props.field] || ""
    } catch (e) {
      message.error("解析配置失败")
      localForm[props.field] = ""
    }
  },
  { immediate: true }
)

watch(
  localForm,
  () => {
    emit("update:form", {
      ...props.form,
      ...(props.showSort ? { sort: localForm.sort } : {}),
      content: JSON.stringify({
        [props.field]: localForm[props.field]
      })
    })
  },
  { deep: true }
)

function handleSubmit() {
  configFormRef.value?.validate?.().then(() => {
    emit("submit")
  }).catch(() => {})
}

function handleCancel() {
  emit("cancel")
}

defineExpose({ handleSubmit, handleCancel })
</script>

<style scoped>
.full-width {
  width: 100%;
}

</style>
