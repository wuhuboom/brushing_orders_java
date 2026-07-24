<template>
  <a-form
    ref="workBonusFormRef"
    :model="localForm"
    :rules="localRules"
    layout="vertical"
    class="config-form"
  >
    <a-row :gutter="[20, 0]">
      <a-col :span="12">
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="localForm.status">
            <a-radio value="1">启用</a-radio>
            <a-radio value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="统计类型" name="statType">
          <a-radio-group v-model:value="localForm.statType">
            <a-radio value="day">日</a-radio>
            <a-radio value="week">周</a-radio>
            <a-radio value="month">月</a-radio>
            <a-radio value="year">年</a-radio>
            <a-radio value="all">全部</a-radio>
          </a-radio-group>
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
                v-else-if="column.dataIndex === 'bonus'"
                v-model:value="record.bonus"
                :min="0"
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
const workBonusFormRef = ref()
const localForm = reactive({ status: "0", statType: "day", levels: [] })

const levelColumns = [
  { title: "订单金额", dataIndex: "amount", width: 260 },
  { title: "奖金", dataIndex: "bonus", width: 260 },
  { title: "操作", dataIndex: "action", width: 160, fixed: "right" }
]

const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  statType: [{ required: true, message: "请选择统计类型", trigger: "change" }]
})

function addRow() {
  localForm.levels.push({ __rowKey: Date.now() + Math.random(), amount: "", bonus: "" })
}

function saveRow(index) {
  const row = localForm.levels[index]
  if (row.amount === "" || row.bonus === "") {
    message.warning("请填写完整订单金额和奖金")
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
      localForm.status = parsed.status || "1"
      localForm.statType = parsed.statType || "day"
      localForm.levels = (parsed.levels || []).map((item, index) => ({
        ...item,
        __rowKey: item.__rowKey || `${index}-${Date.now()}`
      }))
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
        statType: localForm.statType,
        levels: localForm.levels.map(({ __rowKey, ...item }) => item)
      })
    })
  },
  { deep: true }
)

function handleSubmit() {
  workBonusFormRef.value?.validate?.().then(() => {
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
