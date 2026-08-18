<template>
  <div class="app-container ant-pro-member-page site-management-alignment-page bulletin-page">
    <ant-pro-table
      title="公告列表"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1010, y: 'calc(100vh - 440px)' }"
      @page-change="handlePageChange"
      @refresh="getList"
      @change="handleTableChange"
    >
      <template #search>
        <a-form layout="horizontal" :model="query" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item label="标题">
                <a-input v-model:value="query.title" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item label="是否启用">
                <a-select v-model:value="query.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in enabledOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col v-if="expanded" :xs="24" :sm="12" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="8" class="ant-pro-query-actions bulletin-query-actions">
              <a-space>
                <a-button @click="resetQuery">重置</a-button>
                <a-button type="primary" @click="handleQuery">查询</a-button>
                <a-button type="link" class="bulletin-expand-button" @click="expanded = !expanded">
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
          <a-button type="primary" :disabled="!selectedIds.length" v-hasPermi="['member:bulletin:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:bulletin:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'enabled'">
          <a-badge :status="String(record.isEnabled) === '1' ? 'success' : 'error'" :text="enabledText(record.isEnabled)" />
        </template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:bulletin:edit']">修改</a-button>
            <a-button type="link" @click="handleI18n(record)" v-hasPermi="['member:bulletin:edit']">国际化</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:bulletin:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-drawer
      v-model:open="open"
      :title="title"
      width="70%"
      :destroy-on-close="true"
      root-class-name="bulletin-editor-drawer"
      @close="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" size="large" class="bulletin-editor-form">
        <a-row :gutter="24">
          <a-col :span="24">
            <a-form-item label="标题" name="title">
              <a-textarea v-model:value="form.title" placeholder="标题" :rows="3" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="序号" name="sortOrder">
              <a-input-number v-model:value="form.sortOrder" :precision="0" style="width: 100%" />
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
            <a-form-item label="内容" name="content">
              <editor v-model="form.content" :min-height="527" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <div class="bulletin-drawer-footer">
          <a-space>
            <a-button size="large" @click="cancel">取 消</a-button>
            <a-button type="primary" size="large" :loading="submitting" @click="submitForm">确 定</a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>

    <notice-translation-modal
      v-model="translationOpen"
      title="修改"
      :translations="translationForm"
      @submit="submitTranslations"
    />
  </div>
</template>

<script setup name="Bulletin">
import { DeleteOutlined, DownOutlined, PlusOutlined, UpOutlined } from "@ant-design/icons-vue";
import { addLegacy, delLegacy, getLegacy, listLegacy, updateLegacy } from "@/api/member/legacy";
import NoticeTranslationModal from "@/views/member/message/NoticeTranslationModal.vue";

const resource = "bulletins";
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
const total = ref(0);
const selectedIds = ref([]);
const expanded = ref(false);
const createTimeRange = ref([]);
const formRef = ref();
const translationOpen = ref(false);
const translationForm = ref({});
const translationRecord = ref();
const query = reactive({
  pageNum: 1,
  pageSize: 20,
  title: undefined,
  isEnabled: undefined,
  orderByColumn: undefined,
  isAsc: undefined,
});
const form = reactive(emptyForm());

const columns = [
  { title: "标题", dataIndex: "title", width: 500, fixed: "left" },
  { title: "序号", dataIndex: "sortOrder", key: "sortOrder", width: 100, sorter: true, defaultSortOrder: "ascend" },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 100, sorter: true },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170, sorter: true, defaultSortOrder: "descend" },
  { title: "操作", key: "operation", width: 140, fixed: "right" },
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
  onChange: (keys) => { selectedIds.value = keys; },
}));
const rules = {
  title: [{ required: true, message: "标题是必填项！", trigger: "blur" }],
  sortOrder: [{ required: true, message: "序号是必填项！", trigger: "change" }],
  isEnabled: [{ required: true, message: "是否启用是必填项！", trigger: "change" }],
  content: [{ required: true, message: "内容是必填项！", trigger: "change" }],
};

