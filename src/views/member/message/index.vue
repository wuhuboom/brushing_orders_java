<template>
  <div class="app-container ant-pro-member-page">
    <ant-pro-table
      title="站内信列表"
      :columns="messageColumns"
      :data-source="messageList"
      :loading="loading"
      row-key="id"
      :row-selection="rowSelection"
      :pagination="pagination"
      :scroll="{ x: 1080, y: 'calc(100vh - 430px)' }"
      @page-change="handlePageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams" class="ant-pro-query-form">
          <a-row :gutter="[24, 16]" align="middle">
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item label="标题">
                <a-input
                  v-model:value="queryParams.title"
                  placeholder="请输入"
                  allow-clear
                  @pressEnter="handleQuery"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="6">
              <a-form-item label="会员列表">
                <a-select
                  v-model:value="queryParams.memberList"
                  :options="userOptions"
                  placeholder="请选择"
                  show-search
                  allow-clear
                  :filter-option="false"
                  :loading="loadingUsers"
                  @search="handleUserSearch"
                  @focus="ensureUsersLoaded"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="5">
              <a-form-item label="是否启用">
                <a-select
                  v-model:value="queryParams.isEnabled"
                  :options="enabledOptions"
                  placeholder="请选择"
                  allow-clear
                />
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
                  style="width: 100%"
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
            v-hasPermi="['member:message:remove']"
          >
            <DeleteOutlined />
            删除
          </a-button>
        </a-popconfirm>
        <a-button
          type="primary"
          @click="handleAdd"
          v-hasPermi="['member:message:add']"
        >
          <PlusOutlined />
          创建
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <span>{{ record.title }}</span>
          <a-tooltip title="复制标题">
            <a-button
              type="text"
              size="small"
              class="copy-title-button"
              @click="copyTitle(record.title)"
            >
              <CopyOutlined />
            </a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'members'">
          {{ record.memberNames || record.memberList || "-" }}
        </template>
        <template v-else-if="column.key === 'enabled'">
          <a-tag :color="Number(record.isEnabled) === 1 ? 'success' : 'default'">
            {{ Number(record.isEnabled) === 1 ? "启用" : "禁用" }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              @click="handleUpdate(record)"
              v-hasPermi="['member:message:edit']"
            >
              修改
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="openTranslations(record)"
              v-hasPermi="['member:message:edit']"
            >
              国际化
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleCopy(record)"
              v-hasPermi="['member:message:add']"
            >
              复制
            </a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-modal
      v-model:open="open"
      :title="title"
      width="800px"
      ok-text="确 定"
      cancel-text="取 消"
      :confirm-loading="submitting"
      destroy-on-close
      @ok="submitForm"
      @cancel="cancel"
    >
      <a-form
        ref="messageRef"
        :model="form"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="标题" name="title">
          <a-input v-model:value="form.title" placeholder="标题" />
        </a-form-item>
        <a-form-item label="会员列表" name="memberList">
          <a-select
            v-model:value="form.memberList"
            :options="userOptions"
            placeholder="会员列表"
            mode="multiple"
            show-search
            allow-clear
            :filter-option="false"
            :loading="loadingUsers"
            @search="handleUserSearch"
            @focus="ensureUsersLoaded"
          />
        </a-form-item>
        <a-form-item label="是否启用" name="isEnabled">
          <a-radio-group v-model:value="form.isEnabled">
            <a-radio :value="0">禁用</a-radio>
            <a-radio :value="1">启用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="内容" name="content">
          <editor v-model="form.content" :min-height="220" />
        </a-form-item>
      </a-form>
    </a-modal>

    <notice-translation-modal
      v-model="translationOpen"
      :title="translationTitle"
      :translations="translationForm"
      @submit="submitTranslations"
    />
  </div>
</template>

<script setup name="Message">
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import {
  CopyOutlined,
  DeleteOutlined,
  DownOutlined,
  PlusOutlined,
  UpOutlined,
} from "@ant-design/icons-vue";
import {
  listMessage,
  getMessage,
  delMessage,
  addMessage,
  updateMessage,
} from "@/api/member/message";
import { listOrderuser } from "@/api/member/orderuser";
import NoticeTranslationModal from "./NoticeTranslationModal.vue";
import { createEmptyTranslations } from "@/views/member/components/translationLanguages";

const { proxy } = getCurrentInstance();
const messageList = ref([]);
const usersList = ref([]);
const loadingUsers = ref(false);
const loading = ref(false);
const open = ref(false);
const submitting = ref(false);
const title = ref("");
const total = ref(0);
const selectedIds = ref([]);
const messageRef = ref();
const translationOpen = ref(false);
const translationTitle = ref("");
const translationRow = ref(null);
const translationForm = ref(createEmptyTranslations());
const advancedSearchVisible = ref(false);

const enabledOptions = [
  { label: "禁用", value: 0 },
  { label: "启用", value: 1 },
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  title: undefined,
  memberList: undefined,
  isEnabled: undefined,
  createTimeRange: [],
});

