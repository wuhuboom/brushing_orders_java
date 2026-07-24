<template>
  <a-form
    ref="rewardFormRef"
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
        <a-form-item label="是否跨级领取" name="allowCrossLevel">
          <a-radio-group v-model:value="localForm.allowCrossLevel">
            <a-radio :value="1">是</a-radio>
            <a-radio :value="0">否</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>

      <a-col :span="12">
        <a-form-item label="是否允许任务中领取" name="allowInTask">
          <a-radio-group v-model:value="localForm.allowInTask">
            <a-radio :value="1">是</a-radio>
            <a-radio :value="0">否</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-col>
      <a-col :span="12">
        <a-form-item label="领取需要完成的任务组数" name="requiredTaskGroups">
          <a-input-number
            v-model:value="localForm.requiredTaskGroups"
            :min="0"
            :precision="0"
            class="full-width"
            placeholder="请输入任务组数"
          />
        </a-form-item>
      </a-col>

      <a-col :span="24">
        <a-form-item label="等级奖励列表">
          <ant-pro-table
            title="等级奖励列表"
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
                v-if="column.dataIndex === 'order'"
                v-model:value="record.order"
                :min="1"
                :precision="0"
                class="full-width"
                @blur="sortByOrder"
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

const rewardFormRef = ref()
const localForm = reactive({
  status: 1,
  allowCrossLevel: 0,
  allowInTask: 0,
  requiredTaskGroups: 0,
  levels: []
})

const levelColumns = [
  { title: "序号", dataIndex: "order", width: 220 },
  { title: "奖金", dataIndex: "bonus", width: 260 },
  { title: "操作", dataIndex: "action", width: 160, fixed: "right" }
]

const localRules = reactive({
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  allowCrossLevel: [{ required: true, message: "请选择是否跨级领取", trigger: "change" }],
  allowInTask: [{ required: true, message: "请选择是否允许任务中领取", trigger: "change" }],
  requiredTaskGroups: [
    { required: true, message: "请输入需要完成的任务组数", trigger: "blur" },
    { type: "number", min: 0, message: "任务组数不能小于0", trigger: "blur" }
  ]
})

function initFormData(content) {
  if (!content) {
    return
  }
  try {
    const parsed = JSON.parse(content)
    Object.assign(localForm, parsed)
    localForm.levels = (parsed.levels || []).map((item, index) => ({
      ...item,
      __rowKey: item.__rowKey || `${index}-${Date.now()}`,
      order: item.order ?? index + 1
    }))
  } catch (e) {
    message.error("解析配置失败")
  }
}

watch(
  () => props.form.content,
  initFormData,
  { immediate: true }
)

function updateParentForm() {
  emit("update:form", {
    ...props.form,
    content: JSON.stringify({
      ...localForm,
      levels: localForm.levels.map(({ __rowKey, ...item }) => item)
    })
  })
}

watch(localForm, updateParentForm, { deep: true })

function addRow() {
  const maxOrder = localForm.levels.length > 0
    ? Math.max(...localForm.levels.map(level => level.order || 0))
    : 0
  localForm.levels.push({ __rowKey: Date.now() + Math.random(), order: maxOrder + 1, bonus: 0 })
  message.success("添加成功")
}

function saveRow(index) {
  message.success(`第${index + 1}行数据已保存`)
  updateParentForm()
}

function deleteRow(index) {
  localForm.levels.splice(index, 1)
  message.success("删除成功")
}

function sortByOrder() {
  localForm.levels.sort((a, b) => (a.order || 0) - (b.order || 0))
}

function handleSubmit() {
  rewardFormRef.value?.validate?.().then(() => {
    emit("submit")
  }).catch(() => {
    message.error("表单验证失败，请检查输入")
  })
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
