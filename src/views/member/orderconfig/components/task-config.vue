<template>
  <a-form
    ref="taskFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="8">
        <a-form-item label="任务背景图" name="taskBackground">
          <image-upload v-model="localForm.taskBackground" :limit="1" />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="连单图片" name="continuousOrderImage">
          <image-upload v-model="localForm.continuousOrderImage" :limit="1" />
        </a-form-item>
      </a-col>
      <a-col :span="8">
        <a-form-item label="连单标题" name="continuousOrderTitle">
          <a-input v-model:value="localForm.continuousOrderTitle" placeholder="请输入连单标题" allow-clear />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="连单内容" name="continuousOrderContent">
          <a-textarea
            v-model:value="localForm.continuousOrderContent"
            :rows="5"
            placeholder="请输入连单内容"
          />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="任务通知天数" name="taskNotificationDays">
          <a-input-number
            v-model:value="localForm.taskNotificationDays"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入天数"
          />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="任务通知条数" name="taskNotificationCount">
          <a-input-number
            v-model:value="localForm.taskNotificationCount"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入条数"
          />
        </a-form-item>
      </a-col>
    </a-row>
  </a-form>
</template>

<script setup>
import { reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"

const props = defineProps({
  form: {
    type: Object,
    default: () => ({})
  },
  loading: Boolean
})

const emit = defineEmits(["update:form", "submit", "cancel"])
const taskFormRef = ref()
const localForm = reactive({
  taskBackground: "",
  continuousOrderImage: "",
  continuousOrderTitle: "",
  continuousOrderContent: "",
  taskNotificationDays: 0,
  taskNotificationCount: 0
})

const contentFieldKeys = Object.keys(localForm)

const localRules = reactive({
  taskBackground: [{ required: true, message: "请上传任务背景图", trigger: "change" }],
  continuousOrderImage: [{ required: true, message: "请上传连单图片", trigger: "change" }],
  continuousOrderTitle: [{ required: true, message: "请输入连单标题", trigger: "blur" }],
  continuousOrderContent: [
    { required: true, message: "请输入连单内容", trigger: "blur" },
    { min: 5, message: "内容长度不能少于5字符", trigger: "blur" }
  ],
  taskNotificationDays: [{ required: true, message: "请输入任务通知天数", trigger: "blur" }],
  taskNotificationCount: [{ required: true, message: "请输入任务通知条数", trigger: "blur" }]
})

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      contentFieldKeys.forEach(key => {
        localForm[key] = parsed[key] ?? localForm[key]
      })
    } catch (e) {
      message.error("解析配置失败")
    }
  },
  { immediate: true }
)

watch(
  localForm,
  () => {
    const toSave = contentFieldKeys.reduce((result, key) => {
      result[key] = localForm[key]
      return result
    }, {})

    emit("update:form", {
      ...props.form,
      content: JSON.stringify(toSave)
    })
  },
  { deep: true }
)

function handleSubmit() {
  taskFormRef.value?.validate?.().then(() => {
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
