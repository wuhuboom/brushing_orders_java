<template>
  <div class="app-container ant-pro-member-page site-management-alignment-page customer-service-page">
    <ant-pro-table
      title="客服列表"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1402, y: 'calc(100vh - 440px)' }"
      @page-change="handlePageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form
          layout="horizontal"
          :model="query"
          class="ant-pro-query-form customer-query-form"
        >
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xl="6" class="customer-query-col">
              <a-form-item label="名称">
                <a-input v-model:value="query.name" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xl="6" class="customer-query-col">
              <a-form-item label="是否启用">
                <a-select v-model:value="query.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in enabledOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col
              v-show="!compactQuery || expanded"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="8"
              :xl="6"
              class="customer-query-col customer-created-time"
            >
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" :xl="6" class="ant-pro-query-actions customer-query-col">
              <a-space>
                <a-button @click="resetQuery">重置</a-button>
                <a-button type="primary" @click="handleQuery">查询</a-button>
                <a-button v-if="compactQuery" type="link" @click="expanded = !expanded">
                  {{ expanded ? "收起" : "展开" }}
                  <UpOutlined v-if="expanded" />
                  <DownOutlined v-else />
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #toolbar>
        <a-popconfirm
          title="删除选中的记录？"
          ok-text="确定"
          cancel-text="取消"
          :disabled="!selectedIds.length"
          @confirm="handleDelete"
        >
          <a-button type="primary" :disabled="!selectedIds.length" v-hasPermi="['member:cusservice:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:cusservice:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          <span>{{ record.name || "-" }}</span>
        </template>
        <template v-else-if="column.key === 'image'">
          <image-preview v-if="record.image" :src="record.image" :width="32" :height="32" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-badge :status="String(record.isEnabled) === '1' ? 'success' : 'error'" :text="enabledText(record.isEnabled)" />
        </template>
        <template v-else-if="column.key === 'link'">
          <div class="customer-service-link-cell">
            <span class="customer-service-link">{{ record.link || "-" }}</span>
            <a-tooltip title="复制">
              <a-button v-if="record.link" type="text" size="small" aria-label="复制链接" @click="copyLink(record.link)">
                <CopyOutlined />
              </a-button>
            </a-tooltip>
          </div>
        </template>
        <template v-else-if="column.key === 'createBy'">{{ record.createBy || "-" }}</template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'remarks'">{{ record.remarks || "-" }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:cusservice:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:cusservice:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-drawer
      v-model:open="open"
      :title="title"
      width="60%"
      size="large"
      :destroy-on-close="true"
      root-class-name="customer-service-drawer"
      @close="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large" class="customer-service-form">
        <a-row :gutter="[24, 0]">
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" placeholder="名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :precision="0" placeholder="序号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="图片" name="image">
              <image-upload v-model="form.image" :limit="1" :is-show-tip="false" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="是否启用" name="isEnabled">
              <a-radio-group v-model:value="form.isEnabled">
                <a-radio v-for="item in enabledOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="链接" name="link">
              <a-input v-model:value="form.link" placeholder="链接" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remarks">
              <a-textarea v-model:value="form.remarks" :rows="3" placeholder="备注" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="customer-service-drawer-footer">
          <a-space>
            <a-button size="large" @click="cancel">取 消</a-button>
            <a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup name="Cusservice">
import { CopyOutlined, DeleteOutlined, DownOutlined, PlusOutlined, UpOutlined } from "@ant-design/icons-vue";
import { addCusservice, delCusservice, getCusservice, listCusservice, updateCusservice } from "@/api/member/cusservice";
import ImagePreview from "@/components/ImagePreview/index.vue";
import ImageUpload from "@/components/ImageUpload/index.vue";

const { proxy } = getCurrentInstance();
const enabledOptions = [
  { label: "禁用", value: "0" },
  { label: "启用", value: "1" },
];

const rows = ref([]);
const loading = ref(false);
const submitting = ref(false);
const open = ref(false);
const title = ref("");
const expanded = ref(false);
const compactQuery = ref(typeof window !== "undefined" && window.innerWidth <= 1366);
const total = ref(0);
const selectedIds = ref([]);
const createTimeRange = ref([]);
const formRef = ref();
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  name: undefined,
  isEnabled: undefined,
  orderByColumn: "sortOrder",
  isAsc: "asc",
});
const form = reactive(emptyForm());

const columns = [
  { title: "名称", dataIndex: "name", key: "name", width: 200, fixed: "left" },
  { title: "序号", dataIndex: "sortOrder", key: "sortOrder", width: 100, sorter: true, defaultSortOrder: "ascend" },
  { title: "图片", dataIndex: "image", key: "image", width: 60 },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 100, sorter: true },
  { title: "链接", dataIndex: "link", key: "link", width: 300 },
  { title: "创建人", dataIndex: "createBy", key: "createBy", width: 160, ellipsis: true },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170, sorter: true },
  { title: "备注", dataIndex: "remarks", key: "remarks", width: 200, ellipsis: true },
  { title: "操作", key: "operation", width: 80, fixed: "right" },
];
const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: false,
  showQuickJumper: false,
  size: "small",
}));
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  columnWidth: 32,
  fixed: "left",
  onChange: (keys) => { selectedIds.value = keys; },
}));
const rules = {
  name: [{ required: true, message: "名称是必填项！", trigger: "blur" }],
  sortOrder: [{ required: true, message: "序号是必填项！", trigger: "change" }],
  image: [{ required: true, message: "图片是必填项！", trigger: "change" }],
  isEnabled: [{ required: true, message: "是否启用是必填项！", trigger: "change" }],
  link: [{ required: true, message: "链接是必填项！", trigger: "blur" }],
};

