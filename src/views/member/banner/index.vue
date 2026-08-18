<template>
  <div class="app-container ant-pro-member-page site-management-alignment-page banner-page">
    <ant-pro-table
      title="横幅列表"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{ x: 1282, y: 'calc(100vh - 440px)' }"
      :pagination="pagination"
      @page-change="handlePageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form layout="horizontal" :model="query" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="名称">
                <a-input v-model:value="query.name" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="类型">
                <a-select v-model:value="query.type" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in bannerTypeOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="expanded" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="是否启用">
                <a-select v-model:value="query.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in enabledOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="expanded" :xs="24" :sm="12" :md="8" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="8" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重置</a-button>
                <a-button type="primary" @click="handleQuery">查询</a-button>
                <a-button type="link" @click="expanded = !expanded">
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
          <a-button type="primary" :disabled="!selectedIds.length" v-hasPermi="['member:banner:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:banner:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-badge status="processing" :text="dictText(bannerTypeOptions, record.type)" />
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-badge :status="String(record.isEnabled) === '1' ? 'success' : 'error'" :text="enabledText(record.isEnabled)" />
        </template>
        <template v-else-if="column.key === 'media'">
          <media-preview v-if="record.media" :src="record.media" :width="52" :height="52" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'link'">{{ record.link || "-" }}</template>
        <template v-else-if="column.key === 'remarks'">{{ record.remarks || "-" }}</template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:banner:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:banner:add']">复制</a-button>
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
      root-class-name="banner-drawer"
      @close="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large" class="banner-form">
        <a-row :gutter="[24, 0]">
          <a-col :span="12">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="form.name" placeholder="名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型" name="type">
              <a-select v-model:value="form.type" placeholder="请选择">
                <a-select-option v-for="item in bannerTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
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
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :precision="0" placeholder="序号" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="链接" name="link">
              <a-input v-model:value="form.link" placeholder="链接" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="媒体" name="media">
              <media-upload v-model="form.media" :limit="1" :is-show-tip="false" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="备注" name="remarks">
              <a-textarea v-model:value="form.remarks" :rows="3" placeholder="备注" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="banner-drawer-footer">
          <a-space>
            <a-button size="large" @click="cancel">取 消</a-button>
            <a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup name="Banner">
import { DeleteOutlined, DownOutlined, PlusOutlined, UpOutlined } from "@ant-design/icons-vue";
import { addBanner, delBanner, getBanner, listBanner, updateBanner } from "@/api/member/banner";
import MediaPreview from "@/components/MediaPreview/index.vue";
import MediaUpload from "@/components/MediaUpload/index.vue";

const { proxy } = getCurrentInstance();
const bannerTypeOptions = [
  { label: "首页", value: "1" },
  { label: "任务", value: "2" },
  { label: "登录", value: "3" },
  { label: "合作伙伴", value: "4" },
];
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
const selectedIds = ref([]);
const createTimeRange = ref([]);
const formRef = ref();
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  name: undefined,
  type: undefined,
  isEnabled: undefined,
  orderByColumn: "sortOrder",
  isAsc: "asc",
});
const form = reactive(emptyForm());

const columns = [
  { title: "名称", dataIndex: "name", key: "name", width: 200, fixed: "left" },
  { title: "类型", dataIndex: "type", key: "type", width: 100, sorter: true },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 100, sorter: true },
  { title: "序号", dataIndex: "sortOrder", key: "sortOrder", width: 100, sorter: true, defaultSortOrder: "ascend" },
  { title: "媒体", dataIndex: "media", key: "media", width: 100 },
  { title: "链接", dataIndex: "link", key: "link", width: 200, ellipsis: true },
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
  type: [{ required: true, message: "类型是必填项！", trigger: "change" }],
  isEnabled: [{ required: true, message: "是否启用是必填项！", trigger: "change" }],
  sortOrder: [{ required: true, message: "序号是必填项！", trigger: "change" }],
  media: [{ required: true, message: "媒体是必填项！", trigger: "change" }],
};

