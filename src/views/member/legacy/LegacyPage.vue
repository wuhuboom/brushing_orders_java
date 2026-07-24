<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      :title="title"
      :columns="columns"
      :data-source="tableList"
      :loading="loading"
      row-key="__rowKey"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: scrollX }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col
              v-for="field in searchFields"
              :key="field.prop || field.minProp || field.startProp"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="field.lg || 6"
            >
              <a-form-item :label="field.label">
                <a-select
                  v-if="field.type === 'select'"
                  v-model:value="queryParams[field.prop]"
                  allow-clear
                  :placeholder="field.placeholder || `请选择${field.label}`"
                >
                  <a-select-option
                    v-for="option in dictOptions(field.dict, field.options)"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-select-option>
                </a-select>
                <a-range-picker
                  v-else-if="field.type === 'daterange'"
                  v-model:value="queryParams[field.prop]"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :show-time="field.showTime !== false"
                  class="full-width"
                />
                <a-space v-else-if="field.type === 'numberRange'" compact class="full-width">
                  <a-input-number
                    v-model:value="queryParams[field.minProp]"
                    :min="field.min"
                    :precision="field.precision"
                    placeholder="请输入"
                    class="range-number"
                  />
                  <a-input disabled value="~" class="range-divider" />
                  <a-input-number
                    v-model:value="queryParams[field.maxProp]"
                    :min="field.min"
                    :precision="field.precision"
                    placeholder="请输入"
                    class="range-number"
                  />
                </a-space>
                <a-input-number
                  v-else-if="field.type === 'number'"
                  v-model:value="queryParams[field.prop]"
                  :min="field.min"
                  :precision="field.precision"
                  class="full-width"
                />
                <a-input
                  v-else
                  v-model:value="queryParams[field.prop]"
                  allow-clear
                  :placeholder="field.placeholder || `请输入${field.label}`"
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template v-if="toolbar.length" #toolbar>
        <a-button
          v-if="hasToolbar('add')"
          type="primary"
          @click="handleAdd"
          v-hasPermi="[perms.add]"
        >创建</a-button>
        <a-button
          v-if="hasToolbar('edit')"
          :disabled="single"
          @click="handleUpdate()"
          v-hasPermi="[perms.edit]"
        >修改</a-button>
        <a-button
          v-if="hasToolbar('copy')"
          :disabled="single"
          @click="handleCopy()"
          v-hasPermi="[perms.add]"
        >复制</a-button>
        <a-button
          v-if="hasToolbar('i18n')"
          :disabled="single"
          @click="handleI18n()"
          v-hasPermi="[perms.edit]"
        >国际化</a-button>
        <a-button
          v-if="hasToolbar('show')"
          :disabled="multiple"
          @click="handleHidden('0')"
          v-hasPermi="[perms.edit]"
        >显示</a-button>
        <a-button
          v-if="hasToolbar('hide')"
          :disabled="multiple"
          @click="handleHidden('1')"
          v-hasPermi="[perms.edit]"
        >隐藏</a-button>
        <a-button
          v-if="hasToolbar('delete')"
          danger
          :disabled="multiple"
          @click="handleDelete()"
          v-hasPermi="[perms.remove]"
        >删除</a-button>
        <a-button
          v-if="hasToolbar('export')"
          @click="handleExport"
          v-hasPermi="[perms.export]"
        >导出</a-button>
        <slot name="toolbarExtra" :ids="ids" :single="single" :multiple="multiple" />
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dict || column.options">
          <dict-tag :options="dictOptions(column.dict, column.options)" :value="record[column.dataIndex]" />
        </template>
        <template v-else-if="column.type === 'image'">
          <media-preview :src="record[column.dataIndex]" :width="50" :height="50" />
        </template>
        <template v-else-if="column.type === 'date'">
          {{ parseTime(record[column.dataIndex]) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button
              v-if="dialogFields.length && (hasToolbar('edit') || allowRowEdit)"
              type="link"
              size="small"
              @click="handleUpdate(record)"
              v-hasPermi="[perms.edit]"
            >修改</a-button>
            <a-button
              v-if="dialogFields.length && hasToolbar('copy')"
              type="link"
              size="small"
              @click="handleCopy(record)"
              v-hasPermi="[perms.add]"
            >复制</a-button>
            <a-button
              v-if="hasToolbar('delete')"
              type="link"
              size="small"
              danger
              @click="handleDelete(record)"
              v-hasPermi="[perms.remove]"
            >删除</a-button>
            <slot name="rowActions" :record="record" />
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      :title="dialogTitle"
      v-model:open="open"
      width="760px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="legacyRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col v-for="field in dialogFields" :key="field.prop" :span="field.span || 12">
            <a-form-item :label="field.label" :name="field.prop">
              <a-select
                v-if="field.type === 'select'"
                v-model:value="form[field.prop]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                allow-clear
              >
                <a-select-option
                  v-for="option in dictOptions(field.dict, field.options)"
                  :key="option.value"
                  :value="option.value"
                >{{ option.label }}</a-select-option>
              </a-select>
              <media-upload
                v-else-if="field.type === 'image'"
                v-model="form[field.prop]"
                :limit="1"
              />
              <a-date-picker
                v-else-if="field.type === 'date'"
                v-model:value="form[field.prop]"
                value-format="YYYY-MM-DD"
                :placeholder="field.placeholder || `请选择${field.label}`"
                class="full-width"
              />
              <a-input-number
                v-else-if="field.type === 'number'"
                v-model:value="form[field.prop]"
                :precision="field.precision"
                :min="field.min"
                class="full-width"
              />
              <a-textarea
                v-else-if="field.type === 'textarea'"
                v-model:value="form[field.prop]"
                :rows="field.rows || 3"
                :placeholder="field.placeholder || `请输入${field.label}`"
              />
              <editor
                v-else-if="field.type === 'editor'"
                v-model="form[field.prop]"
                :min-height="field.minHeight || 180"
              />
              <a-input
                v-else
                v-model:value="form[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, reactive, ref, toRefs, unref } from 'vue'
import { addLegacy, delLegacy, getLegacy, listLegacy, updateLegacy, updateLegacyHidden } from '@/api/member/legacy'
import mediaPreview from '@/components/MediaPreview/index.vue'
import mediaUpload from '@/components/MediaUpload/index.vue'
import { expandDateRanges, selectionFlags } from '@/utils/management-rules'

const props = defineProps({
  title: { type: String, required: true },
  resource: { type: String, required: true },
  columns: { type: Array, required: true },
  searchFields: { type: Array, default: () => [] },
  dialogFields: { type: Array, default: () => [] },
  toolbar: { type: Array, default: () => [] },
  dicts: { type: Array, default: () => ['user_yes_no'] },
  perms: { type: Object, default: () => ({}) },
  exportPath: { type: String, default: '' },
  scrollX: { type: Number, default: 1600 },
  listRequest: { type: Function, default: null },
  getRequest: { type: Function, default: null },
  addRequest: { type: Function, default: null },
  updateRequest: { type: Function, default: null },
  deleteRequest: { type: Function, default: null },
  hiddenRequest: { type: Function, default: null },
  initialQuery: { type: Object, default: () => ({}) },
  allowRowEdit: { type: Boolean, default: false }
})

const { proxy } = getCurrentInstance()
const dictStore = props.dicts.length ? proxy.useDict(...props.dicts) : {}

const tableList = ref([])
const open = ref(false)
const loading = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const dialogTitle = ref('')
const submitting = ref(false)
const legacyRef = ref()

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10
  },
  rules: {}
})