function emptyForm() {
  return { id: undefined, name: undefined, sortOrder: undefined, image: undefined, isEnabled: "1", link: undefined, remarks: undefined };
}
function queryPayload() {
  const payload = { ...query };
  if (createTimeRange.value?.length === 2) {
    payload.params = {
      beginTime: `${createTimeRange.value[0]} 00:00:00`,
      endTime: `${createTimeRange.value[1]} 23:59:59`,
    };
  }
  return payload;
}
async function getList() {
  loading.value = true;
  try {
    const response = await listCusservice(queryPayload());
    rows.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}
function enabledText(value) {
  return enabledOptions.find((item) => item.value === String(value))?.label ?? "-";
}
function formatDateTime(value) {
  return value ? proxy.parseTime(value) : "-";
}
function syncCompactQuery() {
  compactQuery.value = window.innerWidth <= 1366;
}
async function copyLink(value) {
  try {
    await navigator.clipboard.writeText(value);
    proxy.$modal.msgSuccess("复制成功");
  } catch {
    proxy.$modal.msgError("复制失败");
  }
}
function handlePageChange({ page, pageSize }) {
  query.pageNum = page;
  query.pageSize = pageSize;
  getList();
}
function handleQuery() {
  query.pageNum = 1;
  getList();
}
const sortColumnMap = {
  sortOrder: "sortOrder",
  enabled: "isEnabled",
  createTime: "createTime",
};
function handleTableChange(_pagination, _filters, sorter) {
  query.orderByColumn = sorter?.order ? sortColumnMap[sorter.columnKey] || undefined : undefined;
  query.isAsc = sorter?.order === "ascend" ? "asc" : sorter?.order === "descend" ? "desc" : undefined;
  query.pageNum = 1;
  getList();
}
function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 20,
    name: undefined,
    isEnabled: undefined,
    orderByColumn: "sortOrder",
    isAsc: "asc",
  });
  createTimeRange.value = [];
  expanded.value = false;
  getList();
}
function resetForm() {
  Object.assign(form, emptyForm());
  nextTick(() => formRef.value?.clearValidate());
}
function handleAdd() {
  resetForm();
  title.value = "创建";
  open.value = true;
}
async function loadRecord(id) {
  const response = await getCusservice(id);
  return response.data || {};
}
async function handleUpdate(row) {
  resetForm();
  Object.assign(form, await loadRecord(row.id));
  title.value = "修改";
  open.value = true;
}
async function handleCopy(row) {
  resetForm();
  const data = { ...(await loadRecord(row.id)) };
  delete data.id;
  delete data.createBy;
  delete data.createTime;
  delete data.updateBy;
  delete data.updateTime;
  delete data.translations;
  delete data.translationsId;
  Object.assign(form, data);
  title.value = "创建";
  open.value = true;
}
function cancel() {
  open.value = false;
  resetForm();
}
async function submitForm() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    if (form.id) {
      await updateCusservice({ ...form });
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addCusservice({ ...form });
      proxy.$modal.msgSuccess("创建成功");
    }
    open.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
}
async function handleDelete() {
  await delCusservice(selectedIds.value.join(","));
  proxy.$modal.msgSuccess("删除成功");
  selectedIds.value = [];
  await getList();
}

onMounted(() => window.addEventListener("resize", syncCompactQuery));
onBeforeUnmount(() => window.removeEventListener("resize", syncCompactQuery));
getList();
</script>

<style scoped lang="scss">
:global(body:has(.site-management-alignment-page)::-webkit-scrollbar) {
  width: 15px;
}

:global(body:has(.site-management-alignment-page) .copyright) {
  display: none;
}

:global(body:has(.site-management-alignment-page) .app-main) {
  padding-bottom: 0 !important;
}

.customer-service-page {
  margin-top: 44px;
}

.customer-query-form :deep(.ant-picker) {
  height: 32px;
}

.customer-service-page :deep(.ant-table-body) {
  height: calc(100vh - 440px);
}

.customer-service-page :deep(.ant-table-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.customer-service-page :deep(.ant-table-thead > tr > th),
.customer-service-page :deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  font-size: 15px;
  line-height: 1.57143;
}

.customer-service-page :deep(.ant-pagination-item),
.customer-service-page :deep(.ant-pagination-prev),
.customer-service-page :deep(.ant-pagination-next) {
  min-width: 24px;
  height: 24px;
  line-height: 22px;
}

.customer-service-link-cell {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
}

.customer-service-link {
  flex: 1;
  min-width: 0;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-all;
}

.customer-service-link-cell :deep(.ant-btn) {
  flex: none;
}

.customer-service-form :deep(.ant-input-number) {
  width: 100%;
}

.customer-service-form :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
.customer-service-form :deep(.ant-upload.ant-upload-select-picture-card) {
  width: 102px;
  height: 102px;
}

.customer-service-form :deep(.ant-upload-list-picture-card) {
  height: 102px;
}

:global(.customer-service-drawer .ant-drawer-header) {
  height: 57px;
  min-height: 57px;
}

.customer-service-form :deep(textarea.ant-input) {
  padding: 7px 11px;
  font-size: 16px;
  line-height: 1.5715;
}

.customer-service-drawer-footer {
  text-align: right;
}

@media (min-width: 992px) and (max-width: 1366px) {
  .customer-query-col {
    flex: 0 0 33.333333%;
    max-width: 33.333333%;
  }
}

@media (max-width: 768px) {
  .customer-service-page {
    margin-top: 12px;
  }

  .customer-service-page :deep(.ant-table-body) {
    height: auto;
  }

  :global(.customer-service-drawer .ant-drawer-content-wrapper) {
    width: 100% !important;
  }
}
</style>
