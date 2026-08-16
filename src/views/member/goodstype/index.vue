<template>
  <div class="app-container ant-pro-member-page category-page">
    <ant-pro-table
      title="类目列表"
      :columns="goodstypeColumns"
      :data-source="goodstypeList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{ x: 1450, y: 'calc(100vh - 440px)' }"
      :pagination="{ current: queryParams.pageNum, pageSize: queryParams.pageSize, total }"
      @page-change="handleAntPageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="标题">
                <a-input v-model:value="queryParams.title" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="是否启用">
                <a-select v-model:value="queryParams.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="dict in goods_enabled" :key="dict.value" :value="dict.value">{{ dict.label }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6">
              <a-form-item label="创建时间">
                <a-range-picker v-model:value="dateRange" :placeholder="['请选择', '请选择']" value-format="YYYY-MM-DD" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :xl="6" class="ant-pro-query-actions">
              <a-space><a-button @click="resetQuery">重置</a-button><a-button type="primary" @click="handleQuery">查询</a-button></a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>
      <template #toolbar>
        <a-button type="primary" :disabled="multiple" @click="handleDelete()" v-hasPermi="['member:goodstype:remove']">
          <template #icon><DeleteOutlined /></template>删除
        </a-button>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:goodstype:add']">
          <template #icon><PlusOutlined /></template>创建
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'id'"><span class="category-id">{{ record.id }}</span></template>
        <template v-else-if="column.key === 'title'">
          <span>{{ record.title || '-' }}</span>
          <a-button class="copy-title" type="link" aria-label="复制" @click="copyTitle(record.title)"><CopyOutlined /></a-button>
        </template>
        <template v-else-if="column.key === 'enabled'"><a-badge :status="isCategoryEnabled(record.isEnabled) ? 'success' : 'default'" :text="enabledText(record.isEnabled)" /></template>
        <template v-else-if="column.key === 'image'"><image-preview v-if="record.image" :src="record.image" :width="50" :height="50" /><span v-else>-</span></template>
        <template v-else-if="column.key === 'date'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'subTitle' || column.key === 'remarks'">{{ record[column.dataIndex] || '-' }}</template>
        <template v-else-if="column.key === 'operation'"><a-space><a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:goodstype:edit']">修改</a-button><a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:goodstype:add']">复制</a-button></a-space></template>
      </template>
    </ant-pro-table>

    <a-drawer
      :title="title"
      v-model:open="open"
      width="60%"
      :destroy-on-close="true"
      class="category-editor-drawer"
      @close="cancel"
    >
      <a-form ref="goodstypeRef" :model="form" :rules="rules" layout="vertical" size="large" class="category-editor-form">
        <a-row :gutter="24">
          <a-col :span="24"><a-form-item label="标题" name="title"><a-input v-model:value="form.title" placeholder="标题" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="是否启用" name="isEnabled"><a-radio-group v-model:value="form.isEnabled"><a-radio v-for="dict in editorEnabledOptions" :key="dict.value" :value="dict.value">{{ dict.label }}</a-radio></a-radio-group></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="序号" name="serialNumber"><a-input-number v-model:value="form.serialNumber" placeholder="序号" :min="0" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="图片" name="image"><image-upload v-model="form.image" :limit="1" :is-show-tip="false" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="二级标题" name="subTitle"><a-textarea v-model:value="form.subTitle" placeholder="二级标题" :rows="3" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注" name="remarks"><a-textarea v-model:value="form.remarks" placeholder="备注" :rows="3" /></a-form-item></a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="category-drawer-footer"><a-space><a-button size="large" @click="cancel">取 消</a-button><a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button></a-space></div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup name="Goodstype">
import {
  listGoodstype,
  getGoodstype,
  delGoodstype,
  addGoodstype,
  updateGoodstype,
} from "@/api/member/goodstype";
import { CopyOutlined, DeleteOutlined, PlusOutlined } from "@ant-design/icons-vue";

const { proxy } = getCurrentInstance();
const { goods_enabled } = proxy.useDict("goods_enabled");

const goodstypeList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");
const submitting = ref(false);
const goodstypeRef = ref();
const dateRange = ref([]);
const editorEnabledOptions = computed(() => {
  const options = unref(goods_enabled);
  if (!Array.isArray(options) || options.length === 0) {
    return [{ label: "禁用", value: "1" }, { label: "启用", value: "0" }];
  }
  return [...options].sort((left, right) => Number(right.value) - Number(left.value));
});

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    serialNumber: null,
    image: null,
    subTitle: null,
    remarks: null,
    orderByColumn: null,
    isAsc: null,
  },
  rules: {
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }],
    isEnabled: [
      { required: true, message: "是否启用不能为空", trigger: "change" },
    ],
    serialNumber: [
      { required: true, message: "序号不能为空", trigger: "blur" },
    ],
  },
});

const { queryParams, form, rules } = toRefs(data);

