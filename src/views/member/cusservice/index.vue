<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="客服列表"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1450 }"
      @page-change="handlePageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="query" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="名称">
                <a-input v-model:value="query.name" allow-clear placeholder="请输入" @pressEnter="handleQuery" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :md="8" :lg="6">
              <a-form-item label="是否启用">
                <a-select v-model:value="query.isEnabled" allow-clear placeholder="请选择">
                  <a-select-option v-for="item in enabledOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="16" :md="10" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createTimeRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col flex="auto" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重置</a-button>
                <a-button type="primary" @click="handleQuery">查询</a-button>
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
          <a-button danger :disabled="!selectedIds.length" v-hasPermi="['member:cusservice:remove']">
            <DeleteOutlined />删除
          </a-button>
        </a-popconfirm>
        <a-button type="primary" @click="handleAdd" v-hasPermi="['member:cusservice:add']">
          <PlusOutlined />创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'image'">
          <image-preview :src="record.image" :width="52" :height="52" />
        </template>
        <template v-else-if="column.key === 'enabled'">{{ enabledText(record.isEnabled) }}</template>
        <template v-else-if="column.key === 'link'">
          <a-space :size="4">
            <a-typography-text :ellipsis="{ tooltip: record.link }" style="max-width: 210px">
              {{ record.link || "-" }}
            </a-typography-text>
            <a-tooltip title="复制">
              <a-button v-if="record.link" type="text" size="small" aria-label="复制链接" @click="copyLink(record.link)">
                <CopyOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
        <template v-else-if="column.key === 'createTime'">{{ formatDateTime(record.createTime) }}</template>
        <template v-else-if="column.key === 'operation'">
          <a-space>
            <a-button type="link" @click="handleUpdate(record)" v-hasPermi="['member:cusservice:edit']">修改</a-button>
            <a-button type="link" @click="handleCopy(record)" v-hasPermi="['member:cusservice:add']">复制</a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-model:open="open"
      :title="title"
      width="680px"
      ok-text="确定"
      cancel-text="取消"
      :confirm-loading="submitting"
      :destroy-on-close="true"
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="form.name" placeholder="名称" />
        </a-form-item>
        <a-form-item label="序号" name="sortOrder">
          <a-input-number v-model:value="form.sortOrder" :precision="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="图片" name="image">
          <image-upload v-model="form.image" :limit="1" />
        </a-form-item>
        <a-form-item label="是否启用" name="isEnabled">
          <a-radio-group v-model:value="form.isEnabled">
            <a-radio v-for="item in enabledOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="链接" name="link">
          <a-input v-model:value="form.link" placeholder="请输入" />
        </a-form-item>
        <a-form-item label="备注" name="remarks">
          <a-textarea v-model:value="form.remarks" :rows="3" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup name="Cusservice">
import { CopyOutlined, DeleteOutlined, PlusOutlined } from "@ant-design/icons-vue";
import { addCusservice, delCusservice, getCusservice, listCusservice, updateCusservice } from "@/api/member/cusservice";
import ImagePreview from "@/components/ImagePreview/index.vue";
import ImageUpload from "@/components/ImageUpload/index.vue";

const { proxy } = getCurrentInstance();
const enabledOptions = [
  { label: "禁用", value: "1" },
  { label: "启用", value: "0" },
];

const rows = ref([]);
const loading = ref(false);
const submitting = ref(false);
const open = ref(false);
const title = ref("");
const total = ref(0);
const selectedIds = ref([]);
const createTimeRange = ref([]);
const formRef = ref();
const query = reactive({ pageNum: 1, pageSize: 20, name: undefined, isEnabled: undefined });
const form = reactive(emptyForm());

const columns = [
  { title: "名称", dataIndex: "name", width: 170 },
  { title: "序号", dataIndex: "sortOrder", width: 90 },
  { title: "图片", dataIndex: "image", key: "image", width: 100 },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 110 },
  { title: "链接", dataIndex: "link", key: "link", width: 260 },
  { title: "创建人", dataIndex: "createBy", width: 120 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "备注", dataIndex: "remarks", width: 180 },
  { title: "操作", key: "operation", width: 140, fixed: "right" },
];
const pagination = computed(() => ({
  current: query.pageNum,
  pageSize: query.pageSize,
  total: total.value,
  showSizeChanger: false,
  showQuickJumper: false,
}));
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
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
  return { id: undefined, name: undefined, sortOrder: undefined, image: undefined, isEnabled: "0", link: undefined, remarks: undefined };
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
function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 20, name: undefined, isEnabled: undefined });
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

getList();
</script>
