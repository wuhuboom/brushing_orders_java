<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table v-if="refreshTable" title="组织列表" row-key="deptId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="false" :default-expand-all-rows="true" :children-column-name="'children'" :scroll="{ x: 940 }" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="8"><a-form-item label="名称"><a-input v-model:value="query.deptName" allow-clear placeholder="请输入" @pressEnter="load" /></a-form-item></a-col>
          <a-col :span="8"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="load">查询</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:dept:remove']" @click="remove"><DeleteOutlined />删除</a-button>
        <a-button type="primary" v-hasPermi="['system:dept:add']" @click="create"><PlusOutlined />创建</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" v-hasPermi="['system:dept:edit']" @click="edit(record)">修改</a-button><a-button type="link" v-hasPermi="['system:dept:add']" @click="copy(record)">复制</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="800px" wrap-class-name="legacy-form-modal" destroy-on-close @cancel="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="24"><a-form-item label="父级ID" name="parentId"><a-tree-select v-model:value="form.parentId" :tree-data="parentOptions" :field-names="{ value: 'deptId', label: 'deptName', children: 'children' }" tree-default-expand-all style="width:100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="名称" name="deptName"><a-input v-model:value="form.deptName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></template>
    </a-modal>
  </div>
</template>

<script setup name="Dept">
import { addDept, delDept, getDept, listDept, updateDept } from '@/api/system/dept'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const open = ref(false)
const title = ref('')
const refreshTable = ref(true)
const formRef = ref()
const query = reactive({ deptName: undefined })
const form = reactive({ deptId: undefined, parentId: 0, deptName: '', orderNum: 0, status: '0', remark: '' })
const rules = { deptName: [{ required: true, message: '名称不能为空' }] }
const columns = [
  { title: 'ID', dataIndex: 'deptId', width: 90 },
  { title: '名称', dataIndex: 'deptName', width: 260 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 280, ellipsis: true },
  { title: '操作', key: 'operation', width: 180, fixed: 'right' }
]
const parentOptions = computed(() => [{ deptId: 0, deptName: '顶级组织', children: rows.value }])
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys }, checkStrictly: true }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listDept(params()); rows.value = proxy.handleTree(result.data || [], 'deptId') } finally { loading.value = false } }
function resetQuery() { query.deptName = undefined; dateRange.value = []; load() }
function clear() { Object.assign(form, { deptId: undefined, parentId: 0, deptName: '', orderNum: 0, status: '0', remark: '' }); formRef.value?.clearValidate?.() }
function create() { clear(); title.value = '创建'; open.value = true }
async function fill(id, asCopy = false) { clear(); Object.assign(form, (await getDept(id)).data || {}); if (asCopy) { form.deptId = undefined; form.deptName = `${form.deptName}-复制` }; title.value = asCopy ? '复制' : '修改'; open.value = true }
const edit = row => fill(row.deptId)
const copy = row => fill(row.deptId, true)
async function submit() { await formRef.value?.validate(); form.deptId ? await updateDept(form) : await addDept(form); proxy.$modal.msgSuccess('保存成功'); close(); load() }
function close() { open.value = false; clear() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除组织 ${ids} 吗？`); await delDept(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>

<style scoped>
:global(.legacy-form-modal .ant-modal-body) { min-height: 262px; padding-top: 15px; }
</style>
