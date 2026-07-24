<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="策略列表" row-key="strategyId" :columns="columns" :data-source="rows" :loading="loading"
      :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }"
      :scroll="{ x: 1050 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form">
          <a-row :gutter="[24,16]" align="middle">
            <a-col :span="7"><a-form-item label="代码"><a-input v-model:value="query.strategyCode" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
            <a-col :span="7"><a-form-item label="名称"><a-input v-model:value="query.strategyName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
            <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
            <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:strategy:remove']" @click="remove()"><DeleteOutlined />删除</a-button>
        <a-button type="primary" v-hasPermi="['system:strategy:add']" @click="create"><PlusOutlined />创建</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'"><a-space>
          <a-button type="link" v-hasPermi="['system:strategy:edit']" @click="edit(record)">修改</a-button>
          <a-button type="link" v-hasPermi="['system:strategy:add']" @click="copy(record)">复制</a-button>
        </a-space></template>
      </template>
    </ant-pro-table>

    <a-drawer v-model:open="open" :title="title" width="800px" destroy-on-close :body-style="{ paddingBottom: '72px' }" @close="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="代码" name="strategyCode"><a-input v-model:value="form.strategyCode" placeholder="代码" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="名称" name="strategyName"><a-input v-model:value="form.strategyName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="资源列表"><a-tree checkable check-strictly :tree-data="menuTree" :field-names="{ title: 'label', key: 'id', children: 'children' }" :checked-keys="checkedKeys" @check="onCheck" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><div class="legacy-drawer-footer"><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></div></template>
    </a-drawer>
  </div>
</template>

<script setup name="SystemStrategy">
import { addStrategy, deleteStrategies, getStrategy, listStrategies, updateStrategy } from '@/api/system/alignment'
import { treeselect } from '@/api/system/menu'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { decorateLegacyResourceTree } from '@/utils/legacyResourceTree'

const { proxy } = getCurrentInstance()
const rows = ref([])
const loading = ref(false)
const total = ref(0)
const selected = ref([])
const dateRange = ref([])
const open = ref(false)
const title = ref('')
const formRef = ref()
const menuTree = ref([])
const checkedKeys = ref([])
const queryExpanded = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, strategyCode: undefined, strategyName: undefined })
const form = reactive({ strategyId: undefined, strategyCode: '', strategyName: '', remark: '', menuIds: [] })
const rules = { strategyCode: [{ required: true, message: '策略代码不能为空' }], strategyName: [{ required: true, message: '策略名称不能为空' }] }
const columns = [
  { title: 'ID', dataIndex: 'strategyId', width: 90, fixed: 'left' },
  { title: '代码', dataIndex: 'strategyCode', width: 210 },
  { title: '名称', dataIndex: 'strategyName', width: 200 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', ellipsis: true, width: 260 },
  { title: '操作', key: 'operation', width: 180, fixed: 'right' }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))
function requestParams() { const params = { ...query }; if (dateRange.value?.length) [params.beginTime, params.endTime] = dateRange.value; return params }
async function load() { loading.value = true; try { const res = await listStrategies(requestParams()); rows.value = res.rows || []; total.value = res.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function resetQuery() { Object.assign(query, { pageNum: 1, strategyCode: undefined, strategyName: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function loadTree() { if (!menuTree.value.length) menuTree.value = decorateLegacyResourceTree((await treeselect()).data || []) }
function resetForm() { Object.assign(form, { strategyId: undefined, strategyCode: '', strategyName: '', remark: '', menuIds: [] }); checkedKeys.value = []; formRef.value?.clearValidate?.() }
async function create() { resetForm(); await loadTree(); title.value = '创建'; open.value = true }
async function fill(id, asCopy = false) { resetForm(); await loadTree(); const data = (await getStrategy(id)).data || {}; Object.assign(form, data); checkedKeys.value = data.menuIds || []; if (asCopy) { form.strategyId = undefined; form.strategyCode = `${data.strategyCode}_copy`; form.strategyName = `${data.strategyName}-复制` }; title.value = asCopy ? '复制' : '修改'; open.value = true }
function edit(row) { return fill(row?.strategyId || selected.value[0]) }
function copy(row) { return fill(row?.strategyId || selected.value[0], true) }
function onCheck(keys) { checkedKeys.value = Array.isArray(keys) ? keys : keys.checked || [] }
async function submit() { await formRef.value?.validate(); form.menuIds = checkedKeys.value; form.strategyId ? await updateStrategy(form) : await addStrategy(form); proxy.$modal.msgSuccess('保存成功'); close(); load() }
function close() { open.value = false; resetForm() }
async function remove(row) { const ids = row?.strategyId || selected.value; await proxy.$modal.confirm(`确认删除策略 ${ids} 吗？`); await deleteStrategies(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>

<style scoped>.legacy-drawer-footer{display:flex;justify-content:flex-end}.ant-tree{padding:4px 0}</style>