const { queryParams, form, rules } = toRefs(data)

props.searchFields.forEach((field) => {
  if (field.prop) queryParams.value[field.prop] = null
  if (field.minProp) queryParams.value[field.minProp] = null
  if (field.maxProp) queryParams.value[field.maxProp] = null
})
Object.assign(queryParams.value, props.initialQuery)

props.dialogFields.forEach((field) => {
  if (field.required) {
    rules.value[field.prop] = [{ required: true, message: `${field.label}不能为空`, trigger: field.type === 'select' ? 'change' : 'blur' }]
  }
})

const selectableToolbar = ['edit', 'copy', 'i18n', 'show', 'hide', 'delete']
const rowSelection = computed(() => {
  const selectable = props.toolbar.some((item) => selectableToolbar.includes(item))
  if (!selectable) {
    return undefined
  }
  return {
    selectedRowKeys: ids.value,
    onChange: (_keys, rows) => handleSelectionChange(rows)
  }
})

function dictOptions(dict, options) {
  if (Array.isArray(options)) {
    return options
  }
  if (!dict) {
    return []
  }
  return unref(dictStore[dict]) || []
}

function hasToolbar(type) {
  return props.toolbar.includes(type)
}

function getList() {
  loading.value = true
  const request = props.listRequest || ((query) => listLegacy(props.resource, query))
  request(buildQueryParams()).then((response) => {
    tableList.value = (response.rows || []).map((row, index) => ({
      ...row,
      __rowKey: row.id ?? `${props.resource}_${index}_${row.statDate || row.username || ''}`
    }))
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  props.searchFields.forEach((field) => {
    if (field.prop) queryParams.value[field.prop] = null
    if (field.minProp) queryParams.value[field.minProp] = null
    if (field.maxProp) queryParams.value[field.maxProp] = null
  })
  handleQuery()
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = pageSize
  getList()
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id)
  const flags = selectionFlags(selection.length)
  single.value = flags.single
  multiple.value = flags.multiple
}

function reset() {
  const next = { id: null }
  props.dialogFields.forEach((field) => {
    next[field.prop] = field.defaultValue ?? null
  })
  form.value = next
  nextTick(() => legacyRef.value?.clearValidate?.())
}

function handleAdd() {
  reset()
  open.value = true
  dialogTitle.value = `创建${props.title}`
}

function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  const request = props.getRequest || ((value) => getLegacy(props.resource, value))
  request(id).then((response) => {
    form.value = response.data || {}
    open.value = true
    dialogTitle.value = `修改${props.title}`
  })
}