function emptyForm() {
  return { id: undefined, title: undefined, sortOrder: undefined, isEnabled: "1", content: "", i18nContent: undefined };
}
function flipEnabled(value) {
  if (value === undefined || value === null || value === "") return value;
  if (String(value) === "0") return "1";
  if (String(value) === "1") return "0";
  return String(value);
}
function fromApiRecord(record) {
  return record ? { ...record, isEnabled: flipEnabled(record.isEnabled) } : {};
}
function toApiRecord(record) {
  return record ? { ...record, isEnabled: flipEnabled(record.isEnabled) } : {};
}
function queryPayload() {
  const payload = { ...query };
  payload.isEnabled = flipEnabled(payload.isEnabled);
  if (createTimeRange.value?.length === 2) {
    payload.beginCreateTime = `${createTimeRange.value[0]} 00:00:00`;
    payload.endCreateTime = `${createTimeRange.value[1]} 23:59:59`;
  }
  return payload;
}
async function getList() {
  loading.value = true;
  try {
    const response = await listLegacy(resource, queryPayload());
    rows.value = (response.rows || []).map(fromApiRecord);
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
function handlePageChange({ page, pageSize }) {
  query.pageNum = page;
  query.pageSize = pageSize;
  getList();
}
function handleTableChange(_pagination, _filters, sorter) {
  const activeSorter = Array.isArray(sorter)
    ? sorter.find((item) => item?.order)
    : sorter;
  const sortColumnMap = {
    sortOrder: "sortOrder",
    enabled: "isEnabled",
    createTime: "createTime",
  };
  query.orderByColumn = activeSorter?.order ? sortColumnMap[activeSorter.columnKey] : undefined;
  if (!activeSorter?.order) {
    query.isAsc = undefined;
  } else if (activeSorter.columnKey === "enabled") {
    query.isAsc = activeSorter.order === "ascend" ? "desc" : "asc";
  } else {
    query.isAsc = activeSorter.order === "ascend" ? "asc" : "desc";
  }
  query.pageNum = 1;
  getList();
}
function handleQuery() {
  query.pageNum = 1;
  getList();
}
function resetQuery() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 20,
    title: undefined,
    isEnabled: undefined,
    orderByColumn: undefined,
    isAsc: undefined,
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
async function loadRecord(id) {
  const response = await getLegacy(resource, id);
  return fromApiRecord(response.data || {});
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
  delete data.createTime;
  delete data.updateTime;
  Object.assign(form, data);
  title.value = "创建";
  open.value = true;
}
function parseTranslations(value) {
  if (value && typeof value === "object") return { ...value };
  if (!value || typeof value !== "string") return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}
async function handleI18n(row) {
  const record = await loadRecord(row.id);
  translationRecord.value = record;
  translationForm.value = parseTranslations(record.i18nContent);
  translationOpen.value = true;
}
async function submitTranslations(translations) {
  if (!translationRecord.value?.id) return;
  await updateLegacy(resource, {
    ...toApiRecord(translationRecord.value),
    i18nContent: JSON.stringify(translations),
  });
  proxy.$modal.msgSuccess("修改成功");
  translationOpen.value = false;
  translationRecord.value = undefined;
  await getList();
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
      await updateLegacy(resource, toApiRecord(form));
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addLegacy(resource, toApiRecord(form));
      proxy.$modal.msgSuccess("创建成功");
    }
    open.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
}
async function handleDelete() {
  await delLegacy(resource, selectedIds.value.join(","));
  proxy.$modal.msgSuccess("删除成功");
  selectedIds.value = [];
  await getList();
}

getList();
</script>

<style scoped>
:global(body:has(.site-management-alignment-page)::-webkit-scrollbar) {
  width: 15px;
}

:global(body:has(.site-management-alignment-page) .copyright) {
  display: none;
}

:global(body:has(.site-management-alignment-page) .app-main) {
  padding-bottom: 0 !important;
}

.bulletin-page {
  margin-top: 44px;
}

.bulletin-page :deep(.ant-table-body) {
  height: calc(100vh - 440px);
}

.bulletin-page :deep(.ant-table-body::-webkit-scrollbar) {
  width: 15px;
  height: 15px;
}

.bulletin-query-actions {
  margin-left: auto;
}

.bulletin-expand-button {
  padding-inline: 4px;
}

.bulletin-page :deep(.ant-table-thead > tr > th),
.bulletin-page :deep(.ant-table-tbody > tr > td) {
  padding: 12px 8px;
  font-size: 15px;
  line-height: 1.57143;
}

.bulletin-page :deep(.ant-table-tbody > tr > td) {
  line-height: 1.57143;
}

:global(.bulletin-editor-drawer .ant-drawer-body) {
  padding: 24px;
  overflow: auto;
}

:global(.bulletin-editor-drawer .ant-drawer-header) {
  height: 57px;
  min-height: 57px;
}

:global(.bulletin-editor-drawer .ant-drawer-footer) {
  padding: 8px 16px;
}

.bulletin-editor-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.bulletin-editor-form :deep(textarea.ant-input) {
  padding: 7px 11px;
  font-size: 16px;
  line-height: 1.5715;
}

.bulletin-drawer-footer {
  text-align: right;
}
</style>