function emptyForm() {
  return { id: undefined, name: undefined, type: undefined, isEnabled: "1", sortOrder: undefined, link: undefined, media: undefined, remarks: undefined };
}

function queryPayload() {
  const payload = {
    ...query,
    type: toBackendBannerType(query.type),
    isEnabled: invertEnabled(query.isEnabled),
  };
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
    const response = await listBanner(queryPayload());
    rows.value = (response.rows || []).map(fromApiRecord);
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}

function dictText(options, value) {
  return options.find((item) => String(item.value) === String(value))?.label ?? value ?? "-";
}
function enabledText(value) {
  return enabledOptions.find((item) => item.value === String(value))?.label ?? "-";
}
function formatDateTime(value) {
  return value ? proxy.parseTime(value) : "-";
}
function mapNumberBoundary(value, offset) {
  if (value === undefined || value === null || value === "") return undefined;
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? String(numericValue + offset) : value;
}
function toBackendBannerType(value) {
  return mapNumberBoundary(value, -1);
}
function toUiBannerType(value) {
  return mapNumberBoundary(value, 1);
}
function invertEnabled(value) {
  if (value === undefined || value === null || value === "") return undefined;
  if (String(value) === "0") return "1";
  if (String(value) === "1") return "0";
  return value;
}
function fromApiRecord(record = {}) {
  return {
    ...record,
    type: toUiBannerType(record.type),
    isEnabled: invertEnabled(record.isEnabled),
  };
}
function toApiRecord(record) {
  return {
    ...record,
    type: toBackendBannerType(record.type),
    isEnabled: invertEnabled(record.isEnabled),
  };
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
  type: "type",
  enabled: "isEnabled",
  sortOrder: "sortOrder",
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
    type: undefined,
    isEnabled: undefined,
    orderByColumn: "sortOrder",
    isAsc: "asc",
  });
  createTimeRange.value = [];
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
async function handleUpdate(row) {
  resetForm();
  const response = await getBanner(row.id);
  Object.assign(form, fromApiRecord(response.data || {}));
  title.value = "修改";
  open.value = true;
}
async function handleCopy(row) {
  resetForm();
  const response = await getBanner(row.id);
  const data = fromApiRecord(response.data || {});
  delete data.id;
  delete data.createTime;
  delete data.updateTime;
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
      await updateBanner(toApiRecord(form));
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addBanner(toApiRecord(form));
      proxy.$modal.msgSuccess("创建成功");
    }
    open.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
}
async function handleDelete() {
  await delBanner(selectedIds.value.join(","));
  proxy.$modal.msgSuccess("删除成功");
  selectedIds.value = [];
  await getList();
}

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

.banner-page {
  margin-top: 44px;
}

.banner-page :deep(.ant-table-body) {
  height: calc(100vh - 440px);
}

.banner-page :deep(.ant-table-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.banner-page :deep(.ant-table-thead > tr > th),
.banner-page :deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  font-size: 15px;
  line-height: 1.57143;
}

.banner-form :deep(.ant-input-number) {
  width: 100%;
}

.banner-form :deep(textarea.ant-input) {
  padding: 7px 11px;
  font-size: 16px;
  line-height: 1.5715;
}

.banner-form :deep(.ant-upload-list-picture-card .ant-upload-list-item-container),
.banner-form :deep(.ant-upload.ant-upload-select-picture-card) {
  width: 102px;
  height: 102px;
}

.banner-form :deep(.ant-upload-list-picture-card) {
  height: 102px;
}

.banner-form :deep(.component-upload-media .media-card),
.banner-form :deep(.component-upload-media .media-upload-card) {
  width: 102px;
  height: 102px;
}

:global(.banner-drawer .ant-drawer-header) {
  height: 57px;
  min-height: 57px;
}

.banner-drawer-footer {
  text-align: right;
}

@media (max-width: 768px) {
  .banner-page {
    margin-top: 12px;
  }

  :global(.banner-drawer .ant-drawer-content-wrapper) {
    width: 100% !important;
  }
}
</style>