const form = reactive(emptyForm());
const rules = {
  title: [{ required: true, message: "标题是必填项！", trigger: "blur" }],
  memberList: [
    {
      required: true,
      type: "array",
      message: "会员列表是必填项！",
      trigger: "change",
    },
  ],
  isEnabled: [{ required: true, message: "是否启用是必填项！", trigger: "change" }],
  content: [{ required: true, message: "内容是必填项！", trigger: "blur" }],
};

const messageColumns = [
  { title: "ID", dataIndex: "id", width: 90 },
  { title: "标题", dataIndex: "title", key: "title", width: 280 },
  { title: "会员列表", dataIndex: "memberList", key: "members", width: 300 },
  { title: "是否启用", dataIndex: "isEnabled", key: "enabled", width: 110 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "operation", width: 190, fixed: "right" },
];

const userOptions = computed(() =>
  usersList.value.map((user) => ({
    value: Number(user.id),
    label: user.username || String(user.id),
  }))
);
const pagination = computed(() => ({
  current: queryParams.pageNum,
  pageSize: queryParams.pageSize,
  total: total.value,
  showQuickJumper: true,
  pageSizeOptions: ["20", "50", "100"],
}));
const rowSelection = computed(() => ({
  selectedRowKeys: selectedIds.value,
  onChange: (keys) => {
    selectedIds.value = keys;
  },
}));

function emptyForm() {
  return {
    id: null,
    title: "",
    memberList: [],
    isEnabled: 1,
    content: "",
    translationsId: null,
    translations: null,
  };
}

function resetForm() {
  Object.assign(form, emptyForm());
  messageRef.value?.clearValidate?.();
}

async function loadUsers(keyword = "") {
  const normalizedKeyword = typeof keyword === "string" ? keyword.trim() : "";
  loadingUsers.value = true;
  try {
    const response = await listOrderuser({
      pageNum: 1,
      pageSize: 100,
      username: normalizedKeyword || undefined,
    });
    const selectedIds = [
      ...splitMemberIds(form.memberList),
      Number(queryParams.memberList),
    ].filter((id) => Number.isFinite(id) && id > 0);
    const selectedUsers = selectedIds.map((id) => {
      const current = usersList.value.find((user) => Number(user.id) === id);
      return current || { id, username: String(id) };
    });
    usersList.value = [...selectedUsers, ...(response.rows || [])].filter(
      (user, index, all) =>
        all.findIndex((item) => Number(item.id) === Number(user.id)) === index
    );
  } finally {
    loadingUsers.value = false;
  }
}

