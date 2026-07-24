<template>
  <a-form
    ref="balanceFormRef"
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
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="累计存储时长" name="cumulativeStorageDuration">
          <a-input-number
            v-model:value="localForm.cumulativeStorageDuration"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入时长（天）"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="级别">
          <ant-pro-table
            title="级别列表"
            :columns="levelColumns"
            :data-source="localForm.levels"
            row-key="__rowKey"
            :pagination="false"
            :tool-options="{ refresh: false }"
          >
            <template #toolbar>
              <a-button type="primary" @click="addRow">新增</a-button>
            </template>
            <template #bodyCell="{ column, record, index }">
              <a-input-number
                v-if="column.dataIndex === 'amount'"
                v-model:value="record.amount"
                :min="0"
                :precision="2"
                class="full-width"
                @blur="saveRow(index)"
              />
              <a-input-number
                v-else-if="column.dataIndex === 'dailyRate'"
                v-model:value="record.dailyRate"
                :min="0"
                :max="100"
                :precision="2"
                class="full-width"
                @blur="saveRow(index)"
              />
              <a-space v-else-if="column.dataIndex === 'action'">
                <a-button type="link" size="small" @click="saveRow(index)">保存</a-button>
                <a-button type="link" size="small" danger @click="deleteRow(index)">删除</a-button>
              </a-space>
            </template>
          </ant-pro-table>
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="规则" name="rules">
          <editor v-model="localForm.rules" :min-height="200" />
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
const balanceFormRef = ref()
const localForm = reactive({
  status: 1,
  cumulativeStorageDuration: 0,
  levels: [],
  rules: ""
})

const levelColumns = [
  { title: "金额", dataIndex: "amount", width: 260 },
  { title: "日利率 (%)", dataIndex: "dailyRate", width: 260 },
  { title: "操作", dataIndex: "action", width: 160, fixed: "right" }
]

const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  cumulativeStorageDuration: [{ required: true, message: "请输入累计存储时长", trigger: "blur" }],
  rules: [
    { required: true, message: "请输入规则内容", trigger: "blur" },
    { min: 10, message: "内容不少于10个字符", trigger: "blur" }
  ]
})

function addRow() {
  localForm.levels.push({ __rowKey: Date.now() + Math.random(), amount: "", dailyRate: "" })
}

function saveRow(index) {
  const row = localForm.levels[index]
  if (row.amount === "" || row.dailyRate === "") {
    message.warning("请填写完整金额和日利率")
    return
  }
  message.success("保存成功")
}

function deleteRow(index) {
  localForm.levels.splice(index, 1)
  message.success("删除成功")
}

watch(
  () => props.form.content,
  newContent => {
    if (!newContent) {
      return
    }
    try {
      const parsed = JSON.parse(newContent)
      localForm.status = parsed.status ?? 1
      localForm.cumulativeStorageDuration = parsed.cumulativeStorageDuration ?? 0
      localForm.levels = (parsed.levels || []).map((item, index) => ({
        ...item,
        __rowKey: item.__rowKey || `${index}-${Date.now()}`
      }))
      localForm.rules = parsed.rules || ""
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
        cumulativeStorageDuration: localForm.cumulativeStorageDuration,
        levels: localForm.levels.map(({ __rowKey, ...item }) => item),
        rules: localForm.rules
      })
    })
  },
  { deep: true }
)

function handleSubmit() {
  balanceFormRef.value?.validate?.().then(() => {
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
