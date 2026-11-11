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
import { computed, nextTick, reactive, ref, watch } from "vue"

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
const sourceContent = ref({})
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
    let parsed = newContent
    if (typeof newContent === "string" && newContent.trim()) {
      try {
        parsed = JSON.parse(newContent)
      } catch {
        // Older settings may contain the HTML body directly.
      }
    }
    sourceContent.value = parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? { ...parsed }
      : {}
    localForm[props.field] = typeof parsed === "string"
      ? parsed
      : sourceContent.value[props.field] || ""
    if (props.field === "eventContent" && typeof parsed !== "string") {
      const containers = [sourceContent.value]
      if (sourceContent.value.value && typeof sourceContent.value.value === "object" && !Array.isArray(sourceContent.value.value)) {
        containers.push(sourceContent.value.value)
      }
      localForm.eventContent = ["eventContent", "event", "content", "value"]
        .flatMap(key => containers.map(container => container[key]))
        .find(value => typeof value === "string" && value.trim()) ?? ""
    }
  },
  { immediate: true }
)

function syncForm() {
  const content = {
    ...sourceContent.value,
    [props.field]: localForm[props.field]
  }
  if (props.field === "eventContent") {
    const containers = [content]
    if (content.value && typeof content.value === "object" && !Array.isArray(content.value)) {
      content.value = { ...content.value, eventContent: localForm.eventContent }
      containers.push(content.value)
    }
    for (const container of containers) {
      for (const key of ["event", "content", "value"]) {
        if (typeof container[key] === "string") container[key] = localForm.eventContent
      }
    }
  }
  emit("update:form", {
    ...props.form,
    ...(props.showSort ? { sort: localForm.sort } : {}),
    content: JSON.stringify(content)
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

<style scoped>
.full-width {
  width: 100%;
}

</style>
