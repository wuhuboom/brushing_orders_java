<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="文件列表" row-key="fileId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 1460 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="桶"><a-input v-model:value="query.bucket" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="类型"><a-input v-model:value="query.fileType" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="哈希"><a-input v-model:value="query.fileHash" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="内容类型"><a-input v-model:value="query.contentType" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="大小"><a-input-number v-model:value="query.fileSize" :min="0" style="width:100%" placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="reset">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar><a-button danger :disabled="!selected.length" v-hasPermi="['system:file:remove']" @click="remove"><DeleteOutlined />删除</a-button></template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'hash'">{{ record.fileHash || '-' }}</template>
        <template v-else-if="column.key === 'media'"><a v-if="record.fileUrl" :href="record.fileUrl" target="_blank" rel="noopener"><EyeOutlined /> 预览</a><span v-else>-</span></template>
        <template v-else-if="column.key === 'size'">{{ record.fileSize ?? '-' }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="SystemFile">
import { deleteFiles, listFiles } from '@/api/system/alignment'
import { DeleteOutlined, EyeOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const queryExpanded = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, bucket: undefined, fileType: undefined, fileHash: undefined, contentType: undefined, fileSize: undefined })
const columns = [
  { title: 'ID', dataIndex: 'fileId', width: 90, fixed: 'left' },
  { title: '桶', dataIndex: 'bucket', width: 110 },
  { title: '类型', dataIndex: 'fileType', width: 100 },
  { title: '哈希', dataIndex: 'fileHash', key: 'hash', width: 260 },
  { title: '内容类型', dataIndex: 'contentType', width: 190 },
  { title: '媒体', key: 'media', width: 90 },
  { title: '大小', dataIndex: 'fileSize', key: 'size', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 260, ellipsis: true }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listFiles(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function reset() { Object.assign(query, { pageNum: 1, bucket: undefined, fileType: undefined, fileHash: undefined, contentType: undefined, fileSize: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除文件 ${ids} 吗？存在引用的文件将被拒绝删除。`); await deleteFiles(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }

load()
</script>
