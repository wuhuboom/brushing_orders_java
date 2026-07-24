<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table title="文件引用列表" row-key="referenceId" :columns="columns" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: query.pageNum, pageSize: query.pageSize, total }" :scroll="{ x: 1320 }" @page-change="pageChange" @refresh="load">
      <template #search>
        <a-form layout="horizontal" size="large" class="ant-pro-query-form"><a-row :gutter="[24, 16]" align="middle">
          <a-col :span="7"><a-form-item label="文件ID"><a-input-number v-model:value="query.fileId" :min="1" style="width:100%" placeholder="请输入" /></a-form-item></a-col>
          <a-col :span="7"><a-form-item label="名称"><a-input v-model:value="query.referenceName" allow-clear placeholder="请输入" @pressEnter="search" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="关联类型"><a-select v-model:value="query.referenceType" allow-clear :options="FILE_REFERENCE_KINDS" placeholder="请选择关联类型" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="关联ID"><a-input v-model:value="query.referenceTargetId" allow-clear placeholder="请输入" /></a-form-item></a-col>
          <a-col v-show="queryExpanded" :span="7"><a-form-item label="创建时间"><a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" /></a-form-item></a-col>
          <a-col flex="auto" class="ant-pro-query-actions"><a-space><a-button @click="reset">重置</a-button><a-button type="primary" @click="search">查询</a-button><a-button type="link" @click="queryExpanded = !queryExpanded">{{ queryExpanded ? '收起' : '展开' }}</a-button></a-space></a-col>
        </a-row></a-form>
      </template>
      <template #toolbar>
        <a-button danger :disabled="!selected.length" v-hasPermi="['system:file-reference:remove']" @click="remove"><DeleteOutlined />删除</a-button>
        <a-upload :show-upload-list="false" :custom-request="upload"><a-button type="primary" v-hasPermi="['system:file-reference:add']"><UploadOutlined />上传</a-button></a-upload>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'media'"><a v-if="record.fileUrl" :href="record.fileUrl" target="_blank" rel="noopener"><EyeOutlined /> 预览</a><span v-else>-</span></template>
        <template v-else-if="column.key === 'kind'">{{ fileReferenceKindLabel(record.referenceType) }}</template>
        <template v-else-if="column.key === 'time'">{{ parseTime(record.createTime) }}</template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="SystemFileReference">
import request from '@/utils/request'
import { FILE_REFERENCE_KINDS, fileReferenceKindLabel } from '@/constants/fileReference'
import { deleteFileReferences, listFileReferences } from '@/api/system/alignment'
import { DeleteOutlined, EyeOutlined, UploadOutlined } from '@ant-design/icons-vue'

const { proxy } = getCurrentInstance()
const rows = ref([])
const selected = ref([])
const dateRange = ref([])
const loading = ref(false)
const total = ref(0)
const queryExpanded = ref(false)
const query = reactive({ pageNum: 1, pageSize: 20, fileId: undefined, referenceName: undefined, referenceType: undefined, referenceTargetId: undefined })
const columns = [
  { title: 'ID', dataIndex: 'referenceId', width: 90, fixed: 'left' },
  { title: '文件ID', dataIndex: 'fileId', width: 100 },
  { title: '名称', dataIndex: 'referenceName', width: 220, ellipsis: true },
  { title: '媒体', key: 'media', width: 90 },
  { title: '关联类型', dataIndex: 'referenceType', key: 'kind', width: 140 },
  { title: '关联ID', dataIndex: 'referenceTargetId', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'time', width: 180 },
  { title: '备注', dataIndex: 'remark', width: 260, ellipsis: true }
]
const rowSelection = computed(() => ({ selectedRowKeys: selected.value, onChange: keys => { selected.value = keys } }))

function params() { const value = { ...query }; if (dateRange.value?.length) [value.beginTime, value.endTime] = dateRange.value; return value }
async function load() { loading.value = true; try { const result = await listFileReferences(params()); rows.value = result.rows || []; total.value = result.total || 0 } finally { loading.value = false } }
function search() { query.pageNum = 1; load() }
function reset() { Object.assign(query, { pageNum: 1, fileId: undefined, referenceName: undefined, referenceType: undefined, referenceTargetId: undefined }); dateRange.value = []; load() }
function pageChange({ page, pageSize }) { query.pageNum = page; query.pageSize = pageSize; load() }
async function remove() { const ids = selected.value; await proxy.$modal.confirm(`确认删除文件引用 ${ids} 吗？删除引用不会删除物理文件。`); await deleteFileReferences(ids); proxy.$modal.msgSuccess('删除成功'); selected.value = []; load() }
async function upload({ file, onSuccess, onError }) { const data = new FormData(); data.append('file', file); data.append('referenceType', 'ADMIN_UPLOAD'); try { const result = await request({ url: '/common/upload', method: 'post', data, headers: { 'Content-Type': 'multipart/form-data' } }); onSuccess?.(result); proxy.$modal.msgSuccess('上传成功'); load() } catch (error) { onError?.(error) } }

load()
</script>
