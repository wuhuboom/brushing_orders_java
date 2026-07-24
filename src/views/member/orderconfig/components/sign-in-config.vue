<template>
  <a-form
    ref="signInFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="24">
        <a-form-item label="序号" name="sort">
          <a-input-number v-model:value="localForm.sort" :min="0" class="full-width" placeholder="请输入序号" />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="localForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="自动签到" name="autoSign">
          <a-radio-group v-model:value="localForm.autoSign">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="连续签到验证" name="continuousVerify">
          <a-radio-group v-model:value="localForm.continuousVerify">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="最低完成任务次数" name="minTaskCount">
          <a-input-number
            v-model:value="localForm.minTaskCount"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入最低任务次数"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="赠送金额" name="giftAmount">
          <a-textarea
            v-model:value="localForm.giftAmount"
            :rows="3"
            placeholder="请输入赠送金额（支持多行描述）"
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
const signInFormRef = ref()
const localForm = reactive({ ...props.form })
const contentFieldKeys = ["status", "autoSign", "continuousVerify", "minTaskCount", "giftAmount"]

;["id", "type", "name", "createTime", "updateTime", "content"].forEach(key => {
  delete localForm[key]
})

const localRules = reactive({
  sort: [{ required: true, message: "序号不能为空", trigger: "blur" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  autoSign: [{ required: true, message: "请选择自动签到", trigger: "change" }],
  continuousVerify: [{ required: true, message: "请选择连续签到验证", trigger: "change" }],
  minTaskCount: [{ required: true, message: "请输入最低完成任务次数", trigger: "blur" }],
  giftAmount: [{ required: true, message: "请输入赠送金额", trigger: "blur" }]
})

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      const fields = contentFieldKeys.reduce((result, key) => {
        result[key] = parsed[key]
        return result
      }, {})
      fields.sort = props.form.sort ?? localForm.sort
      Object.assign(localForm, fields)
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
      sort: localForm.sort,
      content: JSON.stringify(toSave)
    })
  },
  { deep: true }
)

function handleSubmit() {
  signInFormRef.value?.validate?.().then(() => {
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
