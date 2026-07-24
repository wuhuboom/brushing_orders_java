<template>
  <div class="app-container">
    <a-form v-show="showSearch" ref="queryRef" :model="queryParams" layout="inline" class="ant-pro-search-form">
      <a-form-item label="字典名称" name="dictType">
        <a-select v-model:value="queryParams.dictType" class="ant-pro-search-control">
          <a-select-option v-for="item in typeOptions" :key="item.dictId" :value="item.dictType">
            {{ item.dictName }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="字典标签" name="dictLabel">
        <a-input v-model:value="queryParams.dictLabel" placeholder="请输入字典标签" allow-clear @pressEnter="handleQuery" />
      </a-form-item>
      <a-form-item label="状态" name="status">
        <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear>
          <a-select-option v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
            {{ dict.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item class="ant-pro-search-actions">
        <a-button @click="resetQuery">重置</a-button>
        <a-button type="primary" @click="handleQuery">查询</a-button>
      </a-form-item>
    </a-form>

    <ant-pro-table
      row-key="dictCode"
      title="字典数据"
      :columns="dataColumns"
      :data-source="dataList"
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
        <a-button @click="handleClose">关闭</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'dictLabel'">
          <span v-if="(record.listClass == '' || record.listClass == 'default') && (record.cssClass == '' || record.cssClass == null)">{{ record.dictLabel }}</span>
          <a-tag v-else :color="getTagColor(record.listClass)" :class="record.cssClass">{{ record.dictLabel }}</a-tag>
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

    <a-modal v-model:open="open" :title="title" width="520px" destroy-on-close @cancel="cancel">
      <a-form ref="dataRef" :model="form" :rules="rules" :label-col="{ style: { width: '80px' } }" :wrapper-col="{ flex: 1 }">
        <a-form-item label="字典类型" name="dictType">
          <a-input v-model:value="form.dictType" disabled />
        </a-form-item>
        <a-form-item label="数据标签" name="dictLabel">
          <a-input v-model:value="form.dictLabel" placeholder="请输入数据标签" />
        </a-form-item>
        <a-form-item label="数据键值" name="dictValue">
          <a-input v-model:value="form.dictValue" placeholder="请输入数据键值" />
        </a-form-item>
        <a-form-item label="样式属性" name="cssClass">
          <a-input v-model:value="form.cssClass" placeholder="请输入样式属性" />
        </a-form-item>
        <a-form-item label="显示排序" name="dictSort">
          <a-input-number v-model:value="form.dictSort" :min="0" class="dict-full-control" />
        </a-form-item>
        <a-form-item label="回显样式" name="listClass">
          <a-select v-model:value="form.listClass">
            <a-select-option v-for="item in listClassOptions" :key="item.value" :value="item.value">
              {{ item.label }}({{ item.value }})
            </a-select-option>
          </a-select>
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

<script setup name="Data">
import useDictStore from '@/store/modules/dict'
import { optionselect as getDictOptionselect, getType } from "@/api/system/dict/type"
import { listData, getData, delData, addData, updateData } from "@/api/system/dict/data"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const dataList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const defaultDictType = ref("")
const typeOptions = ref([])
const route = useRoute()
const dataRef = ref(null)
const listClassOptions = ref([
  { value: "default", label: "默认" },
  { value: "primary", label: "主要" },
  { value: "success", label: "成功" },
  { value: "info", label: "信息" },
  { value: "warning", label: "警告" },
  { value: "danger", label: "危险" }
])

const dataColumns = [
  { title: "字典编码", dataIndex: "dictCode", width: 110 },
  { title: "字典标签", dataIndex: "dictLabel", key: "dictLabel", width: 180 },
  { title: "字典键值", dataIndex: "dictValue", width: 140 },
  { title: "字典排序", dataIndex: "dictSort", width: 120 },
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
    dictType: undefined,
    dictLabel: undefined,
    status: undefined
  },
  rules: {
    dictLabel: [{ required: true, message: "数据标签不能为空", trigger: "blur" }],
    dictValue: [{ required: true, message: "数据键值不能为空", trigger: "blur" }],
    dictSort: [{ required: true, message: "数据顺序不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const rowSelection = computed(() => ({
  selectedRowKeys: ids.value,
  onChange: (_selectedRowKeys, selectedRows) => handleSelectionChange(selectedRows)
}))

function getTypes(dictId) {
  getType(dictId).then(response => {
    queryParams.value.dictType = response.data.dictType
    defaultDictType.value = response.data.dictType
    getList()
  })
}

function getTypeList() {
  getDictOptionselect().then(response => {
    typeOptions.value = response.data
  })
}

function getList() {
  loading.value = true
  listData(queryParams.value).then(response => {
    dataList.value = response.rows
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
    dictCode: undefined,
    dictLabel: undefined,
    dictValue: undefined,
    cssClass: undefined,
    listClass: "default",
    dictSort: 0,
    status: "0",
    remark: undefined
  }
  dataRef.value?.clearValidate?.()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function handleClose() {
  const obj = { path: "/system/dict" }
  proxy.$tab.closeOpenPage(obj)
}

function resetQuery() {
  proxy.resetForm("queryRef")
  queryParams.value.dictType = defaultDictType.value
  handleQuery()
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page
  queryParams.value.pageSize = pageSize
  getList()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "添加字典数据"
  form.value.dictType = queryParams.value.dictType
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.dictCode)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

function handleUpdate(row) {
  reset()
  const dictCode = row.dictCode || ids.value
  getData(dictCode).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改字典数据"
  })
}

function submitForm() {
  dataRef.value?.validate().then(() => {
    if (form.value.dictCode != undefined) {
      updateData(form.value).then(() => {
        useDictStore().removeDict(queryParams.value.dictType)
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
    } else {
      addData(form.value).then(() => {
        useDictStore().removeDict(queryParams.value.dictType)
        proxy.$modal.msgSuccess("新增成功")
        open.value = false
        getList()
      })
    }
  }).catch(() => {})
}

function handleDelete(row = {}) {
  const dictCodes = row.dictCode || ids.value
  proxy.$modal.confirm(`是否确认删除字典编码为 "${dictCodes}" 的数据项？`).then(function() {
    return delData(dictCodes)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
    useDictStore().removeDict(queryParams.value.dictType)
  }).catch(() => {})
}

function handleExport() {
  proxy.download("system/dict/data/export", {
    ...queryParams.value
  }, `dict_data_${new Date().getTime()}.xlsx`)
}

getTypes(route.params && route.params.dictId)
getTypeList()

function getTagColor(listClass) {
  const colorMap = {
    primary: "processing",
    success: "success",
    info: "default",
    warning: "warning",
    danger: "error"
  }
  return colorMap[listClass] || undefined
}
</script>

<style scoped>
.dict-full-control {
  width: 100%;
}
</style>
