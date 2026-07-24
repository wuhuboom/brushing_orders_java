<template>
  <a-form
    ref="pointsFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="24">
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="localForm.status">
            <a-radio value="0">启用</a-radio>
            <a-radio value="1">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="赠送积分" name="giftPoints">
          <a-textarea
            v-model:value="localForm.giftPoints"
            :rows="5"
            placeholder="请输入赠送积分描述（支持多行文本）"
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
const pointsFormRef = ref()
const localForm = reactive({
  status: "1",
  giftPoints: ""
})

const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  giftPoints: [
    { required: true, message: "请输入赠送积分描述", trigger: "blur" },
    { min: 5, message: "内容长度不能少于5字符", trigger: "blur" }
  ]
})

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      localForm.status = parsed.status || "1"
      localForm.giftPoints = parsed.giftPoints || ""
    } catch (e) {
      message.error("解析配置失败")
    }
  },
  { immediate: true }
)

watch(
  localForm,
  () => {
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        status: localForm.status,
        giftPoints: localForm.giftPoints
      })
    })
  },
  { deep: true }
)

function handleSubmit() {
  pointsFormRef.value?.validate?.().then(() => {
    emit("submit")
  }).catch(() => {})
}

function handleCancel() {
  emit("cancel")
}

defineExpose({ handleSubmit, handleCancel })
</script>