function handleCopy(row) {
  const id = row?.id || ids.value[0]
  const getRequest = props.getRequest || ((value) => getLegacy(props.resource, value))
  const addRequest = props.addRequest || ((data) => addLegacy(props.resource, data))
  getRequest(id).then((response) => {
    const copied = { ...(response.data || {}) }
    delete copied.id
    delete copied.createTime
    delete copied.updateTime
    addRequest(copied).then(() => {
      proxy.$modal.msgSuccess('复制成功')
      getList()
    })
  })
}

function handleI18n() {
  const id = ids.value[0]
  const request = props.getRequest || ((value) => getLegacy(props.resource, value))
  request(id).then((response) => {
    form.value = response.data || {}
    open.value = true
    dialogTitle.value = `${props.title}国际化`
  })
}

function handleHidden(isHidden) {
  const request = props.hiddenRequest || ((hidden, values) => updateLegacyHidden(props.resource, hidden, values))
  request(isHidden, ids.value).then(() => {
    proxy.$modal.msgSuccess('操作成功')
    getList()
  })
}

function cancel() {
  open.value = false
  reset()
}

async function submitForm() {
  try {
    await legacyRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const request = form.value.id != null
      ? (props.updateRequest || ((data) => updateLegacy(props.resource, data)))
      : (props.addRequest || ((data) => addLegacy(props.resource, data)))
    await request(form.value)
    proxy.$modal.msgSuccess('操作成功')
    open.value = false
    getList()
  } catch (_) {
    // 请求层已展示具体错误，这里阻止事件处理器产生未处理异常。
  } finally {
    submitting.value = false
  }
}

function handleDelete(row) {
  const id = row?.id || ids.value
  proxy.$modal.confirm(`是否确认删除${props.title}编号为 "${id}" 的数据项？`).then(() => {
    const request = props.deleteRequest || ((value) => delLegacy(props.resource, value))
    return request(id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function handleExport() {
  proxy.download(props.exportPath || `member/legacy/${props.resource}/export`, {
    ...queryParams.value
  }, `${props.resource}_${new Date().getTime()}.csv`)
}

function buildQueryParams() {
  return expandDateRanges(queryParams.value, props.searchFields)
}

defineExpose({ getList, handleQuery })

getList()
</script>

<style scoped>
.full-width {
  width: 100%;
}

.range-number {
  width: calc(50% - 18px);
}

.range-divider {
  width: 36px;
  text-align: center;
  pointer-events: none;
}
</style>
