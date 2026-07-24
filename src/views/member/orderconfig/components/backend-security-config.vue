<template>
  <a-form
    ref="securityFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="24">
        <a-form-item label="启用登录验证码" name="enabledLoginCaptcha">
          <a-radio-group v-model:value="localForm.enabledLoginCaptcha">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="连续登录失败次数" name="consecutiveLoginFailures">
          <a-input-number
            v-model:value="localForm.consecutiveLoginFailures"
            :min="1"
            :precision="0"
            class="full-width"
            placeholder="请输入失败次数"
          />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="失败冻结时长（分钟）" name="freezeDurationMinutes">
          <a-input-number
            v-model:value="localForm.freezeDurationMinutes"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入冻结时长"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="白名单列表" name="whitelist">
          <a-textarea
            v-model:value="localForm.whitelist"
            :rows="3"
            placeholder="请输入白名单（逗号分隔，如 IP1,IP2）"
          />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="黑名单国家代码列表" name="blacklistCountryCodes">
          <a-textarea
            v-model:value="localForm.blacklistCountryCodes"
            :rows="3"
            placeholder="请输入黑名单国家代码（逗号分隔，如 US,CN）"
          />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="黑名单国家数据请求地址" name="blacklistCountryApiUrl">
          <a-input
            v-model:value="localForm.blacklistCountryApiUrl"
            placeholder="请输入 API 地址（如 https://api.example.com/blacklist）"
            allow-clear
          />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="黑名单国家数据分隔符" name="blacklistCountrySeparator">
          <a-input
            v-model:value="localForm.blacklistCountrySeparator"
            placeholder="请输入分隔符（如 , 或 |）"
            allow-clear
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="黑名单列表" name="blacklist">
          <a-textarea
            v-model:value="localForm.blacklist"
            :rows="3"
            placeholder="请输入黑名单（逗号分隔，如 IP1,IP2）"
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
const securityFormRef = ref()
const localForm = reactive({ ...props.form })
const contentFieldKeys = [
  "enabledLoginCaptcha",
  "consecutiveLoginFailures",
  "freezeDurationMinutes",
  "whitelist",
  "blacklistCountryCodes",
  "blacklistCountryApiUrl",
  "blacklistCountrySeparator",
  "blacklist"
]

;["id", "type", "name", "createTime", "updateTime", "content"].forEach(key => {
  delete localForm[key]
})

const localRules = reactive({
  enabledLoginCaptcha: [{ required: true, message: "请选择是否启用登录验证码", trigger: "change" }],
  consecutiveLoginFailures: [{ required: true, message: "请输入连续登录失败次数", trigger: "blur" }],
  freezeDurationMinutes: [{ required: true, message: "请输入失败冻结时长", trigger: "blur" }],
  whitelist: [{ required: true, message: "请输入白名单列表", trigger: "blur" }],
  blacklistCountryCodes: [{ required: true, message: "请输入黑名单国家代码列表", trigger: "blur" }],
  blacklistCountryApiUrl: [{ required: true, message: "请输入黑名单国家数据请求地址", trigger: "blur" }],
  blacklistCountrySeparator: [{ required: true, message: "请输入黑名单国家数据分隔符", trigger: "blur" }],
  blacklist: [{ required: true, message: "请输入黑名单列表", trigger: "blur" }]
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
      content: JSON.stringify(toSave)
    })
  },
  { deep: true }
)

function handleSubmit() {
  securityFormRef.value?.validate?.().then(() => {
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
