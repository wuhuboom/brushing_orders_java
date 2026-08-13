<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="登录日志列表"
      :columns="columns"
      :data-source="loginLogList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      :scroll="{ x: 1540 }"
      @page-change="handlePageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item label="用户名">
                <a-input
                  v-model:value="queryParams.username"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item label="IP">
                <a-input
                  v-model:value="queryParams.ip"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="5">
              <a-form-item label="是否成功">
                <a-select v-model:value="queryParams.success" placeholder="请选择" allow-clear>
                  <a-select-option
                    v-for="option in yesNoOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
                <a-button
                  type="link"
                  class="ant-pro-expand-btn"
                  @click="advancedSearchVisible = !advancedSearchVisible"
                >
                  {{ advancedSearchVisible ? '收起' : '展开' }}
                  <UpOutlined v-if="advancedSearchVisible" />
                  <DownOutlined v-else />
                </a-button>
              </a-space>
            </a-col>
          </a-row>
          <a-row v-if="advancedSearchVisible" :gutter="[24, 16]" class="advanced-query-row">
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="queryParams.createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  allow-clear
                  class="full-width"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-popconfirm
          title="删除选中的记录？"
          ok-text="确 定"
          cancel-text="取 消"
          @confirm="confirmDelete"
        >
          <a-button
            danger
            :disabled="!selectedIds.length"
            v-hasPermi="['member:memberloginlog:remove']"
          >
            <DeleteOutlined />
            删除
          </a-button>
        </a-popconfirm>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          {{ record.username || '-' }}
        </template>
        <template v-else-if="column.key === 'success'">
          <dict-tag :options="yesNoOptions" :value="record.success" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ record.createTime ? parseTime(record.createTime) : '-' }}
        </template>
        <template v-else-if="column.key === 'requestHeaders'">
          <json-tree-cell :value="record.requestHeaders" />
        </template>
        <template v-else-if="column.key === 'loginParams'">
          <json-tree-cell :value="record.loginParams" />
        </template>
        <template v-else>
          {{ record[column.dataIndex] || '-' }}
        </template>
      </template>
    </ant-pro-table>
  </div>
</template>

<script setup name="Memberloginlog">
import { computed, getCurrentInstance, reactive, ref } from 'vue'
import { DeleteOutlined, DownOutlined, UpOutlined } from '@ant-design/icons-vue'
import { delMemberloginlog, listMemberloginlog } from '@/api/member/memberloginlog'
import JsonTreeCell from './JsonTreeCell.vue'

const { proxy } = getCurrentInstance()
const yesNoOptions = [
  { label: '否', value: '0' },
  { label: '是', value: '1' }
]

const loginLogList = ref([])
const loading = ref(true)
const total = ref(0)
const selectedIds = ref([])
const advancedSearchVisible = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  username: null,
  ip: null,
  success: null,
  createTimeRange: []
})

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 100 },
  { title: '用户名', dataIndex: 'username', key: 'username', width: 150 },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 190 },
  { title: '地址', dataIndex: 'address', key: 'address', width: 260 },
  { title: '是否成功', dataIndex: 'success', key: 'success', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '请求头', dataIndex: 'requestHeaders', key: 'requestHeaders', width: 320 },
  { title: '参数', dataIndex: 'loginParams', key: 'loginParams', width: 320 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (keys) => {
    selectedIds.value = keys
  }
}))

async function getList() {
  loading.value = true
  try {
    const [beginTime, endTime] = queryParams.createTimeRange || []
    const response = await listMemberloginlog({
      ...queryParams,
      createTimeRange: undefined,
      params: {
        beginTime: beginTime || undefined,
        endTime: endTime || undefined
      }
    })
    loginLogList.value = response.rows || []
    total.value = Number(response.total || 0)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  selectedIds.value = []
  getList()
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 20,
    username: null,
    ip: null,
    success: null,
    createTimeRange: []
  })
  selectedIds.value = []
  getList()
}

function handlePageChange({ page, pageSize }) {
  queryParams.pageNum = page
  queryParams.pageSize = pageSize
  selectedIds.value = []
  getList()
}

async function confirmDelete() {
  if (!selectedIds.value.length) return
  await delMemberloginlog(selectedIds.value)
  proxy.$modal.msgSuccess('操作成功')
  selectedIds.value = []
  await getList()
}

getList()
</script>

<style scoped>
.advanced-query-row {
  margin-top: 16px;
}

.full-width {
  width: 100%;
}
</style>