async function getList() {
  loading.value = true;
  try {
    const [beginTime, endTime] = queryParams.createTimeRange || [];
    const response = await listMessage({
      ...queryParams,
      createTimeRange: undefined,
      params: {
        beginTime: beginTime || undefined,
        endTime: endTime || undefined,
      },
    });
    messageList.value = response.rows || [];
    total.value = Number(response.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  selectedIds.value = [];
  getList();
}

function resetQuery() {
  Object.assign(queryParams, {
    pageNum: 1,
    pageSize: 20,
    title: undefined,
    memberList: undefined,
    isEnabled: undefined,
    createTimeRange: [],
  });
  selectedIds.value = [];
  getList();
}

function handlePageChange({ page, pageSize }) {
  queryParams.pageNum = page;
  queryParams.pageSize = pageSize;
  selectedIds.value = [];
  getList();
}

function handleAdd() {
  resetForm();
  title.value = "创建";
  open.value = true;
}

async function handleUpdate(row) {
  resetForm();
  const response = await getMessage(row.id);
  const memberIds = splitMemberIds(response.data.memberList);
  usersList.value = [
    ...memberIds.map((id) => ({
      id,
      username: String(id),
    })),
    ...usersList.value,
  ].filter(
    (user, index, all) =>
      all.findIndex((item) => Number(item.id) === Number(user.id)) === index
  );
  Object.assign(form, response.data, {
    memberList: memberIds,
  });
  title.value = "修改";
  open.value = true;
}

async function handleCopy(row) {
  resetForm();
  const response = await getMessage(row.id);
  const memberIds = splitMemberIds(response.data.memberList);
  usersList.value = [
    ...memberIds.map((id) => ({ id, username: String(id) })),
    ...usersList.value,
  ].filter(
    (user, index, all) =>
      all.findIndex((item) => Number(item.id) === Number(user.id)) === index
  );
  Object.assign(form, response.data, {
    id: null,
    createTime: null,
    translationsId: null,
    translations: null,
    memberList: memberIds,
  });
  title.value = "创建";
  open.value = true;
}

async function confirmDelete() {
  if (!selectedIds.value.length) {
    return;
  }
  await delMessage(selectedIds.value);
  proxy.$modal.msgSuccess("删除成功");
  selectedIds.value = [];
  await getList();
}

async function submitForm() {
  try {
    await messageRef.value?.validate();
  } catch {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      ...form,
      memberList: form.memberList.join(","),
    };
    if (form.id) {
      await updateMessage(payload);
      proxy.$modal.msgSuccess("修改成功");
    } else {
      await addMessage(payload);
      proxy.$modal.msgSuccess("新增成功");
    }
    open.value = false;
    await getList();
  } finally {
    submitting.value = false;
  }
}

async function openTranslations(row) {
  const response = await getMessage(row.id);
  translationRow.value = response.data;
  translationForm.value = {
    ...createEmptyTranslations(),
    ...(response.data.translations || {}),
  };
  translationTitle.value = "修改";
  translationOpen.value = true;
}

async function submitTranslations(translations) {
  if (!translationRow.value) {
    return;
  }
  const translationsId =
    translations.id || translationRow.value.translationsId || undefined;
  await updateMessage({
    id: translationRow.value.id,
    translationsId,
    translations: {
      ...translations,
      id: translationsId,
    },
  });
  proxy.$modal.msgSuccess("修改成功");
  translationOpen.value = false;
  translationRow.value = null;
  await getList();
}

async function copyTitle(value) {
  try {
    await navigator.clipboard.writeText(value || "");
  } catch {
    proxy.$modal.msgError("复制失败");
  }
}

function splitMemberIds(value) {
  if (Array.isArray(value)) {
    return value
      .map(Number)
      .filter((id) => Number.isFinite(id) && id > 0);
  }
  return String(value || "")
    .split(",")
    .map((id) => Number(id.trim()))
    .filter((id) => Number.isFinite(id) && id > 0);
}

let userSearchTimer;
function handleUserSearch(value) {
  clearTimeout(userSearchTimer);
  userSearchTimer = setTimeout(() => loadUsers(value), 300);
}

function ensureUsersLoaded() {
  if (!usersList.value.length) {
    loadUsers();
  }
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function cancel() {
  open.value = false;
  resetForm();
}

onMounted(getList);
</script>

<style scoped>
.copy-title-button {
  width: 24px;
  height: 24px;
  margin-left: 2px;
  padding: 0;
  color: #1677ff;
}

.advanced-query-row {
  margin-top: 16px;
}

:deep(.ant-btn-link) {
  padding-inline: 4px;
}
</style>
