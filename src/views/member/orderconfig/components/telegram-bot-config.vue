<template>
  <a-form
    ref="telegramFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="12">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="localForm.name" placeholder="请输入 Telegram 机器人名称" allow-clear />
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="令牌" name="token">
          <a-input-password v-model:value="localForm.token" placeholder="请输入 Telegram 机器人令牌" allow-clear />
        </a-form-item>
      </a-col>
    </a-row>

    <a-tabs v-model:activeKey="activeTab" type="card" class="config-tabs">
      <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label">
        <a-row :gutter="[20, 0]">
          <a-col :span="8">
            <a-form-item
              :label="`${tab.label}是否启用`"
              :name="['tabs', tab.key, 'enabled']"
              :rules="[{ required: true, message: `请选择${tab.label}是否启用`, trigger: 'change' }]"
            >
              <a-radio-group v-model:value="localForm.tabs[tab.key].enabled">
                <a-radio value="0">启用</a-radio>
                <a-radio value="1">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item
              :label="`${tab.label}频道/群组ID`"
              :name="['tabs', tab.key, 'channelId']"
              :rules="[{ required: true, message: `请输入${tab.label}频道/群组ID`, trigger: 'blur' }]"
            >
              <a-input
                v-model:value="localForm.tabs[tab.key].channelId"
                :placeholder="`请输入${tab.label}频道/群组ID`"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              :label="`${tab.label}内容`"
              :name="['tabs', tab.key, 'content']"
              :rules="[
                { required: true, message: `请输入${tab.label}内容`, trigger: 'blur' },
                { min: 10, message: '内容不少于10字符', trigger: 'blur' }
              ]"
            >
              <editor v-model="localForm.tabs[tab.key].content" :min-height="200" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-tab-pane>
    </a-tabs>
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
const telegramFormRef = ref()
const activeTab = ref("register")

const tabs = [
  { key: "register", label: "注册" },
  { key: "login", label: "登录" },
  { key: "withdrawal", label: "提现" },
  { key: "completeTask", label: "完成任务" },
  { key: "signIn", label: "签到" },
  { key: "balanceTreasureOut", label: "余额宝转出" },
  { key: "balanceTreasureIn", label: "余额宝转入" }
]

const localRules = reactive({
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
  token: [{ required: true, message: "请输入令牌", trigger: "blur" }]
})

const localForm = reactive({
  name: "",
  token: "",
  tabs: {}
})

tabs.forEach(tab => {
  localForm.tabs[tab.key] = {
    enabled: "1",
    channelId: "",
    content: ""
  }
})

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      localForm.name = parsed.name || ""
      localForm.token = parsed.token || ""
      tabs.forEach(tab => {
        Object.assign(localForm.tabs[tab.key], parsed.tabs?.[tab.key] || {})
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
    const tabsData = {}
    tabs.forEach(tab => {
      tabsData[tab.key] = { ...localForm.tabs[tab.key] }
    })
    emit("update:form", {
      ...props.form,
      content: JSON.stringify({
        name: localForm.name,
        token: localForm.token,
        tabs: tabsData
      })
    })
  },
  { deep: true }
)

function handleSubmit() {
  telegramFormRef.value?.validate?.().then(() => {
    emit("submit")
  }).catch(() => {})
}

function handleCancel() {
  emit("cancel")
}

defineExpose({ handleSubmit, handleCancel })
</script>

<style scoped>
.config-tabs {
  margin-top: 4px;
}

:deep(.editor-container) {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}
</style>
