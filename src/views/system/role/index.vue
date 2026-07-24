<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="角色列表" row-key="roleId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 1180 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="代码"><a-input v-model:value="query.roleKey" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="名称"><a-input v-model:value="query.roleName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否内置"><a-select v-model:value="query.isBuiltin" allow-clear placeholder="请选择"><a-select-option value="Y">是</a-select-option><a-select-option value="N">否</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:role:remove']" @click="remove"><DeleteOutlined />删除</a-button>
        <a-button type="primary" v-hasPermi="['system:role:add']" @click="create"><PlusOutlined />创建</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'builtin'">{{ record.isBuiltin === 'Y' ? '是' : '否' }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" v-hasPermi="['system:role:edit']" @click="edit(record)">修改</a-button><a-button type="link" v-hasPermi="['system:role:add']" @click="copy(record)">复制</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <a-drawer v-model:open="open" :title="title" width="70%" destroy-on-close :body-style="{ paddingBottom: '72px' }" @close="close">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="代码" name="roleKey"><a-input v-model:value="form.roleKey" placeholder="代码" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="名称" name="roleName"><a-input v-model:value="form.roleName" placeholder="名称" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="是否隐藏手机号码"><a-radio-group v-model:value="form.hidePhone"><a-radio value="N">否</a-radio><a-radio value="Y">是</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="是否内置"><a-radio-group v-model:value="form.isBuiltin" disabled><a-radio value="N">否</a-radio><a-radio value="Y">是</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注"><a-textarea v-model:value="form.remark" :rows="3" placeholder="备注" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="策略列表"><a-checkbox-group v-model:value="form.strategyIds" class="legacy-checkbox-list"><a-checkbox v-for="item in strategyOptions" :key="item.value" :value="item.value">{{ item.label }}</a-checkbox></a-checkbox-group></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="资源列表">
            <a-tree checkable check-strictly :tree-data="menuTree" :field-names="{ title: 'label', key: 'id', children: 'children' }" :checked-keys="checkedKeys" @check="onResourceCheck" />
          </a-form-item></a-col>
          <a-col :span="24"><a-form-item label="数据行权限">
            <a-table size="small" row-key="_key" :columns="ruleColumns" :data-source="form.dataRules" :pagination="false" bordered>
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.key === 'resource'"><a-select v-model:value="record.menuId" show-search :filter-option="filterOption" :options="resourceOptions" placeholder="请选择资源" style="width:100%" /></template>
                <template v-else-if="column.key === 'scope'"><a-select v-model:value="record.scopeType" :options="scopeOptions" style="width:100%" /></template>
                <template v-else-if="column.key === 'operation'"><a-button type="link" danger @click="form.dataRules.splice(index, 1)">删除</a-button></template>
              </template>
            </a-table>
            <a-button type="dashed" block class="add-rule" @click="addRule"><PlusOutlined />添加一行数据</a-button>
          </a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer><div class="legacy-drawer-footer"><a-space><a-button size="large" @click="close">取消</a-button><a-button type="primary" size="large" @click="submit">确定</a-button></a-space></div></template>
    </a-drawer>
  </div>
</template>

