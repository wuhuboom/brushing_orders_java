<template>
  <a-form ref="notificationFormRef" :model="localForm" layout="vertical" class="config-form">
    <a-tabs v-model:activeKey="activeTab" type="card" class="config-tabs">
      <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label">
        <a-row :gutter="[20, 0]">
          <a-col :span="8">
            <a-form-item
              :label="`${tab.label}是否启用`"
              :name="[tab.key, 'enabled']"
              :rules="[{ required: true, message: `请选择${tab.label}是否启用`, trigger: 'change' }]"
            >
              <a-radio-group v-model:value="localForm[tab.key].enabled">
                <a-radio :value="0">启用</a-radio>
                <a-radio :value="1">停用</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item
              :label="`${tab.label}标题`"
              :name="[tab.key, 'title']"
              :rules="[{ required: true, message: `请输入${tab.label}标题`, trigger: 'blur' }]"
            >
              <a-input v-model:value="localForm[tab.key].title" placeholder="请输入标题" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              :label="`${tab.label}内容`"
              :name="[tab.key, 'content']"
              :rules="[
                { required: true, message: `请输入${tab.label}内容`, trigger: 'blur' },
                { min: 10, message: '内容长度不能少于10字符', trigger: 'blur' }
              ]"
            >
              <editor v-model="localForm[tab.key].content" :min-height="200" />
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
const notificationFormRef = ref()
const activeTab = ref("gift")

const tabs = [
  { key: "gift", label: "赠送" },
  { key: "deduction", label: "扣款" },
  { key: "recharge", label: "充值" },
  { key: "withdrawing", label: "提现中" },
  { key: "withdrawalUnfreeze", label: "提现解冻" },
  { key: "withdrawal", label: "提现" },
  { key: "task", label: "任务" },
  { key: "principalReturn", label: "本金返还" },
  { key: "rebate", label: "返佣" },
  { key: "subRebate", label: "下级返佣" },
  { key: "signIn", label: "签到" },
  { key: "fee", label: "手续费" },
  { key: "deposit", label: "存款" },
  { key: "bonus", label: "奖金" },
  { key: "baseSalary", label: "底薪" },
  { key: "aid", label: "援助金" },
  { key: "registerBonus", label: "注册赠送" },
  { key: "productShare", label: "商品分润" },
  { key: "taskReward", label: "任务奖励" },
  { key: "balanceOut", label: "余额宝转出" },
  { key: "balanceIn", label: "余额宝转入" },
  { key: "workBonus", label: "工作奖金" },
  { key: "upgradeBonus", label: "升级奖金" },
  { key: "other", label: "其他" }
]

const localForm = reactive({})
tabs.forEach(tab => {
  localForm[tab.key] = {
    enabled: 1,
    title: "",
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
      tabs.forEach(tab => {
        Object.assign(localForm[tab.key], parsed[tab.key] || {})
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
    const data = {}
    tabs.forEach(tab => {
      data[tab.key] = { ...localForm[tab.key] }
    })
    emit("update:form", {
      ...props.form,
      content: JSON.stringify(data)
    })
  },
  { deep: true }
)

function handleSubmit() {
  notificationFormRef.value?.validate?.().then(() => {
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
