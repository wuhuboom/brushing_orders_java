<template>
  <div class="app-container ant-pro-member-page settings-page">
    <ant-pro-table
      title="设置列表"
      :columns="orderconfigColumns"
      :data-source="orderconfigList"
      :loading="loading"
      row-key="id"
      :pagination="{
        current: queryParams.pageNum,
        pageSize: queryParams.pageSize,
        total,
        showSizeChanger: false,
        showQuickJumper: false,
      }"
      :scroll="{ x: 1050, y: 'calc(100vh - 440px)' }"
      @page-change="handleAntPageChange"
      @refresh="getList"
    >
      <template #search>
        <a-form layout="horizontal" :model="queryParams">
          <a-row :gutter="24" align="middle">
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item label="类型">
                <a-select
                  v-model:value="queryParams.type"
                  :options="typeOptions"
                  placeholder="请选择"
                  allow-clear
                  show-search
                  :filter-option="filterTypeOption"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :sm="12" :lg="8">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model:value="createdDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['请选择', '请选择']"
                  class="full-width"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :lg="8" class="ant-pro-query-actions">
              <a-space>
                <a-button @click="resetQuery">重 置</a-button>
                <a-button type="primary" @click="handleQuery">查 询</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          {{ displayName(record) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatDateTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'updateTime'">
          {{ formatDateTime(record.updateTime) }}
        </template>
        <template v-else-if="column.key === 'operation'">
          <a-space :size="8">
            <a-button
              type="link"
              size="small"
              class="operation-link"
              @click="handleUpdate(record)"
              v-hasPermi="['member:orderconfig:edit']"
            >
              修改
            </a-button>
            <a-button
              type="link"
              size="small"
              class="operation-link"
              :disabled="!isI18nType(record.type)"
              @click="openTranslationDialog(record)"
              v-hasPermi="['member:orderconfig:edit']"
            >
              国际化
            </a-button>
          </a-space>
        </template>
      </template>
    </ant-pro-table>

    <a-drawer
      v-model:open="open"
      :title="title"
      width="85%"
      size="large"
      destroy-on-close
      :mask-closable="drawerReadonly"
      :body-style="{ paddingBottom: drawerReadonly ? '24px' : '72px' }"
      class="settings-form-drawer"
      @close="cancel"
    >
      <a-form
        ref="baseFormRef"
        :model="form"
        :rules="baseRules"
        layout="vertical"
        class="settings-base-form"
      >
        <a-row :gutter="24">
          <a-col :span="24">
            <a-form-item label="类型" name="type">
              <a-select
                v-model:value="form.type"
                :options="typeOptions"
                disabled
                placeholder="类型"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="24">
            <a-form-item label="序号" name="sort">
              <a-input-number
                v-model:value="form.sort"
                :min="0"
                :precision="0"
                :disabled="drawerReadonly"
                placeholder="序号"
                class="full-width"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div :class="{ 'settings-readonly-content': drawerReadonly }">
        <component
          :is="dialogComponent"
          v-if="open && currentType && dialogComponent"
          ref="componentRef"
          :form="form"
          :loading="saving"
          :readonly="drawerReadonly"
          @update:form="handleFormUpdate"
          @submit="persistForm"
          @cancel="cancel"
        />
      </div>

      <template v-if="!drawerReadonly" #footer>
        <div class="drawer-footer">
          <a-space>
            <a-button @click="cancel">取 消</a-button>
            <a-button type="primary" :loading="saving" @click="handleDrawerSubmit">
              确 定
            </a-button>
          </a-space>
        </div>
      </template>
    </a-drawer>

    <translation-dialog
      v-model="translationOpen"
      :title="translationTitle"
      :translations="translationForm"
      :type="currentTranslationRow?.type"
      :setting-id="currentTranslationRow?.id"
      :language-fields="websiteTranslationLanguageFields"
      @submit="submitTranslations"
    />
  </div>
</template>

<script setup name="Orderconfig">
import { getCurrentInstance, reactive, ref, toRefs } from "vue";
import {
  getOrderconfig,
  listOrderconfig,
  updateOrderconfig,
} from "@/api/member/orderconfig";
import TranslationDialog from "@/views/member/components/TranslationDrawer.vue";
import { createEmptyTranslations } from "@/views/member/components/translationLanguages";

const { proxy } = getCurrentInstance();
const websiteTranslationLanguageFields = [
  "enUs",
  "jaJp",
  "arSa",
  "esEs",
  "svSe",
  "itIt",
  "deDe",
  "noNo",
  "ruRu",
  "huHu",
  "plPl",
  "skSk",
  "frFr",
  "csCz",
  "ptBr",
  "hiIn",
  "koKr",
];

const typeDefinitions = [
  ["website", "网站设置"],
  ["trade", "交易设置"],
  ["signin", "签到设置"],
  ["register", "注册协议"],
  ["about", "关于我们"],
  ["certificate", "证书"],
  ["help", "帮助中心"],
  ["terms", "条款"],
  ["event", "事件"],
  ["transaction", "交易说明"],
  ["order", "订单说明"],
  ["usage", "使用说明"],
  ["points", "积分设置"],
  ["task", "任务设置"],
  ["notification", "通知设置"],
  ["backend", "后端安全设置"],
  ["front", "前端安全设置"],
  ["bonus", "彩金规则"],
  ["balance", "余额宝设置"],
  ["telegram", "Telegram机器人设置"],
  ["work", "工作奖金设置"],
  ["privacy", "隐私协议"],
  ["email", "邮箱"],
  ["reward", "等级奖金"],
  ["error", "错误代码"],
  ["backendRateLimit", "后端限流设置"],
  ["frontendRateLimit", "前端限流设置"],
];

const typeOptions = typeDefinitions.map(([value, label]) => ({ value, label }));
const typeNameMap = Object.fromEntries(typeDefinitions);
const i18nTypes = new Set([
  "register",
  "about",
  "certificate",
  "help",
  "terms",
  "event",
  "transaction",
  "order",
  "usage",
  "task",
  "notification",
  "bonus",
  "balance",
  "privacy",
  "email",
  "error",
]);

const componentLoaders = {
  website: () => import("./components/website-config.vue"),
  trade: () => import("./components/trade-config.vue"),
  signin: () => import("./components/sign-in-config.vue"),
  register: () => import("./components/register-protocol-config.vue"),
  about: () => import("./components/about-us-config.vue"),
  certificate: () => import("./components/certificate-config.vue"),
  help: () => import("./components/help-center-config.vue"),
  terms: () => import("./components/terms-config.vue"),
  event: () => import("./components/event-config.vue"),
  transaction: () => import("./components/transaction-description-config.vue"),
  order: () => import("./components/order-description-config.vue"),
  usage: () => import("./components/usage-description-config.vue"),
  points: () => import("./components/points-config.vue"),
  task: () => import("./components/task-config.vue"),
  notification: () => import("./components/notification-config.vue"),
  backend: () => import("./components/backend-security-config.vue"),
  front: () => import("./components/backend-security-config.vue"),
  bonus: () => import("./components/bonus-config.vue"),
  balance: () => import("./components/balance-treasure-config.vue"),
  telegram: () => import("./components/telegram-bot-config.vue"),
  work: () => import("./components/work-bonus-config.vue"),
  privacy: () => import("./components/privacy-protocol-config.vue"),
  email: () => import("./components/email-config.vue"),
  reward: () => import("./components/reward-config.vue"),
  error: () => import("./components/error-code-config.vue"),
  backendRateLimit: () => import("./components/rate-limiter-config.vue"),
  frontendRateLimit: () => import("./components/rate-limiter-config.vue"),
};

const orderconfigColumns = [
  {
    title: "类型",
    dataIndex: "name",
    align: "left",
    fixed: "left",
    width: 500,
    sorter: true,
  },
  {
    title: "序号",
    dataIndex: "sort",
    align: "center",
    width: 100,
    sorter: true,
  },
  {
    title: "创建时间",
    dataIndex: "createTime",
    align: "center",
    width: 170,
    sorter: true,
  },
  {
    title: "最后修改时间",
    dataIndex: "updateTime",
    align: "center",
    width: 170,
    sorter: true,
  },
  {
    title: "操作",
    key: "operation",
    align: "center",
    fixed: "right",
    width: 100,
  },
];

const emptyForm = () => ({
  id: null,
  type: null,
  sort: null,
  name: null,
  content: null,
  translationsId: null,
  translations: null,
  createTime: null,
  updateTime: null,
});

const data = reactive({
  form: emptyForm(),
  queryParams: {
    pageNum: 1,
    pageSize: 100,
    type: null,
  },
});
const { queryParams, form } = toRefs(data);

const orderconfigList = ref([]);
const loading = ref(true);
const saving = ref(false);
const total = ref(0);
const open = ref(false);
const title = ref("");
const drawerReadonly = ref(false);
const currentType = ref("");
const dialogComponent = ref(null);
const componentRef = ref();
const baseFormRef = ref();
const createdDateRange = ref([]);
const translationOpen = ref(false);
const translationTitle = ref("");
const translationForm = ref(createEmptyTranslations());
const currentTranslationRow = ref(null);
const baseRules = {
  type: [{ required: true, message: "类型为必填项", trigger: "change" }],
  sort: [{ required: true, message: "序号为必填项", trigger: "blur" }],
};

function displayName(record) {
  return typeNameMap[record?.type] || record?.name || "-";
}

function isI18nType(type) {
  return i18nTypes.has(type);
}

function filterTypeOption(input, option) {
  return String(option?.label || "")
    .toLowerCase()
    .includes(String(input || "").toLowerCase());
}

function formatDateTime(value) {
  return value ? proxy.parseTime(value) : "-";
}

async function loadComponent(type) {
  const loader = componentLoaders[type];
  if (!loader) {
    dialogComponent.value = null;
    proxy.$modal.msgError(`暂不支持配置类型：${type}`);
    return false;
  }
  const module = await loader();
  dialogComponent.value = module.default;
  return true;
}

async function getList() {
  loading.value = true;
  try {
    const params = {
      ...queryParams.value,
      beginCreateTime: createdDateRange.value?.[0],
      endCreateTime: createdDateRange.value?.[1],
    };
    const response = await listOrderconfig(params);
    orderconfigList.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
}

function reset() {
  Object.assign(form.value, emptyForm());
}

function cancel() {
  open.value = false;
  currentType.value = "";
  dialogComponent.value = null;
  componentRef.value = null;
  drawerReadonly.value = false;
  reset();
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.value.type = null;
  createdDateRange.value = [];
  handleQuery();
}

function handleAntPageChange({ page, pageSize }) {
  queryParams.value.pageNum = page;
  queryParams.value.pageSize = pageSize;
  getList();
}

async function openSettingsDrawer(row, readonly) {
  reset();
  const response = await getOrderconfig(row.id);
  Object.assign(form.value, response.data);
  currentType.value = response.data.type || row.type;
  if (!(await loadComponent(currentType.value))) {
    return;
  }
  drawerReadonly.value = readonly;
  title.value = "修改";
  open.value = true;
}

function handleUpdate(row) {
  openSettingsDrawer(row, false);
}

async function openTranslationDialog(row) {
  if (!isI18nType(row.type)) {
    return;
  }
  const response = await getOrderconfig(row.id);
  currentTranslationRow.value = response.data;
  translationForm.value = {
    ...createEmptyTranslations(),
    ...(response.data.translations || {}),
  };
  translationTitle.value = "修改";
  translationOpen.value = true;
}

async function submitTranslations(translations) {
  const row = currentTranslationRow.value;
  if (!row) {
    return;
  }
  saving.value = true;
  try {
    const translationsId = translations.id || row.translationsId;
    await updateOrderconfig({
      id: row.id,
      translationsId,
      translations: {
        ...translations,
        id: translationsId,
      },
    });
    proxy.$modal.msgSuccess("修改成功");
    translationOpen.value = false;
    currentTranslationRow.value = null;
    await getList();
  } finally {
    saving.value = false;
  }
}

function handleFormUpdate(updatedForm) {
  Object.assign(form.value, updatedForm);
}

async function handleDrawerSubmit() {
  try {
    await baseFormRef.value?.validate?.();
  } catch {
    return;
  }
  if (componentRef.value?.handleSubmit) {
    await componentRef.value.handleSubmit();
  } else {
    await persistForm();
  }
}

async function persistForm() {
  if (saving.value || !form.value.id) {
    return;
  }
  saving.value = true;
  try {
    await updateOrderconfig(form.value);
    proxy.$modal.msgSuccess("修改成功");
    open.value = false;
    currentType.value = "";
    dialogComponent.value = null;
    await getList();
  } finally {
    saving.value = false;
  }
}

getList();
</script>

<style scoped>
.full-width {
  width: 100%;
}

.settings-kind-link {
  color: rgba(0, 0, 0, 0.88);
  cursor: pointer;
}

.settings-kind-link:hover {
  color: #1677ff;
}

.settings-kind-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 8px;
  vertical-align: 2px;
  background: #1677ff;
  border-radius: 50%;
}

.operation-link {
  height: auto;
  padding: 0;
}

.drawer-footer {
  text-align: right;
}

.settings-base-form :deep(.ant-form-item) {
  margin-bottom: 24px;
}

.settings-readonly-content {
  pointer-events: none;
}

.settings-readonly-content :deep(.ant-input),
.settings-readonly-content :deep(.ant-input-number),
.settings-readonly-content :deep(.ant-select-selector),
.settings-readonly-content :deep(.ql-toolbar),
.settings-readonly-content :deep(.ql-container) {
  background: #f5f5f5;
}
</style>