<script setup name="Role">
import { addRole, delRole, getRole, listRole, updateRole } from '@/api/system/role'
import { roleMenuTreeselect, treeselect } from '@/api/system/menu'
import { getRoleAlignment, listStrategies } from '@/api/system/alignment'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { decorateLegacyResourceTree } from '@/utils/legacyResourceTree'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const queryExpanded = ref(false)
const formRef = ref()
const menuTree = ref([])
const checkedKeys = ref([])
const strategyOptions = ref([])
const query = reactive({ pageNum: 1, pageSize: 20, roleKey: undefined, roleName: undefined, isBuiltin: undefined })
const form = reactive({ roleId: undefined, roleKey: '', roleName: '', roleSort: 0, status: '0', hidePhone: '', isBuiltin: 'N', remark: '', strategyIds: [], menuIds: [], dataRules: [], menuCheckStrictly: false })
const rules = { roleKey: [{ required: true, message: '代码不能为空' }], roleName: [{ required: true, message: '名称不能为空' }] }
const columns = [
  { title: 'ID', dataIndex: 'roleId', width: 90, fixed: 'left' },
  { title: '代码', dataIndex: 'roleKey', width: 190 },
  { title: '名称', dataIndex: 'roleName', width: 200 },
  { title: '是否内置', dataIndex: 'isBuiltin', key: 'builtin', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 300, ellipsis: true },
  { title: '操作', key: 'operation', width: 180, fixed: 'right' }
]
const ruleColumns = [{ title: '资源', key: 'resource', width: '55%' }, { title: '类型', key: 'scope', width: '30%' }, { title: '操作', key: 'operation', width: '15%' }]
const scopeOptions = [{ value: 'ALL', label: '全部' }, { value: 'DEPT_AND_CHILD', label: '组织及其子组织' }, { value: 'DEPT', label: '组织' }, { value: 'SELF', label: '本人' }]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))
const resourceOptions = computed(() => flattenOptions(menuTree.value))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listRole(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function resetQuery() { Object.assign(query, { pageNum: 1, roleKey: undefined, roleName: undefined, isBuiltin: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
function flattenOptions(nodes = [], prefix = '') { return nodes.flatMap(node => { const label = prefix ? `${prefix} / ${node.label}` : node.label; return [{ label, value: node.id }, ...flattenOptions(node.children || [], label)] }) }
function clear() { Object.assign(form, { roleId: undefined, roleKey: '', roleName: '', roleSort: 0, status: '0', hidePhone: '', isBuiltin: 'N', remark: '', strategyIds: [], menuIds: [], dataRules: [], menuCheckStrictly: false }); checkedKeys.value = []; formRef.value?.clearValidate?.() }
async function loadOptions(roleId) { const requests = [treeselect(), listStrategies({ pageNum: 1, pageSize: 1000 })]; if (roleId) requests.push(roleMenuTreeselect(roleId)); const [menus, strategies, roleMenus] = await Promise.all(requests); menuTree.value = decorateLegacyResourceTree(roleMenus?.menus || menus.data || []); strategyOptions.value = (strategies.rows || []).map(item => ({ label: `${item.strategyName}[${item.strategyCode}]`, value: item.strategyId })); checkedKeys.value = roleMenus?.checkedKeys || [] }
async function create() { clear(); await loadOptions(); title.value = '创建'; open.value = true }
async function fill(id, asCopy = false) { clear(); await loadOptions(id); const [detail, alignment] = await Promise.all([getRole(id), getRoleAlignment(id)]); Object.assign(form, detail.data || {}); form.strategyIds = detail.data?.strategyIds || alignment.data?.strategyIds || []; form.dataRules = (detail.data?.dataRules || alignment.data?.dataRules || []).map((item, index) => ({ ...item, _key: `${id}-${index}-${Date.now()}` })); if (asCopy) { form.roleId = undefined; form.roleKey = `${form.roleKey}_copy`; form.roleName = `${form.roleName}-复制`; form.isBuiltin = 'N' }; title.value = asCopy ? '复制' : '修改'; open.value = true }
const edit = row => fill(row.roleId)
const copy = row => fill(row.roleId, true)
function onResourceCheck(keys) { checkedKeys.value = Array.isArray(keys) ? keys : keys.checked || [] }
function addRule() { form.dataRules.push({ _key: `new-${Date.now()}-${form.dataRules.length}`, menuId: undefined, scopeType: 'SELF' }) }
function filterOption(input, option) { return String(option.label || '').toLowerCase().includes(String(input || '').toLowerCase()) }
async function submit() { await formRef.value?.validate(); form.menuIds = checkedKeys.value; const payload = { ...form, dataRules: form.dataRules.map(({ _key, ...item }) => item) }; form.roleId ? await updateRole(payload) : await addRole(payload); proxy.$modal.msgSuccess('保存成功'); close(); load() }
function close() { open.value = false; clear() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除角色 ${ids} 吗？`); await delRole(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>

<style scoped>
.legacy-checkbox-list { display: flex; flex-wrap: wrap; gap: 12px 20px; width: 100%; }
.legacy-checkbox-list :deep(.ant-checkbox-wrapper) { margin-inline-start: 0; }
.legacy-drawer-footer { display: flex; justify-content: flex-end; }
.ant-tree { padding: 4px 0; }
.add-rule { margin-top: 8px; }
</style>
