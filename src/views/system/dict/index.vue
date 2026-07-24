<template>
  <div class="app-container">
    <a-form v-show="showSearch" ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form">
      <a-form-item label="字典名称" name="dictName">
        <a-input v-model:value="queryParams.dictName" placeholder="请输入字典名称" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="字典类型" name="dictType">
        <a-input v-model:value="queryParams.dictType" placeholder="请输入字典类型" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear>
          <a-select-option v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
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
      row-key="dictId"
      title="字典列表"
      :columns="dictColumns"
      :data-source="typeList"
      :loading="loading"
      :row-selection="rowSelection"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #toolbar>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['system:dict:add']">新增</a-button>
        <a-button :disabled="single" @click="handleUpdate" v-hasPermi="['system:dict:edit']">修改</a-button>
        <a-button danger :disabled="multiple" @click="handleDelete()" v-hasPermi="['system:dict:remove']">删除</a-button>
        <a-button @click="handleExport" v-hasPermi="['system:dict:export']">导出</a-button>
        <a-button danger @click="handleRefreshCache" v-hasPermi="['system:dict:remove']">刷新缓存</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'dictType'">
          <router-link :to="'/system/dict-data/index/' + record.dictId" class="link-type">
            {{ record.dictType }}
          </router-link>
        </template>
        <template v-else-if="column.key === 'status'">
          <dict-tag :options="sys_normal_disable" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ parseTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" size="small" @click="handleUpdate(record)" v-hasPermi="['system:dict:edit']">修改</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record)" v-hasPermi="['system:dict:remove']">删除</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal v-model:open="open" :title="title" width="500px" destroy-on-close @cancel="cancel">
      <a-form ref="dictRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" :wrapper-col="{ flex: 1 }">
        <a-form-item label="字典名称" name="dictName">
          <a-input v-model:value="form.dictName" placeholder="请输入字典名称" />
        </a-form-item>
        <a-form-item label="字典类型" name="dictType">
          <a-input v-model:value="form.dictType" placeholder="请输入字典类型" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio>
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

<script setup name="Dict">
import useDictStore from '@/store/modules/dict'
import { listType, getType, delType, addType, updateType, refreshCache } from "@/api/system/dict/type"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const typeList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const dictRef = ref(null)

const dictColumns = [
  { title: "字典编号", dataIndex: "dictId", width: 110 },
  { title: "字典名称", dataIndex: "dictName", width: 180 },
  { title: "字典类型", dataIndex: "dictType", key: "dictType", width: 220 },
  { title: "状态", dataIndex: "status", key: "status", width: 120 },
  { title: "备注", dataIndex: "remark", width: 180 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "operation", width: 150, fixed: "right" }
]

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  },
  rules: {
    dictName: [{ required: true, message: "字典名称不能为空", trigger: "blur" }],
    dictType: [{ required: true, message: "字典类型不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows)
}))

function getList() {
  loading.value = true
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    typeList.value = response.rows
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
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    status: "0",
    remark: undefined
  }
  dictRef.value?.clearValidate?.()
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

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加字典类型"
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.dictId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = pageSize
  getList()
}

function handleUpdate(row) {
  reset()
  const dictId = row.dictId || ids.value
  getType(dictId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改字典类型"
  })
}

function submitForm() {
  dictRef.value?.validate().then(() => {
    if (form.value.dictId != undefined) {
      updateType(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addType(form.value).then(() => {
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  }).catch(() => {})
}

function handleDelete(row = {}) {
  const dictIds = row.dictId || ids.value
  proxy.$modal.confirm(`是否确认删除字典编号为 "${dictIds}" 的数据项？`).then(function() {
    return delType(dictIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

function handleExport() {
  proxy.download("system/dict/type/export", {
    ...queryParams.value
  }, `dict_${new Date().getTime()}.xlsx`)
}

function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新成功")
    useDictStore().cleanDict()
  })
}

getList()
</script>