const goodstypeColumns = [
  { title: "ID", key: "id", dataIndex: "id", width: 90 },
  { title: "标题", key: "title", dataIndex: "title", width: 220 },
  { title: "是否启用", key: "enabled", dataIndex: "isEnabled", width: 120, sorter: true },
  { title: "序号", key: "serialNumber", dataIndex: "serialNumber", width: 100, sorter: true },
  { title: "图片", key: "image", dataIndex: "image", width: 100 },
  { title: "二级标题", key: "subTitle", dataIndex: "subTitle", width: 180 },
  { title: "创建时间", key: "date", dataIndex: "createTime", width: 190, sorter: true },
  { title: "备注", key: "remarks", dataIndex: "remarks", width: 160 },
  { title: "操作", key: "operation", width: 130, fixed: "right" },
];
const rowSelection = computed(() => ({ selectedRowKeys: ids.value, onChange: (_keys, rows) => handleSelectionChange(rows) }));
function handleAntPageChange({ page, pageSize }) { queryParams.value.pageNum = page; queryParams.value.pageSize = pageSize; getList(); }

const sortColumnMap = {
  enabled: "isEnabled",
  serialNumber: "serialNumber",
  date: "createTime",
};

function handleTableChange(_pagination, _filters, sorter) {
  queryParams.value.orderByColumn = sorter?.order ? sortColumnMap[sorter.columnKey] || null : null;
  queryParams.value.isAsc = sorter?.order === "ascend" ? "asc" : sorter?.order === "descend" ? "desc" : null;
  queryParams.value.pageNum = 1;
  getList();
}

function isCategoryEnabled(value) {
  return String(value) === "0";
}

function enabledText(value) {
  const options = Array.isArray(goods_enabled.value) ? goods_enabled.value : goods_enabled;
  return options?.find((item) => String(item.value) === String(value))?.label
    ?? (isCategoryEnabled(value) ? "启用" : "禁用");
}

function formatDateTime(value) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}/.test(value)) return value.replace("T", " ").slice(0, 19);
  const raw = Number(value);
  const date = new Date(Number.isFinite(raw) && raw < 1e12 ? raw * 1000 : value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())} ${pad(date.getUTCHours())}:${pad(date.getUTCMinutes())}:${pad(date.getUTCSeconds())}`;
}

async function copyTitle(value) {
  if (!value) return;
  await navigator.clipboard.writeText(value);
  proxy.$modal.msgSuccess("复制成功");
}

/** 查询类目管理列表 */
function getList() {
  loading.value = true;
  listGoodstype(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    goodstypeList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    title: null,
    isEnabled: "0",
    serialNumber: null,
    image: null,
    subTitle: null,
    remarks: null,
  };
  nextTick(() => goodstypeRef.value?.clearValidate?.());
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.title = null;
  queryParams.value.isEnabled = undefined;
  queryParams.value.orderByColumn = null;
  queryParams.value.isAsc = null;
  dateRange.value = [];
  handleQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "创建";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getGoodstype(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = "修改";
  });
}

function handleCopy(row) {
  const _id = row?.id || ids.value[0];
  getGoodstype(_id).then((response) => {
    const copied = { ...response.data };
    delete copied.id;
    delete copied.createTime;
    delete copied.updateTime;
    addGoodstype(copied).then(() => {
      proxy.$modal.msgSuccess("复制成功");
      getList();
    });
  });
}

/** 提交按钮 */
async function submitForm() {
  try {
    await goodstypeRef.value?.validate();
  } catch {
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id != null) {
      await updateGoodstype(form.value);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addGoodstype(form.value);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    getList();
  } finally {
    submitting.value = false;
  }
}

/** 删除按钮操作 */
function handleDelete(row = {}) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm('是否确认删除类目管理编号为"' + _ids + '"的数据项？')
    .then(function () {
      return delGoodstype(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download(
    "member/goodstype/export",
    {
      ...queryParams.value,
    },
    `goodstype_${new Date().getTime()}.xlsx`
  );
}

getList();
</script>

<style scoped lang="scss">
.category-page { margin-top: 44px; }
.category-page :deep(.ant-table-body) { height: calc(100vh - 440px); }
.category-page :deep(.ant-pagination-item),
.category-page :deep(.ant-pagination-prev),
.category-page :deep(.ant-pagination-next) { min-width: 24px; height: 24px; line-height: 22px; }
.category-id { color: #1677ff; }
.copy-title { height: 22px; margin-left: 4px; padding: 0 2px !important; font-size: 14px; }
.ant-pro-query-form :deep(.ant-select),
.ant-pro-query-form :deep(.ant-picker) { width: 100%; }
.category-editor-form :deep(.ant-input-number) { width: 100%; }
.category-editor-form :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
.category-editor-form :deep(.ant-upload.ant-upload-select-picture-card) { width: 102px; height: 102px; }
.category-drawer-footer { text-align: right; }

@media (max-width: 768px) {
  .category-page { margin-top: 12px; }
  .category-page :deep(.ant-table-body) { height: auto; }
  :global(.category-editor-drawer .ant-drawer-content-wrapper) { width: 100% !important; }
}
</style>
