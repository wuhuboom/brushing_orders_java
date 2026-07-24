<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="职位列表"
      row-key="postId"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }"
      :scroll="{ x: 900 }"
      @page-change="pageChange"
      @refresh="load"
    >
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :span="8"><a-form-item label="名称"><a-input v-model:value="query.postName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
            <a-col :span="8"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
            <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="search">查询</a-button></a-space></a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:post:remove']" @click="remove()"><DeleteOutlined />删除</a-button>
        <a-button type="primary" v-hasPermi="['system:post:add']" @click="create"><PlusOutlined />创建</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" v-hasPermi="['system:post:edit']" @click="edit(record)">修改</a-button><a-button type="link" v-hasPermi="['system:post:add']" @click="copy(record)">复制</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="800px" wrap-class-name="legacy-form-modal" destroy-on-close @cancel="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="名称" name="postName"><a-input v-model:value="form.postName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="角色列表"><a-checkbox-group v-model:value="form.roleIds" class="legacy-checkbox-list"><a-checkbox v-for="item in roleOptions" :key="item.value" :value="item.value" :disabled="item.disabled">{{ item.label }}</a-checkbox></a-checkbox-group></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></template>
    </a-modal>
  </div>
</template>

<script setup name="SystemPosition">
import { addPost, delPost, getPost, listPost, updatePost } from '@/api/system/post'
import { listRole } from '@/api/system/role'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const roleOptions = ref([])
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const formRef = ref()
const query = reactive({ pageNum: 1, pageSize: 20, postName: undefined })
const form = reactive({ postId: undefined, postName: '', postCode: undefined, postSort: 0, status: '0', roleIds: [], remark: '' })
const rules = { postName: [{ required: true, message: '名称不能为空' }] }
const columns = [
  { title: 'ID', dataIndex: 'postId', width: 90 },
  { title: '名称', dataIndex: 'postName', width: 220 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 300, ellipsis: true },
  { title: '操作', key: 'operation', width: 180, fixed: 'right' }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listPost(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function resetQuery() { query.pageNum = 1; query.postName = undefined; dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function loadRoles() { const result = await listRole({ pageNum: 1, pageSize: 1000 }); roleOptions.value = (result.rows || []).map(item => ({ label: `${item.roleName}[${item.roleKey}]`, value: item.roleId, disabled: item.status === '1' })) }
function clear() { Object.assign(form, { postId: undefined, postName: '', postCode: undefined, postSort: 0, status: '0', roleIds: [], remark: '' }); formRef.value?.clearValidate?.() }
async function create() { clear(); await loadRoles(); title.value = '创建'; open.value = true }
async function fill(id, asCopy = false) { clear(); await loadRoles(); Object.assign(form, (await getPost(id)).data || {}); form.roleIds = form.roleIds || []; if (asCopy) { form.postId = undefined; form.postCode = undefined; form.postName = `${form.postName}-复制` }; title.value = asCopy ? '复制' : '修改'; open.value = true }
const edit = row => fill(row?.postId || selected.value[0])
const copy = row => fill(row?.postId || selected.value[0], true)
async function submit() { await formRef.value?.validate(); form.postId ? await updatePost(form) : await addPost(form); proxy.$modal.msgSuccess('保存成功'); close(); load() }
function close() { open.value = false; clear() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除职位 ${ids} 吗？`); await delPost(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>

<style scoped>
:global(.legacy-form-modal .ant-modal-body) { min-height: 262px; padding-top: 15px; }
.legacy-checkbox-list { display: flex; flex-wrap: wrap; gap: 12px 20px; width: 100%; }
.legacy-checkbox-list :deep(.ant-checkbox-wrapper) { margin-inline-start: 0; }
</style>
