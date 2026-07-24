<template>
  <div class="app-container">
    <a-form v-show="showSearch" ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form">
      <a-form-item label="参数名称" name="configName">
        <a-input
          v-model:value="queryParams.configName"
          placeholder="请输入参数名称"
          allow-clear
          @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="参数键名" name="configKey">
        <a-input
          v-model:value="queryParams.configKey"
          placeholder="请输入参数键名"
          allow-clear
          @pressEnter="handleQuery"
        />
      </a-form-item>
      <a-form-item label="系统内置" name="configType">
        <a-select v-model:value="queryParams.configType" placeholder="请选择系统内置" allow-clear>
          <a-select-option v-for="dict in sys_yes_no" :key="dict.value" :value="dict.value">
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="创建时间">
        <a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" />
      </a-form-item>
      <a-form-item class="ant-pro-search-actions">
        <a-button @click="resetQuery">重置</a-button>
        <a-button type="primary" @click="handleQuery">查询</a-button>
      </a-form-item>
    </a-form>

    <ant-pro-table
      row-key="configId"
      title="参数列表"
      :columns="configColumns"
      :data-source="configList"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['system:config:add']">新增</a-button>
        <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['system:config:edit']">修改</a-button>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['system:config:remove']">删除</a-button>
        <a-button @click="handleExport" v-hasPermi="['system:config:export']">导出</a-button>
        <a-button danger @click="handleRefreshCache" v-hasPermi="['system:config:remove']">刷新缓存</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'configType'">
          <dict-tag :options="sys_yes_no" :value="record.configType" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:config:edit']">修改</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record)" v-hasPermi="['system:config:remove']">删除</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="520px" destroy-on-close @cancel="cancel">
      <a-form ref="configRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" :wrapper-col="{ flex: 1 }">
        <a-form-item label="参数名称" name="configName">
          <a-input v-model:value="form.configName" placeholder="请输入参数名称" />
        </a-form-item>
        <a-form-item label="参数键名" name="configKey">
          <a-input v-model:value="form.configKey" placeholder="请输入参数键名" />
        </a-form-item>
        <a-form-item label="参数键值" name="configValue">
          <a-textarea v-model:value="form.configValue" placeholder="请输入参数键值" :rows="3" />
        </a-form-item>
        <a-form-item label="系统内置" name="configType">
          <a-radio-group v-model:value="form.configType">
            <a-radio v-for="dict in sys_yes_no" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="form.remark" placeholder="请输入内容" :rows="3" />
        </a-form-item>
      </a-form>
      <template #footer>
        <div class="modal-footer-actions">
          <a-space>
            <a-button type="primary" @click="submitForm">确定</a-button>
            <a-button @click="cancel">取消</a-button>
          </a-space>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup name="Config">
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from "@/api/system/config"

const { proxy } = getCurrentInstance()
const { sys_yes_no } = proxy.useDict("sys_yes_no")

const configList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const configRef = ref(null)

const configColumns = [
  { title: "参数主键", dataIndex: "configId", width: 110 },
  { title: "参数名称", dataIndex: "configName", width: 180 },
  { title: "参数键名", dataIndex: "configKey", width: 220 },
  { title: "参数键值", dataIndex: "configValue", width: 220 },
  { title: "系统内置", dataIndex: "configType", key: "configType", width: 120 },
  { title: "备注", dataIndex: "remark", width: 180 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "operation", width: 150, fixed: "right" }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  },
  rules: {
    configName: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
    configKey: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
    configValue: [{ required: true, message: "参数键值不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows)
}))

function getList() {
  loading.value = true
  listConfig(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    configList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    configId: undefined,
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
    configType: "Y",
    remark: undefined
  }
  configRef.value?.clearValidate?.()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.configId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = pageSize
  getList()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加参数"
}

function handleUpdate(row) {
  reset()
  const configId = row.configId || ids.value
  getConfig(configId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改参数"
  })
}

function submitForm() {
  configRef.value?.validate().then(() => {
    if (form.value.configId != undefined) {
      updateConfig(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addConfig(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  }).catch(() => {})
}

function handleDelete(row = {}) {
  const configIds = row.configId || ids.value
  proxy.$modal.confirm(`是否确认删除参数编号为 "${configIds}" 的数据项？`).then(function () {
    return delConfig(configIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("system/config/export", {
    ...queryParams.value
  }, `config_${new Date().getTime()}.xlsx`)
}

function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新缓存成功")
  })
}

getList()
</script>
