<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="登录日志列表" row-key="infoId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 1500 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="用户名"><a-input v-model:value="query.userName" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="IP"><a-input v-model:value="query.ipaddr" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="是否成功"><a-select v-model:value="query.status" allow-clear placeholder="请选择"><a-select-option value="0">是</a-select-option><a-select-option value="1">否</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD HH:mm:ss" show-time /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="reset">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar><a-button danger :disabled="!selected.length" v-hasPermi="['system:login-log:remove','monitor:logininfor:remove']" @click="remove"><DeleteOutlined />删除</a-button></template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'success'">{{ record.status === '0' ? '是' : '否' }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.loginTime) }}</template>
        <template v-else-if="column.key === 'headers'"><json-preview :value="record.requestHeaders" /></template>
        <template v-else-if="column.key === 'params'"><json-preview :value="record.requestParams" /></template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Logininfor">
import { delLogininfor, list } from '@/api/monitor/logininfor'
import { DeleteOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const queryExpanded = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, userName: undefined, ipaddr: undefined, status: undefined, orderByColumn: 'loginTime', isAsc: 'descending' })
const columns = [
  { title: 'ID', dataIndex: 'infoId', width: 90, fixed: 'left' }, { title: '用户名', dataIndex: 'userName', width: 150 }, { title: 'IP', dataIndex: 'ipaddr', width: 150 },
  { title: '地址', dataIndex: 'loginLocation', width: 180 }, { title: '是否成功', dataIndex: 'status', key: 'success', width: 110 }, { title: '创建时间', dataIndex: 'loginTime', key: 'time', width: 180 },
  { title: '请求头', key: 'headers', width: 300 }, { title: '参数', key: 'params', width: 300 }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await list(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function reset() { Object.assign(query, { pageNum: 1, userName: undefined, ipaddr: undefined, status: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除登录日志 ${ids} 吗？`); await delLogininfor(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>
