<template>
  <a-form
    ref="emailFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="12">
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="localForm.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="验证码状态" name="captchaStatus">
          <a-radio-group v-model:value="localForm.captchaStatus">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="主机" name="host">
          <a-input v-model:value="localForm.host" placeholder="请输入主机地址" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="端口" name="port">
          <a-input-number
            v-model:value="localForm.port"
            :min="1"
            :max="65535"
            :precision="0"
            class="full-width"
            placeholder="请输入端口"
          />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="localForm.username" placeholder="请输入用户名" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="localForm.password" placeholder="请输入密码" allow-clear />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="协议" name="protocol">
          <a-input v-model:value="localForm.protocol" placeholder="请输入协议" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="默认编码" name="defaultEncoding">
          <a-input v-model:value="localForm.defaultEncoding" placeholder="请输入默认编码" allow-clear />
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="默认验证码" name="defaultCaptcha">
          <a-input v-model:value="localForm.defaultCaptcha" placeholder="请输入默认验证码" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="验证码有效期" name="captchaExpiry">
          <a-input-number
            v-model:value="localForm.captchaExpiry"
            :min="1"
            :precision="0"
            class="full-width"
            placeholder="请输入有效期（分钟）"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="标题" name="title">
          <a-input v-model:value="localForm.title" placeholder="请输入邮件标题" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="24">
        <a-form-item label="内容" name="contents">
          <a-textarea v-model:value="localForm.contents" :rows="6" placeholder="请输入邮件内容" />
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

const emailFormRef = ref()
const localForm = reactive({ ...props.form })
const contentFieldKeys = [
  "status",
  "captchaStatus",
  "host",
  "port",
  "username",
  "password",
  "protocol",
  "defaultEncoding",
  "defaultCaptcha",
  "captchaExpiry",
  "title",
  "contents"
]

;["id", "type", "name", "createTime", "updateTime", "content"].forEach(key => {
  delete localForm[key]
})

const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  captchaStatus: [{ required: true, message: "请选择验证码状态", trigger: "change" }],
  host: [{ required: true, message: "请输入主机地址", trigger: "blur" }],
  port: [{ required: true, message: "请输入端口", trigger: "blur" }],
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  protocol: [{ required: true, message: "请输入协议", trigger: "blur" }],
  defaultEncoding: [{ required: true, message: "请输入默认编码", trigger: "blur" }],
  defaultCaptcha: [{ required: true, message: "请输入默认验证码", trigger: "blur" }],
  captchaExpiry: [{ required: true, message: "请输入验证码有效期", trigger: "blur" }],
  title: [{ required: true, message: "请输入邮件标题", trigger: "blur" }],
  contents: [
    { required: true, message: "请输入邮件内容", trigger: "blur" },
    { min: 10, message: "内容长度不能少于10字符", trigger: "blur" }
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
  emailFormRef.value?.validate?.().then(() => {
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
