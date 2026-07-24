<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="操作日志列表" row-key="operId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 2800 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="资源"><a-input v-model:value="query.resourceCode" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="用户名"><a-input v-model:value="query.operName" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="IP"><a-input v-model:value="query.operIp" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="地址"><a-input v-model:value="query.operLocation" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="状态"><a-select v-model:value="query.status" allow-clear placeholder="请选择"><a-select-option :value="0">成功</a-select-option><a-select-option :value="1">失败</a-select-option></a-select></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD HH:mm:ss" show-time /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="方法"><a-input v-model:value="query.method" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="请求地址"><a-input v-model:value="query.operUrl" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="请求头"><a-input v-model:value="query.requestHeaders" allow-clear /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="查询"><a-input v-model:value="query.queryString" allow-clear /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="参数"><a-input v-model:value="query.requestParams" allow-clear /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="消息体"><a-input v-model:value="query.requestBody" allow-clear /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="reset">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar><a-button danger :disabled="!selected.length" v-hasPermi="['system:operation-log:remove','monitor:operlog:remove']" @click="remove"><DeleteOutlined />删除</a-button></template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">{{ Number(record.status) === 0 ? '成功' : '失败' }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.operTime) }}</template>
        <template v-else-if="column.key === 'method'">{{ record.requestMethod || record.method || '-' }}</template>
        <template v-else-if="column.key === 'headers'"><json-preview :value="record.requestHeaders" /></template>
        <template v-else-if="column.key === 'query'"><json-preview :value="record.queryString" /></template>
        <template v-else-if="column.key === 'params'"><json-preview :value="record.requestParams || record.operParam" /></template>
        <template v-else-if="column.key === 'body'"><json-preview :value="record.requestBody" /></template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Operlog">
import { delOperlog, list } from '@/api/monitor/operlog'
import { DeleteOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const queryExpanded = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, resourceCode: undefined, operName: undefined, operIp: undefined, operLocation: undefined, status: undefined, method: undefined, operUrl: undefined, requestHeaders: undefined, queryString: undefined, requestParams: undefined, requestBody: undefined })
const columns = [
  { title: 'ID', dataIndex: 'operId', width: 90, fixed: 'left' }, { title: '资源', dataIndex: 'resourceCode', width: 180 }, { title: '用户名', dataIndex: 'operName', width: 130 },
  { title: 'IP', dataIndex: 'operIp', width: 140 }, { title: '地址', dataIndex: 'operLocation', width: 170 }, { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'operTime', key: 'time', width: 180 }, { title: '耗时(毫秒)', dataIndex: 'costTime', width: 120 }, { title: '方法', key: 'method', width: 110 },
  { title: '请求地址', dataIndex: 'operUrl', width: 240, ellipsis: true }, { title: '请求头', key: 'headers', width: 250 }, { title: '查询', key: 'query', width: 250 },
  { title: '参数', key: 'params', width: 250 }, { title: '消息体', key: 'body', width: 250 }, { title: '消息', dataIndex: 'message', width: 220, ellipsis: true }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await list(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function reset() { Object.assign(query, { pageNum: 1, resourceCode: undefined, operName: undefined, operIp: undefined, operLocation: undefined, status: undefined, method: undefined, operUrl: undefined, requestHeaders: undefined, queryString: undefined, requestParams: undefined, requestBody: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除操作日志 ${ids} 吗？`); await delOperlog(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>
